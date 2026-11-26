package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

/**
 * Snapping of the point row ([RoutePointCards]): a fling goes on to the next card in its direction
 * and skips the drive-through dots in between, a slow release rests on the nearest card or dot.
 * Item starts snap to the row's start padding. Items outside the viewport are placed by their fixed
 * sizes, so a card several dots away is found too.
 */
internal class RoutePointRowSnapping(
    private val listState: LazyListState,
    private val isCard: List<Boolean>,
    private val cardSizePx: Int,
    private val dotSizePx: Int,
    private val spacingPx: Int,
    private val minFlingVelocityPx: Float,
) : SnapLayoutInfoProvider {

    // No free decay: the snap target alone decides where a fling ends
    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f

    override fun calculateSnapOffset(velocity: Float): Float {
        val offsets = itemOffsets() ?: return 0f
        val target = when {
            velocity >= minFlingVelocityPx ->
                offsets.indices.firstOrNull { isCard[it] && offsets[it] > 0 } ?: offsets.lastIndex
            velocity <= -minFlingVelocityPx ->
                offsets.indices.lastOrNull { isCard[it] && offsets[it] < 0 } ?: 0
            else -> offsets.indices.minBy { abs(offsets[it]) }
        }
        return offsets[target].toFloat()
    }

    /** Distance of every item's start from the snap position; null before the first layout. */
    private fun itemOffsets(): IntArray? {
        val layoutInfo = listState.layoutInfo
        val count = minOf(isCard.size, layoutInfo.totalItemsCount)
        val visible = layoutInfo.visibleItemsInfo.filter { it.index < count }.associateBy { it.index }
        val first = visible.keys.minOrNull() ?: return null

        val offsets = IntArray(count)
        offsets[first] = visible.getValue(first).offset
        for (index in first + 1 until count) {
            offsets[index] = visible[index]?.offset ?: (offsets[index - 1] + sizeOf(index - 1, visible) + spacingPx)
        }
        for (index in first - 1 downTo 0) {
            offsets[index] = offsets[index + 1] - sizeOf(index, visible) - spacingPx
        }
        return offsets
    }

    private fun sizeOf(index: Int, visible: Map<Int, LazyListItemInfo>): Int =
        visible[index]?.size ?: if (isCard[index]) cardSizePx else dotSizePx
}
