package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_DEPTH = 5
private const val LEAF_SLOTS = 32  // 2^5
private val SLOT_WIDTH = 38.dp
private val Y_STEP = 68.dp
private val NODE_RADIUS = 17.dp

@Composable
fun MorseTreeView(currentCode: String, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    val activeColor = MaterialTheme.colorScheme.primary
    val activeOnColor = MaterialTheme.colorScheme.onPrimary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val inactiveOnColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dotEdgeColor = MaterialTheme.colorScheme.tertiary
    val dashEdgeColor = MaterialTheme.colorScheme.secondary

    val canvasWidthDp = SLOT_WIDTH * LEAF_SLOTS
    val canvasHeightDp = Y_STEP * (MAX_DEPTH + 1)

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Canvas(modifier = Modifier.size(canvasWidthDp, canvasHeightDp)) {
            drawMorseNode(
                node = MORSE_TREE,
                depth = 0,
                position = 0,
                currentCode = currentCode,
                entered = "",
                textMeasurer = textMeasurer,
                activeColor = activeColor,
                activeOnColor = activeOnColor,
                inactiveColor = inactiveColor,
                inactiveOnColor = inactiveOnColor,
                dotEdgeColor = dotEdgeColor,
                dashEdgeColor = dashEdgeColor,
            )
        }
    }
}

private fun DrawScope.drawMorseNode(
    node: MorseTreeNode?,
    depth: Int,
    position: Int,
    currentCode: String,
    entered: String,
    textMeasurer: TextMeasurer,
    activeColor: Color,
    activeOnColor: Color,
    inactiveColor: Color,
    inactiveOnColor: Color,
    dotEdgeColor: Color,
    dashEdgeColor: Color,
) {
    if (node == null) return

    val yStepPx = size.height / (MAX_DEPTH + 1)
    val nodeRadiusPx = NODE_RADIUS.toPx()

    val slotSize = size.width / (1 shl depth)
    val x = (position + 0.5f) * slotSize
    val y = depth * yStepPx + yStepPx * 0.5f

    val onPath = currentCode.startsWith(entered)
    val isCurrent = entered.isNotEmpty() && entered == currentCode

    val childDepth = depth + 1
    val childSlotSize = size.width / (1 shl childDepth)

    node.dot?.let { dotChild ->
        val childX = (position * 2 + 0.5f) * childSlotSize
        val childY = childDepth * yStepPx + yStepPx * 0.5f
        val onDotPath = currentCode.startsWith("$entered.")
        drawLine(
            color = if (onDotPath) dotEdgeColor else inactiveColor.copy(alpha = 0.5f),
            start = Offset(x, y),
            end = Offset(childX, childY),
            strokeWidth = if (onDotPath) 3.dp.toPx() else 1.5f.dp.toPx()
        )
        drawMorseNode(
            node = dotChild, depth = childDepth, position = position * 2,
            currentCode = currentCode, entered = "$entered.",
            textMeasurer = textMeasurer,
            activeColor = activeColor, activeOnColor = activeOnColor,
            inactiveColor = inactiveColor, inactiveOnColor = inactiveOnColor,
            dotEdgeColor = dotEdgeColor, dashEdgeColor = dashEdgeColor,
        )
    }

    node.dash?.let { dashChild ->
        val childX = (position * 2 + 1 + 0.5f) * childSlotSize
        val childY = childDepth * yStepPx + yStepPx * 0.5f
        val onDashPath = currentCode.startsWith("$entered-")
        drawLine(
            color = if (onDashPath) dashEdgeColor else inactiveColor.copy(alpha = 0.5f),
            start = Offset(x, y),
            end = Offset(childX, childY),
            strokeWidth = if (onDashPath) 3.dp.toPx() else 1.5f.dp.toPx()
        )
        drawMorseNode(
            node = dashChild, depth = childDepth, position = position * 2 + 1,
            currentCode = currentCode, entered = "$entered-",
            textMeasurer = textMeasurer,
            activeColor = activeColor, activeOnColor = activeOnColor,
            inactiveColor = inactiveColor, inactiveOnColor = inactiveOnColor,
            dotEdgeColor = dotEdgeColor, dashEdgeColor = dashEdgeColor,
        )
    }

    if (depth == 0) return  // root has no circle — it's just the branch origin

    val circleColor = when {
        isCurrent -> activeColor
        onPath -> activeColor.copy(alpha = 0.45f)
        else -> inactiveColor
    }
    val textColor = when {
        isCurrent || onPath -> activeOnColor
        else -> inactiveOnColor
    }

    drawCircle(color = circleColor, radius = nodeRadiusPx, center = Offset(x, y))

    val label = node.char?.toString() ?: ""
    if (label.isNotEmpty()) {
        val measured = textMeasurer.measure(
            label,
            TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
        )
        drawText(
            measured,
            topLeft = Offset(
                x = x - measured.size.width / 2f,
                y = y - measured.size.height / 2f
            )
        )
    }
}
