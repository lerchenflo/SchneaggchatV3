package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.undercover_winner_civilians
import schneaggchatv3mp.composeapp.generated.resources.undercover_winner_mr_white
import schneaggchatv3mp.composeapp.generated.resources.undercover_winner_undercover

class UndercoverViewModel : ViewModel() {

    enum class ActualRole {
        CIVILIAN,
        UNDERCOVER,
        MR_WHITE
    }

    enum class Phase {
        SETUP,
        PASS_PHONE,
        REVEAL,
        CHOOSE_STARTER,
        DISCUSSION,
        VOTING,
        MR_WHITE_GUESS,
        GAME_OVER
    }

    data class Player(
        val id: String,
        val name: String,
        val actualRole: ActualRole,
        val isAlive: Boolean
    )

    data class VotingResult(
        val eliminatedPlayerId: String,
        val eliminatedPlayerName: String,
        val revealedRole: ActualRole
    )

    data class UiState(
        val phase: Phase = Phase.SETUP,

        val setupPlayerNameInput: String = "",
        val setupPlayers: List<String> = emptyList(),
        val setupMrWhiteCount: Int = 1,
        val setupUndercoverCount: Int = 1,

        val autoHideEnabled: Boolean = false,
        val autoHideSeconds: Int = 5,

        val selectedWordPair: UndercoverWordPair? = null,
        val players: List<Player> = emptyList(),

        val currentRevealIndex: Int = 0,

        val selectedStarterPlayerId: String? = null,

        val votingSelectedPlayerId: String? = null,
        val votingResult: VotingResult? = null,

        val mrWhiteGuessInput: String = "",
        val mrWhiteGuessWasCorrect: Boolean? = null,

        val winnerText: UiText? = null
    )

    var state by mutableStateOf(UiState())
        private set

    private var revealAutoHideJob: Job? = null

    fun updateSetupPlayerNameInput(newValue: String) {
        state = state.copy(setupPlayerNameInput = newValue)
    }

    fun addSetupPlayer() {
        val trimmed = state.setupPlayerNameInput.trim()
        if (trimmed.isBlank()) return
        if (state.setupPlayers.any { it.equals(trimmed, ignoreCase = true) }) return
        state = state.copy(
            setupPlayers = state.setupPlayers + trimmed,
            setupPlayerNameInput = ""
        )
        coerceRoleCountsToValidRange()
    }

    fun removeSetupPlayer(name: String) {
        state = state.copy(setupPlayers = state.setupPlayers - name)
        coerceRoleCountsToValidRange()
    }

    fun incrementMrWhiteCount() {
        state = state.copy(setupMrWhiteCount = state.setupMrWhiteCount + 1)
        coerceRoleCountsToValidRange()
    }

    fun decrementMrWhiteCount() {
        state = state.copy(setupMrWhiteCount = (state.setupMrWhiteCount - 1).coerceAtLeast(0))
        coerceRoleCountsToValidRange()
    }

    fun incrementUndercoverCount() {
        state = state.copy(setupUndercoverCount = state.setupUndercoverCount + 1)
        coerceRoleCountsToValidRange()
    }

    fun decrementUndercoverCount() {
        state = state.copy(setupUndercoverCount = (state.setupUndercoverCount - 1).coerceAtLeast(0))
        coerceRoleCountsToValidRange()
    }

    fun toggleAutoHide(enabled: Boolean) {
        state = state.copy(autoHideEnabled = enabled)
    }

    fun incrementAutoHideSeconds() {
        state = state.copy(autoHideSeconds = (state.autoHideSeconds + 1).coerceAtMost(30))
    }

    fun decrementAutoHideSeconds() {
        state = state.copy(autoHideSeconds = (state.autoHideSeconds - 1).coerceAtLeast(1))
    }

    fun canStartGame(): Boolean {
        val n = state.setupPlayers.size
        val special = state.setupMrWhiteCount + state.setupUndercoverCount
        if (n < 3) return false
        if (special >= n) return false
        if (state.setupMrWhiteCount < 0 || state.setupUndercoverCount < 0) return false
        if (UNDERCOVER_WORD_PAIRS.isEmpty()) return false
        return true
    }

    fun startGame() {
        if (!canStartGame()) return

        val wordPair = UNDERCOVER_WORD_PAIRS.random(Random)
        val names = state.setupPlayers
        val roles = buildList {
            repeat(state.setupMrWhiteCount) { add(ActualRole.MR_WHITE) }
            repeat(state.setupUndercoverCount) { add(ActualRole.UNDERCOVER) }
            repeat(names.size - state.setupMrWhiteCount - state.setupUndercoverCount) { add(ActualRole.CIVILIAN) }
        }.shuffled(Random)

        val players = names.mapIndexed { index, name ->
            Player(
                id = "p$index",
                name = name,
                actualRole = roles[index],
                isAlive = true
            )
        }

        state = state.copy(
            phase = Phase.PASS_PHONE,
            selectedWordPair = wordPair,
            players = players,
            currentRevealIndex = 0,
            selectedStarterPlayerId = null,
            votingSelectedPlayerId = null,
            votingResult = null,
            mrWhiteGuessInput = "",
            mrWhiteGuessWasCorrect = null,
            winnerText = null
        )
    }

    fun currentRevealPlayerOrNull(): Player? {
        val idx = state.currentRevealIndex
        return state.players.getOrNull(idx)
    }

    fun currentRevealPlayerNameOrEmpty(): String {
        return currentRevealPlayerOrNull()?.name.orEmpty()
    }

    fun onConfirmPlayerIdentity() {
        if (state.phase != Phase.PASS_PHONE) return
        state = state.copy(phase = Phase.REVEAL)
        scheduleAutoHideIfEnabled()
    }

    fun onHideAndPassPhone() {
        if (state.phase != Phase.REVEAL) return
        cancelAutoHide()

        val nextIndex = state.currentRevealIndex + 1
        if (nextIndex >= state.players.size) {
            state = state.copy(phase = Phase.CHOOSE_STARTER)
        } else {
            state = state.copy(
                phase = Phase.PASS_PHONE,
                currentRevealIndex = nextIndex
            )
        }
    }


    fun getWordForPlayer(player: Player): String? {
        val pair = state.selectedWordPair ?: return null
        return when (player.actualRole) {
            ActualRole.MR_WHITE -> null
            ActualRole.CIVILIAN -> pair.civilianWord
            ActualRole.UNDERCOVER -> pair.undercoverWord
        }
    }

    fun starterCandidates(): List<Player> {
        return state.players.filter { it.isAlive && it.actualRole != ActualRole.MR_WHITE }
    }

    fun selectRandomStarterIfNeeded() {
        if (state.phase != Phase.CHOOSE_STARTER) return
        if (state.selectedStarterPlayerId != null) return
        val candidates = starterCandidates()
        val picked = candidates.randomOrNull(Random) ?: return
        state = state.copy(selectedStarterPlayerId = picked.id)
    }

    fun getPlayerNameById(playerId: String): String? {
        return state.players.firstOrNull { it.id == playerId }?.name
    }

    fun selectStarter(playerId: String) {
        if (state.phase != Phase.CHOOSE_STARTER) return
        val candidateIds = starterCandidates().map { it.id }.toSet()
        if (playerId !in candidateIds) return
        state = state.copy(selectedStarterPlayerId = playerId)
    }

    fun confirmStarter() {
        if (state.phase != Phase.CHOOSE_STARTER) return
        val candidateIds = starterCandidates().map { it.id }.toSet()
        val selected = state.selectedStarterPlayerId
        if (selected == null || selected !in candidateIds) return
        state = state.copy(phase = Phase.DISCUSSION)
    }

    fun startVoting() {
        if (state.phase != Phase.DISCUSSION) return
        state = state.copy(
            phase = Phase.VOTING,
            votingSelectedPlayerId = null,
            votingResult = null
        )
    }

    fun votingCandidates(): List<Player> {
        return state.players.filter { it.isAlive }
    }

    fun selectVote(playerId: String) {
        if (state.phase != Phase.VOTING) return
        if (state.votingResult != null) return
        val candidateIds = votingCandidates().map { it.id }.toSet()
        if (playerId !in candidateIds) return
        state = state.copy(votingSelectedPlayerId = playerId)
    }

    fun confirmVote() {
        if (state.phase != Phase.VOTING) return
        if (state.votingResult != null) return
        val targetId = state.votingSelectedPlayerId ?: return
        val target = state.players.firstOrNull { it.id == targetId && it.isAlive } ?: return

        val updatedPlayers = state.players.map { p ->
            if (p.id == targetId) p.copy(isAlive = false) else p
        }

        state = state.copy(
            players = updatedPlayers,
            votingResult = VotingResult(
                eliminatedPlayerId = target.id,
                eliminatedPlayerName = target.name,
                revealedRole = target.actualRole
            )
        )
    }

    fun onContinueAfterVotingResult() {
        if (state.phase != Phase.VOTING) return
        val result = state.votingResult ?: return

        if (result.revealedRole == ActualRole.MR_WHITE) {
            state = state.copy(
                phase = Phase.MR_WHITE_GUESS,
                mrWhiteGuessInput = "",
                mrWhiteGuessWasCorrect = null
            )
            return
        }

        val winner = evaluateWinner(players = state.players)
        if (winner != null) {
            state = state.copy(
                phase = Phase.GAME_OVER,
                winnerText = winner
            )
            return
        }

        state = state.copy(phase = Phase.DISCUSSION)
    }

    fun updateMrWhiteGuessInput(newValue: String) {
        if (state.phase != Phase.MR_WHITE_GUESS) return
        state = state.copy(mrWhiteGuessInput = newValue)
    }

    fun submitMrWhiteGuess() {
        if (state.phase != Phase.MR_WHITE_GUESS) return
        val civilianWord = state.selectedWordPair?.civilianWord ?: return
        val guess = state.mrWhiteGuessInput.trim()
        val correct = guess.equals(civilianWord, ignoreCase = true)

        if (correct) {
            state = state.copy(
                phase = Phase.GAME_OVER,
                mrWhiteGuessWasCorrect = true,
                winnerText = UiText.StringResourceText(Res.string.undercover_winner_mr_white)
            )
            return
        }

        val winnerAfterWrongGuess = evaluateWinner(players = state.players)
        state = state.copy(
            phase = if (winnerAfterWrongGuess == null) Phase.DISCUSSION else Phase.GAME_OVER,
            mrWhiteGuessWasCorrect = false,
            winnerText = winnerAfterWrongGuess
        )
    }

    fun resetGame() {
        cancelAutoHide()
        state = UiState(
            setupMrWhiteCount = state.setupMrWhiteCount,
            setupUndercoverCount = state.setupUndercoverCount,
            autoHideEnabled = state.autoHideEnabled,
            autoHideSeconds = state.autoHideSeconds
        )
        coerceRoleCountsToValidRange()
    }

    private fun evaluateWinner(players: List<Player>): UiText? {
        val alive = players.filter { it.isAlive }
        val aliveCivilians = alive.count { it.actualRole == ActualRole.CIVILIAN }
        val aliveUndercovers = alive.count { it.actualRole == ActualRole.UNDERCOVER }
        val aliveMrWhites = alive.count { it.actualRole == ActualRole.MR_WHITE }

        if (aliveUndercovers == 0 && aliveMrWhites == 0) {
            return UiText.StringResourceText(Res.string.undercover_winner_civilians)
        }

        if (aliveCivilians <= 1 && (aliveUndercovers + aliveMrWhites) > 0) {
            return UiText.StringResourceText(Res.string.undercover_winner_undercover)
        }

        return null
    }

    private fun scheduleAutoHideIfEnabled() {
        cancelAutoHide()
        if (!state.autoHideEnabled) return

        revealAutoHideJob = viewModelScope.launch {
            delay(state.autoHideSeconds.toLong() * 1000L)
            if (state.phase == Phase.REVEAL) {
                onHideAndPassPhone()
            }
        }
    }

    private fun cancelAutoHide() {
        revealAutoHideJob?.cancel()
        revealAutoHideJob = null
    }

    private fun coerceRoleCountsToValidRange() {
        val n = state.setupPlayers.size
        if (n <= 0) {
            state = state.copy(
                setupMrWhiteCount = state.setupMrWhiteCount.coerceAtLeast(0),
                setupUndercoverCount = state.setupUndercoverCount.coerceAtLeast(0)
            )
            return
        }

        val maxSpecial = (n - 1).coerceAtLeast(0)
        val mr = state.setupMrWhiteCount.coerceIn(0, maxSpecial)
        val under = state.setupUndercoverCount.coerceIn(0, (maxSpecial - mr).coerceAtLeast(0))
        state = state.copy(
            setupMrWhiteCount = mr,
            setupUndercoverCount = under
        )
    }
}
