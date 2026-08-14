with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "r") as f:
    code = f.read()

import re

screenshot_action = """                    IconButton(onClick = {
                        webViewRef?.let { wv ->
                            if (wv.width > 0 && wv.height > 0) {
                                val bitmap = android.graphics.Bitmap.createBitmap(wv.width, wv.height, android.graphics.Bitmap.Config.ARGB_8888)
                                val saveAction = { bmp: android.graphics.Bitmap ->
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val contentValues = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "kaios_ss_${System.currentTimeMillis()}.png")
                                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/KaiOS")
                                            }
                                            val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                            if (uri != null) {
                                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                }
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    android.widget.Toast.makeText(context, "Screenshot saved!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                var activity: android.app.Activity? = null
                                var currentCtx = context
                                while (currentCtx is android.content.ContextWrapper) {
                                    if (currentCtx is android.app.Activity) {
                                        activity = currentCtx
                                        break
                                    }
                                    currentCtx = currentCtx.baseContext
                                }

                                val window = activity?.window
                                if (window != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    val location = IntArray(2)
                                    wv.getLocationInWindow(location)
                                    val rect = android.graphics.Rect(
                                        location[0], location[1],
                                        location[0] + wv.width, location[1] + wv.height
                                    )
                                    try {
                                        android.view.PixelCopy.request(window, rect, bitmap, { result ->
                                            if (result == android.view.PixelCopy.SUCCESS) {
                                                saveAction(bitmap)
                                            } else {
                                                val canvas = android.graphics.Canvas(bitmap)
                                                wv.draw(canvas)
                                                saveAction(bitmap)
                                            }
                                        }, android.os.Handler(android.os.Looper.getMainLooper()))
                                    } catch (e: Exception) {
                                        val canvas = android.graphics.Canvas(bitmap)
                                        wv.draw(canvas)
                                        saveAction(bitmap)
                                    }
                                } else {
                                    val canvas = android.graphics.Canvas(bitmap)
                                    wv.draw(canvas)
                                    saveAction(bitmap)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Screenshot")
                    }
                    IconButton(onClick = { showConsole = true }) {"""

code = code.replace("                    IconButton(onClick = { showConsole = true }) {", screenshot_action)

with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "w") as f:
    f.write(code)
