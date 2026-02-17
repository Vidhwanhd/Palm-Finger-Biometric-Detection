package com.example.palmfinger.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _brightness = MutableStateFlow(0.0)
    val brightness: StateFlow<Double> = _brightness

    private val _lightType = MutableStateFlow("Unknown")
    val lightType: StateFlow<String> = _lightType

    private val _blurScore = MutableStateFlow(0.0)
    val blurScore: StateFlow<Double> = _blurScore

    private val _handSide = MutableStateFlow("Unknown")
    val handSide: StateFlow<String> = _handSide

    private val _fingerCount = MutableStateFlow(0)
    val fingerCount: StateFlow<Int> = _fingerCount

    private val _deviceId = MutableStateFlow(
        Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
    )
    val deviceId: StateFlow<String> = _deviceId

    // ================= UPDATE FUNCTIONS =================

    fun updateBrightness(value: Double, type: String) {
        _brightness.value = value
        _lightType.value = type
    }

    fun updateBlurScore(value: Double) {
        _blurScore.value = value
    }

    fun updateHandSide(value: String) {
        _handSide.value = value
    }

    fun incrementFingerCount() {
        _fingerCount.value++
    }

    fun resetFingerCount() {
        _fingerCount.value = 0
    }
}
