package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.simulator.SimulatorScreen
import com.example.ui.simulator.SimulatorViewModel
import com.example.ui.simulator.LocalServerManager
import com.example.ui.theme.MyApplicationTheme

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.simulator.LauncherScreen
import com.example.ui.simulator.StoreScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Start local server and persistent foreground service immediately
    LocalServerManager.startServer(this)
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permission = "android.permission.POST_NOTIFICATIONS"
        if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 101)
        }
    }

    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val simulatorViewModel: SimulatorViewModel = viewModel()
        
        NavHost(navController = navController, startDestination = "launcher") {
            composable("launcher") {
                LauncherScreen(
                    onNavigateToStore = { navController.navigate("store") },
                    onLaunchApp = { url ->
                        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        simulatorViewModel.updateUrl(url)
                        navController.navigate("simulator/$encoded")
                    }
                )
            }
            composable("store") {
                StoreScreen(
                    onBack = { navController.popBackStack() },
                    onLaunchApp = { url ->
                        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        simulatorViewModel.updateUrl(url)
                        navController.navigate("simulator/$encoded")
                    }
                )
            }
            composable("simulator/{url}") { backStackEntry ->
                SimulatorScreen(
                    viewModel = simulatorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
      }
    }
  }
}
