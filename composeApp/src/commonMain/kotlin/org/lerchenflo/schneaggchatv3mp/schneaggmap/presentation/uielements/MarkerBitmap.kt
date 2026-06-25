package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Composites [profilePicture] (scaled into a [pictureSize] square, top/top-center) with
 * [username] and [statusText] drawn underneath, stacked on a rounded [backgroundColor] pill.
 * Everything else in the resulting bitmap is transparent, so it can be used directly as a map
 * marker icon.
 */
fun mergeProfilePictureWithStatusText(
    profilePicture: ImageBitmap,
    username: String,
    statusText: String,
    backgroundColor: Color,
    nameColor: Color,
    statusColor: Color,
    textMeasurer: TextMeasurer,
    density: Density,
    pictureSize: Dp = 40.dp,
): ImageBitmap {
    val nameStyle = TextStyle(
        color = nameColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    val statusStyle = TextStyle(color = statusColor, fontSize = 10.sp, textAlign = TextAlign.Center)

    val nameLayout = textMeasurer.measure(text = AnnotatedString(username), style = nameStyle)
    val statusLayout = textMeasurer.measure(text = AnnotatedString(statusText), style = statusStyle)

    val pictureSizePx = with(density) { pictureSize.toPx() }.roundToInt()
    val horizontalPaddingPx = with(density) { 6.dp.toPx() }
    val verticalPaddingPx = with(density) { 3.dp.toPx() }
    val lineSpacingPx = with(density) { 1.dp.toPx() }
    val spacingPx = with(density) { 4.dp.toPx() }
    val cornerRadiusPx = with(density) { 6.dp.toPx() }

    val textBlockWidth = maxOf(nameLayout.size.width, statusLayout.size.width).toFloat()
    val textBlockHeight = nameLayout.size.height + lineSpacingPx + statusLayout.size.height

    val pillWidth = textBlockWidth + horizontalPaddingPx * 2
    val pillHeight = textBlockHeight + verticalPaddingPx * 2

    val totalWidth = maxOf(pictureSizePx.toFloat(), pillWidth)
    val totalHeight = pictureSizePx + spacingPx + pillHeight

    val result = ImageBitmap(
        width = totalWidth.roundToInt(),
        height = totalHeight.roundToInt(),
        config = ImageBitmapConfig.Argb8888,
        hasAlpha = true,
    )

    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(result),
        size = Size(totalWidth, totalHeight),
    ) {
        val pictureLeft = ((totalWidth - pictureSizePx) / 2f).roundToInt()
        drawImage(
            image = profilePicture,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(profilePicture.width, profilePicture.height),
            dstOffset = IntOffset(pictureLeft, 0),
            dstSize = IntSize(pictureSizePx, pictureSizePx),
        )

        val pillLeft = (totalWidth - pillWidth) / 2f
        val pillTop = pictureSizePx + spacingPx
        drawRoundRect(
            color = backgroundColor,
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        )

        val nameTop = pillTop + verticalPaddingPx
        drawText(
            textLayoutResult = nameLayout,
            topLeft = Offset(pillLeft + (pillWidth - nameLayout.size.width) / 2f, nameTop),
        )

        val statusTop = nameTop + nameLayout.size.height + lineSpacingPx
        drawText(
            textLayoutResult = statusLayout,
            topLeft = Offset(pillLeft + (pillWidth - statusLayout.size.width) / 2f, statusTop),
        )
    }

    return result
}
