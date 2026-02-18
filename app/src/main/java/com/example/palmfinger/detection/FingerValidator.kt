package com.example.palmfinger.detection

import kotlin.math.sqrt

object FingerValidator {

    private const val MATCH_THRESHOLD = 0.85f

    private fun normalize(vector: FloatArray): FloatArray {

        var magnitude = 0f
        for (v in vector) {
            magnitude += v * v
        }

        magnitude = sqrt(magnitude)

        if (magnitude == 0f) return vector

        return vector.map { it / magnitude }.toFloatArray()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {

        if (a.size != b.size || a.isEmpty())
            return 0f

        val na = normalize(a)
        val nb = normalize(b)

        var dot = 0f
        for (i in na.indices) {
            dot += na[i] * nb[i]
        }

        return dot
    }

    fun validateHand(
        storedHand: String?,
        detectedHand: String,
        storedEmbedding: FloatArray?,
        newEmbedding: FloatArray?
    ): ValidationResult {

        if (storedHand == null || storedEmbedding == null || newEmbedding == null) {
            return ValidationResult(false, "Palm data missing", 0f)
        }

        if (!storedHand.equals(detectedHand, true)) {
            return ValidationResult(false, "Incorrect Hand", 0f)
        }

        val similarity = cosineSimilarity(storedEmbedding, newEmbedding)

        if (similarity < MATCH_THRESHOLD) {
            return ValidationResult(false, "Finger does not match", similarity)
        }

        return ValidationResult(true, "Finger matched successfully", similarity)
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String,
    val confidence: Float
)
