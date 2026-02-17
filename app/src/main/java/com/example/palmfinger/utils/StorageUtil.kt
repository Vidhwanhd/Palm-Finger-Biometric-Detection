package com.example.palmfinger.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

object StorageUtil {

    private const val FOLDER_NAME = "Finger Data"

    fun saveImageToPublicFolder(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ) {

        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/$FOLDER_NAME"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                95,
                stream
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(
                MediaStore.Images.Media.IS_PENDING,
                0
            )
            resolver.update(uri, contentValues, null, null)
        }
    }
}
