package com.test.ytx

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.test.ytx.data.SettingsRepository
import com.test.ytx.ui.MainScreen
import com.test.ytx.ui.SettingsScreen
import com.test.ytx.ui.SetupScreen
import com.test.ytx.ui.theme.YTXTheme
import com.test.ytx.viewmodel.MainViewModel
import com.test.ytx.viewmodel.SettingsViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val repository = SettingsRepository.getInstance(applicationContext)
        val app = application
        
        setContent {
            YTXTheme {
                val navController = rememberNavController()
                val isSetupCompleted by repository.isSetupCompleted.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(5000) // Increased to 5 seconds to show the full thunder animation
                    showSplash = false
                }

                if (showSplash || isSetupCompleted == null) {
                    HtmlSplashScreen()
                } else if (!isSetupCompleted!!) {
                    SetupScreen(
                        onComplete = { path, url ->
                            scope.launch {
                                repository.updateDownloadPath(path)
                                repository.updateServerUrl(url)
                                repository.setSetupCompleted(true)
                            }
                        }
                    )
                } else {
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            val mainViewModel: MainViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return MainViewModel(app, repository) as T
                                    }
                                }
                            )
                            MainScreen(
                                viewModel = mainViewModel,
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return SettingsViewModel(repository) as T
                                    }
                                }
                            )
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlSplashScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(0xFF000000.toInt()) // Solid black background
                loadUrl("file:///android_asset/splash.html")
            }
        }
    )
}
