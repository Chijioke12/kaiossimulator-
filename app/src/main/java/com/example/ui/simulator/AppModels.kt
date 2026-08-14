package com.example.ui.simulator

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import org.json.JSONObject

data class StoreApp(
    val name: String,
    val description: String,
    val author: String,
    val zipUrl: String,
    val manifestUrl: String = "",
    val iconUrl: String,
    val size: String = ""
)

data class InstalledApp(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val iconData: String?, 
    val startPageUrl: String
)

fun scanInstalledApps(context: Context): List<InstalledApp> {
    val appsDir = File(context.filesDir, "kaios_apps")
    if (!appsDir.exists() || !appsDir.isDirectory) return emptyList()

    val installed = mutableListOf<InstalledApp>()
    for (dir in appsDir.listFiles() ?: emptyArray()) {
        if (!dir.isDirectory) continue
        
        val allFiles = dir.walkTopDown().toList()
        val manifestFile = allFiles.firstOrNull { it.name == "manifest.webapp" }
        
        if (manifestFile != null) {
            try {
                val json = JSONObject(manifestFile.readText())
                val name = json.optString("name", dir.name)
                val description = json.optString("description", "")
                val version = json.optString("version", "1.0")
                
                var iconData: String? = null
                val iconsObj = json.optJSONObject("icons")
                if (iconsObj != null) {
                    val keys = iconsObj.keys()
                    var largestSize = 0
                    var bestIconPath = ""
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val size = key.toIntOrNull() ?: 0
                        if (size > largestSize) {
                            largestSize = size
                            bestIconPath = iconsObj.getString(key)
                        }
                    }
                    if (bestIconPath.isNotEmpty()) {
                        val iconFile = allFiles.firstOrNull { it.absolutePath.endsWith(bestIconPath.replace("/", File.separator)) }
                            ?: File(dir, bestIconPath.trimStart('/'))
                        if (iconFile.exists()) {
                            val bytes = iconFile.readBytes()
                            iconData = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        }
                    }
                }
                
                val startPage = allFiles.firstOrNull { it.name == "index.html" } 
                    ?: allFiles.firstOrNull { it.name.endsWith(".html") }
                    ?: File(dir, "index.html")
                    
                val relativePath = startPage.absolutePath.removePrefix(context.filesDir.absolutePath).trimStart('/')
                val url = "http://localhost:${LocalServerManager.serverPort}/$relativePath"
                
                installed.add(InstalledApp(dir.name, name, description, version, iconData, url))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val startPage = allFiles.firstOrNull { it.name == "index.html" } 
                ?: allFiles.firstOrNull { it.name.endsWith(".html") }
            
            if (startPage != null && startPage.exists()) {
                val relativePath = startPage.absolutePath.removePrefix(context.filesDir.absolutePath).trimStart('/')
                val url = "http://localhost:${LocalServerManager.serverPort}/$relativePath"
                installed.add(InstalledApp(dir.name, dir.name, "Sideloaded App", "1.0", null, url))
            }
        }
    }
    return installed.sortedBy { it.name }
}

@Composable
fun AppIcon(app: InstalledApp, modifier: Modifier = Modifier) {
    val bitmap = remember(app.iconData) {
        if (app.iconData != null) {
            try {
                val bytes = android.util.Base64.decode(app.iconData, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FallbackIcon(app.name)
        }
    }
}

@Composable
private fun FallbackIcon(name: String) {
    if (name.isNotEmpty()) {
        Text(
            text = name.take(1).uppercase(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    } else {
        Icon(Icons.Default.Apps, contentDescription = null, tint = Color.Gray)
    }
}
