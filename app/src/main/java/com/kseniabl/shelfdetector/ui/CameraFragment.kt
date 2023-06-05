package com.kseniabl.shelfdetector.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kseniabl.shelfdetector.DataWorker
import com.kseniabl.shelfdetector.MainViewModel
import com.kseniabl.shelfdetector.R
import com.kseniabl.shelfdetector.SDetector
import com.kseniabl.shelfdetector.UIElements
import com.kseniabl.shelfdetector.UIElements.showAlertDialog
import com.kseniabl.shelfdetector.UIElements.showSnackbar
import com.kseniabl.shelfdetector.databinding.FragmentCameraBinding
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment: Fragment(), UIElements.AlertDialogChoices {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var executor: ExecutorService
    private lateinit var bitmap: Bitmap
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        executor = Executors.newSingleThreadExecutor()

        binding.previewView.post { requestPermission() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.state.collect {
                        when (it) {
                            is MainViewModel.UIActions.ShowSnackbar -> {
                                view.showSnackbar(it.msg)
                            }
                            is MainViewModel.UIActions.DisplayDetectorResult -> {
                                loadBoundingBoxes(it.image, it.list)
                            }
                        }
                    }
                }
            }
        }

    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener( {
            cameraProvider = cameraProviderFuture.get()
            bindPreview()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindPreview() {
        val preview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        binding.previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy(Size(DataWorker.widthConst, DataWorker.heightConst), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.previewView.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            imageProxy.use { bitmap = imageProxy.toBitmap() }
            bitmap = DataWorker.rotateBitmap(bitmap, rotationDegrees)

            viewModel.detect(bitmap)

            imageProxy.close()
        }

        cameraProvider.unbindAll()

        preview.setSurfaceProvider(binding.previewView.surfaceProvider)
        camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis)
    }

    private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.e(TAG, "Camera permission granted 1")
                accessToCamera()
            } else {
                Log.e(TAG, "Camera permission wasn't grant")
                prohibitCamera()
            }
        }

    private fun requestPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                Log.e(TAG, "Camera permission granted 2")
                accessToCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                requireContext().showAlertDialog(
                    this, RATIONALE_DIALOG_TAG, R.string.camera_rationale, R.string.camera_message,
                    R.string.rationale_ask_ok, R.string.rationale_ask_cancel
                )
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // enable zoom by gesture
    private fun enableZoom() {
        val scaleGestureDetector = ScaleGestureDetector(requireContext(), scaleListener)
        binding.previewView.setOnTouchListener { view, motionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
            view.performClick()
            return@setOnTouchListener true
        }
    }

    private val scaleListener = object : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = camera.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
            camera.cameraControl.setZoomRatio(scale)
            return true
        }

    }

    private fun accessToCamera() {
        binding.notCameraAllowText.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        binding.detectionsView.visibility = View.VISIBLE
        setupCamera()
        enableZoom()
    }

    private fun prohibitCamera() {
        binding.notCameraAllowText.visibility = View.VISIBLE
        binding.previewView.visibility = View.GONE
        binding.detectionsView.visibility = View.GONE
    }

    override fun onDialogNegativeButtonClicked(tag: String) {
        if (tag == RATIONALE_DIALOG_TAG)
            prohibitCamera()
    }

    override fun onDialogPositiveButtonClicked(tag: String) {
        if (tag == RATIONALE_DIALOG_TAG)
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun loadBoundingBoxes(image: Bitmap?, list: List<SDetector.Rectangle>) {
        image?.let {
            binding.detectionsView.setBoundingBoxes(list, image)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        executor.shutdown()
    }

    companion object {
        const val TAG = "CameraFragment"
        const val RATIONALE_DIALOG_TAG = "shouldShowRequestPermissionRationale"
    }
}