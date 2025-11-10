package com.lerchenflo.hallenmanager.sharedUi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.atan2

@Composable
fun UnderConstruction(
    modifier: Modifier = Modifier,
    stickColor: Color = Color.Black,
    horseColor: Color = Color(0xFF8B4513),
    hatColor: Color = Color(0xFF654321),
    canvasPadding: Dp = 16.dp,
    gallopIntervalMs: Long = 600L,
    legPhaseOffset: Float = 5f, // Controls delay between leg movements (0.0 to 1.0)
    autoStart: Boolean = true
) {
    val gallopPhase = remember { Animatable(0f) }
    val headBob = remember { Animatable(0f) }
    val bodyBounce = remember { Animatable(0f) }
    val backgroundScroll = remember { Animatable(0f) }

    val horseOffsetX = remember { Animatable(0f) }
    val horseOffsetY = remember { Animatable(0f) }
    val horseVelocityX = remember { mutableStateOf(0f) }
    val horseVelocityY = remember { mutableStateOf(0f) }

    val riderOffsetX = remember { Animatable(0f) }
    val riderOffsetY = remember { Animatable(0f) }
    val riderVelocityX = remember { mutableStateOf(0f) }
    val riderVelocityY = remember { mutableStateOf(0f) }
    val riderRotation = remember { Animatable(0f) }

    val isDraggingHorse = remember { mutableStateOf(false) }
    val isDraggingRider = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)

            if (!isDraggingHorse.value) {
                horseVelocityY.value += 0.5f
                horseVelocityX.value *= 0.95f
                horseVelocityY.value *= 0.95f

                val springForceX = -horseOffsetX.value * 0.02f
                val springForceY = -horseOffsetY.value * 0.02f
                horseVelocityX.value += springForceX
                horseVelocityY.value += springForceY

                launch {
                    horseOffsetX.snapTo(horseOffsetX.value + horseVelocityX.value)
                    horseOffsetY.snapTo(horseOffsetY.value + horseVelocityY.value)
                }
            }

            if (!isDraggingRider.value) {
                val targetX = horseOffsetX.value
                val targetY = horseOffsetY.value

                val deltaX = targetX - riderOffsetX.value
                val deltaY = targetY - riderOffsetY.value

                riderVelocityX.value += deltaX * 0.05f
                riderVelocityY.value += deltaY * 0.05f + 0.5f

                riderVelocityX.value *= 0.92f
                riderVelocityY.value *= 0.92f

                launch {
                    riderOffsetX.snapTo(riderOffsetX.value + riderVelocityX.value)
                    riderOffsetY.snapTo(riderOffsetY.value + riderVelocityY.value)

                    val targetRotation = atan2(riderVelocityY.value, riderVelocityX.value) * 0.3f
                    riderRotation.animateTo(
                        targetRotation,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                }
            }
        }
    }

    LaunchedEffect(autoStart, gallopIntervalMs) {
        if (!autoStart) return@LaunchedEffect

        while (true) {
            launch {
                gallopPhase.animateTo(1f, tween(gallopIntervalMs.toInt(), easing = LinearOutSlowInEasing))
                gallopPhase.snapTo(0f)
            }

            launch {
                bodyBounce.animateTo(1f, tween(gallopIntervalMs.toInt() / 2, easing = FastOutSlowInEasing))
                bodyBounce.animateTo(0f, tween(gallopIntervalMs.toInt() / 2, easing = FastOutSlowInEasing))
            }

            launch {
                headBob.animateTo(1f, tween(gallopIntervalMs.toInt() / 2, easing = FastOutSlowInEasing))
                headBob.animateTo(0f, tween(gallopIntervalMs.toInt() / 2, easing = FastOutSlowInEasing))
            }

            launch {
                backgroundScroll.animateTo(
                    backgroundScroll.value + 30f,
                    tween(gallopIntervalMs.toInt(), easing = LinearOutSlowInEasing)
                )
            }

            delay(gallopIntervalMs)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(canvasPadding)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val centerX = size.width * 0.5f
                            val groundY = size.height * 0.75f
                            val horseY = groundY - 150f

                            val distToHorse = sqrt(
                                (offset.x - (centerX + horseOffsetX.value)) * (offset.x - (centerX + horseOffsetX.value)) +
                                        (offset.y - (horseY + horseOffsetY.value)) * (offset.y - (horseY + horseOffsetY.value))
                            )

                            val riderY = horseY - 90f
                            val distToRider = sqrt(
                                (offset.x - (centerX + riderOffsetX.value)) * (offset.x - (centerX + riderOffsetX.value)) +
                                        (offset.y - (riderY + riderOffsetY.value)) * (offset.y - (riderY + riderOffsetY.value))
                            )

                            if (distToRider < 100f) {
                                isDraggingRider.value = true
                            } else if (distToHorse < 150f) {
                                isDraggingHorse.value = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isDraggingHorse.value) {
                                horseVelocityX.value = dragAmount.x
                                horseVelocityY.value = dragAmount.y
                                kotlinx.coroutines.MainScope().launch {
                                    horseOffsetX.snapTo(horseOffsetX.value + dragAmount.x)
                                    horseOffsetY.snapTo(horseOffsetY.value + dragAmount.y)
                                }
                            } else if (isDraggingRider.value) {
                                riderVelocityX.value = dragAmount.x
                                riderVelocityY.value = dragAmount.y
                                kotlinx.coroutines.MainScope().launch {
                                    riderOffsetX.snapTo(riderOffsetX.value + dragAmount.x)
                                    riderOffsetY.snapTo(riderOffsetY.value + dragAmount.y)
                                }
                            }
                        },
                        onDragEnd = {
                            isDraggingHorse.value = false
                            isDraggingRider.value = false
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val centerX = w * 0.5f
            val groundY = h * 0.75f
            val horizonY = groundY - 100f

            val scrollOffset = backgroundScroll.value % w

            // DRAW BACKGROUND FIRST
            // Sky
            drawRect(
                color = Color(0xFF87CEEB),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(w, horizonY)
            )

            // Clouds (moving)
            val cloudPositions = listOf(
                Triple(w * 0.15f, h * 0.15f, 40f),
                Triple(w * 0.35f, h * 0.25f, 50f),
                Triple(w * 0.65f, h * 0.18f, 45f),
                Triple(w * 0.85f, h * 0.22f, 35f)
            )

            for ((baseCloudX, cloudY, cloudSize) in cloudPositions) {
                // Draw clouds twice for seamless scrolling
                for (offset in listOf(-w, 0f, w)) {
                    val cloudX = (baseCloudX - scrollOffset * 0.15f + offset) % (w + w) - w * 0.5f
                    if (cloudX > -cloudSize * 2 && cloudX < w + cloudSize * 2) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            center = Offset(cloudX, cloudY),
                            radius = cloudSize
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            center = Offset(cloudX + cloudSize * 0.6f, cloudY),
                            radius = cloudSize * 0.8f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            center = Offset(cloudX - cloudSize * 0.6f, cloudY),
                            radius = cloudSize * 0.8f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            center = Offset(cloudX, cloudY - cloudSize * 0.4f),
                            radius = cloudSize * 0.7f
                        )
                    }
                }
            }

            // Sun
            val sunX = w * 0.85f
            val sunY = h * 0.15f
            drawCircle(
                color = Color(0xFFFDB813),
                center = Offset(sunX, sunY),
                radius = 40f
            )
            for (i in 0..7) {
                val angle = (i * PI / 4f).toFloat()
                val rayStart = Offset(sunX + cos(angle) * 50f, sunY + sin(angle) * 50f)
                val rayEnd = Offset(sunX + cos(angle) * 70f, sunY + sin(angle) * 70f)
                drawLine(
                    color = Color(0xFFFDB813).copy(alpha = 0.6f),
                    start = rayStart,
                    end = rayEnd,
                    strokeWidth = 3f
                )
            }

            // Mountains (moving slowly)
            for (offset in listOf(-w, 0f, w)) {
                val mountainPath = Path().apply {
                    val baseX = -scrollOffset * 0.3f + offset
                    moveTo(baseX, horizonY)
                    lineTo(baseX + w * 0.2f, horizonY - 80f)
                    lineTo(baseX + w * 0.35f, horizonY - 50f)
                    lineTo(baseX + w * 0.5f, horizonY - 120f)
                    lineTo(baseX + w * 0.65f, horizonY - 70f)
                    lineTo(baseX + w * 0.8f, horizonY - 100f)
                    lineTo(baseX + w, horizonY - 60f)
                    lineTo(baseX + w, horizonY)
                    close()
                }
                drawPath(mountainPath, color = Color(0xFF8B7355).copy(alpha = 0.6f))
            }

            // Desert floor
            drawRect(
                color = Color(0xFFE8C5A5),
                topLeft = Offset(0f, horizonY),
                size = androidx.compose.ui.geometry.Size(w, groundY - horizonY)
            )

            // Cacti (moving)
            val cactiPositions = listOf(
                Pair(w * 0.15f, groundY - 50f),
                Pair(w * 0.45f, groundY - 48f),
                Pair(w * 0.75f, groundY - 45f),
                Pair(w * 0.9f, groundY - 40f)
            )

            for ((baseCactusX, cactusBaseY) in cactiPositions) {
                for (offset in listOf(-w, 0f, w)) {
                    val cactusX = (baseCactusX - scrollOffset * 0.6f + offset) % (w + w)
                    if (cactusX > -50f && cactusX < w + 50f) {
                        drawLine(
                            color = Color(0xFF2D5016),
                            start = Offset(cactusX, cactusBaseY),
                            end = Offset(cactusX, cactusBaseY - 35f),
                            strokeWidth = 8f
                        )
                        drawLine(
                            color = Color(0xFF2D5016),
                            start = Offset(cactusX, cactusBaseY - 20f),
                            end = Offset(cactusX - 12f, cactusBaseY - 20f),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = Color(0xFF2D5016),
                            start = Offset(cactusX - 12f, cactusBaseY - 20f),
                            end = Offset(cactusX - 12f, cactusBaseY - 30f),
                            strokeWidth = 6f
                        )
                    }
                }
            }

            // Ground
            drawRect(
                color = Color(0xFFD4A574),
                topLeft = Offset(0f, groundY),
                size = androidx.compose.ui.geometry.Size(w, h - groundY)
            )

            drawLine(
                color = Color(0xFF8B7355),
                start = Offset(0f, groundY),
                end = Offset(w, groundY),
                strokeWidth = 3f
            )

            for (i in 0..20) {
                val baseRockX = (w / 20f) * i
                for (offset in listOf(-w, 0f, w)) {
                    val rockX = (baseRockX - scrollOffset + offset + (i * 7f) % 20f) % (w + w)
                    if (rockX > -10f && rockX < w + 10f) {
                        drawCircle(
                            color = Color(0xFF8B7355).copy(alpha = 0.5f),
                            center = Offset(rockX, groundY + 5f),
                            radius = 2f + (i % 3)
                        )
                    }
                }
            }

            // NOW DRAW HORSE AND RIDER ON TOP
            val bounce = sin(bodyBounce.value * PI).toFloat() * 15f
            val phase = gallopPhase.value

            val horseBodyWidth = 180f
            val horseBodyHeight = 80f
            val horseBodyCenterX = centerX + horseOffsetX.value
            val horseBodyCenterY = groundY - 150f - bounce + horseOffsetY.value

            val neckLength = 90f
            val headSize = 45f

            fun getLegPosition(legIndex: Int): Pair<Float, Float> {
                val offset = legIndex * PI.toFloat() * legPhaseOffset
                val velocityFactor = (riderVelocityX.value * 0.02f).coerceIn(-1f, 1f)

                // Use bodyBounce instead of gallopPhase for leg movement
                // bodyBounce goes 0->1->0 twice per gallopInterval, giving us the gallop rhythm
                val adjustedPhase = bodyBounce.value + velocityFactor
                val t = (adjustedPhase * PI.toFloat() + offset) % (2f * PI.toFloat())

                val verticalInfluence = (riderVelocityY.value * 0.01f).coerceIn(-0.3f, 0.3f)
                val stride = sin(t) * (0.5f + kotlin.math.abs(velocityFactor) * 0.3f)
                val lift = (1f - cos(t)) * (0.15f + kotlin.math.abs(verticalInfluence))

                return Pair(stride, lift)
            }

            val legLength = 90f
            val legStrokeWidth = 8f

            val legs = listOf(
                Pair(-0.35f, 0),
                Pair(-0.25f, 1),
                Pair(0.25f, 2),
                Pair(0.35f, 3)
            )

            for ((xOffset, legIndex) in legs) {
                val legX = horseBodyCenterX + horseBodyWidth * xOffset
                val legY = horseBodyCenterY + horseBodyHeight * 0.5f

                val (stride, lift) = getLegPosition(legIndex)

                val kneeX = legX + stride * 30f
                val kneeY = legY + legLength * 0.4f - lift * 20f
                val hoofX = legX + stride * 40f
                val hoofY = groundY - 10f - lift * 15f

                drawLine(
                    color = horseColor,
                    start = Offset(legX, legY),
                    end = Offset(kneeX, kneeY),
                    strokeWidth = legStrokeWidth
                )
                drawLine(
                    color = horseColor,
                    start = Offset(kneeX, kneeY),
                    end = Offset(hoofX, hoofY),
                    strokeWidth = legStrokeWidth
                )

                drawCircle(
                    color = horseColor.copy(alpha = 0.8f),
                    center = Offset(hoofX, hoofY),
                    radius = 5f
                )
            }

            drawOval(
                color = horseColor,
                topLeft = Offset(
                    horseBodyCenterX - horseBodyWidth * 0.5f,
                    horseBodyCenterY - horseBodyHeight * 0.5f
                ),
                size = androidx.compose.ui.geometry.Size(horseBodyWidth, horseBodyHeight)
            )

            val neckStartX = horseBodyCenterX - horseBodyWidth * 0.4f
            val neckStartY = horseBodyCenterY - horseBodyHeight * 0.3f
            val neckEndX = neckStartX - 40f
            val neckEndY = neckStartY - neckLength + headBob.value * 5f

            drawLine(
                color = horseColor,
                start = Offset(neckStartX, neckStartY),
                end = Offset(neckEndX, neckEndY),
                strokeWidth = 25f
            )

            val headCenterX = neckEndX - 10f
            val headCenterY = neckEndY - 10f

            drawOval(
                color = horseColor,
                topLeft = Offset(headCenterX - headSize * 0.6f, headCenterY - headSize * 0.4f),
                size = androidx.compose.ui.geometry.Size(headSize * 1.2f, headSize * 0.8f)
            )

            val earPath = Path().apply {
                moveTo(headCenterX - 10f, headCenterY - headSize * 0.4f)
                lineTo(headCenterX - 18f, headCenterY - headSize * 0.65f)
                lineTo(headCenterX - 5f, headCenterY - headSize * 0.5f)
                close()
            }
            drawPath(earPath, color = horseColor)

            for (i in 0..4) {
                val maneX = neckStartX - (i * 10f)
                val maneY = neckStartY - (i * 18f)
                val maneWave = sin(phase * 2f * PI.toFloat() + i * 0.5f) * 5f
                drawLine(
                    color = horseColor.copy(alpha = 0.7f),
                    start = Offset(maneX, maneY),
                    end = Offset(maneX + 15f + maneWave, maneY - 20f),
                    strokeWidth = 4f
                )
            }

            val tailPath = Path().apply {
                val tailStartX = horseBodyCenterX + horseBodyWidth * 0.5f
                val tailStartY = horseBodyCenterY
                val tailWave = sin(phase * 2f * PI.toFloat()) * 15f
                moveTo(tailStartX, tailStartY)
                cubicTo(
                    tailStartX + 30f + tailWave, tailStartY + 20f,
                    tailStartX + 40f + tailWave, tailStartY + 60f,
                    tailStartX + 35f + tailWave * 0.5f, tailStartY + 90f
                )
            }
            drawPath(tailPath, color = horseColor, style = Stroke(width = 6f))

            val saddleY = horseBodyCenterY - horseBodyHeight * 0.5f - 10f
            val baseHipX = horseBodyCenterX + riderOffsetX.value
            val baseHipY = saddleY - bounce + riderOffsetY.value

            val rotation = riderRotation.value
            val cosR = cos(rotation)
            val sinR = sin(rotation)

            fun rotatePoint(x: Float, y: Float, centerX: Float, centerY: Float): Offset {
                val dx = x - centerX
                val dy = y - centerY
                return Offset(
                    centerX + dx * cosR - dy * sinR,
                    centerY + dx * sinR + dy * cosR
                )
            }

            val hipX = baseHipX
            val hipY = baseHipY

            val thighLength = 50f
            val calfLength = 45f
            val legStroke = 6f

            val leftKnee = rotatePoint(hipX - 35f, hipY + thighLength, hipX, hipY)
            val leftFoot = rotatePoint(leftKnee.x + 5f, leftKnee.y + calfLength, hipX, hipY)

            drawLine(color = stickColor, start = Offset(hipX, hipY), end = leftKnee, strokeWidth = legStroke)
            drawLine(color = stickColor, start = leftKnee, end = leftFoot, strokeWidth = legStroke)

            val rightKnee = rotatePoint(hipX + 35f, hipY + thighLength, hipX, hipY)
            val rightFoot = rotatePoint(rightKnee.x - 5f, rightKnee.y + calfLength, hipX, hipY)

            drawLine(color = stickColor, start = Offset(hipX, hipY), end = rightKnee, strokeWidth = legStroke)
            drawLine(color = stickColor, start = rightKnee, end = rightFoot, strokeWidth = legStroke)

            val torsoLength = 70f
            val shoulder = rotatePoint(hipX, hipY - torsoLength - bounce * 0.3f, hipX, hipY)

            drawLine(color = stickColor, start = Offset(hipX, hipY), end = shoulder, strokeWidth = legStroke)

            val leftElbow = rotatePoint(shoulder.x - 35f, shoulder.y + 25f, hipX, hipY)
            val leftHand = rotatePoint(leftElbow.x - 15f, leftElbow.y + 20f, hipX, hipY)

            drawLine(color = stickColor, start = shoulder, end = leftElbow, strokeWidth = legStroke)
            drawLine(color = stickColor, start = leftElbow, end = leftHand, strokeWidth = legStroke)

            val rightElbow = rotatePoint(shoulder.x + 35f, shoulder.y + 25f, hipX, hipY)
            val rightHand = rotatePoint(rightElbow.x + 15f, rightElbow.y + 20f, hipX, hipY)

            drawLine(color = stickColor, start = shoulder, end = rightElbow, strokeWidth = legStroke)
            drawLine(color = stickColor, start = rightElbow, end = rightHand, strokeWidth = legStroke)

            drawLine(
                color = Color.Gray,
                start = leftHand,
                end = Offset(headCenterX, headCenterY + 15f),
                strokeWidth = 2f
            )

            val headRadius = 18f
            val cowboyHead = rotatePoint(shoulder.x, shoulder.y - 35f - headBob.value * 3f, hipX, hipY)

            drawCircle(
                color = stickColor,
                center = cowboyHead,
                radius = headRadius,
                style = Stroke(width = 5f)
            )

            val hatBrimWidth = 60f
            val hatCrownHeight = 25f
            val hatCrownWidth = 35f

            val hatLeft = rotatePoint(cowboyHead.x - hatBrimWidth * 0.5f, cowboyHead.y - headRadius, hipX, hipY)
            val hatRight = rotatePoint(cowboyHead.x + hatBrimWidth * 0.5f, cowboyHead.y - headRadius, hipX, hipY)

            drawLine(color = hatColor, start = hatLeft, end = hatRight, strokeWidth = 4f)

            val hatTop = rotatePoint(
                cowboyHead.x - hatCrownWidth * 0.5f,
                cowboyHead.y - headRadius - hatCrownHeight,
                hipX,
                hipY
            )

            drawRoundRect(
                color = hatColor,
                topLeft = hatTop,
                size = androidx.compose.ui.geometry.Size(hatCrownWidth, hatCrownHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "Coming soon... (Drag the horse or rider!)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    MaterialTheme {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5E6D3))) {
            UnderConstruction(
                legPhaseOffset = 0.5f  // Adjust this value: 0.25f (fast), 0.5f (normal), 1.0f (slow)
            )
        }
    }
}