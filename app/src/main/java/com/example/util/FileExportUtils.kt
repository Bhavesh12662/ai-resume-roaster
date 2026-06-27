package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileExportUtils {

    /**
     * Saves PDF bytes to the device's Downloads folder using MediaStore.
     * Works on API 29+ without requiring runtime storage permissions.
     */
    fun savePdfToDownloads(context: Context, bytes: ByteArray, fileName: String): Uri? {
        val resolvedName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, resolvedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val outputStream: OutputStream? = resolver.openOutputStream(uri)
                    if (outputStream != null) {
                        outputStream.write(bytes)
                        outputStream.close()
                        Toast.makeText(context, "Saved to Downloads: $resolvedName", Toast.LENGTH_LONG).show()
                        uri
                    } else {
                        Toast.makeText(context, "Failed to write PDF file.", Toast.LENGTH_SHORT).show()
                        null
                    }
                } else {
                    Toast.makeText(context, "Failed to create download entry.", Toast.LENGTH_SHORT).show()
                    null
                }
            } else {
                // Fallback for older Android versions
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, resolvedName)
                val fos = FileOutputStream(file)
                fos.write(bytes)
                fos.close()
                Toast.makeText(context, "Saved to Downloads: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Instantly opens a share sheet to let users send/export the PDF via email, Drive, messaging apps, etc.
     */
    fun sharePdf(context: Context, bytes: ByteArray, fileName: String) {
        val resolvedName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        try {
            // Write to local cache
            val cacheFile = File(context.cacheDir, resolvedName)
            val fos = FileOutputStream(cacheFile)
            fos.write(bytes)
            fos.close()

            // Get FileProvider Uri
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            // Setup share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AI Resume Analysis Report - $resolvedName")
                putExtra(Intent.EXTRA_TEXT, "Here is the beautifully generated AI analysis, scores, and actionable feedback for my resume!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share or Save Resume Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
