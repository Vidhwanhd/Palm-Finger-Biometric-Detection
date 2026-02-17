package com.example.palmfinger

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.example.palmfinger.base.BaseActivity
import com.example.palmfinger.ui.navigation.AppNavigation

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // DO NOT call setContent here
    }

    override fun onPermissionGranted() {
        setContent {
            AppNavigation()
        }
    }

    override fun onPermissionDenied() {
        // Optional: Show error UI or close app
        finish()
    }
}
