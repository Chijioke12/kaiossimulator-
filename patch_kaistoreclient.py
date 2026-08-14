with open("app/src/main/java/com/example/ui/simulator/KaiStoreClient.kt", "r") as f:
    code = f.read()

import re

code = code.replace("e.printStackTrace()", 'e.printStackTrace()\n            android.util.Log.e("KaiStoreClient", "Fetch apps failed", e)')

with open("app/src/main/java/com/example/ui/simulator/KaiStoreClient.kt", "w") as f:
    f.write(code)
