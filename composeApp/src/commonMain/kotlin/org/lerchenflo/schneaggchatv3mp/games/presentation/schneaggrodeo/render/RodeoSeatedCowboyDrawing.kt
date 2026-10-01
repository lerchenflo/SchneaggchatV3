package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyPart

/**
 * The cowboy's upper body sitting in or on something a vehicle draws itself (balloon basket, cow,
 * boat): torso from the hips at ([hipX], [hipY]) on the vehicle's grid ([p]) up, head and hat.
 * [armToX] / [armToY] reach out to reins or a bar, if given. [wounds] leave out what the ravens
 * pecked off (see RodeoCowboyPart).
 */
internal fun DrawScope.drawSeatedCowboy(
    p: (Float, Float) -> Offset,
    hipX: Float,
    hipY: Float,
    unit: Float,
    colors: ColorScheme,
    armToX: Float? = null,
    armToY: Float? = null,
    wounds: Int = 0,
) {
    val shoulderX = hipX + 0.6f
    val shoulderY = hipY + 4.6f
    drawLine(colors.primary, p(hipX, hipY + 0.8f), p(shoulderX, shoulderY), 2.4f * unit, StrokeCap.Round)
    if (armToX != null && armToY != null && !RodeoCowboyPart.ARM.isLostAt(wounds)) {
        drawLine(colors.primary, p(shoulderX + 0.2f, shoulderY - 0.6f), p(armToX, armToY), 0.8f * unit, StrokeCap.Round)
    }
    drawCircle(colors.onSurface, radius = 1.6f * unit, center = p(hipX + 0.9f, hipY + 6.6f))
    drawCircle(colors.onSurface, radius = 0.45f * unit, center = p(hipX + 2.4f, hipY + 6.4f))
    if (RodeoCowboyPart.HAT.isLostAt(wounds)) return
    drawRoundRect(HAT_COLOR, p(hipX - 1.7f, hipY + 8.4f), Size(5.2f * unit, 0.6f * unit), CornerRadius(0.3f * unit))
    drawRoundRect(HAT_COLOR, p(hipX - 0.6f, hipY + 10.5f), Size(2.8f * unit, 2.2f * unit), CornerRadius(0.7f * unit))
}
