package com.kseniabl.shelfdetector

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.kseniabl.shelfdetector.DataWorker.getResizedBitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

interface ModelResultListener {
    fun onModelResult(image: Bitmap?, detectionsCount: IntArray, boxesTensor: FloatArray)
    fun onModelError(message: String)
}

class SDetector(
    private val assetManager: AssetManager,
    private val listener: ModelResultListener
) {

    private var interpreter: Interpreter
    private var tensorImage = TensorImage(DataType.FLOAT32)
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(0f, 255f))
        .build()

    private val boxesTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000, 4), DataType.FLOAT32)
    private val detectionsCountTensor = TensorBuffer.createFixedSize(intArrayOf(4), DataType.UINT8)
    private val labelsTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000), DataType.FLOAT32)
    private val scoresTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000), DataType.FLOAT32)
    private val outputBuffers = mutableMapOf<Int, Any>(
        0 to boxesTensor.buffer,
        1 to detectionsCountTensor.buffer,
        2 to labelsTensor.buffer,
        3 to scoresTensor.buffer
    )

    init {
        val options = Interpreter.Options()
        options.useNNAPI = true

        interpreter = Interpreter(getModel(), options)
        interpreter.allocateTensors()
    }

    fun detect(bitmap: Bitmap) {
        outputBuffers.values.forEach { buffer ->
            (buffer as ByteBuffer).rewind()
        }
        val preparedImage = bitmap.getResizedBitmap()

        try {
            tensorImage.load(preparedImage)
            tensorImage = imageProcessor.process(tensorImage)
            interpreter.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputBuffers)

            listener.onModelResult(preparedImage, detectionsCountTensor.intArray, boxesTensor.floatArray)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Memory Issue: ${e.message}")
            listener.onModelError("Sorry, we have some memory issue :(")
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            listener.onModelError("Sorry, we have some problems with the model :(")
        }
    }

    fun close() {
        interpreter.close()
    }

    private fun getModel(): MappedByteBuffer {
        val assetFileDescriptor = assetManager.openFd("sku-base-640-480-fp16.tflite")
        val fileDescriptor = assetFileDescriptor.fileDescriptor
        val startOffset = assetFileDescriptor.startOffset
        val length = assetFileDescriptor.length

        val fileChannel = FileInputStream(fileDescriptor).channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length)
    }

    data class Rectangle(val left: Float, val top: Float, val right: Float, val bottom: Float)

    companion object {
        const val TAG = "SDetector"
    }
}
