package com.example.palmfinger.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object StorageUtil {

    private const val FOLDER_NAME = "Finger Data"

    fun saveImageToPublicFolder(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Boolean {

        return try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                // Scoped Storage (Android 10+)

                val resolver = context.contentResolver

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/$FOLDER_NAME"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    val stream: OutputStream? =
                        resolver.openOutputStream(it)

                    stream?.use {
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            it
                        )
                    }

                    contentValues.clear()
                    contentValues.put(
                        MediaStore.MediaColumns.IS_PENDING,
                        0
                    )

                    resolver.update(uri, contentValues, null, null)
                }

            } else {

                // Legacy (Android 9 and below)

                val directory = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ),
                    FOLDER_NAME
                )

                if (!directory.exists())
                    directory.mkdirs()

                val file = File(directory, fileName)
                val stream = FileOutputStream(file)

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    100,
                    stream
                )

                stream.flush()
                stream.close()
            }

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
