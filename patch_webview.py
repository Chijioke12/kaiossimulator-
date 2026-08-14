with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "r") as f:
    code = f.read()

import re

# We need to add onRelease = { it.destroy() } to AndroidView
new_android_view = """                                AndroidView(
                                    factory = { context ->
                                        WebView(context).apply {
                                            setupWebView(this)
                                            
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                    val u = request?.url.toString()
                                                    if (u.startsWith("http://") || u.startsWith("https://")) {
                                                        viewModel.updateUrl(u)
                                                        return false
                                                    }
                                                    return true
                                                }
                                            }
                                            
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                    consoleMessage?.let {
                                                        val log = "${it.messageLevel().name}: ${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}"
                                                        viewModel.addLog(log)
                                                    }
                                                    return true
                                                }

                                                override fun onJsAlert(
                                                    view: WebView?,
                                                    url: String?,
                                                    message: String?,
                                                    result: JsResult?
                                                ): Boolean {
                                                    viewModel.showAlert(message ?: "")
                                                    result?.confirm()
                                                    return true
                                                }
                                            }
                                            
                                            loadUrl(urlState)
                                        }
                                    },
                                    update = { wv ->
                                        if (wv.url != urlState) {
                                            wv.loadUrl(urlState)
                                        }
                                        webViewRef = wv
                                    },
                                    onRelease = { wv ->
                                        wv.destroy()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )"""

code = re.sub(r'AndroidView\(\s*factory = \{ context ->.*?modifier = Modifier\.fillMaxSize\(\)\s*\)', new_android_view, code, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "w") as f:
    f.write(code)
