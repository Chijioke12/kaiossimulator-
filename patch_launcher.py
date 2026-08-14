with open("app/src/main/java/com/example/ui/simulator/LauncherScreen.kt", "r") as f:
    code = f.read()

import re

new_topbar = """        topBar = {
            TopAppBar(
                title = { Text("KaiOS Launcher", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { onLaunchApp("kaios://home") }) {
                        Icon(androidx.compose.material.icons.Icons.Default.PhoneIphone, contentDescription = "Open Simulator")
                    }
                    IconButton(onClick = onNavigateToStore) {
                        Icon(Icons.Default.Store, contentDescription = "Store")
                    }
                }
            )
        }"""

code = re.sub(r'        topBar = \{\s*TopAppBar\(.*?\}\s*\)\s*\}', new_topbar, code, flags=re.DOTALL)

if "import androidx.compose.material.icons.filled.PhoneIphone" not in code:
    code = code.replace("import androidx.compose.material.icons.filled.Delete", "import androidx.compose.material.icons.filled.Delete\nimport androidx.compose.material.icons.filled.PhoneIphone")

with open("app/src/main/java/com/example/ui/simulator/LauncherScreen.kt", "w") as f:
    f.write(code)
