import re

with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "r") as f:
    code = f.read()

# Remove fileLauncher
code = re.sub(r'val fileLauncher.*?(?=var isMenuOpen)', '', code, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/simulator/SimulatorScreen.kt", "w") as f:
    f.write(code)
