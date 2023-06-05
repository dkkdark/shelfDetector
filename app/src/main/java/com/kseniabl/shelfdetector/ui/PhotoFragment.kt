package com.kseniabl.shelfdetector.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.kseniabl.shelfdetector.DataWorker.getBitmapFromUri
import com.kseniabl.shelfdetector.MainViewModel
import com.kseniabl.shelfdetector.SDetector
import com.kseniabl.shelfdetector.UIElements.showSnackbar
import com.kseniabl.shelfdetector.databinding.FragmentPhotoBinding
import kotlinx.coroutines.launch

class PhotoFragment: Fragment() {

    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.uploadImage.setOnClickListener {
            createIntent()
        }
        binding.closeButton.setOnClickListener {
            closeImage()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.state.collect {
                        when (it) {
                            is MainViewModel.UIActions.ShowSnackbar -> {
                                view.showSnackbar(it.msg)
                            }
                            is MainViewModel.UIActions.DisplayDetectorResult -> {
                                loadImageAndBoundingBoxes(it.image, it.list)
                            }
                        }
                    }
                }
            }
        }

    }

    private fun createIntent() {
        val pickImg = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        resultListener.launch(pickImg)
    }

    private val resultListener = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                val uri = data?.data
                val bitmap = uri?.getBitmapFromUri(requireActivity().contentResolver)
                bitmap?.let {
                    viewModel.detect(it)
                }

            }
        }

    private fun loadImageAndBoundingBoxes(image: Bitmap?, list: List<SDetector.Rectangle>) {
        openImage()
        Glide.with(binding.galleryImage.context)
            .asBitmap().load(image)
            .into(binding.galleryImage)
        image?.let {
            binding.detectionsView.setBoundingBoxes(list, it)
        }
    }

    private fun closeImage() {
        binding.uploadImage.visibility = View.VISIBLE
        binding.galleryImage.visibility = View.GONE
        binding.closeButton.visibility = View.GONE
        binding.detectionsView.visibility = View.GONE
    }

    private fun openImage() {
        binding.uploadImage.visibility = View.GONE
        binding.galleryImage.visibility = View.VISIBLE
        binding.closeButton.visibility = View.VISIBLE
        binding.detectionsView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}