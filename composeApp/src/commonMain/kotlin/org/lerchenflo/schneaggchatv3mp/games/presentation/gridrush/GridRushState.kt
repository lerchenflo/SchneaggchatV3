package org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush

sealed interface GridRushAction {
    data object StartGame : GridRushAction
    data object StopGame : GridRushAction
    data object RestartGame : GridRushAction
    data class OnDragStart(val cell: Cell) : GridRushAction
    data class OnDragMove(val cell: Cell) : GridRushAction
    data object OnDragEnd : GridRushAction
}

/** Logical tile colors; the screen maps them to actual render colors. */
enum class GridTileColor { A, B, C, D, E }

data class Cell(
    val row: Int,
    val col: Int,
)

data class GridRushState(
    val isPlaying: Boolean = false,
    val isGameOver: Boolean = false,
    val won: Boolean = false,
    val score: Int = 0,
    val movesUsed: Int = 0,
    val parMoves: Int = 0,
    val elapsedMillis: Long = 0L,
    val rows: Int = 8,
    val cols: Int = 8,
    /** rows x cols grid, row 0 is the top; null = empty cell */
    val board: List<List<GridTileColor?>> = emptyList(),
    /** Cells of the drag currently in progress, in drag order */
    val dragPath: List<Cell> = emptyList(),
) {
    /** A chain only clears when it contains at least two tiles */
    val dragValid: Boolean get() = dragPath.size >= 2
}
