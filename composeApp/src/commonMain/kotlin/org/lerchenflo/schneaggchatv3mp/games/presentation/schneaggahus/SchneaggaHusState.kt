package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.DIRECTION.*

sealed interface SchneaggaHusAction{
    data class OnConsecrationClick(val position: Position) : SchneaggaHusAction
}

data class SchneaggaHusState(

    val schneaggList: List<Schneagg>,

    val schneagghusList: List<Schneaggahus>,

    val schneaggaPathList: List<SchneaggaPath>

)

data class Position(
    val x: Int,
    val y: Int
)

data class FloatPosition(
    var x: Float,
    var y: Float
)

data class Schneagg(
    var direction: DIRECTION,

    var position: FloatPosition,

    val color: Color,
) {
    fun move(newDirection: DIRECTION?): Schneagg {
        val resolvedDirection = newDirection ?: direction

        val newPosition = when (resolvedDirection) {
            NORTH -> position.copy(y = position.y - SCHNEAGGHUS_SPEED)
            EAST  -> position.copy(x = position.x + SCHNEAGGHUS_SPEED)
            SOUTH -> position.copy(y = position.y + SCHNEAGGHUS_SPEED)
            WEST  -> position.copy(x = position.x - SCHNEAGGHUS_SPEED)
        }

        return copy(direction = resolvedDirection, position = newPosition)
    }
}

data class Schneaggahus(
    val position: Position,

    val color: Color
)

data class SchneaggaPath(
    val startPoint: DIRECTION,
    val endPoint: DIRECTION,

    //val isDuplicator: Boolean = false,

    val position: Position,

    val toggleOptions: Pair<DIRECTION, DIRECTION>?
) {
    fun isStraight() : Boolean {
        return (startPoint == DIRECTION.NORTH && endPoint == DIRECTION.SOUTH)
                || (startPoint == DIRECTION.WEST && endPoint == DIRECTION.EAST)
                || (startPoint == DIRECTION.SOUTH && endPoint == DIRECTION.NORTH)
                || (startPoint == DIRECTION.EAST && endPoint == DIRECTION.WEST)
    }

    fun rotation(): Float {
        val pair = setOf(startPoint, endPoint)
        return when {
            pair == setOf(DIRECTION.NORTH, DIRECTION.WEST) -> 0f
            pair == setOf(DIRECTION.NORTH, DIRECTION.EAST) -> 90f
            pair == setOf(DIRECTION.SOUTH, DIRECTION.EAST) -> 180f
            pair == setOf(DIRECTION.SOUTH, DIRECTION.WEST) -> 270f
            else -> 0f
        }
    }

}

enum class DIRECTION {
    NORTH,
    EAST,
    SOUTH,
    WEST,
}
