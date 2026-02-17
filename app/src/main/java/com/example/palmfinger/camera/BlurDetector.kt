package com.example.palmfinger.camera

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.pow

object BlurDetector {

    private const val BLUR_THRESHOLD = 150.0

    fun isBlurred(bitmap: Bitmap): Boolean {
        val score = calculateBlurScore(bitmap)
        return score < BLUR_THRESHOLD
    }

    fun calculateBlurScore(bitmap: Bitmap): Double {

        val scaled =
            Bitmap.createScaledBitmap(bitmap, 300, 300, true)

        val width = scaled.width
        val height = scaled.height

        val gray = Array(height) { DoubleArray(width) }

        // Convert to grayscale
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = scaled.getPixel(x, y)
                gray[y][x] =
                    (Color.red(pixel) +
                            Color.green(pixel) +
                            Color.blue(pixel)) / 3.0
            }
        }

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        // Apply Laplacian Kernel
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {

                val laplacian =
                    gray[y - 1][x] +
                            gray[y + 1][x] +
                            gray[y][x - 1] +
                            gray[y][x + 1] -
                            (4 * gray[y][x])

                sum += laplacian
                sumSq += laplacian.pow(2)
                count++
            }
        }

        val mean = sum / count
        val variance = (sumSq / count) - mean.pow(2)

        return variance
    }
}
