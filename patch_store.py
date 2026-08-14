with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "r") as f:
    code = f.read()

import re

# We will modify loadStoreApps to do client-side filtering if it's the fallback json (or just in general).
new_load_store = """    fun loadStoreApps(query: String? = null, category: String? = null) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLoading = true
                errorMsg = null
            }
            try {
                val jsonString = KaiStoreClient.fetchApps(category, query)
                val parsedApps = mutableListOf<StoreApp>()
                
                val rootObj = JSONObject(jsonString)
                val jsonArray = if (rootObj.has("organic")) rootObj.getJSONArray("organic")
                               else if (rootObj.has("apps")) rootObj.getJSONArray("apps")
                               else org.json.JSONArray()

                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val name = jsonObj.optString("name", jsonObj.optString("display", ""))
                    var desc = jsonObj.optString("description", "")
                    var author = "Unknown"
                    if (jsonObj.has("developer")) {
                        val devObj = jsonObj.get("developer")
                        if (devObj is String) author = devObj
                        else if (devObj is JSONObject) author = devObj.optString("name", "Unknown")
                    } else if (jsonObj.has("author")) {
                        author = jsonObj.optString("author")
                    }
                    if (author.isEmpty()) author = "Unknown"

                    var iconUrl = jsonObj.optString("icon", jsonObj.optString("icon_url", jsonObj.optString("thumbnail_url", "")))
                    if (iconUrl.isEmpty() && jsonObj.has("icons")) {
                        val iconsObj = jsonObj.optJSONObject("icons")
                        if (iconsObj != null) {
                            if (iconsObj.has("56")) iconUrl = iconsObj.optString("56")
                            else if (iconsObj.has("84")) iconUrl = iconsObj.optString("84")
                            else if (iconsObj.has("112")) iconUrl = iconsObj.optString("112")
                            else if (iconsObj.keys().hasNext()) iconUrl = iconsObj.optString(iconsObj.keys().next())
                        }
                    }

                    var zipUrl = jsonObj.optString("package_path", "")
                    if (zipUrl.isEmpty()) {
                        zipUrl = jsonObj.optString("download_url", jsonObj.optString("download", jsonObj.optString("url", jsonObj.optString("file", ""))))
                    }
                    
                    val appCategories = mutableListOf<String>()
                    if (jsonObj.has("category")) {
                        appCategories.add(jsonObj.optString("category"))
                    }
                    if (jsonObj.has("category_list")) {
                        val catArray = jsonObj.optJSONArray("category_list")
                        if (catArray != null) {
                            for (c in 0 until catArray.length()) {
                                appCategories.add(catArray.optString(c))
                            }
                        }
                    }
                    
                    var sizeStr = ""
                    if (jsonObj.has("size")) {
                        val s = jsonObj.optLong("size", -1L)
                        if (s > 0) sizeStr = android.text.format.Formatter.formatShortFileSize(context, s)
                    }

                    // Client-side fallback filtering
                    var matchesQuery = true
                    if (!query.isNullOrEmpty()) {
                        val q = query.lowercase()
                        matchesQuery = name.lowercase().contains(q) || desc.lowercase().contains(q) || author.lowercase().contains(q)
                    }
                    
                    var matchesCategory = true
                    if (category != null && rootObj.has("apps")) { // only manually filter if it's the fallback json (which has 'apps' array at root usually)
                        // If it's KaiStore API, category is already filtered by API.
                        // But for fallback, category is something like "Games".
                        val categoryName = categories.find { it.first == category }?.second ?: ""
                        matchesCategory = appCategories.any { it.equals(categoryName, ignoreCase = true) || it == category }
                    }

                    if (matchesQuery && matchesCategory) {
                        parsedApps.add(
                            StoreApp(
                                name = name,
                                description = desc,
                                author = author,
                                zipUrl = zipUrl,
                                iconUrl = iconUrl,
                                size = sizeStr
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    apps = parsedApps
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMsg = "Failed to load apps: ${e.message}"
                    isLoading = false
                }
            }
        }
    }"""

code = re.sub(r'    fun loadStoreApps.*?isLoading = false\n                \}\n            \}\n        \}\n    \}', new_load_store, code, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/simulator/StoreScreen.kt", "w") as f:
    f.write(code)
