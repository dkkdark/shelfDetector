package com.kseniabl.shelfdetector

import android.content.res.AssetManager
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val assets: AssetManager
): ViewModel(), ModelResultListener {

    private val _state = MutableSharedFlow<UIActions>()
    val state = _state.asSharedFlow()

    private var sDetector: SDetector

    init {
        sDetector = SDetector(assetManager = assets, this)
    }

    override fun onModelError(message: String) {
        viewModelScope.launch {
            _state.emit(UIActions.ShowSnackbar(message))
        }
    }

    override fun onModelResult(image: Bitmap?, detectionsCount: IntArray, boxesTensor: FloatArray) {
        val rects = getRects(detectionsCount, boxesTensor)
        viewModelScope.launch {
            _state.emit(UIActions.DisplayDetectorResult(image, rects))
        }
    }

    fun detect(bitmap: Bitmap) {
        sDetector.detect(bitmap)
    }

    fun close() {
        sDetector.close()
    }

    private fun getRects(detectionsCountArray: IntArray, boxesTensor: FloatArray): List<SDetector.Rectangle> {
        var detectionsCount = 0
        detectionsCountArray.forEach { count ->
            detectionsCount += count
        }
        val detections = ArrayList<SDetector.Rectangle>(detectionsCount)

        for (k in 0 until detectionsCount) {
            val det = SDetector.Rectangle(
                boxesTensor[k * 4 + 0],
                boxesTensor[k * 4 + 1],
                boxesTensor[k * 4 + 2],
                boxesTensor[k * 4 + 3],
            )
            detections.add(det)
        }
        return detections
    }

    sealed class UIActions {
        class ShowSnackbar(val msg: String): UIActions()
        class DisplayDetectorResult(val image: Bitmap?, val list: List<SDetector.Rectangle>): UIActions()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY])

                return MainViewModel(
                    application.assets
                ) as T
            }
        }
    }

}