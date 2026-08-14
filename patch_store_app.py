with open("app/src/main/java/com/example/ui/simulator/AppModels.kt", "r") as f:
    code = f.read()

import re

new_store_app = """data class StoreApp(
    val name: String,
    val description: String,
    val author: String,
    val zipUrl: String,
    val iconUrl: String,
    val size: String = ""
)"""

code = re.sub(r'data class StoreApp\([^)]+\)', new_store_app, code)

with open("app/src/main/java/com/example/ui/simulator/AppModels.kt", "w") as f:
    f.write(code)
