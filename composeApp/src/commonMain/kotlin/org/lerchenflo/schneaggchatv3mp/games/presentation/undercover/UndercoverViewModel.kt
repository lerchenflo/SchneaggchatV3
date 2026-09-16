package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.LocalGameSaveSlot
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.undercover_winner_civilians
import schneaggchatv3mp.composeapp.generated.resources.undercover_winner_mr_white
import schneaggchatv3mp.composeapp.generated.resources.undercover_winner_undercover
import kotlin.random.Random

class UndercoverViewModel(
    private val languageService: LanguageService,
    gameSaveRepository: GameSaveRepository,
) : ViewModel() {

    @Serializable
    enum class ActualRole {
        CIVILIAN,
        UNDERCOVER,
        MR_WHITE
    }

    @Serializable
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

    /** Steps of re-checking one's own word mid-game; never persisted, so a restored game never shows a word. */
    enum class SniffStep {
        CLOSED,
        SELECT_PLAYER,
        CONFIRM_IDENTITY,
        REVEAL
    }

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
        val mrWhiteTipEnabled: Boolean = false,

        val selectedWordPair: UndercoverWordPair? = null,
        val players: List<Player> = emptyList(),

        val currentRevealIndex: Int = 0,

        val selectedStarterPlayerId: String? = null,

        val votingSelectedPlayerId: String? = null,
        val votingResult: VotingResult? = null,

        val mrWhiteGuessInput: String = "",
        val mrWhiteGuessWasCorrect: Boolean? = null,

        val winnerText: UiText? = null,
        
        val showRulesDialog: Boolean = false,
        val showPlayerSelector: Boolean = false,

        val sniffStep: SniffStep = SniffStep.CLOSED,
        val sniffPlayerId: String? = null
    )

    var state by mutableStateOf(UiState())
        private set

    private var revealAutoHideJob: Job? = null

    private val saveSession = GameSaveSession(
        game = LocalGameSaveSlot.UNDERCOVER,
        serializer = UndercoverSnapshot.serializer(),
        schemaVersion = UNDERCOVER_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )

    init {
        saveSession.start(
            onRestore = ::restore,
            onAppBackgrounded = {
                // Whoever picks the phone up next must not see a word that was left open
                closeSniff()
                persist()
            }
        )
    }

    /** Leaving the screen or backgrounding the app keeps the running game for the next visit. */
    fun persist() = saveSession.persist(snapshotOrNull())

    /** Null when there is no game worth keeping (still in setup or already over). */
    private fun snapshotOrNull(): UndercoverSnapshot? {
        val current = state
        if (current.phase == Phase.SETUP || current.phase == Phase.GAME_OVER) return null
        val wordPair = current.selectedWordPair ?: return null
        return UndercoverSnapshot(
            phase = current.phase,
            setupPlayers = current.setupPlayers,
            setupMrWhiteCount = current.setupMrWhiteCount,
            setupUndercoverCount = current.setupUndercoverCount,
            autoHideEnabled = current.autoHideEnabled,
            autoHideSeconds = current.autoHideSeconds,
            mrWhiteTipEnabled = current.mrWhiteTipEnabled,
            wordPair = UndercoverWordPairSnapshot(
                civilianWord = wordPair.civilianWord,
                undercoverWord = wordPair.undercoverWord,
                mrWhiteTip = wordPair.mrWhiteTip,
            ),
            players = current.players.map {
                UndercoverPlayerSnapshot(id = it.id, name = it.name, actualRole = it.actualRole, isAlive = it.isAlive)
            },
            currentRevealIndex = current.currentRevealIndex,
            selectedStarterPlayerId = current.selectedStarterPlayerId,
            votingSelectedPlayerId = current.votingSelectedPlayerId,
            votingResult = current.votingResult?.let {
                UndercoverVotingResultSnapshot(
                    eliminatedPlayerId = it.eliminatedPlayerId,
                    eliminatedPlayerName = it.eliminatedPlayerName,
                    revealedRole = it.revealedRole,
                )
            },
            mrWhiteGuessInput = current.mrWhiteGuessInput,
            mrWhiteGuessWasCorrect = current.mrWhiteGuessWasCorrect,
        )
    }

    private fun restore(save: GameSave<UndercoverSnapshot>) {
        val data = save.data
        if (data.players.isEmpty()) return
        state = UiState(
            // A word that was on screen must not show up again for whoever reopens the app,
            // so the player has to confirm their identity again first
            phase = if (data.phase == Phase.REVEAL) Phase.PASS_PHONE else data.phase,
            setupPlayers = data.setupPlayers,
            setupMrWhiteCount = data.setupMrWhiteCount,
            setupUndercoverCount = data.setupUndercoverCount,
            autoHideEnabled = data.autoHideEnabled,
            autoHideSeconds = data.autoHideSeconds,
            mrWhiteTipEnabled = data.mrWhiteTipEnabled,
            selectedWordPair = UndercoverWordPair(
                civilianWord = data.wordPair.civilianWord,
                undercoverWord = data.wordPair.undercoverWord,
                mrWhiteTip = data.wordPair.mrWhiteTip,
            ),
            players = data.players.map {
                Player(id = it.id, name = it.name, actualRole = it.actualRole, isAlive = it.isAlive)
            },
            currentRevealIndex = data.currentRevealIndex.coerceIn(0, data.players.lastIndex),
            selectedStarterPlayerId = data.selectedStarterPlayerId,
            votingSelectedPlayerId = data.votingSelectedPlayerId,
            votingResult = data.votingResult?.let {
                VotingResult(
                    eliminatedPlayerId = it.eliminatedPlayerId,
                    eliminatedPlayerName = it.eliminatedPlayerName,
                    revealedRole = it.revealedRole,
                )
            },
            mrWhiteGuessInput = data.mrWhiteGuessInput,
            mrWhiteGuessWasCorrect = data.mrWhiteGuessWasCorrect,
        )
    }

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

    fun toggleMrWhiteTip(enabled: Boolean) {
        state = state.copy(mrWhiteTipEnabled = enabled)
    }

    fun showRulesDialog() {
        state = state.copy(showRulesDialog = true)
    }

    fun hideRulesDialog() {
        state = state.copy(showRulesDialog = false)
    }

    fun showPlayerSelector() {
        state = state.copy(showPlayerSelector = true)
    }

    fun hidePlayerSelector() {
        state = state.copy(showPlayerSelector = false)
    }

    fun setSetupPlayers(names: List<String>) {
        state = state.copy(setupPlayers = names)
        coerceRoleCountsToValidRange()
    }

    fun canStartGame(): Boolean {
        val n = state.setupPlayers.size
        val special = state.setupMrWhiteCount + state.setupUndercoverCount
        if (n < 3) return false
        if (special >= n) return false
        if (state.setupMrWhiteCount < 0 || state.setupUndercoverCount < 0) return false
        
        // Check if word lists are available by trying to get them
        val currentLanguage = runBlocking { languageService.getCurrentLanguage() }
        val wordPairs = getUndercoverWordPairs(currentLanguage)
        if (wordPairs.isEmpty()) return false
        
        return true
    }

    fun startGame() {
        if (!canStartGame()) return

        // Get the appropriate word list based on current language
        val currentLanguage = runBlocking { languageService.getCurrentLanguage() }
        val wordPairs = getUndercoverWordPairs(currentLanguage)
        val wordPair = wordPairs.random(Random)
        
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

    fun getMrWhiteTipForPlayer(player: Player): String? {
        if (!state.mrWhiteTipEnabled) return null
        if (player.actualRole != ActualRole.MR_WHITE) return null
        return state.selectedWordPair?.mrWhiteTip?.takeIf { it.isNotBlank() }
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

    /** Sniffing is possible once everyone got their word, until the game is decided. */
    fun canSniff(): Boolean = when (state.phase) {
        Phase.CHOOSE_STARTER, Phase.DISCUSSION -> true
        Phase.VOTING -> state.votingResult == null
        else -> false
    }

    fun sniffCandidates(): List<Player> = state.players.filter { it.isAlive }

    fun sniffPlayerOrNull(): Player? = state.players.firstOrNull { it.id == state.sniffPlayerId }

    fun openSniff() {
        if (!canSniff()) return
        state = state.copy(sniffStep = SniffStep.SELECT_PLAYER, sniffPlayerId = null)
    }

    /** Picking a name only asks for confirmation - the word stays hidden until the player confirms it is them. */
    fun selectSniffPlayer(playerId: String) {
        if (state.sniffStep != SniffStep.SELECT_PLAYER) return
        if (sniffCandidates().none { it.id == playerId }) return
        state = state.copy(sniffStep = SniffStep.CONFIRM_IDENTITY, sniffPlayerId = playerId)
    }

    fun confirmSniffIdentity() {
        if (state.sniffStep != SniffStep.CONFIRM_IDENTITY || sniffPlayerOrNull() == null) return
        state = state.copy(sniffStep = SniffStep.REVEAL)
        scheduleAutoHideIfEnabled()
    }

    fun closeSniff() {
        if (state.sniffStep == SniffStep.CLOSED) return
        cancelAutoHide()
        state = state.copy(sniffStep = SniffStep.CLOSED, sniffPlayerId = null)
    }

    fun resetGame() {
        cancelAutoHide()
        saveSession.clear()
        state = UiState(
            setupPlayers = state.setupPlayers,
            setupMrWhiteCount = state.setupMrWhiteCount,
            setupUndercoverCount = state.setupUndercoverCount,
            autoHideEnabled = state.autoHideEnabled,
            autoHideSeconds = state.autoHideSeconds,
            mrWhiteTipEnabled = state.mrWhiteTipEnabled
        )
        coerceRoleCountsToValidRange()
    }

    fun restartWithSamePlayers() {
        if (state.phase != Phase.GAME_OVER) return
        startGame()
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
            if (state.sniffStep == SniffStep.REVEAL) {
                closeSniff()
            } else if (state.phase == Phase.REVEAL) {
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

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}
