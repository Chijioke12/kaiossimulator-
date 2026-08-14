package com.example.ui.simulator

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

fun extractAppZip(zipFile: File, destDir: File): File? {
    var startPage: File? = null
    ZipInputStream(zipFile.inputStream()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val file = File(destDir, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { output ->
                    zis.copyTo(output)
                }
                if (entry.name == "index.html" || entry.name.endsWith("/index.html")) {
                    startPage = file
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    
    if (startPage == null) {
        val allFiles = destDir.walkTopDown().toList()
        startPage = allFiles.firstOrNull { it.name == "index.html" } 
            ?: allFiles.firstOrNull { it.name.endsWith(".html") }
    }
    
    return startPage
}
