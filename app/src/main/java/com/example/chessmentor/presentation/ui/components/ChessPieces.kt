package com.example.chessmentor.presentation.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.chessmentor.R
import com.github.bhlangonijr.chesslib.Piece

object ChessPieces {

    private val bitmapCache = mutableMapOf<Piece, Bitmap>()

    fun draw(
        drawScope: DrawScope,
        piece: Piece,
        x: Float,
        y: Float,
        size: Float,
        context: Context,
        alpha: Float = 1f
    ) {
        val bitmap = getBitmap(piece, context, size.toInt()) ?: return

        val paint = android.graphics.Paint().apply {
            this.alpha = (alpha * 255).toInt()
            isAntiAlias = true
            isFilterBitmap = true
        }

        drawScope.drawContext.canvas.nativeCanvas.drawBitmap(
            bitmap,
            x,
            y,
            paint
        )
    }

    private fun getBitmap(piece: Piece, context: Context, size: Int): Bitmap? {
        if (bitmapCache.containsKey(piece) && bitmapCache[piece]?.width == size) {
            return bitmapCache[piece]
        }

        val resId = when (piece) {
            Piece.WHITE_PAWN -> R.drawable.ic_pawn_white
            Piece.BLACK_PAWN -> R.drawable.ic_pawn_black
            Piece.WHITE_KNIGHT -> R.drawable.ic_knight_white
            Piece.BLACK_KNIGHT -> R.drawable.ic_knight_black
            Piece.WHITE_BISHOP -> R.drawable.ic_bishop_white
            Piece.BLACK_BISHOP -> R.drawable.ic_bishop_black
            Piece.WHITE_ROOK -> R.drawable.ic_rook_white
            Piece.BLACK_ROOK -> R.drawable.ic_rook_black
            Piece.WHITE_QUEEN -> R.drawable.ic_queen_white
            Piece.BLACK_QUEEN -> R.drawable.ic_queen_black
            Piece.WHITE_KING -> R.drawable.ic_king_white
            Piece.BLACK_KING -> R.drawable.ic_king_black
            else -> return null
        }

        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        val bitmap = drawable.toBitmap(width = size, height = size)

        bitmapCache[piece] = bitmap
        return bitmap
    }
}