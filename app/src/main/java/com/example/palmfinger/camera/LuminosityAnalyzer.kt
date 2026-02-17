package com.example.palmfinger.camera

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class LuminosityAnalyzer(
    private val context: Context,
    private val onResult: (LuminosityResult) -> Unit
) {

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val brightnessHistory =
        ArrayDeque<Double>()

    private val maxSamples = 5

    fun getAnalysis(): ImageAnalysis {

        return ImageAnalysis.Builder()
            .setBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            )
            .build()
            .also { analysis ->

                analysis.setAnalyzer(executor) { image ->

                    val brightness =
                        calculateBrightness(image)

                    val smoothed =
                        smoothBrightness(brightness)

                    val type =
                        classifyBrightness(smoothed)

                    val result =
                        LuminosityResult(
                            brightnessScore = smoothed,
                            lightType = type,
                            deviceId = getDeviceId(),
                            timestamp = System.currentTimeMillis()
                        )

                    onResult(result)

                    image.close()
                }
            }
    }

    private fun calculateBrightness(
        image: ImageProxy
    ): Double {

        val buffer: ByteBuffer =
            image.planes[0].buffer

        val data =
            ByteArray(buffer.remaining())

        buffer.get(data)

        var sum = 0.0

        for (byte in data) {
            sum += byte.toInt() and 0xFF
        }

        return sum / data.size
    }

    private fun smoothBrightness(
        value: Double
    ): Double {

        if (brightnessHistory.size >= maxSamples) {
            brightnessHistory.removeFirst()
        }

        brightnessHistory.addLast(value)

        return brightnessHistory.average()
            .roundToInt()
            .toDouble()
    }

    private fun classifyBrightness(
        value: Double
    ): String {

        return when {
            value < 60 -> "Low"
            value > 200 -> "Bright"
            else -> "Normal"
        }
    }

    private fun getDeviceId(): String {

        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
    }

    fun shutdown() {
        executor.shutdown()
    }
}

data class LuminosityResult(
    val brightnessScore: Double,
    val lightType: String,
    val deviceId: String,
    val timestamp: Long
)
