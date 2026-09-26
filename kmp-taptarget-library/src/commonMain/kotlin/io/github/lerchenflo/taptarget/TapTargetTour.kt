package io.github.lerchenflo.taptarget

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// ─────────────────────────────────────────────────────────────────
// Tour DSL
// ─────────────────────────────────────────────────────────────────

enum class FreeRoamBarPosition { Top, Center, Bottom }

/**
 * Content of the info bubble of a step. Runs inside a [androidx.compose.foundation.layout.Column]
 * with a small vertical spacing, so any composables (icons, rows, images...) can be placed here.
 * Use [TourTitle] and [TourDescription] for the default text styling.
 */
typealias TourStepContent = @Composable ColumnScope.() -> Unit

val DefaultTourBackgroundColor: Color = Color.Black.copy(alpha = 0.75f)

/**
 * Describes a single step in an onboarding tour.
 *
 * @param id            Unique identifier; must match the id passed to [tapTarget].
 *                      Null for steps without a spotlight target (info / free-roam steps).
 * @param route         Optional route of the screen the step lives on. When set, the controller
 *                      navigates to this route before the step is displayed so that the target
 *                      composable is guaranteed to be in the composition.
 * @param backgroundColor  Scrim color rendered behind the spotlight hole.
 * @param content       Optional content of the info bubble.
 */
class TourStep(
    val id: String?,
    val route: Any? = null,
    val backgroundColor: Color = DefaultTourBackgroundColor,
    /** Non-null marks this a free-roam step: shows a non-blocking hint bar at this
     *  position instead of a spotlight or blocking dialog. Everything else on screen
     *  stays fully interactive; only the bar's Continue button advances the tour. */
    val freeRoamPosition: FreeRoamBarPosition? = null,
    /** Continue button label. Falls back to [TourStrings.continueButton] if null. */
    val continueButtonText: (@Composable () -> String)? = null,
    /** When true the user must tap exactly on the highlighted target to advance.
     *  When false the user can tap anywhere on the overlay to advance — useful for
     *  steps that just show a button without requiring the user to press it. */
    val requireExactTap: Boolean = true,
    val content: TourStepContent? = null,
)

/** Whether a step expects a tap on the highlighted target or a tap anywhere on screen. */
internal enum class TapHint { EXACT, ANYWHERE }

/** Derives which tap hint to show, or null for free-roam steps that use a Continue button instead. */
internal val TourStep.tapHint: TapHint?
    get() = when {
        freeRoamPosition != null -> null
        id == null -> TapHint.ANYWHERE
        requireExactTap -> TapHint.EXACT
        else -> TapHint.ANYWHERE
    }

/** Immutable tour description produced by [tapTargetTour]. */
class TapTargetTour(val steps: List<TourStep>)

/**
 * Builder populated inside the [tapTargetTour] lambda.
 *
 * Every step type has two variants:
 * - a [StringResource] variant with `title` / `description` for plain text steps
 * - a slot variant with a trailing [TourStepContent] lambda for custom content (icons etc.)
 */
class TourBuilder {
    val steps = mutableListOf<TourStep>()

    /**
     * Adds a step that spotlights the composable registered with [tapTarget] ([id]).
     *
     * ```kotlin
     * tapStep(id = "new_chat", route = Route.ChatSelector) {
     *     TourTitle("Start chatting")
     *     Row { Icon(Icons.Default.Add, null); TourDescription("Tap here to begin") }
     * }
     * ```
     */
    fun tapStep(
        id: String,
        route: Any? = null,
        backgroundColor: Color = DefaultTourBackgroundColor,
        requireExactTap: Boolean = true,
        content: TourStepContent,
    ) {
        steps += TourStep(
            id = id,
            route = route,
            backgroundColor = backgroundColor,
            requireExactTap = requireExactTap,
            content = content,
        )
    }

    fun tapStep(
        id: String,
        title: StringResource? = null,
        description: StringResource? = null,
        route: Any? = null,
        backgroundColor: Color = DefaultTourBackgroundColor,
        requireExactTap: Boolean = true,
    ) {
        steps += TourStep(
            id = id,
            route = route,
            backgroundColor = backgroundColor,
            requireExactTap = requireExactTap,
            content = resourceContent(title, description),
        )
    }

    /** Adds a step without spotlight: full scrim, bubble centered on screen, tap anywhere advances. */
    fun infoStep(
        route: Any? = null,
        backgroundColor: Color = DefaultTourBackgroundColor,
        content: TourStepContent,
    ) {
        steps += TourStep(id = null, route = route, backgroundColor = backgroundColor, content = content)
    }

    fun infoStep(
        title: StringResource? = null,
        description: StringResource? = null,
        route: Any? = null,
        backgroundColor: Color = DefaultTourBackgroundColor,
    ) {
        steps += TourStep(
            id = null,
            route = route,
            backgroundColor = backgroundColor,
            content = resourceContent(title, description),
        )
    }

    /**
     * Adds a step that lets the user freely explore the app while a small, non-blocking
     * hint bar stays anchored at [position]. Unlike [tapStep]/[infoStep], tapping
     * elsewhere on screen does nothing — the tour only advances when the user presses
     * the bar's Continue button.
     */
    fun freeRoamStep(
        route: Any? = null,
        position: FreeRoamBarPosition = FreeRoamBarPosition.Bottom,
        continueButtonText: String? = null,
        content: TourStepContent,
    ) {
        steps += TourStep(
            id = null,
            route = route,
            freeRoamPosition = position,
            continueButtonText = continueButtonText?.let { text -> { text } },
            content = content,
        )
    }

    fun freeRoamStep(
        title: StringResource? = null,
        description: StringResource? = null,
        route: Any? = null,
        position: FreeRoamBarPosition = FreeRoamBarPosition.Bottom,
        continueButtonText: StringResource? = null,
    ) {
        steps += TourStep(
            id = null,
            route = route,
            freeRoamPosition = position,
            continueButtonText = continueButtonText?.let { res -> { stringResource(res) } },
            content = resourceContent(title, description),
        )
    }

    private fun resourceContent(title: StringResource?, description: StringResource?): TourStepContent? {
        if (title == null && description == null) return null
        return {
            title?.let { TourTitle(stringResource(it)) }
            description?.let { TourDescription(stringResource(it)) }
        }
    }
}

/**
 * Entry point for defining a tour.
 *
 * ```kotlin
 * val onboardingTour = tapTargetTour {
 *     tapStep("new_chat", title = Res.string.start_chatting, description = Res.string.start_chatting_desc)
 *     tapStep("settings", route = Route.Settings) { TourTitle("Settings") }
 * }
 * ```
 */
fun tapTargetTour(builder: TourBuilder.() -> Unit): TapTargetTour =
    TapTargetTour(TourBuilder().apply(builder).steps)
