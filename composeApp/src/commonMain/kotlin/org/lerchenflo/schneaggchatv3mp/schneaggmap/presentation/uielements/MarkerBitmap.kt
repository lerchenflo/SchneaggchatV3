package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

/**
 * Composites [profilePictures] into a ring around a translucent [backgroundColor] disc, with a
 * [beerIcon] between each adjacent pair of avatars - a "Hock" (group hangout) marker for several
 * users merged into one point on the map. [avatarSize] is the diameter of each avatar; the ring
 * radius and beer size scale off of it.
 */
fun mergeClusterAvatarsIcon(
    profilePictures: List<ImageBitmap>,
    beerIcon: ImageBitmap,
    backgroundColor: Color,
    density: Density,
    avatarSize: Dp = 24.dp,
): ImageBitmap {
    val avatarSizePx = with(density) { avatarSize.toPx() }
    val borderWidthPx = with(density) { 2.dp.toPx() }
    val smallBeerSizePx = avatarSizePx * 0.45f
    val ringRadiusPx = avatarSizePx * 1.05f
    val smallBeerRadiusPx = avatarSizePx * 0.7f

    val canvasRadiusPx = ringRadiusPx + avatarSizePx / 2f + borderWidthPx
    val canvasSizePx = canvasRadiusPx * 2f

    val result = ImageBitmap(
        width = canvasSizePx.roundToInt().coerceAtLeast(1),
        height = canvasSizePx.roundToInt().coerceAtLeast(1),
        config = ImageBitmapConfig.Argb8888,
        hasAlpha = true,
    )

    fun DrawScope.drawCenteredImage(image: ImageBitmap, center: Offset, size: Float) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset((center.x - size / 2f).roundToInt(), (center.y - size / 2f).roundToInt()),
            dstSize = IntSize(size.roundToInt(), size.roundToInt()),
        )
    }

    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(result),
        size = Size(canvasSizePx, canvasSizePx),
    ) {
        val center = Offset(canvasRadiusPx, canvasRadiusPx)

        //Translucent disc, like a table seen from above.
        drawCircle(color = backgroundColor, radius = canvasRadiusPx, center = center)

        val count = profilePictures.size
        if (count > 0) {
            val angleStep = (2 * PI / count).toFloat()
            profilePictures.forEachIndexed { index, avatar ->
                val angle = index * angleStep

                //Avatar on the outer ring.
                val avatarCenter = Offset(
                    center.x + ringRadiusPx * cos(angle),
                    center.y + ringRadiusPx * sin(angle),
                )
                drawCenteredImage(avatar, avatarCenter, avatarSizePx)

                //Beer drawn on top of the avatar, pulled towards the center so it faces inward -
                //as if each person is holding it out towards the middle of the table.
                val beerCenter = Offset(
                    center.x + smallBeerRadiusPx * cos(angle),
                    center.y + smallBeerRadiusPx * sin(angle),
                )
                drawCenteredImage(beerIcon, beerCenter, smallBeerSizePx)
            }
        }
    }

    return result
}
