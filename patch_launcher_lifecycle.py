with open("app/src/main/java/com/example/ui/simulator/LauncherScreen.kt", "r") as f:
    code = f.read()

old_load = """    fun loadApps() {
        scope.launch(Dispatchers.IO) {
            val apps = scanInstalledApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
            }
        }
    }

    LaunchedEffect(Unit) {
        loadApps()
    }"""

new_load = """    fun loadApps() {
        scope.launch(Dispatchers.IO) {
            val apps = scanInstalledApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loadApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }"""

code = code.replace(old_load, new_load)

if "import androidx.compose.runtime.DisposableEffect" not in code:
    code = code.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.DisposableEffect")

with open("app/src/main/java/com/example/ui/simulator/LauncherScreen.kt", "w") as f:
    f.write(code)
