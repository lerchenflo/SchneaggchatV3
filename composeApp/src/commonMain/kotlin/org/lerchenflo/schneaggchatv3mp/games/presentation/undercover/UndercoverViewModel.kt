package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.LocalGameSaveSlot
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadController
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
    gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UndercoverState())

    private val highscoreUpload = HighscoreUploadController(
        game = GameId.UNDERCOVER,
        repository = gameHighscoreRepository,
        scope = viewModelScope,
    )

    val state = combine(_state, highscoreUpload.state) { state, upload ->
        state.copy(highscoreUpload = upload)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UndercoverState(),
    )

    private var revealAutoHideJob: Job? = null

    /** The round's word pair. Kept out of the state so only the player at the screen sees a word. */
    private var selectedWordPair: UndercoverWordPair? = null

    /** Loaded once for the current language instead of blocking the main thread on every check. */
    private var wordPairs: List<UndercoverWordPair> = emptyList()

    private val saveSession = GameSaveSession(
        game = LocalGameSaveSlot.UNDERCOVER,
        serializer = UndercoverSnapshot.serializer(),
        schemaVersion = UNDERCOVER_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )

    init {
        viewModelScope.launch {
            wordPairs = getUndercoverWordPairs(languageService.getCurrentLanguage())
            _state.update { it.copy(wordListReady = wordPairs.isNotEmpty()) }
        }
        saveSession.start(
            onRestore = ::restore,
            onAppBackgrounded = {
                // Whoever picks the phone up next must not see a word that was left open
                closeSniff()
                persist()
            }
        )
    }

    fun onAction(action: UndercoverAction) {
        when (action) {
            UndercoverAction.OnShowPlayerSelector -> _state.update { it.copy(showPlayerSelector = true) }
            UndercoverAction.OnHidePlayerSelector -> _state.update { it.copy(showPlayerSelector = false) }
            is UndercoverAction.OnPlayersSelected -> setSetupPlayers(action.players)
            is UndercoverAction.OnRemoveSetupPlayer -> removeSetupPlayer(action.name)
            UndercoverAction.OnIncrementMrWhiteCount -> updateRoleCounts(mrWhiteDelta = 1)
            UndercoverAction.OnDecrementMrWhiteCount -> updateRoleCounts(mrWhiteDelta = -1)
            UndercoverAction.OnIncrementUndercoverCount -> updateRoleCounts(undercoverDelta = 1)
            UndercoverAction.OnDecrementUndercoverCount -> updateRoleCounts(undercoverDelta = -1)
            is UndercoverAction.OnToggleAutoHide -> _state.update { it.copy(autoHideEnabled = action.enabled) }
            UndercoverAction.OnIncrementAutoHideSeconds ->
                _state.update { it.copy(autoHideSeconds = (it.autoHideSeconds + 1).coerceAtMost(30)) }
            UndercoverAction.OnDecrementAutoHideSeconds ->
                _state.update { it.copy(autoHideSeconds = (it.autoHideSeconds - 1).coerceAtLeast(1)) }
            is UndercoverAction.OnToggleMrWhiteTip -> _state.update { it.copy(mrWhiteTipEnabled = action.enabled) }
            UndercoverAction.OnShowRules -> _state.update { it.copy(showRulesDialog = true) }
            UndercoverAction.OnHideRules -> _state.update { it.copy(showRulesDialog = false) }
            UndercoverAction.OnStartGame -> startGame()

            UndercoverAction.OnConfirmPlayerIdentity -> onConfirmPlayerIdentity()
            UndercoverAction.OnHideAndPassPhone -> onHideAndPassPhone()
            UndercoverAction.OnPickRandomStarter -> selectRandomStarterIfNeeded()
            UndercoverAction.OnConfirmStarter -> confirmStarter()
            UndercoverAction.OnStartVoting -> startVoting()
            is UndercoverAction.OnSelectVote -> selectVote(action.playerId)
            UndercoverAction.OnConfirmVote -> confirmVote()
            UndercoverAction.OnContinueAfterVotingResult -> onContinueAfterVotingResult()
            is UndercoverAction.OnMrWhiteGuessChange -> updateMrWhiteGuessInput(action.guess)
            UndercoverAction.OnSubmitMrWhiteGuess -> submitMrWhiteGuess()
            UndercoverAction.OnRestartWithSamePlayers -> restartWithSamePlayers()
            UndercoverAction.OnResetGame -> resetGame()

            UndercoverAction.OnOpenSniff -> openSniff()
            is UndercoverAction.OnSelectSniffPlayer -> selectSniffPlayer(action.playerId)
            UndercoverAction.OnConfirmSniffIdentity -> confirmSniffIdentity()
            UndercoverAction.OnCloseSniff -> closeSniff()

            UndercoverAction.OnUploadHighscores -> highscoreUpload.upload()
            UndercoverAction.OnDeclineHighscoreUpload -> highscoreUpload.decline()
        }
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private fun setSetupPlayers(players: List<GamePlayer>) {
        _state.update {
            it.copy(
                setupPlayers = players.map { player -> player.name },
                setupPlayerUserIds = players.mapNotNull { player ->
                    player.userId?.let { id -> player.name to id }
                }.toMap(),
                showPlayerSelector = false,
            )
        }
        coerceRoleCountsToValidRange()
    }

    private fun removeSetupPlayer(name: String) {
        _state.update {
            it.copy(
                setupPlayers = it.setupPlayers - name,
                setupPlayerUserIds = it.setupPlayerUserIds - name,
            )
        }
        coerceRoleCountsToValidRange()
    }

    private fun updateRoleCounts(mrWhiteDelta: Int = 0, undercoverDelta: Int = 0) {
        _state.update {
            it.copy(
                setupMrWhiteCount = (it.setupMrWhiteCount + mrWhiteDelta).coerceAtLeast(0),
                setupUndercoverCount = (it.setupUndercoverCount + undercoverDelta).coerceAtLeast(0),
            )
        }
        coerceRoleCountsToValidRange()
    }

    /** Keeps the special roles inside what the current number of players allows. */
    private fun coerceRoleCountsToValidRange() {
        _state.update { current ->
            val playerCount = current.setupPlayers.size
            if (playerCount <= 0) {
                return@update current.copy(
                    setupMrWhiteCount = current.setupMrWhiteCount.coerceAtLeast(0),
                    setupUndercoverCount = current.setupUndercoverCount.coerceAtLeast(0),
                )
            }
            val maxSpecial = (playerCount - 1).coerceAtLeast(0)
            val mrWhite = current.setupMrWhiteCount.coerceIn(0, maxSpecial)
            val undercover = current.setupUndercoverCount.coerceIn(0, (maxSpecial - mrWhite).coerceAtLeast(0))
            current.copy(setupMrWhiteCount = mrWhite, setupUndercoverCount = undercover)
        }
    }

    private fun startGame() {
        val current = _state.value
        if (!current.canStartGame) return
        highscoreUpload.reset()

        val wordPair = wordPairs.randomOrNull(Random) ?: return
        selectedWordPair = wordPair

        val names = current.setupPlayers
        val roles = buildList {
            repeat(current.setupMrWhiteCount) { add(UndercoverRole.MR_WHITE) }
            repeat(current.setupUndercoverCount) { add(UndercoverRole.UNDERCOVER) }
            repeat(names.size - current.setupMrWhiteCount - current.setupUndercoverCount) {
                add(UndercoverRole.CIVILIAN)
            }
        }.shuffled(Random)

        val players = names.mapIndexed { index, name ->
            UndercoverPlayer(
                id = "p$index",
                name = name,
                actualRole = roles[index],
                isAlive = true,
                userId = current.setupPlayerUserIds[name],
            )
        }

        _state.update {
            it.copy(
                phase = UndercoverPhase.PASS_PHONE,
                players = players,
                currentRevealIndex = 0,
                revealWord = null,
                revealMrWhiteTip = null,
                selectedStarterPlayerId = null,
                selectedStarterName = null,
                votingSelectedPlayerId = null,
                votingResult = null,
                mrWhiteGuessInput = "",
                mrWhiteGuessWasCorrect = null,
                winnerText = null,
                sniffStep = SniffStep.CLOSED,
                sniffPlayerId = null,
                sniffWord = null,
                sniffMrWhiteTip = null,
            )
        }
    }

    // ─── Reveal ───────────────────────────────────────────────────────────────

    private fun onConfirmPlayerIdentity() {
        val current = _state.value
        if (current.phase != UndercoverPhase.PASS_PHONE) return
        val player = current.currentRevealPlayer ?: return
        _state.update {
            it.copy(
                phase = UndercoverPhase.REVEAL,
                revealWord = wordFor(player),
                revealMrWhiteTip = mrWhiteTipFor(player),
            )
        }
        scheduleAutoHideIfEnabled()
    }

    private fun onHideAndPassPhone() {
        val current = _state.value
        if (current.phase != UndercoverPhase.REVEAL) return
        cancelAutoHide()

        val nextIndex = current.currentRevealIndex + 1
        if (nextIndex >= current.players.size) {
            _state.update {
                it.copy(phase = UndercoverPhase.CHOOSE_STARTER, revealWord = null, revealMrWhiteTip = null)
            }
        } else {
            _state.update {
                it.copy(
                    phase = UndercoverPhase.PASS_PHONE,
                    currentRevealIndex = nextIndex,
                    revealWord = null,
                    revealMrWhiteTip = null,
                )
            }
        }
    }

    private fun wordFor(player: UndercoverPlayer): String? {
        val pair = selectedWordPair ?: return null
        return when (player.actualRole) {
            UndercoverRole.MR_WHITE -> null
            UndercoverRole.CIVILIAN -> pair.civilianWord
            UndercoverRole.UNDERCOVER -> pair.undercoverWord
        }
    }

    private fun mrWhiteTipFor(player: UndercoverPlayer): String? {
        if (!_state.value.mrWhiteTipEnabled) return null
        if (player.actualRole != UndercoverRole.MR_WHITE) return null
        return selectedWordPair?.mrWhiteTip?.takeIf { it.isNotBlank() }
    }

    // ─── Round ────────────────────────────────────────────────────────────────

    private fun selectRandomStarterIfNeeded() {
        val current = _state.value
        if (current.phase != UndercoverPhase.CHOOSE_STARTER) return
        if (current.selectedStarterPlayerId != null) return
        val picked = current.starterCandidates.randomOrNull(Random) ?: return
        _state.update { it.copy(selectedStarterPlayerId = picked.id, selectedStarterName = picked.name) }
    }

    private fun confirmStarter() {
        val current = _state.value
        if (current.phase != UndercoverPhase.CHOOSE_STARTER) return
        val selected = current.selectedStarterPlayerId ?: return
        if (current.starterCandidates.none { it.id == selected }) return
        _state.update { it.copy(phase = UndercoverPhase.DISCUSSION) }
    }

    private fun startVoting() {
        if (_state.value.phase != UndercoverPhase.DISCUSSION) return
        _state.update {
            it.copy(
                phase = UndercoverPhase.VOTING,
                votingSelectedPlayerId = null,
                votingResult = null,
            )
        }
    }

    private fun selectVote(playerId: String) {
        val current = _state.value
        if (current.phase != UndercoverPhase.VOTING || current.votingResult != null) return
        if (current.votingCandidates.none { it.id == playerId }) return
        _state.update { it.copy(votingSelectedPlayerId = playerId) }
    }

    private fun confirmVote() {
        val current = _state.value
        if (current.phase != UndercoverPhase.VOTING || current.votingResult != null) return
        val targetId = current.votingSelectedPlayerId ?: return
        val target = current.players.firstOrNull { it.id == targetId && it.isAlive } ?: return

        _state.update {
            it.copy(
                players = it.players.map { player ->
                    if (player.id == targetId) player.copy(isAlive = false) else player
                },
                votingResult = UndercoverVotingResult(
                    eliminatedPlayerId = target.id,
                    eliminatedPlayerName = target.name,
                    revealedRole = target.actualRole,
                ),
            )
        }
    }

    private fun onContinueAfterVotingResult() {
        val current = _state.value
        if (current.phase != UndercoverPhase.VOTING) return
        val result = current.votingResult ?: return

        if (result.revealedRole == UndercoverRole.MR_WHITE) {
            _state.update {
                it.copy(
                    phase = UndercoverPhase.MR_WHITE_GUESS,
                    mrWhiteGuessInput = "",
                    mrWhiteGuessWasCorrect = null,
                )
            }
            return
        }

        val winner = evaluateWinner(current.players)
        if (winner != null) {
            _state.update { it.copy(phase = UndercoverPhase.GAME_OVER, winnerText = winner) }
            offerWinsForWinningSide()
            return
        }

        _state.update {
            it.copy(
                phase = UndercoverPhase.DISCUSSION,
                votingSelectedPlayerId = null,
                votingResult = null,
            )
        }
    }

    private fun updateMrWhiteGuessInput(newValue: String) {
        if (_state.value.phase != UndercoverPhase.MR_WHITE_GUESS) return
        _state.update { it.copy(mrWhiteGuessInput = newValue) }
    }

    private fun submitMrWhiteGuess() {
        val current = _state.value
        if (current.phase != UndercoverPhase.MR_WHITE_GUESS) return
        val civilianWord = selectedWordPair?.civilianWord ?: return
        val correct = current.mrWhiteGuessInput.trim().equals(civilianWord, ignoreCase = true)

        if (correct) {
            _state.update {
                it.copy(
                    phase = UndercoverPhase.GAME_OVER,
                    mrWhiteGuessWasCorrect = true,
                    winnerText = UiText.StringResourceText(Res.string.undercover_winner_mr_white),
                )
            }
            // Only the Mr. White who guessed wins
            val guesser = current.players.firstOrNull { it.id == current.votingResult?.eliminatedPlayerId }
            offerWins(listOfNotNull(guesser))
            return
        }

        val winnerAfterWrongGuess = evaluateWinner(current.players)
        _state.update {
            it.copy(
                phase = if (winnerAfterWrongGuess == null) UndercoverPhase.DISCUSSION else UndercoverPhase.GAME_OVER,
                mrWhiteGuessWasCorrect = false,
                winnerText = winnerAfterWrongGuess,
            )
        }
        if (winnerAfterWrongGuess != null) offerWinsForWinningSide()
    }

    private fun restartWithSamePlayers() {
        if (_state.value.phase != UndercoverPhase.GAME_OVER) return
        startGame()
    }

    private fun resetGame() {
        cancelAutoHide()
        highscoreUpload.reset()
        saveSession.clear()
        selectedWordPair = null
        _state.update { current ->
            UndercoverState(
                setupPlayers = current.setupPlayers,
                setupPlayerUserIds = current.setupPlayerUserIds,
                setupMrWhiteCount = current.setupMrWhiteCount,
                setupUndercoverCount = current.setupUndercoverCount,
                autoHideEnabled = current.autoHideEnabled,
                autoHideSeconds = current.autoHideSeconds,
                mrWhiteTipEnabled = current.mrWhiteTipEnabled,
                wordListReady = current.wordListReady,
            )
        }
        coerceRoleCountsToValidRange()
    }

    // ─── Sniff ────────────────────────────────────────────────────────────────

    private fun openSniff() {
        if (!_state.value.canSniff) return
        _state.update {
            it.copy(
                sniffStep = SniffStep.SELECT_PLAYER,
                sniffPlayerId = null,
                sniffWord = null,
                sniffMrWhiteTip = null,
            )
        }
    }

    /** Picking a name only asks for confirmation - the word stays hidden until the player confirms it is them. */
    private fun selectSniffPlayer(playerId: String) {
        val current = _state.value
        if (current.sniffStep != SniffStep.SELECT_PLAYER) return
        if (current.sniffCandidates.none { it.id == playerId }) return
        _state.update { it.copy(sniffStep = SniffStep.CONFIRM_IDENTITY, sniffPlayerId = playerId) }
    }

    private fun confirmSniffIdentity() {
        val current = _state.value
        if (current.sniffStep != SniffStep.CONFIRM_IDENTITY) return
        val player = current.sniffPlayer ?: return
        _state.update {
            it.copy(
                sniffStep = SniffStep.REVEAL,
                sniffWord = wordFor(player),
                sniffMrWhiteTip = mrWhiteTipFor(player),
            )
        }
        scheduleAutoHideIfEnabled()
    }

    private fun closeSniff() {
        if (_state.value.sniffStep == SniffStep.CLOSED) return
        cancelAutoHide()
        _state.update {
            it.copy(
                sniffStep = SniffStep.CLOSED,
                sniffPlayerId = null,
                sniffWord = null,
                sniffMrWhiteTip = null,
            )
        }
    }

    // ─── Winners ──────────────────────────────────────────────────────────────

    private fun evaluateWinner(players: List<UndercoverPlayer>): UiText? {
        val roles = winningRoles(players) ?: return null
        return if (UndercoverRole.CIVILIAN in roles) {
            UiText.StringResourceText(Res.string.undercover_winner_civilians)
        } else {
            UiText.StringResourceText(Res.string.undercover_winner_undercover)
        }
    }

    /** Roles of the side that won by elimination, or null while the game is still open. */
    private fun winningRoles(players: List<UndercoverPlayer>): Set<UndercoverRole>? {
        val alive = players.filter { it.isAlive }
        val aliveCivilians = alive.count { it.actualRole == UndercoverRole.CIVILIAN }
        val aliveUndercovers = alive.count { it.actualRole == UndercoverRole.UNDERCOVER }
        val aliveMrWhites = alive.count { it.actualRole == UndercoverRole.MR_WHITE }

        if (aliveUndercovers == 0 && aliveMrWhites == 0) {
            return setOf(UndercoverRole.CIVILIAN)
        }

        if (aliveCivilians <= 1 && (aliveUndercovers + aliveMrWhites) > 0) {
            return setOf(UndercoverRole.UNDERCOVER, UndercoverRole.MR_WHITE)
        }

        return null
    }

    /** The whole winning side gets a win, including teammates that were voted out earlier. */
    private fun offerWinsForWinningSide() {
        val players = _state.value.players
        val roles = winningRoles(players) ?: return
        offerWins(players.filter { it.actualRole in roles })
    }

    /**
     * Asks whether one win should be added for each winner. Wins go to each winner's own account;
     * players without an account are skipped by the controller.
     */
    private fun offerWins(winners: List<UndercoverPlayer>) {
        highscoreUpload.offer(
            difficulty = GameDifficulty.MEDIUM,
            results = winners.map { GamePlayer(name = it.name, userId = it.userId) to 1L },
        )
    }

    // ─── Auto hide ────────────────────────────────────────────────────────────

    private fun scheduleAutoHideIfEnabled() {
        cancelAutoHide()
        if (!_state.value.autoHideEnabled) return

        revealAutoHideJob = viewModelScope.launch {
            delay(_state.value.autoHideSeconds.toLong() * 1000L)
            if (_state.value.sniffStep == SniffStep.REVEAL) {
                closeSniff()
            } else if (_state.value.phase == UndercoverPhase.REVEAL) {
                onHideAndPassPhone()
            }
        }
    }

    private fun cancelAutoHide() {
        revealAutoHideJob?.cancel()
        revealAutoHideJob = null
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    /** Leaving the screen or backgrounding the app keeps the running game for the next visit. */
    fun persist() = saveSession.persist(snapshotOrNull())

    /** Null when there is no game worth keeping (still in setup or already over). */
    private fun snapshotOrNull(): UndercoverSnapshot? {
        val current = _state.value
        if (current.phase == UndercoverPhase.SETUP || current.phase == UndercoverPhase.GAME_OVER) return null
        val wordPair = selectedWordPair ?: return null
        return UndercoverSnapshot(
            phase = current.phase,
            setupPlayers = current.setupPlayers,
            setupPlayerUserIds = current.setupPlayerUserIds,
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
                UndercoverPlayerSnapshot(
                    id = it.id,
                    name = it.name,
                    actualRole = it.actualRole,
                    isAlive = it.isAlive,
                    userId = it.userId,
                )
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
        selectedWordPair = UndercoverWordPair(
            civilianWord = data.wordPair.civilianWord,
            undercoverWord = data.wordPair.undercoverWord,
            mrWhiteTip = data.wordPair.mrWhiteTip,
        )
        val players = data.players.map {
            UndercoverPlayer(
                id = it.id,
                name = it.name,
                actualRole = it.actualRole,
                isAlive = it.isAlive,
                userId = it.userId,
            )
        }
        _state.update { current ->
            UndercoverState(
                // A word that was on screen must not show up again for whoever reopens the app,
                // so the player has to confirm their identity again first
                phase = if (data.phase == UndercoverPhase.REVEAL) UndercoverPhase.PASS_PHONE else data.phase,
                setupPlayers = data.setupPlayers,
                setupPlayerUserIds = data.setupPlayerUserIds,
                setupMrWhiteCount = data.setupMrWhiteCount,
                setupUndercoverCount = data.setupUndercoverCount,
                autoHideEnabled = data.autoHideEnabled,
                autoHideSeconds = data.autoHideSeconds,
                mrWhiteTipEnabled = data.mrWhiteTipEnabled,
                wordListReady = current.wordListReady,
                players = players,
                currentRevealIndex = data.currentRevealIndex.coerceIn(0, data.players.lastIndex),
                selectedStarterPlayerId = data.selectedStarterPlayerId,
                selectedStarterName = players.firstOrNull { it.id == data.selectedStarterPlayerId }?.name,
                votingSelectedPlayerId = data.votingSelectedPlayerId,
                votingResult = data.votingResult?.let {
                    UndercoverVotingResult(
                        eliminatedPlayerId = it.eliminatedPlayerId,
                        eliminatedPlayerName = it.eliminatedPlayerName,
                        revealedRole = it.revealedRole,
                    )
                },
                mrWhiteGuessInput = data.mrWhiteGuessInput,
                mrWhiteGuessWasCorrect = data.mrWhiteGuessWasCorrect,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}
