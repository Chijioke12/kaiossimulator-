package com.example.ui.simulator

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class LocalServer(private val rootDir: File, port: Int = 8080) : NanoHTTPD(port) {
    
    init {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    }

    override fun serve(session: IHTTPSession): Response {
        var uri = session.uri
        if (uri.startsWith("/")) {
            uri = uri.substring(1)
        }
        
        var requestFile = File(rootDir, uri)
        if (!requestFile.exists()) {
            // Also try resolving case-insensitively just like we did before
            val parts = uri.split("/")
            var current = rootDir
            var found = true
            for (part in parts) {
                if (part.isEmpty()) continue
                val next = File(current, part)
                if (next.exists()) {
                    current = next
                } else {
                    val match = current.listFiles()?.firstOrNull { it.name.equals(part, ignoreCase = true) }
                    if (match != null) {
                        current = match
                    } else {
                        found = false
                        break
                    }
                }
            }
            if (found) {
                requestFile = current
            }
        }

        if (!requestFile.exists() || requestFile.isDirectory) {
            // Default to index.html if pointing to directory? 
            if (requestFile.isDirectory) {
                requestFile = File(requestFile, "index.html")
            }
            if (!requestFile.exists()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
            }
        }

        var mime = "application/octet-stream"
        val lowerUri = requestFile.name.lowercase()
        when {
            lowerUri.endsWith(".html") -> mime = "text/html"
            lowerUri.endsWith(".js") -> mime = "application/javascript"
            lowerUri.endsWith(".css") -> mime = "text/css"
            lowerUri.endsWith(".png") -> mime = "image/png"
            lowerUri.endsWith(".jpg") || lowerUri.endsWith(".jpeg") -> mime = "image/jpeg"
            lowerUri.endsWith(".gif") -> mime = "image/gif"
            lowerUri.endsWith(".svg") -> mime = "image/svg+xml"
            lowerUri.endsWith(".json") || lowerUri.endsWith(".webapp") -> mime = "application/json"
            lowerUri.endsWith(".mp3") -> mime = "audio/mpeg"
            lowerUri.endsWith(".wav") -> mime = "audio/wav"
            lowerUri.endsWith(".ogg") -> mime = "audio/ogg"
            lowerUri.endsWith(".zip") -> mime = "application/zip"
        }
        
        return try {
            val res = newChunkedResponse(Response.Status.OK, mime, FileInputStream(requestFile))
            res.addHeader("Access-Control-Allow-Origin", "*")
            res.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD")
            res.addHeader("Access-Control-Allow-Headers", "*")
            res
        } catch (e: FileNotFoundException) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }
    }
}
