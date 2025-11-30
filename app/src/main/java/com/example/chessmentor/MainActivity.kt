package com.example.chessmentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.*
import com.example.chessmentor.domain.usecase.*
import com.example.chessmentor.presentation.viewmodel.GameViewModel
import com.example.chessmentor.presentation.viewmodel.BoardViewModel
import com.example.chessmentor.presentation.viewmodel.StatisticsViewModel
import com.example.chessmentor.presentation.ui.components.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.chessmentor.presentation.viewmodel.TrainingViewModel
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.launch
import com.example.chessmentor.presentation.ui.theme.ChessMentorTheme
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.chessmentor.presentation.ui.util.getIcon // Импорт
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.example.chessmentor.presentation.viewmodel.SettingsViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.ArrowForward
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Передаем контекст в AppContainer
        val container = AppContainer.getInstance(applicationContext)
        val gameViewModel = GameViewModel(container)

        setContent {
            ChessMentorTheme { // <-- Наша новая тема
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChessMentorApp(container, gameViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessMentorApp(
    container: AppContainer,
    gameViewModel: GameViewModel
) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentScreen by remember { mutableStateOf("home") }
    var isLoading by remember { mutableStateOf(false) }

    // Форма регистрации
    var regNickname by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRating by remember { mutableStateOf("1200") }

    // Форма входа
    var loginEmail by remember { mutableStateOf("test@chessmentor.ru") }
    var loginPassword by remember { mutableStateOf("test123") }

    var message by remember { mutableStateOf("") }

    // Корутина для выполнения асинхронных операций
    val coroutineScope = rememberCoroutineScope()

    // Синхронизация пользователя с ViewModel
    LaunchedEffect(currentUser) {
        currentUser?.let { gameViewModel.setUser(it) }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Выбираем иконку для текущего экрана
                            val icon = when(currentScreen) {
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
                                text = when(currentScreen) {
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
                            IconButton(onClick = {
                                if (currentScreen == "game_view") {
                                    currentScreen = "summary"
                                } else if (currentScreen == "summary") {
                                    currentScreen = "games"
                                    gameViewModel.closeGameView()
                                } else {
                                    currentScreen = "home"
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background, // <-- Прозрачный фон
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (currentUser != null && currentScreen != "game_view") {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                            label = { Text("Главная") },
                            selected = currentScreen == "home",
                            onClick = { currentScreen = "home" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.List, contentDescription = "Партии") },
                            label = { Text("Партии") },
                            selected = currentScreen == "games",
                            onClick = { currentScreen = "games" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Тренировка") },
                            label = { Text("Тренировка") },
                            selected = currentScreen == "training",
                            onClick = { currentScreen = "training" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Upload, contentDescription = "Загрузить") },
                            label = { Text("Загрузить") },
                            selected = currentScreen == "upload",
                            onClick = { currentScreen = "upload" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Analytics, contentDescription = "Статистика") },
                            label = { Text("Статистика") },
                            selected = currentScreen == "stats",
                            onClick = { currentScreen = "stats" }
                        )

                    }
                }
            }
        ) { paddingValues ->
            // ... содержимое (when currentScreen) остается без изменений ...
            // Скопируйте его из вашего кода

            // Главный контент
            when (currentScreen) {
                "home" -> HomeScreen(
                    paddingValues = paddingValues,
                    currentUser = currentUser,
                    gameViewModel = gameViewModel, // <-- ДОБАВЛЕНО
                    message = message,
                    isLoading = isLoading,
                    onNavigateToSettings = { currentScreen = "settings" }, // <-- ДОБАВИТЬ
                    onMessageDismiss = { message = "" },
                    onLogout = { currentUser = null },
                    onRegister = { nick, email, pass, rating ->
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val result = container.registerUserUseCase.execute(RegisterUserUseCase.Input(nick, email, pass, rating))
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
                                val result = container.loginUserUseCase.execute(LoginUserUseCase.Input(email, pass))
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
                    },
                    regNickname = regNickname,
                    regEmail = regEmail,
                    regPassword = regPassword,
                    regRating = regRating,
                    loginEmail = loginEmail,
                    loginPassword = loginPassword,
                    onRegNicknameChange = { regNickname = it },
                    onRegEmailChange = { regEmail = it },
                    onRegPasswordChange = { regPassword = it },
                    onRegRatingChange = { regRating = it },
                    onLoginEmailChange = { loginEmail = it },
                    onLoginPasswordChange = { loginPassword = it }
                )

                "games" -> GamesListScreen(
                    paddingValues = paddingValues,
                    gameViewModel = gameViewModel,
                    container = container,
                    onGameClick = { game ->
                        gameViewModel.selectGame(game)
                        currentScreen = "summary"
                    }
                )

                "game_view" -> GameViewScreen(
                    paddingValues = paddingValues,
                    gameViewModel = gameViewModel,
                    user = currentUser // <-- ПЕРЕДАЕМ ПОЛЬЗОВАТЕЛЯ
                )
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

                "summary" -> { // <-- НОВЫЙ ЭКРАН
                    val game = gameViewModel.selectedGame.value
                    if (game != null) {
                        val boardViewModel = remember(game.id) {
                            BoardViewModel().apply {
                                loadGame(game, gameViewModel.selectedGameMistakes)
                            }
                        }
                        val keyMoments = boardViewModel.getKeyMoments()

                        SummaryScreen(
                            paddingValues = paddingValues,
                            game = game,
                            keyMoments = keyMoments,
                            boardViewModel = boardViewModel,
                            onReviewClick = { currentScreen = "game_view" }
                        )
                    }
                }

                "upload" -> UploadGameScreen(
                    paddingValues, gameViewModel,
                    onGameUploaded = { currentScreen = "summary" }
                )



                "settings" -> {
                    currentUser?.let { user ->
                        SettingsScreen(
                            paddingValues = paddingValues,
                            user = user,
                            container = container,
                            onLogout = {
                                currentUser = null
                                currentScreen = "home"
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== HOME SCREEN ====================
// ==================== HOME SCREEN (ГЛАВНЫЙ) ====================
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    currentUser: User?,
    gameViewModel: GameViewModel, // <-- Новый параметр
    onNavigateToSettings: () -> Unit,
    message: String,
    isLoading: Boolean,
    onMessageDismiss: () -> Unit,
    onLogout: () -> Unit,
    onRegister: (String, String, String, Int) -> Unit,
    onLogin: (String, String) -> Unit,
    regNickname: String,
    regEmail: String,
    regPassword: String,
    regRating: String,
    loginEmail: String,
    loginPassword: String,
    onRegNicknameChange: (String) -> Unit,
    onRegEmailChange: (String) -> Unit,
    onRegPasswordChange: (String) -> Unit,
    onRegRatingChange: (String) -> Unit,
    onLoginEmailChange: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit
) {
    // Логика переключения: Вход или Дашборд
    if (currentUser == null) {
        AuthScreenContent(
            paddingValues, message, isLoading, onMessageDismiss,
            onRegister, onLogin, regNickname, regEmail, regPassword, regRating,
            loginEmail, loginPassword, onRegNicknameChange, onRegEmailChange,
            onRegPasswordChange, onRegRatingChange, onLoginEmailChange, onLoginPasswordChange
        )
    } else {
        // ...
        DashboardContent(
            paddingValues = paddingValues,
            user = currentUser,
            gameViewModel = gameViewModel,
            onLogout = onLogout,
            onNavigateToTraining = { /* пока пусто или переход на training */ },
            onNavigateToSettings = onNavigateToSettings // Передаем параметр из HomeScreen
        )
    }

}


// ==================== ЭКРАН ВХОДА (СТАРЫЙ HOME) ====================
@Composable
fun AuthScreenContent(
    paddingValues: PaddingValues,
    message: String,
    isLoading: Boolean,
    onMessageDismiss: () -> Unit,
    onRegister: (String, String, String, Int) -> Unit,
    onLogin: (String, String) -> Unit,
    regNickname: String,
    regEmail: String,
    regPassword: String,
    regRating: String,
    loginEmail: String,
    loginPassword: String,
    onRegNicknameChange: (String) -> Unit,
    onRegEmailChange: (String) -> Unit,
    onRegPasswordChange: (String) -> Unit,
    onRegRatingChange: (String) -> Unit,
    onLoginEmailChange: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf("login") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Приветствие
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Добро пожаловать!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Войдите, чтобы начать")
                    }
                }
            }
        }

        // Сообщение
        if (message.isNotEmpty()) {
            val isError = message.startsWith("❌") || message.startsWith("Ошибка")
            val cleanMessage = message.replace("✅ ", "").replace("❌ ", "")
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (isError) Icons.Default.Error else Icons.Default.CheckCircle, null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(cleanMessage, modifier = Modifier.weight(1f), color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
                        IconButton(onClick = onMessageDismiss) { Icon(Icons.Default.Close, null) }
                    }
                }
            }
        }

        // Табы и формы
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { currentTab = "login" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == "login") MaterialTheme.colorScheme.surface else Color.Transparent, contentColor = if (currentTab == "login") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant),
                    elevation = if (currentTab == "login") ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp),
                    enabled = !isLoading
                ) { Text("Вход") }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { currentTab = "register" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == "register") MaterialTheme.colorScheme.surface else Color.Transparent, contentColor = if (currentTab == "register") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant),
                    elevation = if (currentTab == "register") ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp),
                    enabled = !isLoading
                ) { Text("Регистрация") }
            }
        }

        // Поля ввода
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (currentTab == "login") {
                        Text("С возвращением!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = loginEmail, onValueChange = onLoginEmailChange, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = loginPassword, onValueChange = onLoginPasswordChange, label = { Text("Пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        Button(onClick = { onLogin(loginEmail, loginPassword) }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && loginEmail.isNotBlank() && loginPassword.isNotBlank()) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Войти", fontSize = 16.sp)
                        }
                    } else {
                        Text("Создать аккаунт", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = regNickname, onValueChange = onRegNicknameChange, label = { Text("Никнейм") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = regEmail, onValueChange = onRegEmailChange, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = regPassword, onValueChange = onRegPasswordChange, label = { Text("Пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = regRating, onValueChange = onRegRatingChange, label = { Text("Рейтинг") }, leadingIcon = { Icon(Icons.Default.Star, null) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        Button(onClick = { onRegister(regNickname, regEmail, regPassword, regRating.toIntOrNull() ?: 1200) }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && regNickname.isNotBlank()) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Зарегистрироваться", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
// Остальные компоненты (GamesListScreen, UploadGameScreen и т.д.) остаются без изменений
// ==================== GAMES LIST SCREEN ====================
@Composable
fun GamesListScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel,
    container: AppContainer,
    onGameClick: (Game) -> Unit
) {
    val games = gameViewModel.userGames
    val message by gameViewModel.message

    // Создаем состояние для хранения ошибок по играм
    val mistakesByGame = remember { mutableStateMapOf<Long, List<Mistake>>() }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Заголовок
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Мои партии",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Всего партий: ${games.size}")
                }
            }
        }

        // Сообщение
        if (message.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(message, modifier = Modifier.weight(1f))
                        TextButton(onClick = { gameViewModel.clearMessage() }) {
                            Text("✕")
                        }
                    }
                }
            }
        }

        // Список партий
        if (games.isEmpty()) {
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Партий пока нет",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Загрузите свою первую партию!")
                    }
                }
            }
        } else {
            items(games) { game ->

                val gameMistakes = mistakesByGame[game.id] ?: emptyList<Mistake>()
                var isLoading by remember { mutableStateOf(game.id !in mistakesByGame) }
                // Получаем ошибки для текущей партии
                // Загружаем ошибки, если их еще нет
                LaunchedEffect(game.id) {
                    if (game.id != null && game.id !in mistakesByGame) {
                        isLoading = true
                        try {
                            val mistakes = container.mistakeRepository.findByGameId(game.id)
                            mistakesByGame[game.id] = mistakes
                        } catch (e: Exception) {
                            // Обработка ошибки
                        } finally {
                            isLoading = false
                        }
                    }
                }

                GameCard(
                    game = game,
                    mistakes = gameMistakes,
                    onClick = { onGameClick(game) },
                    onDelete = { gameViewModel.deleteGame(game.id!!) }
                )
            }
        }
    }
}

@Composable
fun GameCard(
    game: Game,
    mistakes: List<Mistake>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Используем Surface для чистого вида с легкой тенью
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Небольшой отступ между карточками
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp, // Легкая тень
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Заголовок: Цвет, Номер, Статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Иконка цвета (квадратик)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (game.playerColor == ChessColor.WHITE) Color.White else Color.Black,
                                MaterialTheme.shapes.extraSmall
                            )
                            .border(1.dp, Color.Gray, MaterialTheme.shapes.extraSmall)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Партия #${game.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Статус (Chip)
                StatusChip(game.analysisStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Метаданные: Контроль времени
            Text(
                text = "Контроль: ${game.timeControl ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 3. Результаты анализа (если есть)
            if (game.isAnalyzed()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Мини-график
                MiniEvaluationGraph(
                    game = game,
                    mistakes = mistakes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Точность и Ошибки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Точность (крупно)
                    AccuracyBadge(accuracy = game.accuracy?.toInt() ?: 0)

                    // Ошибки (мелко)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MistakeCountSmall(MistakeType.BLUNDER, game.blundersCount)
                        MistakeCountSmall(MistakeType.MISTAKE, game.mistakesCount)
                        MistakeCountSmall(MistakeType.INACCURACY, game.inaccuraciesCount)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: AnalysisStatus) {
    val color = when (status) {
        AnalysisStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        AnalysisStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        AnalysisStatus.PENDING -> MaterialTheme.colorScheme.secondary
        AnalysisStatus.FAILED -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = "${status.getIcon()} ${status.getDisplayName()}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = Color.White
        )
    }
}

// ==================== UPLOAD GAME SCREEN ====================
@Composable
fun UploadGameScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel,
    onGameUploaded: () -> Unit
) {
    val pgnInput by gameViewModel.pgnInput
    val selectedColor by gameViewModel.selectedColor
    val timeControl by gameViewModel.timeControl
    val message by gameViewModel.message
    val isLoading by gameViewModel.isLoading

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Заголовок
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload, // Если нет, используйте Icons.Default.Upload
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Загрузить партию",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Вставьте PGN для анализа",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Сообщение
        if (message.isNotEmpty()) {
            val isError = message.startsWith("❌") || message.startsWith("Ошибка")
            // Очищаем текст от старых эмодзи, если они приходят из ViewModel
            val cleanMessage = message.replace("✅ ", "").replace("❌ ", "")

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cleanMessage,
                            modifier = Modifier.weight(1f),
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (!isLoading) {
                            IconButton(onClick = { gameViewModel.clearMessage() }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }
                    }
                }
            }
        }

        // Форма
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // PGN input
                    OutlinedTextField(
                        value = pgnInput,
                        onValueChange = { gameViewModel.pgnInput.value = it },
                        label = { Text("PGN партии") },
                        placeholder = { Text("1. e4 e5 2. Nf3 Nc6 ...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10,
                        enabled = !isLoading,
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                    )

                    // Выбор цвета
                    Text("Ваш цвет:", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedColor == ChessColor.WHITE,
                            onClick = { gameViewModel.selectedColor.value = ChessColor.WHITE },
                            label = { Text("Белые") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.border(1.dp, Color.Gray, androidx.compose.foundation.shape.CircleShape)
                                )
                            },
                            enabled = !isLoading
                        )
                        FilterChip(
                            selected = selectedColor == ChessColor.BLACK,
                            onClick = { gameViewModel.selectedColor.value = ChessColor.BLACK },
                            label = { Text("Чёрные") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            },
                            enabled = !isLoading
                        )
                    }

                    // Контроль времени (опционально)
                    OutlinedTextField(
                        value = timeControl,
                        onValueChange = { gameViewModel.timeControl.value = it },
                        label = { Text("Контроль времени (опционально)") },
                        placeholder = { Text("5+3, 10+0, 15+10") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) }
                    )

                    // Кнопка загрузки
                    Button(
                        onClick = { gameViewModel.uploadGame() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isLoading && pgnInput.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Анализируем...")
                        } else {
                            Icon(Icons.Default.Analytics, contentDescription = null) // Или AutoGraph
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Загрузить и анализировать")
                        }
                    }

                    // Подсказка
                    Divider()

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Пример PGN:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== GAME VIEW SCREEN ====================
@Composable
fun GameViewScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel,
    user: User?
) {
    val game by gameViewModel.selectedGame
    val mistakes = gameViewModel.selectedGameMistakes

    val context = LocalContext.current
    val boardViewModel = remember { BoardViewModel().apply { soundManager = SoundManager(context) } }
    val selectedTheme = remember(user?.preferredTheme) {
        if (user != null) BoardThemes.getAll().find { it.name == user.preferredTheme } ?: BoardThemes.Classic else BoardThemes.Classic
    }

    DisposableEffect(Unit) { onDispose { boardViewModel.soundManager?.release() } }
    LaunchedEffect(game?.id) { game?.let { boardViewModel.loadGame(it, mistakes) } }

    if (game == null) return

    val board by boardViewModel.board
    val currentMoveIndex by boardViewModel.currentMoveIndex
    val currentEvaluation = boardViewModel.getCurrentEvaluation()
    val currentMoveNotation = boardViewModel.getCurrentMoveNotation()
    val highlightedSquares by boardViewModel.highlightedSquares
    val lastMove by boardViewModel.lastMove
    val currentMistake = boardViewModel.getCurrentMistake()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. ДОСКА (Максимально возможная)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Квадрат
                .padding(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Полоска оценки
                EvaluationBar(
                    evaluation = currentEvaluation,
                    modifier = Modifier.width(24.dp).fillMaxHeight().padding(end = 4.dp)
                )

                // Доска
                ChessBoard(
                    board = board,
                    highlightedSquares = highlightedSquares,
                    lastMove = lastMove,
                    modifier = Modifier.fillMaxSize(),
                    flipped = game!!.playerColor == com.example.chessmentor.domain.entity.ChessColor.BLACK,
                    theme = selectedTheme,
                    animateMove = true
                )
            }
        }

        // 2. ПАНЕЛЬ УПРАВЛЕНИЯ (Компактная)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Ход
                Text(
                    text = currentMoveNotation,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Используем большие иконки для удобства
                    IconButton(onClick = { boardViewModel.goToStart() }) { Icon(Icons.Default.FirstPage, null, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = { boardViewModel.goToPreviousMove() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = { boardViewModel.goToNextMove() }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = { boardViewModel.goToEnd() }) { Icon(Icons.Default.LastPage, null, modifier = Modifier.size(32.dp)) }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. АНАЛИЗ ТЕКУЩЕГО ХОДА (Только он!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Занимает всё место внизу
                .padding(horizontal = 12.dp)
        ) {
            if (currentMistake != null) {
                // Если ошибка - показываем карточку
                MistakeCard(currentMistake)
            } else {
                // Если нет ошибки - показываем нейтральное сообщение или оценку
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF81B64C),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Хороший ход",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Оценка: ${formatEvaluation(currentEvaluation)}",
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
// ==================== SUMMARY SCREEN ====================
@Composable
fun SummaryScreen(
    paddingValues: PaddingValues,
    game: Game,
    keyMoments: List<KeyMoment>,
    boardViewModel: BoardViewModel,
    onReviewClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Сводка анализа", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Партия #${game.id}", color = Color.Gray)
                }
            }
        }
        item {
            KeyMomentsCard(
                keyMoments = keyMoments,
                onMomentClick = { moment ->
                    boardViewModel.goToMove(moment.moveIndex)
                    onReviewClick()
                }
            )
        }
        item {
            Button(
                onClick = onReviewClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔬 Перейти к разбору партии", fontSize = 16.sp)
            }
        }
        item {
            val evaluations = boardViewModel.evaluations.toList()
            EvaluationGraph(
                evaluations = evaluations,
                currentMoveIndex = -1,
                keyMoments = keyMoments,
                onMomentClick = {}
            )
        }
        if (game.isAnalyzed()) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Статистика партии", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        // ...
                    }
                }
            }
        }
    }
}

@Composable
fun KeyMomentsCard(
    keyMoments: List<KeyMoment>,
    onMomentClick: (KeyMoment) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ключевые моменты",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            // Сетка 2x3 (Brilliant, Great, Best / Mistake, Blunder, Missed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyMomentItemLarge("Brilliant", keyMoments.count { it.quality == MoveQuality.BRILLIANT })
                KeyMomentItemLarge("Great", keyMoments.count { it.quality == MoveQuality.GREAT_MOVE })
                KeyMomentItemLarge("Best", keyMoments.count { it.quality == MoveQuality.BEST_MOVE })
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyMomentItemLarge("Mistake", keyMoments.count { it.quality == MoveQuality.MISTAKE })
                KeyMomentItemLarge("Blunder", keyMoments.count { it.quality == MoveQuality.BLUNDER })
                KeyMomentItemLarge("Inaccuracy", keyMoments.count { it.quality == MoveQuality.INACCURACY })
            }

            // Список моментов (если есть)
            if (keyMoments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Быстрый обзор:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                keyMoments.take(5).forEach { moment ->
                    KeyMomentRow(moment, onMomentClick)
                }
            }
        }
    }
}

@Composable
fun KeyMomentItemLarge(label: String, count: Int) {
    val color = when (label) {
        "Brilliant" -> Color(0xFF26C6DA)
        "Great" -> Color(0xFF66BB6A)
        "Best" -> Color(0xFF9CCC65)
        "Mistake" -> Color(0xFFFFCA28)
        "Blunder" -> Color(0xFFEF5350)
        "Inaccuracy" -> Color(0xFFFF7043)
        else -> Color.Gray
    }

    val icon = when(label) {
        "Brilliant" -> Icons.Default.Diamond
        "Great" -> Icons.Default.ThumbUp
        "Best" -> Icons.Default.Star
        "Mistake" -> Icons.Default.Error
        "Blunder" -> Icons.Default.Cancel
        "Inaccuracy" -> Icons.Default.Bolt
        else -> Icons.Default.Circle
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (count > 0) color else Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )

        Text(
            text = "$count",
            fontSize = 24.sp, // Вместо style
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else Color.Gray.copy(alpha = 0.5f)
        )

        Text(
            text = label,
            fontSize = 12.sp, // Вместо style
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun KeyMomentRow(moment: KeyMoment, onClick: (KeyMoment) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(moment) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка качества
        Surface(
            color = moment.quality.getColor().copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                imageVector = moment.quality.getIcon(),
                contentDescription = null,
                tint = moment.quality.getColor(),
                modifier = Modifier.size(20.dp).padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Текст хода
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ход ${moment.moveIndex / 2 + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = moment.san,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Оценка
        Text(
            text = formatEvaluation(moment.evaluationChange),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (moment.evaluationChange > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
        )

        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}



@Composable
fun MistakeTypeCount(type: MistakeType, count: Int) {
    if (count > 0) {
        val color = when (type) {
            MistakeType.BLUNDER -> Color(0xFFD32F2F)
            MistakeType.MISTAKE -> Color(0xFFF57C00)
            MistakeType.INACCURACY -> Color(0xFFFBC02D)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = type.getIcon(),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MistakeCard(mistake: Mistake) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (mistake.mistakeType) {
                MistakeType.BLUNDER -> Color(0xFFFFEBEE)
                MistakeType.MISTAKE -> Color(0xFFFFF3E0)
                MistakeType.INACCURACY -> Color(0xFFFFFDE7)
            }
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок ошибки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Определяем цвет иконки
                    val iconColor = when (mistake.mistakeType) {
                        MistakeType.BLUNDER -> Color(0xFFD32F2F)
                        MistakeType.MISTAKE -> Color(0xFFF57C00)
                        MistakeType.INACCURACY -> Color(0xFFFBC02D)
                    }

                    Icon(
                        imageVector = mistake.mistakeType.getIcon(),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ход ${mistake.moveNumber}: ${mistake.mistakeType.getDescription()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "-${mistake.getEvaluationLossInPawns()}",
                    fontSize = 14.sp,
                    color = Color(0xFFD32F2F), // Red
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ходы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Ваш ход:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = mistake.userMove,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F) // Red
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Лучше:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = mistake.bestMove,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32) // Green
                    )
                }
            }

            // Комментарий
            if (mistake.comment != null) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mistake.comment,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
// ==================== TRAINING SCREEN ====================
@Composable
fun TrainingScreen(
    paddingValues: PaddingValues,
    container: AppContainer,
    userId: Long
) {
    // Создаем ViewModel
    // ВАЖНО: используем remember, чтобы пережить рекомпозиции, но viewModel должен жить дольше.
    // В реальном проекте лучше использовать viewModel() из androidx.lifecycle.viewmodel.compose
    // Но для нашей архитектуры с ручным DI:
    val viewModel = remember { TrainingViewModel(container) }

    // Загружаем данные пользователя при старте
    LaunchedEffect(userId) {
        // Получаем пользователя из репозитория (синхронно или через корутину внутри VM)
        // Для простоты, предположим, что мы передадим объект User или загрузим его в VM
        // Здесь мы сделаем финт: загрузим user через container прямо тут
        val user = container.userRepository.findById(userId)
        if (user != null) {
            viewModel.init(user)
        }
    }

    val exercise by viewModel.currentExercise
    val board by viewModel.boardState
    val message by viewModel.message
    val user by viewModel.currentUser
    val isSolved by viewModel.isSolved

    var selectedSquare by remember { mutableStateOf<Square?>(null) }

    if (exercise == null) {
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            if (message != null) {
                Text(message!!, modifier = Modifier.padding(16.dp))
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Инфо о пользователе и рейтинге
        if (user != null) {
            Text(
                text = "Ваш рейтинг: ${user!!.rating}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Карточка задания
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Задача #${exercise!!.id}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = exercise!!.prompt)

                Spacer(modifier = Modifier.height(8.dp))

                val sideText = if (board.sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE) "⚪ Белых" else "⚫ Черных"
                Text("Ход: $sideText", fontWeight = FontWeight.Bold)
            }
        }

        // Доска
        ChessBoard(
            board = board,
            onSquareClick = { square ->
                // Логика UI для выбора клетки (можно вынести в VM, но для кликов оставить тут)
                if (selectedSquare == null) {
                    if (board.getPiece(square).pieceSide == board.sideToMove) {
                        selectedSquare = square
                    }
                } else {
                    if (square == selectedSquare) {
                        selectedSquare = null
                    } else {
                        // Пытаемся сделать ход
                        val move = Move(selectedSquare!!, square)
                        viewModel.onMoveMade(move) // Отправляем во ViewModel
                        selectedSquare = null
                    }
                }
            },
            highlightedSquares = if (selectedSquare != null) setOf(selectedSquare!!) else emptySet(),
            flipped = board.sideToMove == com.github.bhlangonijr.chesslib.Side.BLACK
        )

        // Сообщение о результате
        if (message != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSolved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message!!,
                    modifier = Modifier.padding(16.dp),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Кнопки управления
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isSolved) {
                Button(onClick = { viewModel.loadNextExercise() }) {
                    Text("Следующая задача ➡️")
                }
            } else {
                Button(
                    onClick = { viewModel.resetPosition() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Сбросить")
                }

                Button(
                    onClick = { /* Подсказка */ }
                ) {
                    Text("Подсказка")
                }
            }
        }
    }
}
// ==================== STATISTICS SCREEN ====================
// ==================== STATISTICS SCREEN ====================
@Composable
fun StatisticsScreen(
    paddingValues: PaddingValues,
    user: User,
    container: AppContainer
) {
    val viewModel = remember { StatisticsViewModel(container) }

    LaunchedEffect(user.id) {
        viewModel.loadStatistics(user)
    }

    val stats by viewModel.statistics
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Заголовок
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertChart, // Или Insights
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Моя статистика", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Прогресс обучения", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Загрузка или ошибка
        if (isLoading) {
            item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        if (errorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Основная статистика
        stats?.let { s ->
            item {
                StatCard(
                    icon = Icons.Default.TrendingUp,
                    title = "Рейтинг",
                    value = "${s.currentRating}",
                    subValue = if (s.ratingChange >= 0) "+${s.ratingChange}" else "${s.ratingChange}",
                    subValueColor = if (s.ratingChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            item {
                StatCard(
                    icon = Icons.Default.TrackChanges, // Или LocationSearching
                    title = "Точность",
                    value = "${s.averageAccuracy.toInt()}%",
                    subValue = s.accuracyTrend
                )
            }
            item {
                StatCard(
                    icon = Icons.Default.SportsEsports, // Или Gamepad
                    title = "Партии",
                    value = "${s.analyzedGames}",
                    subValue = "из ${s.totalGames}"
                )
            }

            // Статистика по ошибкам
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Анализ ошибок", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            MistakeTypeCount(MistakeType.BLUNDER, s.blunders)
                            MistakeTypeCount(MistakeType.MISTAKE, s.mistakes)
                            MistakeTypeCount(MistakeType.INACCURACY, s.inaccuracies)
                        }
                    }
                }
            }

            // Проблемные темы
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.secondary) // Или Warning
                            Spacer(Modifier.width(8.dp))
                            Text("Проблемные темы", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        if (s.problemThemes.isEmpty()) {
                            Text("Пока нет данных", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            s.problemThemes.forEach { theme ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${theme.category}: ${theme.themeName}")
                                    Text("${theme.mistakeCount} раз", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // Рекомендации
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000))
                            Spacer(Modifier.width(8.dp))
                            Text("Рекомендации", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        s.recommendations.forEach { recommendation ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Text("•", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text(recommendation)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subValue: String = "",
    subValueColor: Color = Color.Gray
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            if (subValue.isNotEmpty()) {
                Text(subValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = subValueColor)
            }
        }
    }
}

@Composable
fun StatusChip(status: AnalysisStatus) {
    val (color, text) = when (status) {
        AnalysisStatus.COMPLETED -> Color(0xFF81B64C) to "Готово" // Зеленый
        AnalysisStatus.IN_PROGRESS -> Color(0xFFFFA726) to "Анализ..." // Оранжевый
        AnalysisStatus.PENDING -> Color.Gray to "Очередь"
        AnalysisStatus.FAILED -> Color(0xFFFA412D) to "Ошибка"
    }

    Surface(
        color = color.copy(alpha = 0.1f), // Прозрачный фон
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AccuracyBadge(accuracy: Int) {
    Column {
        Text(
            text = "Точность",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Text(
            text = "$accuracy%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (accuracy >= 90) Color(0xFF81B64C) else if (accuracy >= 70) Color(0xFFFFA726) else Color.Gray
        )
    }
}

@Composable
fun MistakeCountSmall(type: MistakeType, count: Int) {
    if (count > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(type.getEmoji(), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
// ==================== SETTINGS SCREEN ====================
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    user: User,
    container: AppContainer,
    onLogout: () -> Unit
) {
    val viewModel = remember { SettingsViewModel(container) }

    LaunchedEffect(user) {
        viewModel.init(user)
    }

    val tempNickname by viewModel.tempNickname
    val isSoundEnabled by viewModel.isSoundEnabled
    val selectedThemeName by viewModel.selectedThemeName
    val message by viewModel.message

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок
        item {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Сообщения
        if (message != null) {
            val isError = message!!.startsWith("❌") || message!!.startsWith("Ошибка")
            val cleanMessage = message!!.replace("✅ ", "").replace("❌ ", "")
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isError) Icons.Default.Error else Icons.Default.CheckCircle, null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text(cleanMessage, modifier = Modifier.weight(1f))
                        }
                        IconButton(onClick = { viewModel.clearMessage() }) { Icon(Icons.Default.Close, null) }
                    }
                }
            }
        }

        // Профиль
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Профиль", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempNickname,
                        onValueChange = { viewModel.tempNickname.value = it },
                        label = { Text("Никнейм") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(user.email, color = Color.Gray)
                    }
                }
            }
        }

        // Параметры
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Параметры", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Звук
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Звуковые эффекты")
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = { viewModel.isSoundEnabled.value = it }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Тема
                    Text("Тема доски по умолчанию", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    BoardThemes.getAll().chunked(2).forEach { rowThemes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowThemes.forEach { theme ->
                                val isSelected = selectedThemeName == theme.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectedThemeName.value = theme.name },
                                    label = { Text(theme.name) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Кнопка Сохранить
        item {
            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Сохранить изменения")
            }
        }

        // Кнопка Выход
        item {
            OutlinedButton(
                onClick = { viewModel.logout(onLogout) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Выйти из аккаунта")
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Версия 1.0.0 (Alpha)", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

// Вспомогательная функция для цвета
fun MistakeType.getComposeColor(): Color = when (this) {
    MistakeType.BLUNDER -> Color(0xFFD32F2F)
    MistakeType.MISTAKE -> Color(0xFFF57C00)
    MistakeType.INACCURACY -> Color(0xFFFBC02D)
}

// ==================== DASHBOARD CONTENT ====================
@Composable
fun DashboardContent(
    paddingValues: PaddingValues,
    user: User,
    gameViewModel: GameViewModel,
    onLogout: () -> Unit,
    onNavigateToTraining: () -> Unit, // Тип указан
    onNavigateToSettings: () -> Unit  // Тип указан

        )
    {
    LaunchedEffect(user.id) {
        gameViewModel.loadUserData()
    }

    val recentGames = gameViewModel.userGames.take(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(), // Убрали padding(paddingValues) отсюда
        verticalArrangement = Arrangement.spacedBy(16.dp),
        // ВАЖНО: Добавляем paddingValues сюда
        contentPadding = PaddingValues(
            top = 2.dp + paddingValues.calculateTopPadding(),
            bottom = 16.dp + paddingValues.calculateBottomPadding(),
            start = 16.dp,
            end = 16.dp
        )
    ) {
        // 1. ХЕДЕР
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Аватарка + Ник + Рейтинг
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.nickname.first().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(user.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text("${user.rating}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                IconButton(onClick = onNavigateToSettings) { // <-- ИСПОЛЬЗУЙТЕ КОЛБЭК
                    Icon(Icons.Default.Settings, null)}
            }
        }

        // 3. ЗАДАЧА ДНЯ
        item {
            DailyPuzzleWidget(onClick = onNavigateToTraining)
        }

        // 4. ПОСЛЕДНИЕ ПАРТИИ
        item {
            Text(
                "Недавние партии",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (recentGames.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.History, null, tint = Color.Gray)
                        Text("История пуста", color = Color.Gray)
                    }
                }
            }
        } else {
            items(recentGames) { game ->
                MiniGameCard(game)
            }
        }

        // 5. СОВЕТ
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Lightbulb, null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Совет: Контролируйте центр доски пешками в начале партии.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


@Composable
fun MiniGameCard(game: Game) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (game.playerColor == ChessColor.WHITE) Color.White else Color.Black)
                        .border(1.dp, Color.Gray)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Партия #${game.id}", fontWeight = FontWeight.Bold)
                    Text(game.timeControl ?: "Без контроля", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            if (game.isAnalyzed()) {
                Text(
                    "${game.accuracy?.toInt() ?: 0}%",
                    fontWeight = FontWeight.Bold,
                    color = if ((game.accuracy ?: 0.0) > 80) Color(0xFF4CAF50) else Color(0xFFFFA726)
                )
            } else {
                Text("В очереди", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DailyPuzzleWidget(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(40.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Задача дня", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Сложная",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Мини-доска (Неинтерактивная, просто картинка ситуации)
            // Используем FEN известной задачи
            val fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
            val board = remember { com.github.bhlangonijr.chesslib.Board().apply { loadFromFen(fen) } }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f) // Широкая, но не высокая
                    .background(Color.LightGray.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ) {
                // Мы хитро обрезаем доску или показываем её целиком, но маленькую
                // Для простоты покажем целиком, но отключим клики (так как клик идет на карточку)
                ChessBoard(
                    board = board,
                    modifier = Modifier.fillMaxSize(),
                    // Отключаем интерактивность, передавая null в onSquareClick
                    onSquareClick = null
                )

                // Наложение "Решить"
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onClick) {
                        Text("Решить сейчас")
                    }
                }
            }


        }
    }
}

@Composable
fun StatsSummaryRow(user: User) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Рейтинг
        SummaryItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingUp,
            value = "${user.rating}",
            label = "Рейтинг",
            color = MaterialTheme.colorScheme.primaryContainer
        )

        // Уровень
        SummaryItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.School, // Или Verified
            value = user.skillLevel.name.take(3), // Сокращение (BEG, INT...)
            label = "Уровень",
            color = MaterialTheme.colorScheme.secondaryContainer
        )

        // Премиум (или другая метрика)
        SummaryItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.WorkspacePremium,
            value = if (user.isPremium) "PRO" else "Free",
            label = "Статус",
            color = MaterialTheme.colorScheme.tertiaryContainer
        )
    }
}

@Composable
fun SummaryItem(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}