package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartOutMode
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartPlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartSegment
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartThrow
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadState
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadStatus
import kotlin.math.round

/** Which segment of the board the dart pad scores. */
enum class DartMultiplier {
    SINGLE,
    DOUBLE,
    TRIPLE
}

/** One scoreboard row, ready to render - no game internals reach the UI. */
data class DartPlayerUi(
    val name: String,
    val score: Int,
    val averageText: String,
    val dartsThrown: Int,
    val isFinished: Boolean,
    val isCurrent: Boolean,
)

fun DartPlayer.toDartPlayerUi(countdown: Int, isCurrent: Boolean) = DartPlayerUi(
    name = name,
    score = score,
    averageText = (round(threeDartAverage(countdown) * 10) / 10).toString(),
    dartsThrown = totalDartsThrown,
    isFinished = isFinished,
    isCurrent = isCurrent,
)

data class DartCounterState(
    val gameStarted: Boolean = false,
    val gameOver: Boolean = false,
    val players: List<DartPlayerUi> = emptyList(),
    val winnerNames: List<String> = emptyList(),

    // Active player's turn
    val currentPlayerName: String = "",
    val currentPlayerScore: Int = 0,
    val currentPlayerIndex: Int = 0,
    val dartsLeft: Int = 0,
    val turnDarts: List<DartThrow> = emptyList(),
    val turnTotal: Int = 0,
    /** Best finishing path for this turn, empty when a finish is out of reach. */
    val checkout: List<DartSegment> = emptyList(),
    val alternativeCheckout: List<DartSegment> = emptyList(),
    /** Whether the engine still has a dart to take back; see [undoEnabled] for the UI. */
    val canUndo: Boolean = false,
    val selectedMultiplier: DartMultiplier = DartMultiplier.SINGLE,

    // Setup
    val playerNames: List<String> = emptyList(),
    val selectedCountdown: Int = 501,
    val selectedOutMode: DartOutMode = DartOutMode.DOUBLE_OUT,

    // Dialogs
    val showPlayerSetup: Boolean = false,
    val showGameConfig: Boolean = false,
    val showStopGameDialog: Boolean = false,
    val showHighscores: Boolean = false,
    val highscoreUpload: HighscoreUploadState = HighscoreUploadState(),
) {
    /** The dart pad only accepts input while a leg is actually running. */
    val padEnabled: Boolean get() = gameStarted && !gameOver

    /**
     * Undo is off once the result reached the leaderboard: taking the winning dart back would
     * re-open a finished leg and let the very same result be uploaded a second time.
     */
    val undoEnabled: Boolean
        get() = canUndo && highscoreUpload.status != HighscoreUploadStatus.UPLOADED

    val canStartGame: Boolean get() = playerNames.isNotEmpty()
}

sealed interface DartCounterAction {
    data object OnAddPlayersClick : DartCounterAction
    data object OnPlayerSetupDismiss : DartCounterAction
    data class OnPlayersSelected(val players: List<GamePlayer>) : DartCounterAction

    data object OnConfigureGameClick : DartCounterAction
    data object OnGameConfigDismiss : DartCounterAction
    data object OnConfirmStartGame : DartCounterAction
    data class OnCountdownSelect(val countdown: Int) : DartCounterAction
    data class OnOutModeSelect(val mode: DartOutMode) : DartCounterAction

    data object OnStopGameClick : DartCounterAction
    data object OnStopGameDismiss : DartCounterAction
    data object OnConfirmStopGame : DartCounterAction

    data object OnHighscoresClick : DartCounterAction
    data object OnHighscoresDismiss : DartCounterAction

    data class OnMultiplierSelect(val multiplier: DartMultiplier) : DartCounterAction
    data class OnDartThrow(val segment: DartSegment) : DartCounterAction
    data object OnUndoThrow : DartCounterAction

    data object OnUploadHighscores : DartCounterAction
    data object OnDeclineHighscoreUpload : DartCounterAction
}
