package org.lerchenflo.schneaggchatv3mp.games.presentation.tetris

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TetrisState(
    val board: List<List<Color?>> = List(20) { List(10) { null } },
    val currentPiece: Tetromino? = null,
    val piecePosition: Pair<Int, Int> = 0 to 0, // Row, Column
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val isPlaying: Boolean = false
)

enum class TetrominoType {
    I, J, L, O, S, T, Z
}

data class Tetromino(
    val type: TetrominoType,
    val rotation: Int = 0, // 0, 1, 2, 3
    val color: Color
) {
    // Returns list of (row, col) offsets relative to pivot
    fun getShape(): List<Pair<Int, Int>> {
        return when (type) {
            TetrominoType.I -> when (rotation % 2) {
                0 -> listOf(0 to -1, 0 to 0, 0 to 1, 0 to 2) 
                else -> listOf(-1 to 1, 0 to 1, 1 to 1, 2 to 1)
            }
            TetrominoType.J -> when (rotation % 4) {
                0 -> listOf(-1 to -1, 0 to -1, 0 to 0, 0 to 1)
                1 -> listOf(-1 to 1, -1 to 0, 0 to 0, 1 to 0)
                2 -> listOf(0 to -1, 0 to 0, 0 to 1, 1 to 1) 
                else -> listOf(1 to -1, 1 to 0, 0 to 0, -1 to 0) // Fixed rotation
            }
            TetrominoType.L -> when (rotation % 4) {
                 0 -> listOf(0 to -1, 0 to 0, 0 to 1, -1 to 1)
                 1 -> listOf(-1 to 0, 0 to 0, 1 to 0, 1 to 1)
                 2 -> listOf(1 to -1, 0 to -1, 0 to 0, 0 to 1)
                 else -> listOf(-1 to -1, -1 to 0, 0 to 0, 1 to 0)
            }
            TetrominoType.O -> listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
            TetrominoType.S -> when (rotation % 2) {
                0 -> listOf(0 to -1, 0 to 0, -1 to 0, -1 to 1)
                else -> listOf(-1 to 0, 0 to 0, 0 to 1, 1 to 1)
            }
            TetrominoType.T -> when (rotation % 4) {
                0 -> listOf(0 to -1, 0 to 0, 0 to 1, -1 to 0)
                1 -> listOf(-1 to 0, 0 to 0, 1 to 0, 0 to 1)
                2 -> listOf(0 to -1, 0 to 0, 0 to 1, 1 to 0)
                else -> listOf(-1 to 0, 0 to 0, 1 to 0, 0 to -1)
            }
            TetrominoType.Z -> when (rotation % 2) {
                0 -> listOf(-1 to -1, -1 to 0, 0 to 0, 0 to 1)
                else -> listOf(1 to 0, 0 to 0, 0 to 1, -1 to 1)
            }
        }
    }
}

class TetrisViewModel : ViewModel() {

    private val _state = MutableStateFlow(TetrisState())
    val state = _state.asStateFlow()

    private var gameLoopJob: Job? = null
    private val tickRate = 500L // ms

    fun startGame() {
        _state.value = TetrisState(isPlaying = true)
        spawnPiece()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (state.value.isPlaying && !state.value.isGameOver) {
                delay(tickRate)
                moveDown()
            }
        }
    }

    fun pauseGame() {
        _state.update { it.copy(isPlaying = false) }
        gameLoopJob?.cancel()
    }
    
    fun resumeGame() {
        if(!state.value.isGameOver) {
            _state.update { it.copy(isPlaying = true) }
            startGameLoop()
        }
    }

    fun restartGame() {
        startGame()
    }

    private fun spawnPiece() {
        val type = TetrominoType.values().random()
        val color = when (type) {
            TetrominoType.I -> Color.Cyan
            TetrominoType.J -> Color.Blue
            TetrominoType.L -> Color(0xFFFFA500) // Orange
            TetrominoType.O -> Color.Yellow
            TetrominoType.S -> Color.Green
            TetrominoType.T -> Color.Magenta
            TetrominoType.Z -> Color.Red
        }
        val piece = Tetromino(type, 0, color)
        val startRow = 1 // Start slightly visible
        val startCol = 4
        
        if (!isValidMove(piece, startRow, startCol)) {
             _state.update { it.copy(isGameOver = true, isPlaying = false) }
             gameLoopJob?.cancel()
        } else {
             _state.update {
                it.copy(
                    currentPiece = piece,
                    piecePosition = startRow to startCol
                )
            }
        }
    }


    fun moveLeft() {
        move(0, -1)
    }

    fun moveRight() {
        move(0, 1)
    }

    private fun move(rowOffset: Int, colOffset: Int) {
        val currentState = _state.value
        if (!currentState.isPlaying || currentState.isGameOver || currentState.currentPiece == null) return

        val (r, c) = currentState.piecePosition
        if (isValidMove(currentState.currentPiece, r + rowOffset, c + colOffset)) {
            _state.update {
                it.copy(piecePosition = (r + rowOffset) to (c + colOffset))
            }
        } else if (rowOffset > 0) {
            // Hit bottom or piece when moving down
            lockPiece()
        }
    }
    
    fun rotate() {
        val currentState = _state.value
        if (!currentState.isPlaying || currentState.isGameOver || currentState.currentPiece == null) return

        val currentPiece = currentState.currentPiece
        val nextRotation = (currentPiece.rotation + 1) % 4
        val rotatedPiece = currentPiece.copy(rotation = nextRotation)
        
        if (isValidMove(rotatedPiece, currentState.piecePosition.first, currentState.piecePosition.second)) {
             _state.update { it.copy(currentPiece = rotatedPiece) }
        } else {
            // Wall kick attempt (simple: try 1 left, 1 right)
             if (isValidMove(rotatedPiece, currentState.piecePosition.first, currentState.piecePosition.second - 1)) {
                 _state.update { it.copy(currentPiece = rotatedPiece, piecePosition = currentState.piecePosition.first to currentState.piecePosition.second - 1) }
             } else if (isValidMove(rotatedPiece, currentState.piecePosition.first, currentState.piecePosition.second + 1)) {
                 _state.update { it.copy(currentPiece = rotatedPiece, piecePosition = currentState.piecePosition.first to currentState.piecePosition.second + 1) }
             }
        }
    }
    
    fun hardDrop() {
       val currentState = _state.value
        if (!currentState.isPlaying || currentState.isGameOver || currentState.currentPiece == null) return
        
        var dropRow = currentState.piecePosition.first
        while (isValidMove(currentState.currentPiece, dropRow + 1, currentState.piecePosition.second)) {
            dropRow++
        }
        
        _state.update { it.copy(piecePosition = dropRow to currentState.piecePosition.second) }
        lockPiece()
    }


    private fun moveDown() {
        move(1, 0)
    }

    private fun isValidMove(piece: Tetromino, row: Int, col: Int): Boolean {
        val board = _state.value.board
        return piece.getShape().all { (rOffset, cOffset) ->
            val targetRow = row + rOffset
            val targetCol = col + cOffset
            
            targetRow < 20 && targetCol in 0..9 && (targetRow < 0 || board[targetRow][targetCol] == null)
        }
    }

    private fun lockPiece() {
        val currentState = _state.value
        val piece = currentState.currentPiece ?: return
        val (pRow, pCol) = currentState.piecePosition
        
        val newBoard = currentState.board.map { it.toMutableList() }.toMutableList()
        
        piece.getShape().forEach { (r, c) ->
            val boardRow = pRow + r
            val boardCol = pCol + c
            if (boardRow in 0..19 && boardCol in 0..9) {
                newBoard[boardRow][boardCol] = piece.color
            }
        }
        
        // Clear lines
        val linesToClear = mutableListOf<Int>()
        newBoard.forEachIndexed { index, row ->
            if (row.all { it != null }) {
                linesToClear.add(index)
            }
        }
        
        var scoreAdd = 0
        if (linesToClear.isNotEmpty()) {
            linesToClear.forEach { 
                newBoard.removeAt(it)
                newBoard.add(0, MutableList(10) { null })
            }
            scoreAdd = when(linesToClear.size) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> 0
            }
        }
        
        _state.update {
            it.copy(
                board = newBoard,
                score = it.score + scoreAdd,
                currentPiece = null
            )
        }
        
        spawnPiece()
    }
}
