package org.lerchenflo.schneaggchatv3mp.games.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.leaderboard

enum class HighscoreUploadStatus {
    /** No finished game with uploadable players. */
    NONE,
    ASKING,
    UPLOADING,
    UPLOADED,
    FAILED,
    DECLINED,

    /** A finished game whose players have no accounts, so there was nothing to offer. */
    NO_ACCOUNTS,
}

data class HighscoreUploadRow(
    val playerName: String,
    val scoreText: String,
)

data class HighscoreUploadState(
    val status: HighscoreUploadStatus = HighscoreUploadStatus.NONE,
    val rows: List<HighscoreUploadRow> = emptyList(),
    /** Players without an account, whose results are not uploaded. */
    val skippedPlayerCount: Int = 0,
    /** Win-counting game: every listed player gets one win added. */
    val winsOnly: Boolean = false,
)

/**
 * End-of-game leaderboard upload for shared-device games. A finished game is only ever offered:
 * nothing reaches the server until the user explicitly confirms, so test games stay off the board.
 */
class HighscoreUploadController(
    private val game: GameId,
    private val repository: GameHighscoreRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(HighscoreUploadState())
    val state: StateFlow<HighscoreUploadState> = _state.asStateFlow()

    private var pendingDifficulty = GameDifficulty.MEDIUM
    private var pendingScores: Map<String, Long> = emptyMap()

    /** Asks whether the final [results] of a finished game should be uploaded; no-op without platform users. */
    fun offer(difficulty: GameDifficulty, results: List<Pair<GamePlayer, Long>>) {
        val uploadable = results.filter { it.first.userId != null }
        if (uploadable.isEmpty()) {
            // Silence here reads as a bug: the game is over and the leaderboard was never mentioned
            pendingScores = emptyMap()
            _state.value = HighscoreUploadState(
                status = if (results.isEmpty()) HighscoreUploadStatus.NONE
                else HighscoreUploadStatus.NO_ACCOUNTS,
                skippedPlayerCount = results.size,
            )
            return
        }
        pendingDifficulty = difficulty
        pendingScores = uploadable.associate { (player, score) -> player.userId!! to score }
        _state.value = HighscoreUploadState(
            status = HighscoreUploadStatus.ASKING,
            rows = uploadable.map { (player, score) ->
                HighscoreUploadRow(
                    playerName = player.name,
                    scoreText = if (game.leaderboard.countsWins) "+$score" else game.leaderboard.formatScore(score),
                )
            },
            skippedPlayerCount = results.size - uploadable.size,
            winsOnly = game.leaderboard.countsWins,
        )
    }

    fun upload() {
        val status = _state.value.status
        if (status != HighscoreUploadStatus.ASKING && status != HighscoreUploadStatus.FAILED) return
        // Captured now: a reset() while the request is in flight must not turn this into an
        // empty submission that reports success without uploading anything.
        val difficulty = pendingDifficulty
        val scores = pendingScores
        _state.update { it.copy(status = HighscoreUploadStatus.UPLOADING) }
        scope.launch {
            val result = withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
                repository.submitBatchScores(game, difficulty, scores)
            }
            val newStatus = when (result) {
                is NetworkResult.Success -> HighscoreUploadStatus.UPLOADED
                // A failure and a timed-out request both need the same way out: retry or skip
                else -> HighscoreUploadStatus.FAILED
            }
            // A reset or a new offer in between wins - a late answer must not revive a dialog
            // for a game that is already over and gone.
            _state.update {
                if (it.status == HighscoreUploadStatus.UPLOADING) it.copy(status = newStatus) else it
            }
        }
    }

    fun decline() {
        if (_state.value.status == HighscoreUploadStatus.UPLOADING) return
        _state.update { it.copy(status = HighscoreUploadStatus.DECLINED) }
    }

    /** A new game started or the finished one was undone: forget the offer. */
    fun reset() {
        pendingScores = emptyMap()
        _state.value = HighscoreUploadState()
    }
}

/**
 * Last resort above the HTTP client's own timeouts: the upload dialog cannot be dismissed and
 * both its buttons are disabled while uploading, so UPLOADING has to end no matter what.
 */
private const val UPLOAD_TIMEOUT_MS = 120_000L

