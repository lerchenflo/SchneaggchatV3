package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
private const val SUBTREE_LEVELS = 3  // levels below the current node on narrow screens
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

    val fullTreeWidthDp = SLOT_WIDTH * LEAF_SLOTS

    BoxWithConstraints(modifier = modifier) {
        val fullTreeFits = maxWidth >= fullTreeWidthDp

        // On narrow (portrait) screens the full tree can never fit, so the view
        // re-roots at the currently entered code and only shows the reachable
        // subtree — everything stays full size without scrolling.
        val rootNode = if (fullTreeFits) MORSE_TREE else nodeForCode(currentCode)
        val rootDepth = rootNode.code.length
        val shownLevels = if (fullTreeFits) {
            MAX_DEPTH
        } else {
            minOf(SUBTREE_LEVELS, MAX_DEPTH - rootDepth)
        }

        val widthModifier = if (fullTreeFits) {
            Modifier.width(fullTreeWidthDp)
        } else {
            Modifier.fillMaxWidth()
        }

        Canvas(
            modifier = widthModifier
                .align(Alignment.TopCenter)
                .heightIn(max = Y_STEP * (shownLevels + 1))
                .fillMaxHeight()
        ) {
            drawMorseNode(
                node = rootNode,
                rootDepth = rootDepth,
                shownLevels = shownLevels,
                position = 0,
                currentCode = currentCode,
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
    node: MorseTreeNode,
    rootDepth: Int,
    shownLevels: Int,
    position: Int,
    currentCode: String,
    textMeasurer: TextMeasurer,
    activeColor: Color,
    activeOnColor: Color,
    inactiveColor: Color,
    inactiveOnColor: Color,
    dotEdgeColor: Color,
    dashEdgeColor: Color,
) {
    val relativeDepth = node.code.length - rootDepth
    val yStepPx = size.height / (shownLevels + 1)
    val nodeRadiusPx = NODE_RADIUS.toPx()

    val slotSize = size.width / (1 shl relativeDepth)
    val x = (position + 0.5f) * slotSize
    val y = relativeDepth * yStepPx + yStepPx * 0.5f

    val onPath = currentCode.startsWith(node.code)
    val isCurrent = node.code.isNotEmpty() && node.code == currentCode

    if (relativeDepth < shownLevels) {
        val childSlotSize = size.width / (1 shl (relativeDepth + 1))
        val childY = (relativeDepth + 1) * yStepPx + yStepPx * 0.5f
        // The two edges directly under the displayed root are the live choices,
        // draw them thicker.
        val baseStrokePx = if (relativeDepth == 0) 2.5f.dp.toPx() else 1.5f.dp.toPx()

        node.dot?.let { dotChild ->
            val childX = (position * 2 + 0.5f) * childSlotSize
            val onDotPath = currentCode.startsWith(dotChild.code)
            drawLine(
                color = if (onDotPath) dotEdgeColor else dotEdgeColor.copy(alpha = 0.4f),
                start = Offset(x, y),
                end = Offset(childX, childY),
                strokeWidth = if (onDotPath) 3.dp.toPx() else baseStrokePx
            )
            drawMorseNode(
                node = dotChild, rootDepth = rootDepth, shownLevels = shownLevels,
                position = position * 2, currentCode = currentCode,
                textMeasurer = textMeasurer,
                activeColor = activeColor, activeOnColor = activeOnColor,
                inactiveColor = inactiveColor, inactiveOnColor = inactiveOnColor,
                dotEdgeColor = dotEdgeColor, dashEdgeColor = dashEdgeColor,
            )
        }

        node.dash?.let { dashChild ->
            val childX = (position * 2 + 1 + 0.5f) * childSlotSize
            val onDashPath = currentCode.startsWith(dashChild.code)
            drawLine(
                color = if (onDashPath) dashEdgeColor else dashEdgeColor.copy(alpha = 0.4f),
                start = Offset(x, y),
                end = Offset(childX, childY),
                strokeWidth = if (onDashPath) 3.dp.toPx() else baseStrokePx
            )
            drawMorseNode(
                node = dashChild, rootDepth = rootDepth, shownLevels = shownLevels,
                position = position * 2 + 1, currentCode = currentCode,
                textMeasurer = textMeasurer,
                activeColor = activeColor, activeOnColor = activeOnColor,
                inactiveColor = inactiveColor, inactiveOnColor = inactiveOnColor,
                dotEdgeColor = dotEdgeColor, dashEdgeColor = dashEdgeColor,
            )
        }
    }

    if (node.code.isEmpty()) return  // the true root has no circle — it's just the branch origin

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
