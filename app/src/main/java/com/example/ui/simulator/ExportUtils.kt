package com.example.ui.simulator

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Utility functions for exporting and downloading applications to public storage.
 */
object ExportUtils {

    /**
     * Saves an input stream to the public Downloads folder using MediaStore on API 29+
     * or standard file access on older APIs.
     */
    fun saveFileToPublicDownloads(context: Context, fileName: String, fileInputStream: InputStream): Boolean {
        val contentResolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        if (outputStream != null) {
                            fileInputStream.copyTo(outputStream)
                            return true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            // Fallback for pre-Q using traditional storage path
            try {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = File(downloadsDir, fileName)
                targetFile.outputStream().use { outputStream ->
                    fileInputStream.copyTo(outputStream)
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * Recursively packs a directory into a ZIP file.
     */
    fun zipDirectory(sourceDir: File, outZipFile: File) {
        ZipOutputStream(outZipFile.outputStream().buffered()).use { zos ->
            zipDirContents(sourceDir, sourceDir, zos)
        }
    }

    private fun zipDirContents(rootDir: File, currentDir: File, zos: ZipOutputStream) {
        val files = currentDir.listFiles() ?: return
        for (file in files) {
            if (file.name == "downloaded.zip") continue // Avoid double-zipping the original installer zip if present
            if (file.isDirectory) {
                zipDirContents(rootDir, file, zos)
            } else {
                val relativePath = file.absolutePath.substring(rootDir.absolutePath.length + 1)
                val entryName = relativePath.replace('\\', '/')
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                file.inputStream().use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
