package com.example.palmfinger.detection

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt
import kotlin.math.pow

class MinutiaeExtractor {

    /**
     * Extract embedding vector from MediaPipe landmarks
     * Returns a normalized feature vector for matching
     */
    fun extractEmbedding(result: HandLandmarkerResult): FloatArray {

        if (result.landmarks().isEmpty()) {
            return FloatArray(0)
        }

        val landmarks = result.landmarks()[0]

        val embedding = mutableListOf<Float>()

        // Reference point: wrist (landmark 0)
        val wrist = landmarks[0]

        // Compute distances from wrist to all key landmarks
        for (i in 1 until landmarks.size) {

            val lm = landmarks[i]

            val dx = lm.x() - wrist.x()
            val dy = lm.y() - wrist.y()
            val dz = lm.z() - wrist.z()

            val distance = sqrt(dx * dx + dy * dy + dz * dz)

            embedding.add(distance)
        }

        // Add inter-finger distances (important for finger identity)
        val fingertipIndices = listOf(4, 8, 12, 16, 20)

        for (i in fingertipIndices.indices) {
            for (j in i + 1 until fingertipIndices.size) {

                val f1 = landmarks[fingertipIndices[i]]
                val f2 = landmarks[fingertipIndices[j]]

                val dx = f1.x() - f2.x()
                val dy = f1.y() - f2.y()
                val dz = f1.z() - f2.z()

                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                embedding.add(dist)
            }
        }

        return normalize(embedding.toFloatArray())
    }

    /**
     * Normalize vector (important for stable comparison)
     */
    private fun normalize(vector: FloatArray): FloatArray {

        var norm = 0.0

        for (v in vector) {
            norm += v.pow(2)
        }

        norm = sqrt(norm)

        if (norm == 0.0) return vector

        return vector.map { (it / norm).toFloat() }.toFloatArray()
    }
}
