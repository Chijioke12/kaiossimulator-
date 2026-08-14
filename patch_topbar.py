with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "r") as f:
    code = f.read()

start_str = "    Scaffold(\n        topBar = {\n            TopAppBar("
end_str = "        }\n    ) { padding ->"

start_idx = code.find(start_str)
end_idx = code.find(end_str, start_idx) + len(end_str)

new_topbar = """    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showConsole = true }) {
                        Icon(Icons.Default.Terminal, contentDescription = "Console Logs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->"""

code = code[:start_idx] + new_topbar + code[end_idx:]

with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "w") as f:
    f.write(code)
