package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
private val Y_STEP = 68.dp
private val NODE_RADIUS = 10.dp
private val LAST_ROW_EXTRA_SPACING = 32.dp  // extra vertical gap before the leaf (letter/digit) row

@Composable
fun MorseTreeView(currentCode: String, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val activeColor = MaterialTheme.colorScheme.primary
    val activeOnColor = MaterialTheme.colorScheme.onPrimary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val inactiveOnColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dotEdgeColor = MaterialTheme.colorScheme.tertiary
    val dashEdgeColor = MaterialTheme.colorScheme.secondary

    val rootNode = MORSE_TREE
    val shownLevels = MAX_DEPTH

    // The tree isn't a perfect binary tree — only the digits reach the last
    // row, and their raw binary-tree slots leave them bunched at the two
    // ends with a large empty gap in the middle. Spread the leaf nodes that
    // actually exist evenly across the width instead of using their slot,
    // even though that means their connecting lines are no longer vertical.
    val leafXFractions = remember(rootNode) {
        val leaves = mutableListOf<String>()
        fun collect(node: MorseTreeNode) {
            if (node.code.length == MAX_DEPTH) {
                leaves += node.code
                return
            }
            node.dot?.let { collect(it) }
            node.dash?.let { collect(it) }
        }
        collect(rootNode)
        leaves.mapIndexed { index, code -> code to (index + 0.5f) / leaves.size }.toMap()
    }

    BoxWithConstraints(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .heightIn(max = Y_STEP * (shownLevels + 1))
                .fillMaxHeight()
        ) {
            drawMorseNode(
                node = rootNode,
                shownLevels = shownLevels,
                position = 0,
                currentCode = currentCode,
                leafXFractions = leafXFractions,
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

/** x-position (in px) for a node at [relativeDepth] / tree-[position]. Leaf-row
 *  nodes use their evenly spread fraction from [leafXFractions] instead of
 *  their raw binary-tree slot. */
private fun DrawScope.xPositionFor(
    code: String,
    position: Int,
    relativeDepth: Int,
    shownLevels: Int,
    leafXFractions: Map<String, Float>,
): Float {
    return if (relativeDepth == shownLevels) {
        val fraction = leafXFractions[code] ?: ((position + 0.5f) / (1 shl relativeDepth))
        fraction * size.width
    } else {
        val slotSize = size.width / (1 shl relativeDepth)
        (position + 0.5f) * slotSize
    }
}

private fun DrawScope.drawMorseNode(
    node: MorseTreeNode,
    shownLevels: Int,
    position: Int,
    currentCode: String,
    leafXFractions: Map<String, Float>,
    textMeasurer: TextMeasurer,
    activeColor: Color,
    activeOnColor: Color,
    inactiveColor: Color,
    inactiveOnColor: Color,
    dotEdgeColor: Color,
    dashEdgeColor: Color,
) {
    val relativeDepth = node.code.length
    val extraLastRowGapPx = LAST_ROW_EXTRA_SPACING.toPx()
    // Reserve LAST_ROW_EXTRA_SPACING out of the total height, then hand it out
    // as one extra gap right before the leaf row.
    val yStepPx = (size.height - extraLastRowGapPx) / (shownLevels + 1)
    val nodeRadiusPx = NODE_RADIUS.toPx()
    val x = xPositionFor(node.code, position, relativeDepth, shownLevels, leafXFractions)
    val y = relativeDepth * yStepPx + yStepPx * 0.5f +
            if (relativeDepth == shownLevels) extraLastRowGapPx else 0f
    val onPath = currentCode.startsWith(node.code)
    val isCurrent = node.code.isNotEmpty() && node.code == currentCode

    if (relativeDepth < shownLevels) {
        val childRelativeDepth = relativeDepth + 1
        val childY = childRelativeDepth * yStepPx + yStepPx * 0.5f +
                if (childRelativeDepth == shownLevels) extraLastRowGapPx else 0f

        // The two edges directly under the displayed root are the live choices,
        // draw them thicker.
        val baseStrokePx = if (relativeDepth == 0) 2.5f.dp.toPx() else 1.5f.dp.toPx()
        node.dot?.let { dotChild ->
            val childPosition = position * 2
            val childX = xPositionFor(dotChild.code, childPosition, childRelativeDepth, shownLevels, leafXFractions)
            val onDotPath = currentCode.startsWith(dotChild.code)
            drawLine(
                color = if (onDotPath) dotEdgeColor else dotEdgeColor.copy(alpha = 0.4f),
                start = Offset(x, y),
                end = Offset(childX, childY),
                strokeWidth = if (onDotPath) 3.dp.toPx() else baseStrokePx
            )
            drawMorseNode(
                node = dotChild, shownLevels = shownLevels,
                position = childPosition, currentCode = currentCode,
                leafXFractions = leafXFractions,
                textMeasurer = textMeasurer,
                activeColor = activeColor, activeOnColor = activeOnColor,
                inactiveColor = inactiveColor, inactiveOnColor = inactiveOnColor,
                dotEdgeColor = dotEdgeColor, dashEdgeColor = dashEdgeColor,
            )
        }
        node.dash?.let { dashChild ->
            val childPosition = position * 2 + 1
            val childX = xPositionFor(dashChild.code, childPosition, childRelativeDepth, shownLevels, leafXFractions)
            val onDashPath = currentCode.startsWith(dashChild.code)
            drawLine(
                color = if (onDashPath) dashEdgeColor else dashEdgeColor.copy(alpha = 0.4f),
                start = Offset(x, y),
                end = Offset(childX, childY),
                strokeWidth = if (onDashPath) 3.dp.toPx() else baseStrokePx
            )
            drawMorseNode(
                node = dashChild, shownLevels = shownLevels,
                position = childPosition, currentCode = currentCode,
                leafXFractions = leafXFractions,
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
            TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
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