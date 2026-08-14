with open("app/src/main/java/com/example/ui/simulator/AppModels.kt", "r") as f:
    code = f.read()

new_func = """fun scanInstalledApps(context: Context): List<InstalledApp> {
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
}"""

import re
code = re.sub(r'fun scanInstalledApps.*?return installed\.sortedBy \{ it\.name \}\n\}', new_func, code, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/simulator/AppModels.kt", "w") as f:
    f.write(code)
