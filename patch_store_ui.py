with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "r") as f:
    code = f.read()

import re

new_text = """                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val subtitle = if (app.size.isNotEmpty()) "By ${app.author} • ${app.size}" else "By ${app.author}"
                                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(app.description, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }"""

code = re.sub(r'                                Column\(modifier = Modifier.weight\(1f\)\) \{\n                                    Text\(app.name, fontWeight = FontWeight.Bold, fontSize = 16.sp\)\n                                    Text\("By \$\{app.author\}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary\)\n                                    Spacer\(modifier = Modifier.height\(4.dp\)\)\n                                    Text\(app.description, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis\)\n                                \}', new_text, code)

with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "w") as f:
    f.write(code)
