with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "r") as f:
    code = f.read()

import re

old_android = """                                },
                                update = { wv ->
                                    if (wv.url != urlState) {
                                        wv.loadUrl(urlState)
                                    }
                                    webViewRef = wv
                                },
                                modifier = Modifier.fillMaxSize()
                            )"""

new_android = """                                },
                                update = { wv ->
                                    if (wv.url != urlState) {
                                        wv.loadUrl(urlState)
                                    }
                                    webViewRef = wv
                                },
                                onRelease = { wv ->
                                    wv.destroy()
                                    webViewRef = null
                                },
                                modifier = Modifier.fillMaxSize()
                            )"""

code = code.replace(old_android, new_android)

# Also check for onResume/onPause for the current screen lifecycle
new_lifecycle = """    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                webViewRef?.onPause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                webViewRef?.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold("""

code = code.replace("    Scaffold(", new_lifecycle)
if "import androidx.compose.runtime.DisposableEffect" not in code:
    code = code.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.DisposableEffect")


with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "w") as f:
    f.write(code)
