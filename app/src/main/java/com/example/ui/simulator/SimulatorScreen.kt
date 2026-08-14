package com.example.ui.simulator

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.simulator.KaiOSKey
import com.example.ui.simulator.KaiOSKey.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import androidx.compose.material.icons.filled.FolderOpen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(viewModel: SimulatorViewModel, onBack: () -> Unit) {
    val urlState by viewModel.url.collectAsStateWithLifecycle()
    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val dpadMappingMode by viewModel.dpadMappingMode.collectAsStateWithLifecycle()

    val displayWidth = if (displayMode == DisplayMode.LANDSCAPE_320X240) 320.dp else 240.dp
    val displayHeight = if (displayMode == DisplayMode.LANDSCAPE_320X240) 240.dp else 320.dp

    var tempUrl by remember { mutableStateOf(urlState) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showConsole by remember { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val workDir = File(context.filesDir, "kaios_apps/${System.currentTimeMillis()}")
                    workDir.mkdirs()
                    
                    val tempFile = File(workDir, "uploaded_file.tmp")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                    
                    val startPage = extractAppZip(tempFile, workDir)

                    if (startPage != null && startPage.exists()) {
                        LocalServerManager.startServer(context)
                        val relativePath = startPage.absolutePath.removePrefix(context.filesDir.absolutePath).trimStart('/')
                        val fileUrl = "http://localhost:${LocalServerManager.serverPort}/$relativePath"
                        withContext(Dispatchers.Main) {
                            tempUrl = fileUrl
                            viewModel.updateUrl(fileUrl)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Could not find index.html", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error loading file", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.keyEvents) {
        viewModel.keyEvents.collect { event ->
            webViewRef?.let { wv ->
                val eventType = if (event.isDown) "keydown" else "keyup"
                val js = """
                    (function() {
                        var kName = '${event.key.keyName}';
                        var isDown = ${event.isDown};
                        var evType = isDown ? 'keydown' : 'keyup';
                        
                        var keyChar = kName;
                        if (kName.startsWith('Key')) keyChar = kName.substring(3);
                        else if (kName === 'Star') keyChar = '*';
                        else if (kName === 'Hash') keyChar = '#';
                        else if (kName === 'Enter') keyChar = 'Enter';
                        
                        var cVal = kName.startsWith('Key') ? 'Digit' + kName.substring(3) : kName;
                        
                        var props = {
                            key: keyChar,
                            code: cVal,
                            bubbles: true,
                            cancelable: true,
                            view: window
                        };
                        var ev = new KeyboardEvent(evType, props);
                        Object.defineProperty(ev, 'keyCode', {get: function() { return ${event.key.keyCode}; }});
                        Object.defineProperty(ev, 'which', {get: function() { return ${event.key.keyCode}; }});
                        
                        var active = document.activeElement;
                        
                        // Manual character insertion for input fields
                        if (isDown && active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA') && (!active.readOnly)) {
                            var typable = (kName.startsWith('Key') || kName === 'Star' || kName === 'Hash' || kName === 'Space');
                            if (typable) {
                                var start = active.selectionStart || 0;
                                var end = active.selectionEnd || 0;
                                var text = active.value || '';
                                active.value = text.substring(0, start) + keyChar + text.substring(end);
                                active.selectionStart = active.selectionEnd = start + 1;
                                active.dispatchEvent(new Event('input', { bubbles: true }));
                                active.dispatchEvent(new Event('change', { bubbles: true }));
                            } else if (kName === 'Backspace') {
                                var start = active.selectionStart || 0;
                                var end = active.selectionEnd || 0;
                                if (start === end && start > 0) {
                                    var text = active.value || '';
                                    active.value = text.substring(0, start - 1) + text.substring(end);
                                    active.selectionStart = active.selectionEnd = start - 1;
                                    active.dispatchEvent(new Event('input', { bubbles: true }));
                                    active.dispatchEvent(new Event('change', { bubbles: true }));
                                } else if (start !== end) {
                                    var text = active.value || '';
                                    active.value = text.substring(0, start) + text.substring(end);
                                    active.selectionStart = active.selectionEnd = start;
                                    active.dispatchEvent(new Event('input', { bubbles: true }));
                                    active.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            }
                        }
                        
                        var target = document.activeElement || document.body || document;
                        target.dispatchEvent(ev);
                    })();
                """.trimIndent()
                wv.evaluateJavascript(js, null)
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                webViewRef?.onPause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                webViewRef?.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        webViewRef?.reload()
                    }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        IconButton(onClick = {
                            webViewRef?.let { wv ->
                                if (wv.width > 0 && wv.height > 0) {
                                    val bitmap = android.graphics.Bitmap.createBitmap(wv.width, wv.height, android.graphics.Bitmap.Config.ARGB_8888)
                                    val saveAction = { bmp: android.graphics.Bitmap ->
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                val contentValues = android.content.ContentValues().apply {
                                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "kaios_ss_${System.currentTimeMillis()}.png")
                                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/KaiOS")
                                                }
                                                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                                if (uri != null) {
                                                    context.contentResolver.openOutputStream(uri)?.use { out ->
                                                        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                    }
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        android.widget.Toast.makeText(context, "Screenshot saved!", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }

                                    var activity: android.app.Activity? = null
                                    var currentCtx = context
                                    while (currentCtx is android.content.ContextWrapper) {
                                        if (currentCtx is android.app.Activity) {
                                            activity = currentCtx
                                            break
                                        }
                                        currentCtx = currentCtx.baseContext
                                    }

                                    val window = activity?.window
                                    if (window != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        val location = IntArray(2)
                                        wv.getLocationInWindow(location)
                                        val rect = android.graphics.Rect(
                                            location[0], location[1],
                                            location[0] + wv.width, location[1] + wv.height
                                        )
                                        try {
                                            android.view.PixelCopy.request(window, rect, bitmap, { result ->
                                                if (result == android.view.PixelCopy.SUCCESS) {
                                                    saveAction(bitmap)
                                                } else {
                                                    val canvas = android.graphics.Canvas(bitmap)
                                                    wv.draw(canvas)
                                                    saveAction(bitmap)
                                                }
                                            }, android.os.Handler(android.os.Looper.getMainLooper()))
                                        } catch (e: Exception) {
                                            val canvas = android.graphics.Canvas(bitmap)
                                            wv.draw(canvas)
                                            saveAction(bitmap)
                                        }
                                    } else {
                                        val canvas = android.graphics.Canvas(bitmap)
                                        wv.draw(canvas)
                                        saveAction(bitmap)
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Screenshot")
                        }
                        IconButton(onClick = { showConsole = true }) {
                            Icon(Icons.Default.BugReport, contentDescription = "Console Logs")
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    // URL Input
                    OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                label = { Text("KaiOS App URL") },
                placeholder = { Text("https://...") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.updateUrl(tempUrl) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Load", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Controls Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Screen Mode Toggle
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Screen Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Choose screen orientation aspect ratio",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = displayMode == DisplayMode.PORTRAIT_240X320,
                                onClick = { viewModel.setDisplayMode(DisplayMode.PORTRAIT_240X320) },
                                label = { Text("Portrait", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                leadingIcon = {
                                    if (displayMode == DisplayMode.PORTRAIT_240X320) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = displayMode == DisplayMode.LANDSCAPE_320X240,
                                onClick = { viewModel.setDisplayMode(DisplayMode.LANDSCAPE_320X240) },
                                label = { Text("320x240", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                leadingIcon = {
                                    if (displayMode == DisplayMode.LANDSCAPE_320X240) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // D-Pad Mapping
                    if (displayMode == DisplayMode.LANDSCAPE_320X240) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                Text(
                                    text = "D-Pad Rotation Mapping",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Compensates for horizontal games designed for rotated portrait phones",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = dpadMappingMode == DpadMappingMode.STANDARD,
                                    onClick = { viewModel.setDpadMappingMode(DpadMappingMode.STANDARD) },
                                    label = { Text("Standard", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = dpadMappingMode == DpadMappingMode.ROTATED_CCW,
                                    onClick = { viewModel.setDpadMappingMode(DpadMappingMode.ROTATED_CCW) },
                                    label = { Text("Rotated CCW", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = dpadMappingMode == DpadMappingMode.ROTATED_CW,
                                    onClick = { viewModel.setDpadMappingMode(DpadMappingMode.ROTATED_CW) },
                                    label = { Text("Rotated CW", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Phone Container
            Surface(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(vertical = 16.dp, horizontal = 24.dp),
                shape = RoundedCornerShape(40.dp),
                color = Color(0xFF1A1A1A), // Dark matte phone body
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF333333))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Display Area
                    Surface(
                        modifier = Modifier
                            .size(width = displayWidth, height = displayHeight),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        setupWebView(this)
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                consoleMessage?.let {
                                                    val source = it.sourceId()?.substringAfterLast('/') ?: "unknown"
                                                    viewModel.addLog("${it.messageLevel().name}: ${it.message()} ($source:${it.lineNumber()})")
                                                }
                                                return true
                                            }
                                                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                                    if (result != null) {
                                                        viewModel.showAlert(message ?: "", result)
                                                    }
                                                    return true
                                                }
                                                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                                    if (result != null) {
                                                        viewModel.showAlert(message ?: "", result)
                                                    }
                                                    return true
                                                }
                                            }
                                            webViewRef = this
                                            loadUrl(urlState)
                                        }
                                    },
                                    update = { wv ->
                                        if (wv.url != urlState) {
                                            wv.loadUrl(urlState)
                                        }
                                        webViewRef = wv
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Keypad
                    SimulatorKeypad(onKeyEvent = { key, isDown -> viewModel.onKeyEvent(key, isDown) })
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

    if (showConsole) {
        ConsoleBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showConsole = false }
        )
    }

    if (alertData != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text("App Alert") },
            text = { Text(alertData!!.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) { Text("OK") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleBottomSheet(
    viewModel: SimulatorViewModel,
    onDismissRequest: () -> Unit
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.5f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Console Logs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row {
                    TextButton(onClick = {
                        val allLogs = logs.joinToString("\n")
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(allLogs))
                        android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text("Copy", fontSize = 12.sp) }
                    TextButton(onClick = { viewModel.clearLogs() }) { Text("Clear", fontSize = 12.sp) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.text.selection.SelectionContainer {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs.size) { index ->
                        Text(logs[index], fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}



private fun injectDeviceStoragePolyfill(webView: WebView?) {
    val js = """
        (function() {
            if (!window.navigator) { window.navigator = {}; }
            if (!window.navigator.getDeviceStorage) {
                window.navigator.getDeviceStorage = function(type) {
                    console.log('Mock getDeviceStorage called for type: ' + type);
                    var mockStorage = {
                        get: function(name) {
                            console.log('Mock storage.get: ' + name);
                            var req = { onsuccess: null, onerror: null, result: null };
                            setTimeout(function() {
                                if (req.onsuccess) req.onsuccess({ target: { result: null } });
                            }, 10);
                            return req;
                        },
                        add: function(blob) {
                            console.log('Mock storage.add');
                            var req = { onsuccess: null, onerror: null };
                            setTimeout(function() {
                                if (req.onsuccess) req.onsuccess();
                            }, 10);
                            return req;
                        },
                        addNamed: function(blob, name) {
                            console.log('Mock storage.addNamed: ' + name);
                            var req = { onsuccess: null, onerror: null };
                            setTimeout(function() {
                                if (req.onsuccess) req.onsuccess();
                            }, 10);
                            return req;
                        },
                        enumerate: function() {
                            console.log('Mock storage.enumerate');
                            var req = { onsuccess: null, onerror: null, result: [] };
                            setTimeout(function() {
                                if (req.onsuccess) req.onsuccess({ target: { result: [] } });
                            }, 10);
                            return req;
                        },
                        delete: function(name) {
                            console.log('Mock storage.delete: ' + name);
                            var req = { onsuccess: null, onerror: null };
                            setTimeout(function() {
                                if (req.onsuccess) req.onsuccess();
                            }, 10);
                            return req;
                        }
                    };
                    return mockStorage;
                };
            }
            if (!window.navigator.getDeviceStorages) {
                window.navigator.getDeviceStorages = function(type) {
                    return [window.navigator.getDeviceStorage(type)];
                };
            }
        })();
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupWebView(webView: WebView) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        
        // Use normal Android WebView responsive scaling
        useWideViewPort = true
        loadWithOverviewMode = true
        
        // Allow autoplaying media without explicit tap on the canvas
        mediaPlaybackRequiresUserGesture = false
        
        // Performance
        cacheMode = WebSettings.LOAD_NO_CACHE
        
        // Remove 'Firefox' from UA to prevent legacy Mozilla CSS rendering bugs in Chromium.
        // We only append KAIOS to default Chromium UA if not already present, if the app checks for KaiOS.
        val defaultUa = userAgentString
        if (!defaultUa.contains("KAIOS")) {
            userAgentString = "$defaultUa KAIOS/2.5"
        }
    }
    
    // Enable cross-origin requests
    webView.settings.allowFileAccess = true
    webView.settings.allowContentAccess = true
    webView.settings.allowFileAccessFromFileURLs = true
    webView.settings.allowUniversalAccessFromFileURLs = true
    webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    
    webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
    
    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            injectDeviceStoragePolyfill(view)
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            injectDeviceStoragePolyfill(view)
        }

        override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
            super.onReceivedError(view, request, error)
            error?.let {
                android.util.Log.e("WebView", "Error: ${it.errorCode} ${it.description} for ${request?.url}")
            }
        }
        
        override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)
            errorResponse?.let {
                android.util.Log.e("WebView", "HTTP Error: ${it.statusCode} ${it.reasonPhrase} for ${request?.url}")
            }
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: android.webkit.WebResourceRequest?
        ): android.webkit.WebResourceResponse? {
            val url = request?.url ?: return super.shouldInterceptRequest(view, request)
            val urlString = url.toString()

            // 1. If it's a local file inside /data/, serve it from local files dir with CORS headers
            if (url.scheme == "http" && url.host == "localhost" && url.path?.startsWith("/data/") == true) {
                try {
                    val reqPath = url.path!!
                    var file = java.io.File(reqPath)
                    
                    if (!file.exists()) {
                        // Fully case-insensitive path resolution
                        val parts = reqPath.split("/")
                        var current = java.io.File("/")
                        var found = true
                        for (part in parts) {
                            if (part.isEmpty()) continue
                            val next = java.io.File(current, part)
                            if (next.exists()) {
                                current = next
                            } else {
                                // Try case-insensitive
                                val match = current.listFiles()?.firstOrNull { it.name.equals(part, ignoreCase = true) }
                                if (match != null) {
                                    current = match
                                } else {
                                    found = false
                                    break
                                }
                            }
                        }
                        if (found) {
                            file = current
                        }
                    }
                    
                    if (file.exists()) {
                        val path = file.absolutePath.lowercase()
                        val mimeType = when {
                            path.endsWith(".html") -> "text/html"
                            path.endsWith(".js") -> "application/javascript"
                            path.endsWith(".css") -> "text/css"
                            path.endsWith(".png") -> "image/png"
                            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
                            path.endsWith(".gif") -> "image/gif"
                            path.endsWith(".svg") -> "image/svg+xml"
                            path.endsWith(".json") || path.endsWith(".webapp") -> "application/json"
                            path.endsWith(".mp3") -> "audio/mpeg"
                            path.endsWith(".wav") -> "audio/wav"
                            path.endsWith(".ogg") -> "audio/ogg"
                            path.endsWith(".zip") -> "application/zip"
                            else -> "application/octet-stream"
                        }
                        return android.webkit.WebResourceResponse(
                            mimeType,
                            "UTF-8",
                            java.io.FileInputStream(file)
                        ).apply {
                            responseHeaders = mapOf(
                                "Access-Control-Allow-Origin" to "*",
                                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                                "Access-Control-Allow-Headers" to "*"
                            )
                        }
                    } else {
                        // Let's try to look deeper if it's a subfolder mismatch
                        android.util.Log.e("Simulator", "File not found: $reqPath")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Return 404
                return android.webkit.WebResourceResponse(
                    "text/plain", "UTF-8", 404, "Not Found", null, java.io.ByteArrayInputStream("Not Found".toByteArray())
                )
            }

            // 2. If it's an external HTTP/HTTPS request, proxy it natively to bypass CORS!
            if ((url.scheme == "http" || url.scheme == "https") && url.host != "localhost" && url.host != "127.0.0.1") {
                try {
                    val conn = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = request.method
                    
                    // Copy request headers
                    request.requestHeaders?.forEach { (key, value) ->
                        conn.setRequestProperty(key, value)
                    }
                    
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    
                    val responseCode = conn.responseCode
                    val responseMessage = conn.responseMessage
                    val contentType = conn.contentType ?: "application/octet-stream"
                    
                    // Extract mime type and encoding
                    val mimeType = contentType.substringBefore(";").trim()
                    val encoding = if (contentType.contains("charset=")) {
                        contentType.substringAfter("charset=").substringBefore(";").trim()
                    } else {
                        "UTF-8"
                    }
                    
                    val inputStream = if (responseCode >= 400) conn.errorStream ?: java.io.ByteArrayInputStream(ByteArray(0)) else conn.inputStream
                    
                    // Copy and inject headers
                    val headers = mutableMapOf<String, String>()
                    conn.headerFields.forEach { (key, values) ->
                        if (key != null && values.isNotEmpty()) {
                            headers[key] = values.joinToString(", ")
                        }
                    }
                    // Force cross-origin headers to satisfy WebView's origin check
                    headers["Access-Control-Allow-Origin"] = "*"
                    headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS, PUT, DELETE"
                    headers["Access-Control-Allow-Headers"] = "*"
                    headers["Access-Control-Allow-Credentials"] = "true"
                    
                    return android.webkit.WebResourceResponse(
                        mimeType,
                        encoding,
                        responseCode,
                        responseMessage,
                        headers,
                        inputStream
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CORS_PROXY", "Failed to proxy $urlString", e)
                }
            }

            return super.shouldInterceptRequest(view, request)
        }
    }
}

@Composable
fun SimulatorKeypad(onKeyEvent: (KaiOSKey, Boolean) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Control Section (LSK, BACKSPACE, RSK)
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LSK
            KeyButton(
                keyToEmit = KaiOSKey.SoftLeft,
                onKeyEvent = onKeyEvent,
                label = "LSK", 
                color = Color(0xFF444444),
                modifier = Modifier.size(width = 64.dp, height = 38.dp)
            )

            // Backspace / CLR
            KeyButton(
                keyToEmit = KaiOSKey.Backspace,
                onKeyEvent = onKeyEvent,
                label = "BACK", 
                color = Color(0xFF2D2D2D),
                modifier = Modifier.size(width = 64.dp, height = 38.dp)
            )
            
            // RSK
            KeyButton(
                keyToEmit = KaiOSKey.SoftRight,
                onKeyEvent = onKeyEvent,
                label = "RSK", 
                color = Color(0xFF444444),
                modifier = Modifier.size(width = 64.dp, height = 38.dp)
            )
        }

        // D-Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KeyButton(keyToEmit = KaiOSKey.ArrowUp, onKeyEvent = onKeyEvent, icon = Icons.Default.ArrowUpward, modifier = Modifier.size(44.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KeyButton(keyToEmit = KaiOSKey.ArrowLeft, onKeyEvent = onKeyEvent, icon = Icons.AutoMirrored.Filled.ArrowBack, modifier = Modifier.size(44.dp))
                KeyButton(
                    keyToEmit = KaiOSKey.Enter,
                    onKeyEvent = onKeyEvent,
                    icon = Icons.Default.Check, 
                    color = Color(0xFFE91E63),
                    modifier = Modifier.size(44.dp)
                )
                KeyButton(keyToEmit = KaiOSKey.ArrowRight, onKeyEvent = onKeyEvent, icon = Icons.AutoMirrored.Filled.ArrowForward, modifier = Modifier.size(44.dp))
            }
            KeyButton(keyToEmit = KaiOSKey.ArrowDown, onKeyEvent = onKeyEvent, icon = Icons.Default.ArrowDownward, modifier = Modifier.size(44.dp))
        }

        // Numpad Section
        val layout = listOf(
            listOf(KaiOSKey.Key1 to "1", KaiOSKey.Key2 to "2", KaiOSKey.Key3 to "3"),
            listOf(KaiOSKey.Key4 to "4", KaiOSKey.Key5 to "5", KaiOSKey.Key6 to "6"),
            listOf(KaiOSKey.Key7 to "7", KaiOSKey.Key8 to "8", KaiOSKey.Key9 to "9"),
            listOf(KaiOSKey.Star to "*", KaiOSKey.Key0 to "0", KaiOSKey.Hash to "#")
        )

        val letters = mapOf(
            "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL",
            "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
            "0" to "+"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            layout.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (key, label) ->
                        KeyButton(
                            keyToEmit = key,
                            onKeyEvent = onKeyEvent,
                            label = label,
                            subLabel = letters[label],
                            modifier = Modifier.size(width = 68.dp, height = 42.dp),
                            color = Color(0xFF333333),
                            fontSize = 18.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun KeyButton(
    keyToEmit: KaiOSKey,
    onKeyEvent: (KaiOSKey, Boolean) -> Unit,
    label: String? = null,
    subLabel: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier.size(40.dp),
    color: Color = Color(0xFF333333),
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(keyToEmit) {
                detectTapGestures(
                    onPress = {
                        val job = scope.launch {
                            onKeyEvent(keyToEmit, true)
                            delay(400)
                            while (isActive) {
                                onKeyEvent(keyToEmit, true)
                                delay(50)
                            }
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            job.cancel()
                            onKeyEvent(keyToEmit, false)
                        }
                    }
                )
            },
        color = color,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (label != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (subLabel != null) {
                        Text(
                            text = subLabel,
                            fontSize = 8.sp,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 8.sp
                        )
                    }
                }
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }
    }
}
