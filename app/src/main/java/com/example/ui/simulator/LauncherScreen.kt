package com.example.ui.simulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    onNavigateToStore: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadApps() {
        scope.launch(Dispatchers.IO) {
            val apps = scanInstalledApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps.sortedBy { it.name.lowercase() }
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loadApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val groupedApps = filteredApps.groupBy { it.name.first().uppercaseChar() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Optimized Lite Wallpaper Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A237E), // Deep Indigo
                            Color(0xFF121212)  // Near Black
                        )
                    )
                )
        )
        // Gradient overlay for readability and status bar visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Search Bar (Matching OS style)
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.fillMaxWidth(),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                    singleLine = true
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Quick actions bottom bar
                Surface(
                    modifier = Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onLaunchApp("kaios://home") }) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = "Home", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onNavigateToStore) {
                            Icon(Icons.Default.Store, contentDescription = "Store", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        ) { padding ->
            if (installedApps.isEmpty()) {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No apps installed", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToStore) {
                            Text("Open Store")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedApps.forEach { (char, apps) ->
                        if (searchQuery.isEmpty()) {
                            item(span = { GridItemSpan(4) }) {
                                Text(
                                    text = char.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                        }
                        items(apps) { app ->
                            AppGridItem(
                                app = app,
                                onClick = { onLaunchApp(app.startPageUrl) },
                                onDelete = {
                                    val appDir = File(context.filesDir, "kaios_apps/${app.id}")
                                    if (appDir.exists()) {
                                        appDir.deleteRecursively()
                                        loadApps()
                                    }
                                },
                                onExport = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val appDir = File(context.filesDir, "kaios_apps/${app.id}")
                                            if (appDir.exists()) {
                                                val tempZipFile = File(context.cacheDir, "${app.name.replace(" ", "_")}_backup.zip")
                                                ExportUtils.zipDirectory(appDir, tempZipFile)
                                                
                                                val fileName = "${app.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")}.zip"
                                                tempZipFile.inputStream().use { fis ->
                                                    val success = ExportUtils.saveFileToPublicDownloads(context, fileName, fis)
                                                    withContext(Dispatchers.Main) {
                                                        if (success) {
                                                            Toast.makeText(context, "Saved $fileName to Downloads!", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to save to Downloads.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                tempZipFile.delete()
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "App files directory not found.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppGridItem(
    app: InstalledApp,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                AppIcon(app = app, modifier = Modifier.padding(8.dp).fillMaxSize())
            }
            
            // Export badge (small download icon at top-left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)) // Beautiful green
                    .clickable { onExport() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export App ZIP", tint = Color.White, modifier = Modifier.size(12.dp))
            }

            // Delete badge (small icon)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
