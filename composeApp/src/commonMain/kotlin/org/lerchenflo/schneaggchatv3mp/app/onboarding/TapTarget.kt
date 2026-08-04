package org.lerchenflo.schneaggchatv3mp.app.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.navigation.parentRootOrNull
import kotlin.collections.get
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds


enum class FreeRoamBarPosition { Top, Center, Bottom }

// ─────────────────────────────────────────────────────────────────
// Step 1 — Tour DSL
// ─────────────────────────────────────────────────────────────────

/**
 * Describes a single step in an onboarding tour.
 *
 * @param id            Unique identifier; must match the id passed to [tapTarget].
 * @param title         Optional headline shown in the info bubble.
 * @param description   Optional body text shown in the info bubble.
 * @param route         Optional screen class. When set, the controller will navigate
 *                      to this route before the step is displayed so that the target
 *                      composable is guaranteed to be in the composition.
 * @param backgroundColor  Scrim color rendered behind the spotlight hole.
 */
data class TourStep(
    val id: String?,
    val title: StringResource? = null,
    val description: StringResource? = null,
    val route: Route? = null,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.75f),
    /** Non-null marks this a free-roam step: shows a non-blocking hint bar at this
     *  position instead of a spotlight or blocking dialog. Everything else on screen
     *  stays fully interactive; only the bar's Continue button advances the tour. */
    val freeRoamPosition: FreeRoamBarPosition? = null,
    /** Continue button label. Falls back to plain "Continue" text if null. */
    val continueButtonText: StringResource? = null,
)

/** Immutable tour description produced by [tapTargetTour]. */
class TapTargetTour(val steps: List<TourStep>)

/** Builder populated inside the [tapTargetTour] lambda. */
class TourBuilder {
    val steps = mutableListOf<TourStep>()

    /**
     * Adds a step to the tour.
     *
     * Example:
     * ```kotlin
     * val tour = tapTargetTour {
     *     step(id = "new_chat", title = "Start chatting", description = "Tap here to begin")
     *     step(id = "settings", title = "Settings", route = Settings::class)
     * }
     * ```
     */
    fun tapStep(
        id: String,
        title: StringResource? = null,
        description: StringResource? = null,
        route: Route? = null,
        backgroundColor: Color = Color.Black.copy(alpha = 0.75f),
    ) {
        steps += TourStep(id, title, description, route, backgroundColor)
    }

    fun infoStep(
        title: StringResource? = null,
        description: StringResource? = null,
        route: Route? = null,
        backgroundColor: Color = Color.Black.copy(alpha = 0.75f),
    ) {
        steps += TourStep(null, title, description, route, backgroundColor)
    }

    /**
     * Adds a step that lets the user freely explore the app while a small, non-blocking
     * hint bar stays anchored at [position]. Unlike [tapStep]/[infoStep], tapping
     * elsewhere on screen does nothing — the tour only advances when the user presses
     * the bar's Continue button.
     *
     * Example:
     * ```kotlin
     * freeRoamStep(
     *     title = Res.string.ttt_explore_title,
     *     description = Res.string.ttt_explore_description,
     *     position = FreeRoamBarPosition.Bottom,
     * )
     * ```
     */
    fun freeRoamStep(
        title: StringResource? = null,
        description: StringResource? = null,
        route: Route? = null,
        position: FreeRoamBarPosition = FreeRoamBarPosition.Bottom,
        continueButtonText: StringResource? = null,
    ) {
        steps += TourStep(
            id = null,
            title = title,
            description = description,
            route = route,
            freeRoamPosition = position,
            continueButtonText = continueButtonText,
        )
    }
}

/**
 * Entry point for defining a tour.
 *
 * ```kotlin
 * val onboardingTour = tapTargetTour {
 *     step("new_chat", title = "Start chatting", description = "Tap here to start a new chat")
 *     step("settings", title = "Settings", route = Settings::class)
 * }
 * ```
 */
fun tapTargetTour(builder: TourBuilder.() -> Unit): TapTargetTour =
    TapTargetTour(TourBuilder().apply(builder).steps)

// ─────────────────────────────────────────────────────────────────
// Step 2 — Controller
// ─────────────────────────────────────────────────────────────────

/** Stores the screen-space bounding box of a registered target composable. */
data class TargetInfo(val bounds: Rect)

data class TourSettings(
    val iconPadding: Dp = 4.dp,
    val cornerRadius: Dp = 16.dp
)

/**
 * Single source of truth for the active onboarding tour.
 *
 * Create exactly one instance with [remember] at the root of your composition, then
 * provide it via [LocalTapTargetController]. Do **not** pass it around manually —
 * call sites only need [tapTarget].
 *
 * @param tour                The tour definition produced by [tapTargetTour].
 * @param onNavigateToRoute   Called when the current step lives on a different screen.
 *                            Implementation depends on the navigation library in use.
 * @param currentRoute        Returns the currently active route class, used to decide
 *                            whether navigation is needed before a step.
 * @param onFinished          Invoked when the tour completes (all steps shown) or
 *                            is skipped. Persist "onboarding_seen" here.
 */
class TapTargetController(
    private val tour: TapTargetTour,
    private val onNavigateToRoute: suspend (Route) -> Unit = {},
    private val currentRoute: () -> Route? = { null },
    private val onFinished: () -> Unit = {},
    val tourSettings: TourSettings = TourSettings()
) {
    // Targets registered by Modifier.tapTarget — keyed by step id.
    private val targets = mutableStateMapOf<String, TargetInfo>()

    /** Zero-based index of the step currently being spotlighted. */
    var currentIndex by mutableStateOf(0)
        private set

    /** Whether the tour is currently active. Defaults to false. */
    var active by mutableStateOf(false)
        private set

    /** The [TourStep] being spotlighted right now, or null when the tour is over. */
    val currentStep: TourStep? get() = if (isActive) tour.steps.getOrNull(currentIndex) else null

    /** The bounds of the currently spotlighted composable, or null if not yet laid out. */
    val currentTarget: TargetInfo? get() = currentStep?.let { targets[it.id] }

    /** True while tour is active and there are still steps left to show. */
    val isActive: Boolean get() = active && currentIndex < tour.steps.size

    // ── Target registration ──────────────────────────────────────

    /** Called automatically by [tapTarget] on every layout pass. */
    fun register(id: String, bounds: Rect) {
        targets[id] = TargetInfo(bounds)
    }

    /** Called automatically by [tapTarget] when the composable leaves the composition. */
    fun unregister(id: String) {
        targets.remove(id)
    }

    /**
     * Resets the tour to the first step and activates it.
     */
    fun start() {
        currentIndex = 0
        active = true
    }

    /**
     * Advances to the next step, or finishes the tour if all steps have been shown.
     * Bind this to the user's tap gesture on the overlay.
     */
    fun next() {
        if (currentIndex < tour.steps.size) currentIndex++
        if (currentIndex >= tour.steps.size) {
            active = false
            onFinished()
        }
    }

    /**
     * Immediately ends the tour without showing the remaining steps.
     * Calls [onFinished] so persistence can happen in the same callback.
     */
    fun skip() {
        currentIndex = tour.steps.size
        active = false
        onFinished()
    }

    private fun isMatchingRoute(targetRoute: Route, activeRoute: Route?): Boolean {
        if (activeRoute == null) return false
        if (targetRoute::class == activeRoute::class) return true
        if (targetRoute == Route.Settings && activeRoute.parentRootOrNull() == Route.Settings) return true
        if (targetRoute == Route.Games && activeRoute.parentRootOrNull() == Route.Games) return true
        return false
    }

    /**
     * Internal: called by [TapTargetOverlay] inside a [LaunchedEffect] to guarantee
     * the target composable is visible before the overlay draws it.
     *
     * 1. If the target is already registered (i.e. the composable is on screen), returns immediately.
     * 2. If the step declares a [TourStep.route] and it differs from [currentRoute], triggers navigation.
     * 3. Polls until the target registers itself (max [timeoutMs] ms).
     *    If the target never appears the overlay simply draws nothing for that frame —
     *    the user can tap through to the next step.
     *
     * **Not intended for use at call sites.**
     */
    suspend fun ensureCurrentStepVisible(timeoutMs: Long = 2000) {
        val step = currentStep ?: return

        val route = step.route
        val activeRoute = currentRoute()
        if (route != null && !isMatchingRoute(route, activeRoute)) {
            onNavigateToRoute(route)
        }

        if (step.id == null) return   // no target to wait for
        if (targets.containsKey(step.id)) return

        withTimeoutOrNull(timeoutMs.milliseconds) {
            while (!targets.containsKey(step.id)) delay(16.milliseconds)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Step 3 — CompositionLocal + Modifier
// ─────────────────────────────────────────────────────────────────

/**
 * Provides the active [TapTargetController] to every composable in the subtree.
 *
 * Defaults to `null` so that [tapTarget] is a no-op in previews, tests,
 * or screens not part of any tour. Swap the default to `error(…)` if you prefer
 * a hard crash when the controller is accidentally missing in production.
 *
 * Use [staticCompositionLocalOf] because the controller instance itself doesn't
 * change during the app's lifetime — avoiding the recomposition-tracking overhead
 * that [compositionLocalOf] would add.
 */
val LocalTapTargetController = staticCompositionLocalOf<TapTargetController?> { null }

/**
 * Registers this composable as the spotlight target for the tour step with [id].
 *
 * This is the **only** thing a call site needs to add — the controller is read
 * automatically from [LocalTapTargetController]:
 *
 * ```kotlin
 * IconButton(
 *     modifier = Modifier.tapTarget("new_chat"),
 *     onClick = { … }
 * ) { Icon(Icons.Default.Add, null) }
 * ```
 *
 * If no controller is provided in the composition tree this modifier becomes a no-op,
 * so existing screens stay safe even outside a tour context.
 */
fun Modifier.tapTarget(id: String): Modifier = composed {
    val controller = LocalTapTargetController.current ?: return@composed this
    onGloballyPositioned { coords ->
        val pos = coords.positionInRoot()
        controller.register(
            id,
            Rect(
                left   = pos.x,
                top    = pos.y,
                right  = pos.x + coords.size.width,
                bottom = pos.y + coords.size.height,
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Step 4 — Overlay
// ─────────────────────────────────────────────────────────────────

/**
 * Full-screen overlay that renders the tour spotlight effect.
 *
 * Mount this **once** as the topmost layer inside the root [Box] that wraps your
 * `NavHost`:
 *
 * ```kotlin
 * CompositionLocalProvider(LocalTapTargetController provides controller) {
 *     Box {
 *         AppNavHost(navController)
 *         TapTargetOverlay(controller)
 *     }
 * }
 * ```
 *
 * The overlay:
 * - Returns immediately when the tour is finished ([TapTargetController.isActive] == false).
 * - Draws nothing for the current frame when the target composable is not yet laid out
 *   (e.g. while navigation is in progress).
 * - Punches an oval hole in the scrim around the target bounds.
 * - Shows an optional title/description bubble just below the spotlight hole.
 * - Advances to the next step only when the highlighted box is clicked.
 */
@Composable
fun TapTargetOverlay(controller: TapTargetController) {
    if (!controller.isActive) return

    LaunchedEffect(controller.currentIndex) {
        controller.ensureCurrentStepVisible()
    }

    val step = controller.currentStep ?: return

    // Free-roam: non-blocking hint bar, user explores freely, advances via button only.
    if (step.freeRoamPosition != null) {
        FreeRoamTourBar(
            step = step,
            position = step.freeRoamPosition,
            onContinue = { controller.next() },
        )
        return
    }

    // Centered: no spotlight target, full block, tap-anywhere advances.
    if (step.id == null) {
        CenteredTourStep(step, onTap = { controller.next() })
        return
    }

    val target = controller.currentTarget ?: return   // not laid out yet → draw nothing

    var bubbleSize by remember(step.id) { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density       = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val edgePaddingPx = with(density) { 16.dp.toPx() }
        val iconPaddingPx = with(density) { controller.tourSettings.iconPadding.toPx() }
        val cornerRadiusPx = with(density) { controller.tourSettings.cornerRadius.toPx() }
        val highlightedBounds = target.bounds.inflate(iconPaddingPx)

        // ── Scrim with punched-out spotlight ─────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(step.id) {
                    detectTapGestures { offset ->
                        if (highlightedBounds.contains(offset)) {
                            controller.next()
                        }
                    }
                }
        ) {
            val scrim = Path().apply { addRect(size.toRect()) }
            val hole = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = highlightedBounds,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                )
            }
            val diff  = Path().apply { op(scrim, hole, PathOperation.Difference) }
            drawPath(diff, color = step.backgroundColor)
        }

        // ── Info bubble ──────────────────────────────────────────────
        if (step.title != null || step.description != null) {

            // Horizontal: keep within [edgePadding, screenWidth - bubbleWidth - edgePadding]
            val clampedX = target.bounds.left
                .coerceIn(
                    edgePaddingPx,
                    (screenWidthPx - bubbleSize.width - edgePaddingPx).coerceAtLeast(edgePaddingPx)
                )
                .toInt()

            // Vertical: prefer below the spotlight; flip above if it would clip the bottom
            val belowY = target.bounds.bottom + edgePaddingPx
            val aboveY = target.bounds.top   - edgePaddingPx - bubbleSize.height

            val clampedY = if (belowY + bubbleSize.height <= screenHeightPx) {
                belowY.toInt()
            } else {
                // Show above; clamp so it never goes above the top edge either
                aboveY.coerceAtLeast(edgePaddingPx).toInt()
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(clampedX, clampedY) }
                    .widthIn(max = 260.dp)
                    .onGloballyPositioned { bubbleSize = it.size }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                ) {
                    Column(Modifier.padding(12.dp)) {
                        step.title?.let {
                            Text(stringResource(it), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                        }
                        step.description?.let {
                            if (step.title != null) Spacer(Modifier.height(4.dp))
                            Text(text = stringResource(it), style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a step with no spotlight — full scrim, no punched hole, bubble centered on screen.
 */
@Composable
private fun CenteredTourStep(step: TourStep, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(step.id) {
                detectTapGestures { onTap() }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = step.backgroundColor)
        }

        if (step.title != null || step.description != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 280.dp)
                    .padding(24.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp,
            ) {
                Column(Modifier.padding(16.dp)) {
                    step.title?.let {
                        Text(stringResource(it), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    }
                    step.description?.let {
                        if (step.title != null) Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(it), style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                    }
                }
            }
        }
    }
}

/**
 * Non-blocking hint bar for free-roam steps. Anchored at [position]; everything outside
 * the bar itself remains fully interactive so the user can explore the real app. Only
 * the Continue button advances — there's no tap-anywhere-to-advance here, since a stray
 * tap during free exploration shouldn't accidentally skip the step.
 */
@Composable
private fun FreeRoamTourBar(
    step: TourStep,
    position: FreeRoamBarPosition,
    onContinue: () -> Unit,
) {
    val alignment = when (position) {
        FreeRoamBarPosition.Top    -> Alignment.TopCenter
        FreeRoamBarPosition.Center -> Alignment.Center
        FreeRoamBarPosition.Bottom -> Alignment.BottomCenter
    }
    // Top/Bottom read as a spanning banner; Center reads as a compact card.
    val widthModifier = if (position == FreeRoamBarPosition.Center) {
        Modifier.widthIn(max = 320.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = alignment,
    ) {
        Surface(
            modifier = widthModifier,
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                step.title?.let {
                    Text(stringResource(it), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
                step.description?.let {
                    if (step.title != null) Spacer(Modifier.height(4.dp))
                    Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onContinue) {
                    Text(step.continueButtonText?.let { stringResource(it) } ?: "Continue")
                }
            }
        }
    }
}


/* Example

// ── Onboarding tour ──────────────────────────────────────────
        // Define your steps here. Modifier.tapTarget("some_id") on any
        // composable below will automatically register it as a spotlight target.
        val onboardingTour = remember {
            tapTargetTour {
                step(
                    id = "new_chat_button",
                    title = "Start chatting",
                    description = "Tap here to open a new chat"
                )
                // step(id = "settings",  title = "Settings",       description = "Adjust your preferences here")
            }
        }
        val tourController = remember {
            TapTargetController(
                tour = onboardingTour,
                onNavigateToRoute = { /* wire to navigator if cross-screen steps are needed */ },
                currentRoute = { rootBackStack.lastOrNull()?.let { it::class } },
                onFinished = { /* preferenceManager.setOnboardingSeen(true) */ }
            )
        }

        CompositionLocalProvider(LocalTapTargetController provides tourController) {

 */