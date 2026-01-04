package com.example.chessmentor.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chessmentor.domain.entity.AnalyzedMove
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.KeyMoment
import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.MoveQuality // <--- ВАЖНО: Импортируем только этот!
import com.example.chessmentor.presentation.ui.components.SoundManager
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import java.io.File
import kotlin.math.abs

class BoardViewModel : ViewModel() {
    companion object { private const val TAG = "BoardViewModel" }

    val board = mutableStateOf(Board())
    val moves = mutableStateListOf<Move>()
    val moveNotations = mutableStateListOf<String>()
    val currentMoveIndex = mutableIntStateOf(-1)
    val highlightedSquares = mutableStateOf<Set<Square>>(emptySet())
    val lastMove = mutableStateOf<Pair<Square, Square>?>(null)

    val evaluations = mutableStateListOf<Int>()
    val analyzedMoves = mutableStateListOf<AnalyzedMove>()
    val mistakes = mutableStateListOf<Mistake>()

    private var playerColor: ChessColor = ChessColor.WHITE
    private var currentGame: Game? = null
    var soundManager: SoundManager? = null

    fun loadGame(game: Game, gameMistakes: List<Mistake>, gameAnalyzedMoves: List<AnalyzedMove> = emptyList(), gameEvaluations: List<Int> = emptyList()) {
        try {
            currentGame = game
            playerColor = game.playerColor
            clearState()
            mistakes.addAll(gameMistakes)
            analyzedMoves.addAll(gameAnalyzedMoves)
            val normalizedPgn = normalizePgn(game.pgnData)
            val halfMoves = parsePgn(normalizedPgn)
            if (halfMoves.isEmpty()) return
            loadMoves(halfMoves)
            loadEvaluations(gameEvaluations, game)
            goToStart()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun clearState() {
        board.value = Board(); moves.clear(); moveNotations.clear(); evaluations.clear()
        mistakes.clear(); analyzedMoves.clear(); currentMoveIndex.intValue = -1
        lastMove.value = null; highlightedSquares.value = emptySet()
    }

    private fun normalizePgn(pgn: String): String {
        val movesOnly = pgn.trim().replace(Regex("""\[[^\]]*\]"""), " ").replace(Regex("""\{[^}]*\}"""), " ")
            .replace(Regex("""\([^)]*\)"""), " ").replace(Regex("""\$\d+"""), " ")
            .replace(Regex("""(1-0|0-1|1/2-1/2|\*)\s*$"""), "").replace(Regex("""\s+"""), " ").trim()
        if (movesOnly.isEmpty()) return ""
        return "[Event \"Imported\"]\n\n$movesOnly *"
    }

    private fun parsePgn(pgn: String): List<Move> {
        if (pgn.isEmpty()) return emptyList()
        val temp = File.createTempFile("g", ".pgn")
        return try {
            temp.writeText(pgn); val h = PgnHolder(temp.absolutePath); h.loadPgn()
            if (h.games.isEmpty()) emptyList() else h.games[0].halfMoves.toList()
        } finally { temp.delete() }
    }

    private fun loadMoves(m: List<Move>) {
        moves.addAll(m)
        var num = 1
        m.forEachIndexed { i, move ->
            moveNotations.add(if (i % 2 == 0) "`$num. " + move.san else "`$num... " + move.san)
            if (i % 2 == 1) num++
        }
    }

    private fun loadEvaluations(ev: List<Int>, g: Game) {
        evaluations.clear()
        if (ev.isNotEmpty()) evaluations.addAll(ev)
        else if (g.hasEvaluations()) evaluations.addAll(g.getEvaluations())
        else evaluations.add(0)
    }

    fun goToStart() { board.value = Board(); currentMoveIndex.intValue = -1; lastMove.value = null; highlightedSquares.value = emptySet() }

    fun goToEnd() {
        if (moves.isEmpty()) return
        goToStart()
        moves.forEach { board.value.doMove(it) }
        currentMoveIndex.intValue = moves.size - 1
        lastMove.value = Pair(moves.last().from, moves.last().to)
        updateHighlights()
    }

    fun goToNextMove() {
        if (currentMoveIndex.intValue >= moves.size - 1) return
        val m = moves[++currentMoveIndex.intValue]
        board.value.doMove(m); lastMove.value = Pair(m.from, m.to); updateHighlights()
    }

    fun goToPreviousMove() {
        if (currentMoveIndex.intValue < 0) return
        board.value.undoMove(); currentMoveIndex.intValue--
        lastMove.value = if (currentMoveIndex.intValue >= 0) Pair(moves[currentMoveIndex.intValue].from, moves[currentMoveIndex.intValue].to) else null
        updateHighlights()
    }

    fun goToMove(idx: Int) {
        goToStart()
        for (i in 0..idx) { if (i < moves.size) { board.value.doMove(moves[i]); currentMoveIndex.intValue = i } }
        if (idx >= 0) lastMove.value = Pair(moves[idx].from, moves[idx].to)
        updateHighlights()
    }

    private fun updateHighlights() {
        val s = mutableSetOf<Square>()
        // Используем MoveQuality из Domain
        if (getCurrentAnalyzedMove()?.quality == MoveQuality.MISTAKE ||
            getCurrentAnalyzedMove()?.quality == MoveQuality.BLUNDER) {
            lastMove.value?.second?.let { s.add(it) }
        }
        highlightedSquares.value = s
    }

    fun getCurrentEvaluation(): Int = evaluations.getOrElse(currentMoveIndex.intValue + 1) { 0 }
    fun getCurrentMoveNotation(): String = moveNotations.getOrElse(currentMoveIndex.intValue) { "Start" }
    fun getCurrentAnalyzedMove(): AnalyzedMove? = if(currentMoveIndex.intValue < 0) null else analyzedMoves.find { it.moveIndex == currentMoveIndex.intValue }

    // Возвращаем MoveQuality из Domain
    fun getCurrentMoveQuality(): MoveQuality? = getCurrentAnalyzedMove()?.quality

    fun getCurrentMistake(): Mistake? = getCurrentAnalyzedMove()?.let { a -> if(a.isMistake()) mistakes.find { it.moveNumber == a.moveNumber && it.color == a.color } else null }
    fun hasRealEvaluations(): Boolean = evaluations.size > 1

    fun getKeyMoments(): List<KeyMoment> {
        return analyzedMoves.map { it.toKeyMoment() }.sortedWith(compareBy({
            // Используем MoveQuality из Domain
            when (it.quality) {
                MoveQuality.BLUNDER -> 0
                MoveQuality.MISTAKE -> 1
                MoveQuality.INACCURACY -> 2
                MoveQuality.MISSED_WIN -> 3
                MoveQuality.BRILLIANT -> 4
                MoveQuality.GREAT_MOVE -> 5
                MoveQuality.BEST_MOVE -> 6
                MoveQuality.EXCELLENT -> 7
                MoveQuality.GOOD -> 8
                MoveQuality.BOOK -> 9
            }
        }, { -abs(it.evaluationChange) }))
    }

    fun getMoveQualityStats(): Map<MoveQuality, Int> = analyzedMoves.groupBy { it.quality }.mapValues { it.value.size }
    fun getPlayerColor(): ChessColor = playerColor
    fun getTotalMoves(): Int = moves.size
    fun isCurrentMovePlayerMove(): Boolean = if (currentMoveIndex.intValue < 0) false else (playerColor == ChessColor.WHITE && currentMoveIndex.intValue % 2 == 0) || (playerColor == ChessColor.BLACK && currentMoveIndex.intValue % 2 != 0)
}