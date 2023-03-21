package com.simon.facecapture.camera.utils.camera

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.camera.core.*
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.lang.Integer.min

@ExperimentalGetImage abstract class BaseImageAnalyzer<T> : ImageAnalysis.Analyzer {

    abstract val graphicOverlay: GraphicOverlay


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {


        val mediaImage = imageProxy.image

        mediaImage?.let {
            detectInImage(InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { results ->

                    onSuccess(
                        results,
                        graphicOverlay,
                        it.cropRect
                    )
                    imageProxy.close()
                }
                .addOnFailureListener {
                    onFailure(it)
                    imageProxy.close()
                }
        }
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    abstract fun stop()

    protected abstract fun onSuccess(
        results: T,
        graphicOverlay: GraphicOverlay,
        rect: Rect
    )

    protected abstract fun onFailure(e: Exception)

}

@SuppressLint("RestrictedApi")
fun MeteringPoint.calculateRelativeCoordinate(cropRect: Rect): RectF {
    val x = x - cropRect.left
    val y = y - cropRect.top
    val width = cropRect.width()
    val height = cropRect.height()

    val newX = x / width
    val newY = y / height

    return RectF(newX, newY, newX, newY)
}
