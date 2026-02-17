package com.example.palmfinger.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as Cam2Manager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val analyzer: LuminosityAnalyzer
) {

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

    fun startCamera() {

        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )
                .setTargetRotation(previewView.display.rotation)
                .build()

            val imageAnalysis =
                analyzer.getAnalysis()

            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(context))
    }

    // ================= AUTO FOCUS =================

    fun triggerAutoFocus() {

        val meteringPoint =
            previewView.meteringPointFactory
                .createPoint(
                    previewView.width / 2f,
                    previewView.height / 2f
                )

        val action =
            FocusMeteringAction.Builder(
                meteringPoint,
                FocusMeteringAction.FLAG_AF
            ).build()

        camera?.cameraControl?.startFocusAndMetering(action)
    }

    // ================= EXPOSURE CONTROL =================

    fun adjustExposure(brightnessScore: Double) {

        val exposureState =
            camera?.cameraInfo?.exposureState ?: return

        val range =
            exposureState.exposureCompensationRange

        val newExposure =
            when {
                brightnessScore < 60 ->
                    range.upper // brighten

                brightnessScore > 200 ->
                    range.lower // darken

                else -> 0
            }

        camera?.cameraControl
            ?.setExposureCompensationIndex(newExposure)
    }

    // ================= CAPTURE BITMAP =================

    fun captureBitmap(
        onBitmapReady: (Bitmap) -> Unit
    ) {

        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(
                    image: ImageProxy
                ) {

                    val bitmap =
                        imageProxyToBitmap(image)

                    image.close()

                    onBitmapReady(bitmap)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    exception.printStackTrace()
                }
            }
        )
    }

    // ================= IMAGE CONVERSION =================

    private fun imageProxyToBitmap(
        image: ImageProxy
    ): Bitmap {

        val buffer: ByteBuffer =
            image.planes[0].buffer

        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size
        )
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private val executor = Executors.newSingleThreadExecutor()


    fun shutdown() {
        try {
            cameraProvider?.unbindAll()
            executor.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ================= CAMERA DETAILS =================

    fun getCameraDetails(): CameraDetails {

        val manager =
            context.getSystemService(
                Context.CAMERA_SERVICE
            ) as Cam2Manager

        val cameraId =
            manager.cameraIdList.first()

        val characteristics =
            manager.getCameraCharacteristics(cameraId)

        val focalLength =
            characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )?.firstOrNull() ?: 0f

        val aperture =
            characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES
            )?.firstOrNull() ?: 0f

        val minFocusDistance =
            characteristics.get(
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
            ) ?: 0f

        return CameraDetails(
            focalLength = focalLength,
            aperture = aperture,
            focusDistance = minFocusDistance
        )
    }
}

data class CameraDetails(
    val focalLength: Float,
    val aperture: Float,
    val focusDistance: Float
)
