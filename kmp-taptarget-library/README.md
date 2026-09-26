# kmp-taptarget-library

Spotlight onboarding tours for Compose Multiplatform. Highlight any composable, explain it in a
bubble with fully custom content, and walk the user through your app across multiple screens.

Supported targets: Android, iOS (arm64, simulator arm64), JVM desktop.

## Features

- **Spotlight steps**: dim the screen, punch a rounded hole around a target composable, and show an
  info bubble next to it. The bubble is placed below the target, or above it when there is no room.
- **Info steps**: a centered card without spotlight, for welcome and goodbye screens.
- **Free-roam steps**: a non-blocking hint bar. The user can use the real app until they press
  Continue.
- **Composable bubble content**: every bubble is a `ColumnScope` slot, so it can hold icons, rows,
  images or anything else. `TourTitle` and `TourDescription` give you the default text styling.
- **Cross-screen tours**: every step can declare a route. The controller navigates there and waits
  for the target to appear. It works with any navigation library.
- **Tap modes**: a step can require a tap exactly on the highlighted target (with a pulsing ring and
  a wrong-tap pulse), or accept a tap anywhere.
- **Auto-scroll**: targets inside scrollable containers are scrolled into the middle of the screen.
- **Skip button**: an optional composable slot, so you control the text, language and style.
- **Localizable**: every built-in text can be overridden, and the DSL accepts `StringResource`
  from Compose resources directly.
- **Robust**: steps whose target never shows up (for example, a button hidden by state) are skipped
  instead of blocking the tour.

## Requirements

- Kotlin Multiplatform with the Compose Multiplatform plugin and Compose compiler plugin
- Android Gradle Plugin with the `com.android.kotlin.multiplatform.library` plugin
- Dependencies used by the module (taken from your version catalog):
  Compose runtime, foundation, ui, material3, components-resources, material-icons-extended,
  and kotlinx-coroutines

## Installation

The library is distributed as a Gradle source module. It is not published to Maven.

1. Copy the `kmp-taptarget-library` directory into the root of your project.

2. Include it in `settings.gradle.kts`:

   ```kotlin
   enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

   include(":kmp-taptarget-library")
   ```

3. Add it to the module that holds your UI (for example `composeApp` or `shared`):

   ```kotlin
   kotlin {
       sourceSets {
           commonMain.dependencies {
               implementation(projects.kmpTaptargetLibrary)
           }
       }
   }
   ```

4. Match `kmp-taptarget-library/build.gradle.kts` to your version catalog. The file uses these
   aliases, so rename them if your catalog uses different ones:

   | Used in build file                   | Artifact                                               |
   |--------------------------------------|--------------------------------------------------------|
   | `libs.plugins.kotlinMultiplatform`   | `org.jetbrains.kotlin.multiplatform`                   |
   | `libs.plugins.androidKmpLibrary`     | `com.android.kotlin.multiplatform.library`             |
   | `libs.plugins.composeMultiplatform`  | `org.jetbrains.compose`                                |
   | `libs.plugins.composeCompiler`       | `org.jetbrains.kotlin.plugin.compose`                  |
   | `libs.runtime`                       | `org.jetbrains.compose.runtime:runtime`                |
   | `libs.foundation`                    | `org.jetbrains.compose.foundation:foundation`          |
   | `libs.ui`                            | `org.jetbrains.compose.ui:ui`                          |
   | `libs.material3`                     | `org.jetbrains.compose.material3:material3`            |
   | `libs.components.resources`          | `org.jetbrains.compose.components:components-resources`|
   | `libs.material.icons.extended`       | `org.jetbrains.compose.material:material-icons-extended`|
   | `libs.kotlinx.coroutines`            | `org.jetbrains.kotlinx:kotlinx-coroutines-core`        |
   | `libs.kotlin.test`                   | `org.jetbrains.kotlin:kotlin-test`                     |

   Also adjust `compileSdk` and `minSdk`. You can remove the `jvm()` target if you do not build
   for desktop.

All public API lives in the package `io.github.lerchenflo.taptarget`.

## Quick start

### 1. Define the tour

```kotlin
@Composable
fun rememberOnboardingTour(): TapTargetTour = remember {
    tapTargetTour {
        infoStep(route = Route.Home) {
            TourTitle("Welcome")
            TourDescription("A short tour through the app.")
        }

        tapStep(id = "new_entry_button", route = Route.Home, requireExactTap = false) {
            TourTitle("New entry")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                TourDescription("Create a new entry here")
            }
        }

        tapStep(id = "bottombar_settings", route = Route.Home) {
            TourTitle("Settings")
        }

        freeRoamStep(route = Route.Settings, position = FreeRoamBarPosition.Bottom) {
            TourTitle("Look around")
            TourDescription("Try the settings, then press Continue.")
        }
    }
}
```

### 2. Create the controller and mount the overlay

Create one controller at the root of your UI and provide it through `LocalTapTargetController`.
Place `TapTargetOverlay` as the last child of a full-size `Box`, so it is drawn on top of
everything else.

```kotlin
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val tour = rememberOnboardingTour()

    val tourController = remember {
        TapTargetController(
            tour = tour,
            onNavigateToRoute = { route -> navigator.navigate(route as Route) },
            currentRoute = { backStack.lastOrNull() },
            onFinished = { scope.launch { preferences.setOnboardingSeen(true) } },
        )
    }

    LaunchedEffect(Unit) {
        if (!preferences.getOnboardingSeen()) tourController.start()
    }

    CompositionLocalProvider(LocalTapTargetController provides tourController) {
        Box(Modifier.fillMaxSize()) {
            AppContent()

            TapTargetOverlay(
                controller = tourController,
                skipButton = { onSkip -> TourSkipButton(text = "Skip tour", onSkip = onSkip) },
            )
        }
    }
}
```

### 3. Mark the targets

Add `Modifier.tapTarget(id)` to every composable a `tapStep` points at. The id must match the
step id.

```kotlin
FloatingActionButton(
    onClick = { /* ... */ },
    modifier = Modifier.tapTarget("new_entry_button"),
) {
    Icon(Icons.Filled.Add, contentDescription = "New entry")
}
```

When no controller is provided, `tapTarget` does nothing. Previews, tests and screens outside a
tour are unaffected.

## Step types

| Builder function | Spotlight | Blocks the app | Advances on                                   |
|------------------|-----------|----------------|-----------------------------------------------|
| `tapStep`        | yes       | yes            | a tap on the target, or anywhere if `requireExactTap = false` |
| `infoStep`       | no        | yes            | a tap anywhere                                |
| `freeRoamStep`   | no        | no             | the Continue button in the hint bar           |

Each builder function comes in two variants:

- **Slot variant**: a trailing `@Composable ColumnScope.() -> Unit` lambda for custom content.
- **Resource variant**: `title` / `description` as `StringResource` for plain text steps.

```kotlin
// Slot variant
tapStep(id = "export") {
    TourTitle("Export")
    TourDescription("Save or share as image")
}

// Resource variant
tapStep(
    id = "export",
    title = Res.string.tour_export_title,
    description = Res.string.tour_export_description,
)
```

Common parameters:

| Parameter            | Description                                                                 |
|----------------------|-----------------------------------------------------------------------------|
| `id`                 | Target id, matches `Modifier.tapTarget(id)`. Only on `tapStep`.             |
| `route`              | Screen the step lives on. The controller navigates there first.             |
| `requireExactTap`    | `true`: only a tap on the target advances. `false`: a tap anywhere advances. |
| `backgroundColor`    | Scrim color. Default: black at 75 % opacity.                                |
| `position`           | Free-roam bar position: `Top`, `Center` or `Bottom`.                        |
| `continueButtonText` | Free-roam button label. Falls back to `TourStrings.continueButton`.         |

## Custom bubble content

The bubble content runs inside a `Column` with small vertical spacing. Use the provided text
composables for consistent styling, and add any other composables you need:

```kotlin
tapStep(id = "entry_card", requireExactTap = false) {
    TourTitle("Entries")
    TourDescription("Each entry can contain:")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        TourDescription("Breaks")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        TourDescription("Drives")
    }
}
```

The spotlight bubble is at most 260 dp wide, and the centered info card at most 280 dp.

## Localization

The overlay has a few built-in texts: the tap hints and the Continue button. They default to
English. Override them with `TourStrings`, which you can resolve from Compose resources at the call
site:

```kotlin
TapTargetOverlay(
    controller = tourController,
    strings = TourStrings(
        tapHighlighted = stringResource(Res.string.tour_hint_tap_highlighted),
        tapAnywhere = stringResource(Res.string.tour_hint_tap_anywhere),
        continueButton = stringResource(Res.string.tour_continue),
    ),
    skipButton = { onSkip ->
        TourSkipButton(text = stringResource(Res.string.tour_skip), onSkip = onSkip)
    },
)
```

## Skip button

`skipButton` is a composable slot that receives an `onSkip` callback. Leave it `null` to hide the
button. `TourSkipButton` is a ready-made `FilledTonalButton`, but any composable works:

```kotlin
skipButton = { onSkip ->
    TextButton(onClick = onSkip) { Text("Not now") }
}
```

The button respects the safe drawing insets. It sits in the half of the screen opposite to the
spotlight or free-roam bar, so it never covers the highlighted target. Pressing it calls
`TapTargetController.skip()`, which also triggers `onFinished`.

## Navigation integration

The controller does not depend on a navigation library. Routes are typed as `Any`, and you provide
three functions:

| Parameter           | Purpose                                                                |
|---------------------|------------------------------------------------------------------------|
| `onNavigateToRoute` | Navigate to the given route. Called when a step's route is not active. |
| `currentRoute`      | Return the currently visible route.                                    |
| `isMatchingRoute`   | Optional. Decide whether the active route shows the step's route. Default: compares classes, so `Route.Detail(1)` and `Route.Detail(2)` count as the same screen. |

A tap on a highlighted target advances the tour. It is not passed to the composable underneath.
To move the user to another screen after a step, give the following step a `route`.

## Controller API

| Member                | Description                                                          |
|-----------------------|----------------------------------------------------------------------|
| `start()`             | Resets to the first step and activates the tour.                     |
| `next()`              | Advances one step. Finishes the tour after the last step.            |
| `skip()`              | Ends the tour immediately and calls `onFinished`.                    |
| `isActive`            | `true` while the tour runs.                                          |
| `currentIndex`        | Index of the current step.                                           |
| `currentStep`         | The current `TourStep`, or `null`.                                   |

To restart the tour from a settings screen, read the controller from the composition:

```kotlin
val controller = LocalTapTargetController.current
Button(onClick = { controller?.start() }) { Text("Restart tour") }
```

## Settings

```kotlin
TapTargetController(
    tour = tour,
    tourSettings = TourSettings(
        iconPadding = 8.dp,         // space between target bounds and spotlight edge
        cornerRadius = 16.dp,       // corner radius of the spotlight hole
        skipMissingTargets = true,  // skip steps whose target does not appear within 2 s
    ),
)
```

## Notes

- **Unique ids on screen**: an id must be registered by only one composable at a time. If a
  `HorizontalPager` keeps several pages composed that share a screen composable, apply
  `tapTarget` conditionally, so only one page registers the id.
- **Hidden targets**: when a target is not in the composition (for example, it only shows when a
  list has content), the step is skipped after 2 seconds. You can disable this with
  `skipMissingTargets = false`. Spotlighting an always-visible parent is often the better choice.
- **Horizontal pagers**: auto-scroll only moves vertically, so a target inside a pager never swipes
  it to another page.
- **Persistence**: the library stores nothing. Save a "seen" flag in `onFinished` and decide in your
  app when to call `start()`.
