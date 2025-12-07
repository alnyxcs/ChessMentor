// presentation/ui/navigation/ChessMentorNavigation.kt
package com.example.chessmentor.presentation.ui.navigation

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
import com.example.chessmentor.presentation.viewmodel.BoardViewModel
import com.example.chessmentor.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import com.example.chessmentor.presentation.ui.screen.*

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
        currentUser?.let { gameViewModel.setUser(it) }
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
                }
            )
        },
        bottomBar = {
            if (currentUser != null && currentScreen != "game_view") {
                ChessMentorBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
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
            onScreenChange = { currentScreen = it },
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
            }
        }


        "stats" -> {
            currentUser?.let { user ->
                StatisticsScreen(
                    paddingValues = paddingValues,
                    user = user,
                    container = container
                )
            }
        }

        "training" -> TrainingScreen(
            paddingValues = paddingValues,
            container = container,
            userId = currentUser?.id ?: 0L
        )

        "summary" -> {
            val game = gameViewModel.selectedGame.value
            if (game != null) {
                val evaluations = gameViewModel.getLatestEvaluations()
                val boardViewModel = remember(game.id) {
                    BoardViewModel().apply {
                        loadGame(
                            game = game,
                            gameMistakes = gameViewModel.selectedGameMistakes,
                            realEvaluations = evaluations
                        )
                    }
                }
                val keyMoments = boardViewModel.getKeyMoments()

                SummaryScreen(
                    paddingValues = paddingValues,
                    game = game,
                    keyMoments = keyMoments,
                    boardViewModel = boardViewModel,
                    onReviewClick = { onScreenChange("game_view") }
                )
            }
        }

        "upload" -> UploadGameScreen(
            paddingValues = paddingValues,
            gameViewModel = gameViewModel,
            onGameUploaded = { onScreenChange("summary") }
        )

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
            }
        }
    }
}