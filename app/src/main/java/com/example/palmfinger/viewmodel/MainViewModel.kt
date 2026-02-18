package com.example.palmfinger.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) :
    AndroidViewModel(application) {

    companion object {
        private const val MAX_FINGERS = 5
    }

    // ---------------- STATE ----------------

    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _lightType = MutableStateFlow("Unknown")
    val lightType: StateFlow<String> = _lightType.asStateFlow()

    private val _blurScore = MutableStateFlow(0f)
    val blurScore: StateFlow<Float> = _blurScore.asStateFlow()

    private val _handSide = MutableStateFlow("Unknown")
    val handSide: StateFlow<String> = _handSide.asStateFlow()

    private val _fingerCount = MutableStateFlow(0)
    val fingerCount: StateFlow<Int> = _fingerCount.asStateFlow()

    private val _deviceId = MutableStateFlow(
        Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
    )
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    // ---------------- UPDATE FUNCTIONS ----------------

    fun updateBrightness(value: Float, type: String) {
        _brightness.value = value
        _lightType.value = type
    }

    fun updateBlurScore(value: Float) {
        _blurScore.value = value
    }

    fun updateHandSide(value: String) {
        _handSide.value = value
    }

    fun incrementFingerCount() {
        if (_fingerCount.value < MAX_FINGERS) {
            _fingerCount.value += 1
        }
    }

    fun setFingerCount(count: Int) {
        _fingerCount.value = count.coerceIn(0, MAX_FINGERS)
    }

    // 🔥 FULL RESET (Important)
    fun resetSession() {
        _brightness.value = 0f
        _lightType.value = "Unknown"
        _blurScore.value = 0f
        _handSide.value = "Unknown"
        _fingerCount.value = 0
    }
}
