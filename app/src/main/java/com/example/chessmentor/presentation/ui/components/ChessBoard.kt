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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.min

data class BoardTheme(
    val lightSquare: Color,
    val darkSquare: Color,
    val highlightColor: Color,
    val lastMoveColor: Color,
    val name: String
)

object BoardThemes {
    val Classic = BoardTheme(
        lightSquare = Color(0xFFF0D9B5),
        darkSquare = Color(0xFFB58863),
        highlightColor = Color(0x9964DD17),
        lastMoveColor = Color(0x99FFD600),
        name = "Классическая"
    )

    val Blue = BoardTheme(
        lightSquare = Color(0xFFDEE3E6),
        darkSquare = Color(0xFF8CA2AD),
        highlightColor = Color(0x9964DD17),
        lastMoveColor = Color(0x99FFEB3B),
        name = "Синяя"
    )

    val Green = BoardTheme(
        lightSquare = Color(0xFFEBECD0),
        darkSquare = Color(0xFF779556),
        highlightColor = Color(0x99FFD54F),
        lastMoveColor = Color(0x99FFA726),
        name = "Зеленая"
    )

    val Wood = BoardTheme(
        lightSquare = Color(0xFFFFCE9E),
        darkSquare = Color(0xFFD18B47),
        highlightColor = Color(0x9964DD17),
        lastMoveColor = Color(0x99FF6F00),
        name = "Дерево"
    )

    fun getAll() = listOf(Classic, Blue, Green, Wood)
}

data class PieceAnimation(
    val piece: Piece,
    val fromSquare: Square,
    val toSquare: Square,
    val progress: Float
)

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
    val context = LocalContext.current
    var animatingPiece by remember { mutableStateOf<PieceAnimation?>(null) }

    val animationProgress by animateFloatAsState(
        targetValue = if (animatingPiece != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = { animatingPiece = null },
        label = "piece_move"
    )

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
        modifier = modifier.background(Color.Black)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(flipped) {
                    detectTapGestures { offset ->
                        // Адаптивная логика клика
                        val boardSize = min(size.width, size.height)
                        val squareSize = boardSize / 8
                        val xOffset = (size.width - boardSize) / 2
                        val yOffset = (size.height - boardSize) / 2

                        val localX = offset.x - xOffset
                        val localY = offset.y - yOffset

                        val file = (localX / squareSize).toInt()
                        val rank = (localY / squareSize).toInt()

                        if (file in 0..7 && rank in 0..7) {
                            val actualRank = if (flipped) rank else 7 - rank
                            val actualFile = if (flipped) 7 - file else file
                            val square = Square.squareAt(actualRank * 8 + actualFile)
                            onSquareClick?.invoke(square)
                        }
                    }
                }
        ) {
            // Адаптивная отрисовка
            val boardSize = min(size.width, size.height)
            val squareSize = boardSize / 8
            val xOffset = (size.width - boardSize) / 2
            val yOffset = (size.height - boardSize) / 2

            translate(left = xOffset, top = yOffset) {
                // 1. Рисуем клетки
                for (rank in 0..7) {
                    for (file in 0..7) {
                        val actualRank = if (flipped) rank else 7 - rank
                        val actualFile = if (flipped) 7 - file else file
                        val square = Square.squareAt(actualRank * 8 + actualFile)
                        val isLight = (rank + file) % 2 == 0

                        // Базовый цвет
                        var squareColor = if (isLight) theme.lightSquare else theme.darkSquare

                        // Цвет последнего хода
                        if (lastMove != null && (square == lastMove.first || square == lastMove.second)) {
                            squareColor = theme.lastMoveColor
                        }

                        // Цвет ВЫБРАННОЙ фигуры (приоритет выше)
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

                // 2. Рисуем фигуры
                for (rank in 0..7) {
                    for (file in 0..7) {
                        val actualRank = if (flipped) rank else 7 - rank
                        val actualFile = if (flipped) 7 - file else file
                        val square = Square.squareAt(actualRank * 8 + actualFile)
                        val piece = board.getPiece(square)

                        val isAnimating = animatingPiece?.let {
                            it.toSquare == square && animationProgress < 1f
                        } ?: false

                        if (piece != Piece.NONE && !isAnimating) {
                            drawPiece(piece, file * squareSize, rank * squareSize, squareSize, context)
                        }
                    }
                }

                // 3. Анимация
                animatingPiece?.let { anim ->
                    val fromFile = if (flipped) 7 - anim.fromSquare.file.ordinal else anim.fromSquare.file.ordinal
                    val fromRank = if (flipped) anim.fromSquare.rank.ordinal else 7 - anim.fromSquare.rank.ordinal
                    val toFile = if (flipped) 7 - anim.toSquare.file.ordinal else anim.toSquare.file.ordinal
                    val toRank = if (flipped) anim.toSquare.rank.ordinal else 7 - anim.toSquare.rank.ordinal

                    val currentX = (fromFile * squareSize) + ((toFile - fromFile) * squareSize * animationProgress)
                    val currentY = (fromRank * squareSize) + ((toRank - fromRank) * squareSize * animationProgress)

                    drawPiece(anim.piece, currentX, currentY, squareSize, context)
                }
            } // end translate
        }

        // Координаты (буквы и цифры)
        // Можно добавить, если нужно, но для чистоты пока убрали, так как они могут перекрываться на маленьких экранах
    }
}

private fun DrawScope.drawPiece(
    piece: Piece,
    x: Float,
    y: Float,
    size: Float,
    context: android.content.Context,
    alpha: Float = 1f
) {
    ChessPieces.draw(this, piece, x, y, size, context, alpha)
}

