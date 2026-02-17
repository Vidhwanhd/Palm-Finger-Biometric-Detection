package com.example.palmfinger.detection

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.abs
import kotlin.math.sqrt

class HandDetector(
    context: Context
) {

    private val detector: HandLandmarker

    // Palm feature template storage
    private var storedPalmFeature: DoubleArray? = null
    private var storedHandSide: String? = null

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

    fun detect(bitmap: Bitmap): HandLandmarkerResult? {
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            detector.detect(mpImage)
        } catch (e: Exception) {
            null
        }
    }

    // ================= HAND SIDE USING MEDIAPIPE =================

    fun getHandSide(result: HandLandmarkerResult): String {

        val rawSide = result.handednesses()
            .firstOrNull()
            ?.firstOrNull()
            ?.categoryName()
            ?: return "Unknown"

        // 🔥 FIX: Reverse because using BACK camera
        return if (rawSide == "Left") "Right" else "Left"
    }


    // ================= FINGER COUNT =================

    fun countFingers(result: HandLandmarkerResult): Int {

        val lm = result.landmarks().firstOrNull() ?: return 0
        var count = 0

        if (lm[4].x() < lm[3].x()) count++        // Thumb
        if (lm[8].y() < lm[6].y()) count++        // Index
        if (lm[12].y() < lm[10].y()) count++      // Middle
        if (lm[16].y() < lm[14].y()) count++      // Ring
        if (lm[20].y() < lm[18].y()) count++      // Little

        return count
    }

    // ================= FINGER IDENTIFICATION =================

    fun identifyFinger(result: HandLandmarkerResult): String {

        val lm = result.landmarks().firstOrNull() ?: return "Unknown"

        return when {
            lm[4].y() < lm[3].y() -> "Thumb"
            lm[8].y() < lm[6].y() -> "Index"
            lm[12].y() < lm[10].y() -> "Middle"
            lm[16].y() < lm[14].y() -> "Ring"
            lm[20].y() < lm[18].y() -> "Little"
            else -> "Unknown"
        }
    }

    // ================= PALM DORSAL CHECK =================

    fun isPalmDorsal(result: HandLandmarkerResult): Boolean {

        val lm = result.landmarks().firstOrNull() ?: return false

        // Compare finger base vs wrist depth
        val wristZ = lm[0].z()
        val indexBaseZ = lm[5].z()
        val pinkyBaseZ = lm[17].z()

        val avgBaseZ = (indexBaseZ + pinkyBaseZ) / 2f

        // If base is farther than wrist → dorsal likely shown
        return avgBaseZ > wristZ
    }


    // ================= FINGER DORSAL CHECK =================

    fun isFingerDorsal(result: HandLandmarkerResult): Boolean {

        val lm = result.landmarks().firstOrNull() ?: return false

        val tipZ = lm[8].z()
        val pipZ = lm[6].z()

        return tipZ > pipZ
    }

    // ================= PALM FEATURE EXTRACTION =================

    fun extractPalmFeature(result: HandLandmarkerResult): DoubleArray {

        val lm = result.landmarks().first()

        val feature = DoubleArray(lm.size * 3)
        var index = 0

        for (point in lm) {
            feature[index++] = point.x().toDouble()
            feature[index++] = point.y().toDouble()
            feature[index++] = point.z().toDouble()
        }

        return feature
    }

    // ================= STORE PALM TEMPLATE =================

    fun storePalmTemplate(
        result: HandLandmarkerResult
    ) {

        storedPalmFeature =
            extractPalmFeature(result)

        storedHandSide =
            getHandSide(result)
    }

    // ================= VALIDATE FINGER AGAINST PALM =================

    fun validateWithPalmRecord(
        result: HandLandmarkerResult
    ): Boolean {

        val currentFeature =
            extractPalmFeature(result)

        val storedFeature =
            storedPalmFeature ?: return false

        val similarity =
            calculateSimilarity(
                storedFeature,
                currentFeature
            )

        return similarity > 0.90
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
