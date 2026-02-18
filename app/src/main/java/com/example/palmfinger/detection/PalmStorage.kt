package com.example.palmfinger.detection

object PalmStorage {

    var storedHandSide: String? = null
    var storedEmbedding: FloatArray? = null

    fun clear() {
        storedHandSide = null
        storedEmbedding = null
    }
}
