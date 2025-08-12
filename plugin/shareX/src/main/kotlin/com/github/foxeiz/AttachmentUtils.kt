package com.github.foxeiz

import android.content.Context
import android.provider.OpenableColumns
import com.lytefast.flexinput.model.Attachment
import java.io.File
import java.io.IOException

object AttachmentUtils {

    fun Attachment<*>.toFile(context: Context): File? {
        return try {
            val resolver = context.contentResolver
            val uri = this.uri
            val name = this.displayName ?: queryDisplayName(resolver, uri) ?: "unnamed"
            val tempFile = File.createTempFile(
                "temp_", name, context.cacheDir
            )

            resolver.openInputStream(uri).use { input ->
                tempFile.outputStream().use { output ->
                    input?.copyTo(output)
                        ?: throw IOException("Unable to open InputStream for $uri")
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Attachment<*>.getFileSize(context: Context): Long {
        return try {
            val resolver = context.contentResolver
            val uri = this.uri
            queryFileSize(resolver, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    private fun queryDisplayName(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri
    ): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    private fun queryFileSize(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri
    ): Long {
        val projection = arrayOf(OpenableColumns.SIZE)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex != -1) {
                return cursor.getLong(sizeIndex)
            }
        }
        return -1L
    }
}