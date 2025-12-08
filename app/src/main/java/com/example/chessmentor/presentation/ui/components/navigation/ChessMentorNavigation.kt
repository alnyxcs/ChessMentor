// presentation/ui/navigation/ChessMentorNavigation.kt
package com.example.chessmentor.presentation.ui.components.navigation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.domain.usecase.LoginUserUseCase
import com.example.chessmentor.domain.usecase.RegisterUserUseCase
import com.example.chessmentor.presentation.ui.components.screen.GameViewScreen
import com.example.chessmentor.presentation.ui.components.screen.GamesListScreen
import com.example.chessmentor.presentation.ui.components.screen.HomeScreen
import com.example.chessmentor.presentation.ui.components.screen.SettingsScreen
import com.example.chessmentor.presentation.ui.components.screen.StatisticsScreen
import com.example.chessmentor.presentation.ui.components.screen.SummaryScreen
import com.example.chessmentor.presentation.ui.components.screen.TrainingScreen
import com.example.chessmentor.presentation.ui.components.screen.UploadGameScreen
import com.example.chessmentor.presentation.ui.screen.EngineSettingsScreen
import com.example.chessmentor.presentation.viewmodel.BoardViewModel
import com.example.chessmentor.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.launch

private const val TAG = "ChessMentorNavigation"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessMentorApp(
    container: AppContainer,
    gameViewModel: GameViewModel
) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentScreen by remember { mutableStateOf("home") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    // Форма регистрации
    var regNickname by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRating by remember { mutableStateOf("1200") }

    // Форма входа
    var loginEmail by remember { mutableStateOf("test@chessmentor.ru") }
    var loginPassword by remember { mutableStateOf("test123") }

    val coroutineScope = rememberCoroutineScope()

    // Синхронизация пользователя с ViewModel
    LaunchedEffect(currentUser) {
        currentUser?.let {
            gameViewModel.setUser(it)
            Log.d(TAG, "User set: ${it.nickname}")
        }
    }

    Scaffold(
        topBar = {
            ChessMentorTopBar(
                currentScreen = currentScreen,
                currentUser = currentUser,
                onNavigateBack = {
                    currentScreen = when (currentScreen) {
                        "game_view" -> "summary"
                        "summary" -> {
                            gameViewModel.closeGameView()
                            "games"
                        }
                        else -> "home"
                    }
                    Log.d(TAG, "Navigate back to: $currentScreen")
                }
            )
        },
        bottomBar = {
            if (currentUser != null && currentScreen != "game_view") {
                ChessMentorBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = {
                        currentScreen = it
                        Log.d(TAG, "Navigate to: $it")
                    }
                )
            }
        }
    ) { paddingValues ->
        NavigationHost(
            paddingValues = paddingValues,
            currentScreen = currentScreen,
            currentUser = currentUser,
            gameViewModel = gameViewModel,
            container = container,
            message = message,
            isLoading = isLoading,
            regNickname = regNickname,
            regEmail = regEmail,
            regPassword = regPassword,
            regRating = regRating,
            loginEmail = loginEmail,
            loginPassword = loginPassword,
            onScreenChange = {
                currentScreen = it
                Log.d(TAG, "Screen changed to: $it")
            },
            onUserChange = { currentUser = it },
            onMessageChange = { message = it },
            onMessageDismiss = { message = "" },
            onRegNicknameChange = { regNickname = it },
            onRegEmailChange = { regEmail = it },
            onRegPasswordChange = { regPassword = it },
            onRegRatingChange = { regRating = it },
            onLoginEmailChange = { loginEmail = it },
            onLoginPasswordChange = { loginPassword = it },
            onRegister = { nick, email, pass, rating ->
                coroutineScope.launch {
                    isLoading = true
                    try {
                        val result = container.registerUserUseCase.execute(
                            RegisterUserUseCase.Input(nick, email, pass, rating)
                        )
                        if (result is RegisterUserUseCase.Result.Success) {
                            currentUser = result.user
                            message = "✅ Регистрация успешна!"
                        } else if (result is RegisterUserUseCase.Result.Error) {
                            message = "❌ ${result.message}"
                        }
                    } catch (e: Exception) {
                        message = "❌ Ошибка: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            onLogin = { email, pass ->
                coroutineScope.launch {
                    isLoading = true
                    try {
                        val result = container.loginUserUseCase.execute(
                            LoginUserUseCase.Input(email, pass)
                        )
                        if (result is LoginUserUseCase.Result.Success) {
                            currentUser = result.user
                            message = "✅ Вход выполнен!"
                        } else if (result is LoginUserUseCase.Result.Error) {
                            message = "❌ ${result.message}"
                        }
                    } catch (e: Exception) {
                        message = "❌ Ошибка: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChessMentorTopBar(
    currentScreen: String,
    currentUser: User?,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (currentScreen) {
                    "games" -> Icons.Default.List
                    "upload" -> Icons.Default.Upload
                    "game_view" -> Icons.Default.Search
                    "summary" -> Icons.Default.Insights
                    "training" -> Icons.Default.FitnessCenter
                    "stats" -> Icons.Default.Analytics
                    "settings" -> Icons.Default.Settings
                    else -> Icons.Default.SportsEsports
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = when (currentScreen) {
                        "games" -> "Мои партии"
                        "upload" -> "Загрузить партию"
                        "game_view" -> "Анализ партии"
                        "summary" -> "Сводка анализа"
                        "training" -> "Тренировка"
                        "stats" -> "Статистика"
                        "settings" -> "Настройки"
                        else -> "Шахматный Ментор"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            if (currentScreen != "home" && currentUser != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun ChessMentorBottomBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
            label = { Text("Главная") },
            selected = currentScreen == "home",
            onClick = { onNavigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "Партии") },
            label = { Text("Партии") },
            selected = currentScreen == "games",
            onClick = { onNavigate("games") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Тренировка") },
            label = { Text("Тренировка") },
            selected = currentScreen == "training",
            onClick = { onNavigate("training") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Upload, contentDescription = "Загрузить") },
            label = { Text("Загрузить") },
            selected = currentScreen == "upload",
            onClick = { onNavigate("upload") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Статистика") },
            label = { Text("Статистика") },
            selected = currentScreen == "stats",
            onClick = { onNavigate("stats") }
        )
    }
}

@Composable
private fun NavigationHost(
    paddingValues: PaddingValues,
    currentScreen: String,
    currentUser: User?,
    gameViewModel: GameViewModel,
    container: AppContainer,
    message: String,
    isLoading: Boolean,
    regNickname: String,
    regEmail: String,
    regPassword: String,
    regRating: String,
    loginEmail: String,
    loginPassword: String,
    onScreenChange: (String) -> Unit,
    onUserChange: (User?) -> Unit,
    onMessageChange: (String) -> Unit,
    onMessageDismiss: () -> Unit,
    onRegNicknameChange: (String) -> Unit,
    onRegEmailChange: (String) -> Unit,
    onRegPasswordChange: (String) -> Unit,
    onRegRatingChange: (String) -> Unit,
    onLoginEmailChange: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onRegister: (String, String, String, Int) -> Unit,
    onLogin: (String, String) -> Unit
) {
    when (currentScreen) {
        "home" -> HomeScreen(
            paddingValues = paddingValues,
            currentUser = currentUser,
            gameViewModel = gameViewModel,
            message = message,
            isLoading = isLoading,
            onNavigateToSettings = { onScreenChange("settings") },
            onMessageDismiss = onMessageDismiss,
            onLogout = { onUserChange(null) },
            onRegister = onRegister,
            onLogin = onLogin,
            regNickname = regNickname,
            regEmail = regEmail,
            regPassword = regPassword,
            regRating = regRating,
            loginEmail = loginEmail,
            loginPassword = loginPassword,
            onRegNicknameChange = onRegNicknameChange,
            onRegEmailChange = onRegEmailChange,
            onRegPasswordChange = onRegPasswordChange,
            onRegRatingChange = onRegRatingChange,
            onLoginEmailChange = onLoginEmailChange,
            onLoginPasswordChange = onLoginPasswordChange
        )

        "games" -> GamesListScreen(
            paddingValues = paddingValues,
            gameViewModel = gameViewModel,
            container = container,
            onGameClick = { game ->
                gameViewModel.selectGame(game)
                onScreenChange("summary")
            }
        )

        "game_view" -> {
            val game = gameViewModel.selectedGame.value
            if (game != null) {
                GameViewScreen(
                    paddingValues = paddingValues,
                    gameViewModel = gameViewModel,
                    user = currentUser
                )
            } else {
                // Если игра не выбрана, возвращаемся к списку
                LaunchedEffect(Unit) {
                    onScreenChange("games")
                }
            }
        }

        "summary" -> {
            val game = gameViewModel.selectedGame.value

            if (game != null) {
                // ✅ ВАЖНО: Данные берём напрямую из GameViewModel
                val mistakes = gameViewModel.selectedGameMistakes
                val analyzedMoves = gameViewModel.selectedGameAnalyzedMoves
                val evaluations = gameViewModel.selectedGameEvaluations

                Log.d(TAG, "SummaryScreen: game=${game.id}, " +
                        "analyzedMoves=${analyzedMoves.size}, " +
                        "evaluations=${evaluations.size}")

                // ✅ ИСПРАВЛЕНО: BoardViewModel создаётся только когда есть данные
                val boardViewModel = remember(game.id, analyzedMoves.size) {
                    Log.d(TAG, "Creating BoardViewModel with ${analyzedMoves.size} moves")
                    BoardViewModel().apply {
                        loadGame(
                            game = game,
                            gameMistakes = mistakes.toList(),
                            gameAnalyzedMoves = analyzedMoves.toList(),
                            gameEvaluations = evaluations.toList()
                        )
                    }
                }

                // ✅ ИСПРАВЛЕНО: keyMoments пересчитываются при изменении analyzedMoves
                val keyMoments = remember(analyzedMoves.size) {
                    boardViewModel.getKeyMoments().also {
                        Log.d(TAG, "KeyMoments: ${it.size}")
                    }
                }

                SummaryScreen(
                    paddingValues = paddingValues,
                    game = game,
                    keyMoments = keyMoments,
                    boardViewModel = boardViewModel,
                    gameViewModel = gameViewModel,
                    onReviewClick = {
                        onScreenChange("game_view")
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.w(TAG, "SummaryScreen: No game selected")
                    onScreenChange("games")
                }
            }
        }

        "upload" -> UploadGameScreen(
            paddingValues = paddingValues,
            gameViewModel = gameViewModel,
            engineSettingsRepository = container.engineSettingsRepository,  // ✅ НОВОЕ
            onGameUploaded = { },
            onAnalysisComplete = { onScreenChange("summary") },
            onOpenEngineSettings = { onScreenChange("engine_settings") }  // ✅ НОВОЕ
        )

        "training" -> TrainingScreen(
            paddingValues = paddingValues,
            container = container,
            userId = currentUser?.id ?: 0L
        )

        "stats" -> {
            currentUser?.let { user ->
                StatisticsScreen(
                    paddingValues = paddingValues,
                    user = user,
                    container = container
                )
            } ?: run {
                // Если пользователь не авторизован
                LaunchedEffect(Unit) {
                    onScreenChange("home")
                }
            }
        }

        "settings" -> {
            currentUser?.let { user ->
                SettingsScreen(
                    paddingValues = paddingValues,
                    user = user,
                    container = container,
                    onLogout = {
                        onUserChange(null)
                        onScreenChange("home")
                    }
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    onScreenChange("home")
                }
            }
        }

        "engine_settings" -> {
            EngineSettingsScreen(
                paddingValues = paddingValues,
                engineSettingsRepository = container.engineSettingsRepository,
                onBack = { onScreenChange("upload") }
            )
        }
    }
}