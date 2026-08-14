with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "r") as f:
    code = f.read()

import re

old_topbar = """                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )"""

new_topbar = """                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { onLaunchApp("kaios://home") }) {
                        Icon(androidx.compose.material.icons.Icons.Default.PhoneIphone, contentDescription = "Open Simulator")
                    }
                }"""

code = code.replace(old_topbar, new_topbar)

if "import androidx.compose.material.icons.filled.PhoneIphone" not in code:
    code = code.replace("import androidx.compose.material.icons.filled.Search", "import androidx.compose.material.icons.filled.Search\nimport androidx.compose.material.icons.filled.PhoneIphone")

with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "w") as f:
    f.write(code)
