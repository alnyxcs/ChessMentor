package com.example.chessmentor.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext // <-- ДОБАВЛЕН ИМПОРТ
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square

/**
 * Тема оформления доски
 */
data class BoardTheme(
    val lightSquare: Color,
    val darkSquare: Color,
    val highlightColor: Color,
    val lastMoveColor: Color,
    val name: String
)

/**
 * Предопределенные темы
 */
object BoardThemes {
    val Classic = BoardTheme(
        lightSquare = Color(0xFFF0D9B5),
        darkSquare = Color(0xFFB58863),
        highlightColor = Color(0x8090EE90),
        lastMoveColor = Color(0x80FFD700),
        name = "Классическая"
    )

    val Blue = BoardTheme(
        lightSquare = Color(0xFFDEE3E6),
        darkSquare = Color(0xFF8CA2AD),
        highlightColor = Color(0x8090EE90),
        lastMoveColor = Color(0x80FFEB3B),
        name = "Синяя"
    )

    val Green = BoardTheme(
        lightSquare = Color(0xFFEBECD0),
        darkSquare = Color(0xFF779556),
        highlightColor = Color(0x80FFD54F),
        lastMoveColor = Color(0x80FFA726),
        name = "Зеленая"
    )

    val Wood = BoardTheme(
        lightSquare = Color(0xFFFFCE9E),
        darkSquare = Color(0xFFD18B47),
        highlightColor = Color(0x8090EE90),
        lastMoveColor = Color(0x80FF6F00),
        name = "Дерево"
    )

    fun getAll() = listOf(Classic, Blue, Green, Wood)
}

/**
 * Состояние анимации фигуры
 */
data class PieceAnimation(
    val piece: Piece,
    val fromSquare: Square,
    val toSquare: Square,
    val progress: Float
)

/**
 * Компонент интерактивной шахматной доски с анимациями
 */
@Composable
fun ChessBoard(
    board: Board,
    highlightedSquares: Set<Square> = emptySet(),
    lastMove: Pair<Square, Square>? = null,
    modifier: Modifier = Modifier,
    flipped: Boolean = false,
    theme: BoardTheme = BoardThemes.Classic,
    animateMove: Boolean = true,
    onSquareClick: ((Square) -> Unit)? = null
) {
    // Получаем контекст для загрузки ресурсов
    val context = LocalContext.current // <-- ВАЖНО!

    // Состояние анимации
    var animatingPiece by remember { mutableStateOf<PieceAnimation?>(null) }

    // Анимация хода
    val animationProgress by animateFloatAsState(
        targetValue = if (animatingPiece != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = {
            animatingPiece = null
        },
        label = "piece_move"
    )

    // Отслеживаем изменение последнего хода для запуска анимации
    LaunchedEffect(lastMove) {
        if (lastMove != null && animateMove) {
            val piece = board.getPiece(lastMove.second)
            if (piece != Piece.NONE) {
                animatingPiece = PieceAnimation(
                    piece = piece,
                    fromSquare = lastMove.first,
                    toSquare = lastMove.second,
                    progress = 0f
                )
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Black)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(flipped) {
                    detectTapGestures { offset ->
                        val squareSize = size.width / 8
                        val file = (offset.x / squareSize).toInt()
                        val rank = (offset.y / squareSize).toInt()

                        if (file in 0..7 && rank in 0..7) {
                            val actualRank = if (flipped) rank else 7 - rank
                            val actualFile = if (flipped) 7 - file else file

                            val square = Square.squareAt(actualRank * 8 + actualFile)
                            onSquareClick?.invoke(square)
                        }
                    }
                }
        ) {
            val squareSize = size.width / 8

            // Рисуем поля доски
            for (rank in 0..7) {
                for (file in 0..7) {
                    val actualRank = if (flipped) rank else 7 - rank
                    val actualFile = if (flipped) 7 - file else file

                    val square = Square.squareAt(actualRank * 8 + actualFile)
                    val isLight = (rank + file) % 2 == 0

                    // Базовый цвет
                    var squareColor = if (isLight) theme.lightSquare else theme.darkSquare

                    // Подсветка последнего хода
                    if (lastMove != null && (square == lastMove.first || square == lastMove.second)) {
                        squareColor = theme.lastMoveColor
                    }

                    // Подсветка выбранных полей
                    if (square in highlightedSquares) {
                        squareColor = theme.highlightColor
                    }

                    drawRect(
                        color = squareColor,
                        topLeft = Offset(file * squareSize, rank * squareSize),
                        size = Size(squareSize, squareSize)
                    )
                }
            }

            // Рисуем фигуры
            for (rank in 0..7) {
                for (file in 0..7) {
                    val actualRank = if (flipped) rank else 7 - rank
                    val actualFile = if (flipped) 7 - file else file

                    val square = Square.squareAt(actualRank * 8 + actualFile)
                    val piece = board.getPiece(square)

                    // Не рисуем фигуру если она анимируется
                    val isAnimating = animatingPiece?.let {
                        it.toSquare == square && animationProgress < 1f
                    } ?: false

                    if (piece != Piece.NONE && !isAnimating) {
                        drawPiece(
                            piece = piece,
                            x = file * squareSize,
                            y = rank * squareSize,
                            size = squareSize,
                            context = context // <-- Передаем контекст
                        )
                    }
                }
            }

            // Рисуем анимирующуюся фигуру
            animatingPiece?.let { anim ->
                val fromFile = if (flipped) 7 - anim.fromSquare.file.ordinal else anim.fromSquare.file.ordinal
                val fromRank = if (flipped) anim.fromSquare.rank.ordinal else 7 - anim.fromSquare.rank.ordinal
                val toFile = if (flipped) 7 - anim.toSquare.file.ordinal else anim.toSquare.file.ordinal
                val toRank = if (flipped) anim.toSquare.rank.ordinal else 7 - anim.toSquare.rank.ordinal

                val startX = fromFile * squareSize
                val startY = fromRank * squareSize
                val endX = toFile * squareSize
                val endY = toRank * squareSize

                val currentX = startX + (endX - startX) * animationProgress
                val currentY = startY + (endY - startY) * animationProgress

                drawPiece(
                    piece = anim.piece,
                    x = currentX,
                    y = currentY,
                    size = squareSize,
                    context = context, // <-- Передаем контекст
                    alpha = 1f
                )
            }
        }

        // Координаты (a-h, 1-8)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            for (rank in 0..7) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val displayRank = if (flipped) rank + 1 else 8 - rank
                    Text(
                        text = displayRank.toString(),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(2.dp)
        ) {
            for (file in 0..7) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    val displayFile = if (flipped) ('h' - file) else ('a' + file)
                    Text(
                        text = displayFile.toString(),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Отрисовка фигуры (вспомогательная функция)
 */
private fun DrawScope.drawPiece(
    piece: Piece,
    x: Float,
    y: Float,
    size: Float,
    context: android.content.Context, // <-- Параметр контекста
    alpha: Float = 1f
) {
    // Вызываем наш новый рисовальщик фигур из ChessPieces.kt
    ChessPieces.draw(this, piece, x, y, size, context, alpha)
}

/**
 * Преобразование Square в читаемый вид (e4, d5, etc.)
 */
fun Square.toNotation(): String {
    val file = this.file.notation
    val rank = this.rank.notation
    return "$file$rank"
}