package com.example.palmfinger.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

abstract class BaseActivity : ComponentActivity() {

    protected abstract fun onPermissionGranted()
    protected open fun onPermissionDenied() {}

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allGranted = permissions.all { it.value }

            if (allGranted) {
                onPermissionGranted()
            } else {
                handlePermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    private fun checkPermissions() {

        val notGranted = requiredPermissions.any {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            onPermissionGranted()
        }
    }

    private fun handlePermissionDenied() {

        val permanentlyDenied = requiredPermissions.any { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED &&
                    !shouldShowRequestPermissionRationale(permission)
        }

        if (permanentlyDenied) {
            Toast.makeText(
                this,
                "Permissions permanently denied. Please enable from Settings.",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "Camera & Storage permissions are required",
                Toast.LENGTH_LONG
            ).show()
        }

        onPermissionDenied()
    }
}
