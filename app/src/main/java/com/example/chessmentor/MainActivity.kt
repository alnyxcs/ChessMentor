// MainActivity.kt
package com.example.chessmentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.presentation.ui.components.navigation.ChessMentorApp
import com.example.chessmentor.presentation.ui.components.theme.ChessMentorTheme
import com.example.chessmentor.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = AppContainer.getInstance(applicationContext)
        val gameViewModel = GameViewModel(container)

        // Логируем архитектуру для отладки
        android.util.Log.i("CPU_ARCH", "ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")

        // Прогреваем движок в фоне
        lifecycleScope.launch {
            try {
                val initialized = container.chessEngine.init()
                android.util.Log.i("MainActivity", "Chess engine initialized: $initialized")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to init engine", e)
            }
        }

        setContent {
            ChessMentorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChessMentorApp(container, gameViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        container.cleanup()
    }
}