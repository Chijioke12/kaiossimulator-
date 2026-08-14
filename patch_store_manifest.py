with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "r") as f:
    code = f.read()

import re

# Update categories
old_cats = """    val categories = listOf(
        Pair(null, "All"),
        Pair("10", "Social"),
        Pair("20", "Games"),
        Pair("30", "Utilities"),
        Pair("40", "Lifestyle"),
        Pair("50", "Entertainment"),
        Pair("60", "Health"),
        Pair("70", "Sports"),
        Pair("80", "News")
    )"""
new_cats = """    val categories = listOf(
        Pair(null, "All"),
        Pair("10", "Games"),
        Pair("20", "Entertainment"),
        Pair("30", "Social"),
        Pair("40", "Shopping"),
        Pair("50", "News"),
        Pair("60", "Utilities"),
        Pair("70", "Lifestyle"),
        Pair("80", "Health"),
        Pair("90", "Sports"),
        Pair("100", "Books/Reference"),
        Pair("110", "Education")
    )"""
code = code.replace(old_cats, new_cats)

# Update StoreApp creation to include manifestUrl
old_app = """                            StoreApp(
                                name = name,
                                description = desc,
                                author = author,
                                zipUrl = zipUrl,
                                iconUrl = iconUrl,
                                size = sizeStr
                            )"""
new_app = """                            StoreApp(
                                name = name,
                                description = desc,
                                author = author,
                                zipUrl = zipUrl,
                                manifestUrl = jsonObj.optString("manifest_url", ""),
                                iconUrl = iconUrl,
                                size = sizeStr
                            )"""
code = code.replace(old_app, new_app)

with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "w") as f:
    f.write(code)
