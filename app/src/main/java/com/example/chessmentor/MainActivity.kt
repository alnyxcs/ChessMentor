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
                        Text(
                            text = when(currentScreen) {
                                "games" -> "📋 Мои партии"
                                "upload" -> "⬆️ Загрузить партию"
                                "game_view" -> "🔍 Анализ партии"
                                else -> "♟️ Шахматный Ментор"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (currentScreen == "game_view") {
                            TextButton(onClick = { currentScreen = "summary" }) {
                                Text("← К сводке")
                            }
                        } else if (currentScreen == "summary") {
                            TextButton(onClick = {
                                currentScreen = "games"
                                gameViewModel.closeGameView()
                            }) {
                                Text("← К партиям")
                            }
                        } else if (currentScreen == "games" || currentScreen == "upload") {
                            TextButton(onClick = { currentScreen = "home" }) {
                                Text("← На главную")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                if (currentUser != null && currentScreen != "game_view") {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Text("🏠") },
                            label = { Text("Главная") },
                            selected = currentScreen == "home",
                            onClick = { currentScreen = "home" }
                        )
                        NavigationBarItem(
                            icon = { Text("📋") },
                            label = { Text("Партии") },
                            selected = currentScreen == "games",
                            onClick = { currentScreen = "games" }
                        )
                        NavigationBarItem(
                            icon = { Text("💪") },
                            label = { Text("Тренировка") },
                            selected = currentScreen == "training",
                            onClick = { currentScreen = "training" }
                        )
                        NavigationBarItem(
                            icon = { Text("⬆️") },
                            label = { Text("Загрузить") },
                            selected = currentScreen == "upload",
                            onClick = { currentScreen = "upload" }
                        )
                        NavigationBarItem(
                            icon = { Text("📊") },
                            label = { Text("Статистика") },
                            selected = currentScreen == "stats",
                            onClick = { currentScreen = "stats" }
                        )
                    }
                }
            }
        ) { paddingValues ->

            // Главный контент
            when (currentScreen) {
                "home" -> HomeScreen(
                    paddingValues, currentUser, message, isLoading,
                    { message = "" },
                    { currentUser = null },
                    { nick, email, pass, rating ->
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
                    { email, pass ->
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
                    regNickname, regEmail, regPassword, regRating, loginEmail, loginPassword,
                    { regNickname = it }, { regEmail = it }, { regPassword = it }, { regRating = it },
                    { loginEmail = it }, { loginPassword = it }
                )

                "games" -> GamesListScreen(
                    paddingValues, gameViewModel, container,
                    onGameClick = { game ->
                        gameViewModel.selectGame(game)
                        currentScreen = "summary"
                    }
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

                "game_view" -> GameViewScreen(
                    paddingValues, gameViewModel
                )
            }
        }
    }
}

// ==================== HOME SCREEN ====================
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    currentUser: User?,
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
                    containerColor = if (currentUser != null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (currentUser != null) {
                        Text(
                            text = "Привет, ${currentUser.nickname}! 👋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Email: ${currentUser.email}")
                        Text("Рейтинг: ${currentUser.rating}")
                        Text("Уровень: ${currentUser.skillLevel.getDisplayName()}")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Выйти")
                        }
                    } else {
                        Text(
                            text = "Добро пожаловать!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Войдите или зарегистрируйтесь")
                    }
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
                        TextButton(onClick = onMessageDismiss) {
                            Text("✕")
                        }
                    }
                }
            }
        }

        // Формы входа/регистрации (только если не залогинен)
        if (currentUser == null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { currentTab = "login" },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentTab == "login")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        ),
                        enabled = !isLoading
                    ) {
                        Text("Вход")
                    }
                    Button(
                        onClick = { currentTab = "register" },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentTab == "register")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        ),
                        enabled = !isLoading
                    ) {
                        Text("Регистрация")
                    }
                }
            }

            if (currentTab == "login") {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔐 Вход", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = onLoginEmailChange,
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = onLoginPasswordChange,
                                label = { Text("Пароль") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )

                            Button(
                                onClick = { onLogin(loginEmail, loginPassword) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && loginEmail.isNotBlank() && loginPassword.isNotBlank()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Входим...")
                                } else {
                                    Text("Войти")
                                }
                            }

                            Divider()
                            Text("💡 Тест: test@chessmentor.ru / test123", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (currentTab == "register") {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📝 Регистрация", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = regNickname,
                                onValueChange = onRegNicknameChange,
                                label = { Text("Никнейм") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = onRegEmailChange,
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = onRegPasswordChange,
                                label = { Text("Пароль") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )

                            OutlinedTextField(
                                value = regRating,
                                onValueChange = onRegRatingChange,
                                label = { Text("Рейтинг (600-3500)") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )

                            Button(
                                onClick = {
                                    onRegister(
                                        regNickname,
                                        regEmail,
                                        regPassword,
                                        regRating.toIntOrNull() ?: 1200
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading &&
                                        regNickname.isNotBlank() &&
                                        regEmail.isNotBlank() &&
                                        regPassword.isNotBlank() &&
                                        regRating.isNotBlank()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Регистрируем...")
                                } else {
                                    Text("Зарегистрироваться")
                                }
                            }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⬆️ Загрузить партию",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Вставьте PGN вашей партии для анализа")
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
                        if (!isLoading) {
                            TextButton(onClick = { gameViewModel.clearMessage() }) {
                                Text("✕")
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
                        enabled = !isLoading
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
                            label = { Text("⚪ Белые") },
                            enabled = !isLoading
                        )
                        FilterChip(
                            selected = selectedColor == ChessColor.BLACK,
                            onClick = { gameViewModel.selectedColor.value = ChessColor.BLACK },
                            label = { Text("⚫ Чёрные") },
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
                        enabled = !isLoading
                    )

                    // Кнопка загрузки
                    Button(
                        onClick = { gameViewModel.uploadGame() },
                        modifier = Modifier.fillMaxWidth(),
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
                            Text("🚀 Загрузить и анализировать")
                        }
                    }

                    // Подсказка
                    Divider()

                    Text(
                        text = "💡 Пример PGN:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
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

// ==================== GAME VIEW SCREEN ====================
@Composable
fun GameViewScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel
) {
    val game by gameViewModel.selectedGame
    val mistakes = gameViewModel.selectedGameMistakes

    // Создаём BoardViewModel для управления доской
    val boardViewModel = remember { BoardViewModel() }

    // Выбранная тема доски
    var selectedTheme by remember { mutableStateOf(BoardThemes.Classic) }

    // Показывать ли выбор темы
    var showThemeSelector by remember { mutableStateOf(false) }

    // Загружаем партию в BoardViewModel
    LaunchedEffect(game?.id) {
        game?.let { currentGame ->
            boardViewModel.loadGame(currentGame, mistakes)
        }
    }

    if (game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Партия не выбрана")
        }
        return
    }

    val board by boardViewModel.board
    val currentMoveIndex by boardViewModel.currentMoveIndex
    val totalMoves = boardViewModel.moves.size
    val currentEvaluation = boardViewModel.getCurrentEvaluation()
    val currentMoveNotation = boardViewModel.getCurrentMoveNotation()
    val highlightedSquares by boardViewModel.highlightedSquares
    val lastMove by boardViewModel.lastMove
    val currentMistake = boardViewModel.getCurrentMistake()
    val evaluations = boardViewModel.evaluations.toList()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Кнопка выбора темы
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Тема: ${selectedTheme.name}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { showThemeSelector = !showThemeSelector },
                modifier = Modifier.height(36.dp)
            ) {
                Text("🎨 Сменить тему")
            }
        }

        // Выбор темы (если открыт)
        if (showThemeSelector) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Выберите тему доски:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BoardThemes.getAll().forEach { theme ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedTheme = theme
                                    showThemeSelector = false
                                }
                            ) {
                                // Превью темы
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(
                                            color = if (selectedTheme == theme)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                Color.Transparent
                                        )
                                        .padding(4.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val squareSize = size.width / 2
                                        // Рисуем 2x2 клетки для превью
                                        for (i in 0..1) {
                                            for (j in 0..1) {
                                                val isLight = (i + j) % 2 == 0
                                                drawRect(
                                                    color = if (isLight) theme.lightSquare else theme.darkSquare,
                                                    topLeft = Offset(i * squareSize, j * squareSize),
                                                    size = Size(squareSize, squareSize)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = theme.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTheme == theme) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Доска с шкалой оценки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Шкала оценки
            EvaluationBar(
                evaluation = currentEvaluation,
                modifier = Modifier.height(330.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Шахматная доска с выбранной темой
            ChessBoard(
                board = board,
                highlightedSquares = highlightedSquares,
                lastMove = lastMove,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                flipped = game!!.playerColor == com.example.chessmentor.domain.entity.ChessColor.BLACK,
                theme = selectedTheme,  // Применяем выбранную тему
                animateMove = true      // Включаем анимации
            )
        }

        // Навигатор ходов
        MoveNavigator(
            currentMoveIndex = currentMoveIndex,
            totalMoves = totalMoves,
            currentMove = currentMoveNotation,
            onFirstMove = { boardViewModel.goToStart() },
            onPreviousMove = { boardViewModel.goToPreviousMove() },
            onNextMove = { boardViewModel.goToNextMove() },
            onLastMove = { boardViewModel.goToEnd() },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Информация об ошибке на текущем ходу
        if (currentMistake != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (currentMistake.mistakeType) {
                        com.example.chessmentor.domain.entity.MistakeType.BLUNDER -> Color(0xFFFFEBEE)
                        com.example.chessmentor.domain.entity.MistakeType.MISTAKE -> Color(0xFFFFF3E0)
                        com.example.chessmentor.domain.entity.MistakeType.INACCURACY -> Color(0xFFFFFDE7)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentMistake.getEmoji()} ${currentMistake.getDescription()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "-${currentMistake.getEvaluationLossInPawns()}",
                            fontSize = 14.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ваш ход:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = currentMistake.userMove,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Лучше:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = currentMistake.bestMove,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }
                    }

                    if (currentMistake.comment != null) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "💡 ${currentMistake.comment}",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Статистика партии (скроллируемая)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Статистика
            if (game!!.isAnalyzed()) {
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📊 Статистика партии",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Точность", fontSize = 14.sp, color = Color.Gray)
                                    Text(
                                        text = "${game!!.accuracy?.toInt() ?: 0}%",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Всего ошибок", fontSize = 14.sp, color = Color.Gray)
                                    Text(
                                        text = "${game!!.totalMistakes}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                MistakeTypeCount(
                                    type = com.example.chessmentor.domain.entity.MistakeType.BLUNDER,
                                    count = game!!.blundersCount
                                )
                                MistakeTypeCount(
                                    type = com.example.chessmentor.domain.entity.MistakeType.MISTAKE,
                                    count = game!!.mistakesCount
                                )
                                MistakeTypeCount(
                                    type = com.example.chessmentor.domain.entity.MistakeType.INACCURACY,
                                    count = game!!.inaccuraciesCount
                                )
                            }
                        }
                    }
                }
            }

            // Список всех ошибок
            if (mistakes.isNotEmpty()) {
                item {
                    Text(
                        text = "🎯 Все ошибки в партии:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(mistakes) { mistake ->
                    MistakeCard(mistake)
                }
            }

            // Отступ снизу
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
    val (emoji, color) = when (label) {
        "Brilliant" -> "💎" to Color(0xFF26C6DA) // Голубой
        "Great" -> "🔥" to Color(0xFF66BB6A) // Зеленый
        "Best" -> "⭐" to Color(0xFF9CCC65) // Светло-зеленый
        "Mistake" -> "❓" to Color(0xFFFFCA28) // Желтый
        "Blunder" -> "❌" to Color(0xFFEF5350) // Красный
        "Inaccuracy" -> "😐" to Color(0xFFFF7043) // Оранжевый
        else -> "" to Color.Gray
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 28.sp)
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else Color.Gray.copy(alpha = 0.5f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
            Text(
                text = moment.quality.getEmoji(),
                modifier = Modifier.padding(8.dp),
                fontSize = 16.sp
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
        Text("›", color = Color.Gray)
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
fun KeyMomentCount(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val (emoji, color) = when (label) {
            "Brilliant" -> "💎" to Color(0xFF03A9F4)
            "Great" -> "👍" to Color(0xFF4CAF50)
            "Best" -> "⭐" to Color(0xFF8BC34A)
            "Mistake" -> "⚠️" to Color(0xFFFF9800)
            "Blunder" -> "❌" to Color(0xFFF44336)
            else -> "🎯" to Color.Gray
        }
        Text(emoji, fontSize = 24.sp)
        Text("$count", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}


@Composable
fun MistakeTypeCount(type: MistakeType, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = type.getEmoji(),
            fontSize = 24.sp
        )
        Text(
            text = "$count",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = type.getDescription(),
            fontSize = 12.sp,
            color = Color.Gray
        )
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
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mistake.getEmoji()} Ход ${mistake.moveNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "-${mistake.getEvaluationLossInPawns()}",
                    fontSize = 14.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        color = Color.Red
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Лучше:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = mistake.bestMove,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                }
            }

            if (mistake.comment != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "💡 ${mistake.comment}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Моя статистика", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Прогресс с момента первого анализа")
                }
            }
        }

        // Загрузка или ошибка
        if (isLoading) {
            item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        if (errorMessage != null) {
            item { Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error) }
        }

        // Основная статистика
        stats?.let { s ->
            item {
                StatCard("📈 Рейтинг", "${s.currentRating} (${if (s.ratingChange >= 0) "+" else ""}${s.ratingChange})")
            }
            item {
                StatCard("🎯 Точность", "${s.averageAccuracy.toInt()}% (${s.accuracyTrend})")
            }
            item {
                StatCard("🎮 Партии", "${s.analyzedGames} / ${s.totalGames} проанализировано")
            }

            // Статистика по ошибкам
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ошибки", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
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
                        Text("💡 Проблемные темы", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        s.problemThemes.forEach { theme ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("${theme.category}: ${theme.themeName} (${theme.mistakeCount} раз)")
                            }
                        }
                    }
                }
            }

            // Рекомендации
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⭐ Рекомендации", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        s.recommendations.forEach { recommendation ->
                            Text("• $recommendation", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 16.sp, color = Color.Gray)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
