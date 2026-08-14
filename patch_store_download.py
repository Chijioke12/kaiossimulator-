with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "r") as f:
    code = f.read()

import re

old_dl = """                                        if (app.zipUrl.isNotEmpty() && !isDownloading) {
                                            downloadingApp = app.name
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val workDir = File(context.filesDir, "kaios_apps/${System.currentTimeMillis()}")
                                                    workDir.mkdirs()
                                                    val tempFile = File(workDir, "downloaded.zip")
                                                    
                                                    KaiStoreClient.downloadApp(app.zipUrl, tempFile)
                                                    val startPage = extractAppZip(tempFile, workDir)

                                                    withContext(Dispatchers.Main) {
                                                        if (startPage != null && startPage.exists()) {
                                                            LocalServerManager.startServer(context)
                                                            val relativePath = startPage.absolutePath.removePrefix(context.filesDir.absolutePath).trimStart('/')
                                                            val fileUrl = "http://localhost:${LocalServerManager.serverPort}/$relativePath"
                                                            reloadInstalled()
                                                            downloadingApp = null
                                                            onLaunchApp(fileUrl)
                                                        } else {
                                                            downloadingApp = null
                                                            Toast.makeText(context, "Could not find index.html in app package", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        downloadingApp = null
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "No download URL available", Toast.LENGTH_SHORT).show()
                                        }"""

new_dl = """                                        if ((app.zipUrl.isNotEmpty() || app.manifestUrl.isNotEmpty()) && !isDownloading) {
                                            downloadingApp = app.name
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    var targetZip = app.zipUrl
                                                    if (targetZip.isEmpty() && app.manifestUrl.isNotEmpty()) {
                                                        val manifestJson = KaiStoreClient.fetchManifest(app.manifestUrl)
                                                        val mObj = JSONObject(manifestJson)
                                                        targetZip = mObj.optString("package_path", "")
                                                    }
                                                    if (targetZip.isEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            downloadingApp = null
                                                            Toast.makeText(context, "No package path in manifest", Toast.LENGTH_SHORT).show()
                                                        }
                                                        return@launch
                                                    }
                                                    
                                                    val workDir = File(context.filesDir, "kaios_apps/${System.currentTimeMillis()}")
                                                    workDir.mkdirs()
                                                    val tempFile = File(workDir, "downloaded.zip")
                                                    
                                                    KaiStoreClient.downloadApp(targetZip, tempFile)
                                                    val startPage = extractAppZip(tempFile, workDir)

                                                    withContext(Dispatchers.Main) {
                                                        if (startPage != null && startPage.exists()) {
                                                            LocalServerManager.startServer(context)
                                                            val relativePath = startPage.absolutePath.removePrefix(context.filesDir.absolutePath).trimStart('/')
                                                            val fileUrl = "http://localhost:${LocalServerManager.serverPort}/$relativePath"
                                                            reloadInstalled()
                                                            downloadingApp = null
                                                            onLaunchApp(fileUrl)
                                                        } else {
                                                            downloadingApp = null
                                                            Toast.makeText(context, "Could not find index.html in app package", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        downloadingApp = null
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "No download URL available", Toast.LENGTH_SHORT).show()
                                        }"""

code = code.replace(old_dl, new_dl)

with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "w") as f:
    f.write(code)
