package com.example.palmfinger.detection

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt

class HandDetector(
    context: Context
) {

    private val detector: HandLandmarker

    // ================= STORED PALM TEMPLATE =================

    private var storedHandSide: String? = null

    private var storedThumb: DoubleArray? = null
    private var storedIndex: DoubleArray? = null
    private var storedMiddle: DoubleArray? = null
    private var storedRing: DoubleArray? = null
    private var storedLittle: DoubleArray? = null

    init {

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options =
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(1)
                .build()

        detector =
            HandLandmarker.createFromOptions(context, options)
    }

    // ================= DETECT =================

    fun detect(bitmap: Bitmap): HandLandmarkerResult? {
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            detector.detect(mpImage)
        } catch (e: Exception) {
            null
        }
    }

    // ================= HAND SIDE =================

    fun getHandSide(result: HandLandmarkerResult): String {

        val rawSide = result.handednesses()
            .firstOrNull()
            ?.firstOrNull()
            ?.categoryName()
            ?: return "Unknown"

        // Reverse because using back camera
        return if (rawSide == "Left") "Right" else "Left"
    }

    fun getStoredHandSide(): String? {
        return storedHandSide
    }

    // ================= PALM DORSAL CHECK =================

    fun isPalmDorsal(result: HandLandmarkerResult): Boolean {

        val lm = result.landmarks().firstOrNull() ?: return false

        val wrist = lm[0]
        val indexBase = lm[5]
        val pinkyBase = lm[17]

        val v1x = indexBase.x() - wrist.x()
        val v1y = indexBase.y() - wrist.y()

        val v2x = pinkyBase.x() - wrist.x()
        val v2y = pinkyBase.y() - wrist.y()

        val normalZ = (v1x * v2y) - (v1y * v2x)

        // Adjust sign if needed depending on camera
        return normalZ < 0
    }



    // ================= FINGER DORSAL CHECK =================

    fun isFingerDorsal(result: HandLandmarkerResult): Boolean {

        val lm = result.landmarks().firstOrNull() ?: return false

        val tipZ = lm[8].z()      // Index tip
        val mcpZ = lm[5].z()      // Index base

        val tipY = lm[8].y()
        val mcpY = lm[5].y()

        val depthCheck = tipZ > mcpZ + 0.01f
        val directionCheck = tipY > mcpY   // finger pointing down

        return depthCheck && directionCheck
    }


    // ================= STORE PALM TEMPLATE =================

    fun storePalmTemplate(result: HandLandmarkerResult) {

        storedHandSide = getHandSide(result)

        storedThumb = extractFingerFeature(result, 1, 4)
        storedIndex = extractFingerFeature(result, 5, 8)
        storedMiddle = extractFingerFeature(result, 9, 12)
        storedRing = extractFingerFeature(result, 13, 16)
        storedLittle = extractFingerFeature(result, 17, 20)
    }

    // ================= VALIDATE FINGER =================

    fun validateFinger(
        result: HandLandmarkerResult,
        fingerName: String
    ): Boolean {

        if (result.landmarks().isEmpty())
            return false

        val currentFeature = when (fingerName) {
            "Thumb" -> extractFingerFeature(result, 1, 4)
            "Index" -> extractFingerFeature(result, 5, 8)
            "Middle" -> extractFingerFeature(result, 9, 12)
            "Ring" -> extractFingerFeature(result, 13, 16)
            "Little" -> extractFingerFeature(result, 17, 20)
            else -> return false
        }

        if (currentFeature.isEmpty())
            return false

        val storedFeature = when (fingerName) {
            "Thumb" -> storedThumb
            "Index" -> storedIndex
            "Middle" -> storedMiddle
            "Ring" -> storedRing
            "Little" -> storedLittle
            else -> null
        } ?: return false

        val similarity =
            calculateSimilarity(storedFeature, currentFeature)

        return similarity > 0.85
    }


    // ================= EXTRACT NORMALIZED FINGER FEATURE =================

    private fun extractFingerFeature(
        result: HandLandmarkerResult,
        start: Int,
        end: Int
    ): DoubleArray {

        val landmarksList = result.landmarks()

        if (landmarksList.isEmpty())
            return DoubleArray(0)

        val lm = landmarksList.first()

        if (lm.size < 21)
            return DoubleArray(0)

        val wrist = lm[0]

        val feature = DoubleArray((end - start + 1) * 3)
        var index = 0

        for (i in start..end) {

            val x = lm[i].x() - wrist.x()
            val y = lm[i].y() - wrist.y()
            val z = lm[i].z()

            feature[index++] = x.toDouble()
            feature[index++] = y.toDouble()
            feature[index++] = z.toDouble()
        }

        return feature
    }

    fun detectExtendedFinger(result: HandLandmarkerResult): String? {

        val lm = result.landmarks().firstOrNull() ?: return null

        val fingers = mapOf(
            "Thumb" to Pair(4, 2),
            "Index" to Pair(8, 6),
            "Middle" to Pair(12, 10),
            "Ring" to Pair(16, 14),
            "Little" to Pair(20, 18)
        )

        for ((name, pair) in fingers) {

            val tip = lm[pair.first]
            val pip = lm[pair.second]

            if (tip.y() < pip.y()) {
                return name
            }
        }

        return null
    }


    // ================= COSINE SIMILARITY =================

    private fun calculateSimilarity(
        f1: DoubleArray,
        f2: DoubleArray
    ): Double {

        var dot = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in f1.indices) {
            dot += f1[i] * f2[i]
            norm1 += f1[i] * f1[i]
            norm2 += f2[i] * f2[i]
        }

        return dot / (sqrt(norm1) * sqrt(norm2))
    }

    fun close() {
        detector.close()
    }
}
