with open("app/src/main/java/com/example/ui/simulator/KaiStoreClient.kt", "r") as f:
    code = f.read()

code = code.replace("        }\n    fun fetchManifest", "        }\n    }\n\n    fun fetchManifest")

# Also fix the double } at the end
if code.endswith("}\n}"):
    code = code[:-2]

with open("app/src/main/java/com/example/ui/simulator/KaiStoreClient.kt", "w") as f:
    f.write(code)
