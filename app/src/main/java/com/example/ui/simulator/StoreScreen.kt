package com.example.ui.simulator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    onBack: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var apps by remember { mutableStateOf<List<StoreApp>>(emptyList()) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var downloadingApp by remember { mutableStateOf<String?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    
    val categories = listOf(
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
    )
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    fun reloadInstalled() {
        installedApps = scanInstalledApps(context)
    }

    fun loadStoreApps(query: String? = null, category: String? = null) {
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
                                manifestUrl = jsonObj.optString("manifest_url", ""),
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
    }

    LaunchedEffect(Unit) {
        reloadInstalled()
        loadStoreApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KaiOS Store", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { onLaunchApp("kaios://home") }) {
                        Icon(androidx.compose.material.icons.Icons.Default.PhoneIphone, contentDescription = "Open Simulator")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (it.length > 2) {
                        loadStoreApps(query = it)
                    } else if (it.isEmpty()) {
                        loadStoreApps(category = selectedCategory)
                    }
                },
                placeholder = { Text("Search apps...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Categories
            if (searchQuery.isEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = cat.first == selectedCategory,
                            onClick = { 
                                selectedCategory = cat.first
                                loadStoreApps(category = selectedCategory)
                            },
                            label = { Text(cat.second) }
                        )
                    }
                }
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMsg != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            } else if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No apps found.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps) { app ->
                        val isInstalled = installedApps.any { it.name.lowercase() == app.name.lowercase() }
                        val isDownloading = downloadingApp == app.name
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInstalled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (app.iconUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = app.iconUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val subtitle = if (app.size.isNotEmpty()) "By ${app.author} • ${app.size}" else "By ${app.author}"
                                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(app.description, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.width(8.dp))

                                if (app.zipUrl.isNotEmpty() || app.manifestUrl.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Saving raw ZIP for ${app.name}...", Toast.LENGTH_SHORT).show()
                                                    }
                                                    var targetZip = app.zipUrl
                                                    if (targetZip.isEmpty() && app.manifestUrl.isNotEmpty()) {
                                                        val manifestJson = KaiStoreClient.fetchManifest(app.manifestUrl)
                                                        val mObj = JSONObject(manifestJson)
                                                        targetZip = mObj.optString("package_path", "")
                                                    }
                                                    if (targetZip.isEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "No download path found", Toast.LENGTH_SHORT).show()
                                                        }
                                                        return@launch
                                                    }
                                                    
                                                    val tempFile = File(context.cacheDir, "store_raw_${System.currentTimeMillis()}.zip")
                                                    KaiStoreClient.downloadApp(targetZip, tempFile)
                                                    
                                                    val fileName = "${app.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")}.zip"
                                                    tempFile.inputStream().use { fis ->
                                                        val success = ExportUtils.saveFileToPublicDownloads(context, fileName, fis)
                                                        withContext(Dispatchers.Main) {
                                                            if (success) {
                                                                Toast.makeText(context, "Saved $fileName to Downloads!", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                Toast.makeText(context, "Failed to save ZIP to Downloads.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                    tempFile.delete()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Save Raw ZIP",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                
                                Button(
                                    onClick = {
                                        if (isInstalled) {
                                            val installed = installedApps.find { it.name.lowercase() == app.name.lowercase() }
                                            if (installed != null) onLaunchApp(installed.startPageUrl)
                                            return@Button
                                        }
                                        if ((app.zipUrl.isNotEmpty() || app.manifestUrl.isNotEmpty()) && !isDownloading) {
                                            downloadingApp = app.name
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    var targetZip = app.zipUrl
                                                    if (targetZip.isEmpty() && app.manifestUrl.isNotEmpty()) {
                                                        val manifestJson = KaiStoreClient.fetchManifest(app.manifestUrl)
                                                        val mObj = JSONObject(manifestJson)
                                                        targetZip = mObj.optString("package_path", "")
                                                    }
                                                    if (targetZip.isEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            downloadingApp = null
                                                            Toast.makeText(context, "No package path in manifest", Toast.LENGTH_SHORT).show()
                                                        }
                                                        return@launch
                                                    }
                                                    
                                                    val workDir = File(context.filesDir, "kaios_apps/${System.currentTimeMillis()}")
                                                    workDir.mkdirs()
                                                    val tempFile = File(workDir, "downloaded.zip")
                                                    
                                                    KaiStoreClient.downloadApp(targetZip, tempFile)
                                                    val startPage = extractAppZip(tempFile, workDir)

                                                    withContext(Dispatchers.Main) {
                                                        if (startPage != null && startPage.exists()) {
                                                            LocalServerManager.startServer(context)
                                                            val relativePath = startPage.absolutePath.removePrefix(context.filesDir.absolutePath).trimStart('/')
                                                            val fileUrl = "http://localhost:${LocalServerManager.serverPort}/$relativePath"
                                                            reloadInstalled()
                                                            downloadingApp = null
                                                            Toast.makeText(context, "Installed successfully!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            downloadingApp = null
                                                            Toast.makeText(context, "Could not find index.html in app package", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        downloadingApp = null
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "No download URL available", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(if (isInstalled) "OPEN" else "GET", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
