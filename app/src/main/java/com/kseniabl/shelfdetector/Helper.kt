package com.kseniabl.shelfdetector

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.View
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.io.InputStream

object UIElements {

    interface AlertDialogChoices {
        fun onDialogPositiveButtonClicked(tag: String)
        fun onDialogNegativeButtonClicked(tag: String)
    }

    fun Context.showAlertDialog(listener: AlertDialogChoices, tag: String, title: Int, message: Int, posButton: Int, negButton: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(posButton) { dialog, _ ->
            listener.onDialogPositiveButtonClicked(tag)
            dialog.dismiss()
        }
        builder.setNegativeButton(negButton) { dialog, _ ->
            listener.onDialogNegativeButtonClicked(tag)
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun View.showSnackbar(message: String) {
        Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
    }
}

object DataWorker {

    fun Bitmap.getResizedBitmap(): Bitmap? {
        return try {
            val originalBitmap = this

            val targetAspectRatio = 3f / 4f

            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height
            val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

            if (aspectRatio != targetAspectRatio) {
                val newWidth: Int
                val newHeight: Int

                if (aspectRatio > targetAspectRatio) {
                    newWidth = (originalHeight.toFloat() * targetAspectRatio).toInt()
                    newHeight = originalHeight
                } else {
                    newWidth = originalWidth
                    newHeight = (originalWidth.toFloat() / targetAspectRatio).toInt()
                }

                val left = (originalWidth - newWidth) / 2
                val top = (originalHeight - newHeight) / 2

                val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, newWidth, newHeight)
                val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, newWidth, newHeight, true)
                val compressedBitmap = Bitmap.createScaledBitmap(scaledBitmap, widthConst, heightConst, true)

                compressedBitmap
            } else {
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, widthConst, heightConst, true)
                scaledBitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun Uri.getBitmapFromUri(contentResolver: ContentResolver): Bitmap? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(this)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // When vertical images was loaded, they've been inverted, so here they rotate
            val orientation = getOrientation(contentResolver)
            val rotatedBitmap = rotateBitmap(bitmap, orientation)

            rotatedBitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun Uri.getOrientation(contentResolver: ContentResolver): Int {
        val cursor = contentResolver.query(this, arrayOf("orientation"), null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }

        return 0
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            90 -> matrix.postRotate(90f)
            180 -> matrix.postRotate(180f)
            270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    const val widthConst = 480
    const val heightConst = 640

}