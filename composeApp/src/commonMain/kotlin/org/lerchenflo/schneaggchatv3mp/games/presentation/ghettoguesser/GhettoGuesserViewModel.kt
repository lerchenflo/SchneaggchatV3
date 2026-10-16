package org.lerchenflo.schneaggchatv3mp.games.presentation.ghettoguesser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.games.data.CommonsPhotoRepository
import org.lerchenflo.schneaggchatv3mp.games.data.PhotoLookup
import org.lerchenflo.schneaggchatv3mp.games.domain.DemoGuessMaps
import org.lerchenflo.schneaggchatv3mp.games.domain.GHETTO_GUESSER_ROUNDS
import org.lerchenflo.schneaggchatv3mp.games.domain.GuessMap
import org.lerchenflo.schneaggchatv3mp.games.domain.GuessRoundResult
import org.lerchenflo.schneaggchatv3mp.games.domain.calculateGuessPoints
import org.lerchenflo.schneaggchatv3mp.games.domain.maxDistanceFrom
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import kotlin.time.TimeSource

/**
 * Photos waiting in the queue besides the one the loader holds while it waits for room,
 * so two photos (data and image) are ready ahead of the current round.
 */
private const val QUEUED_PHOTOS = 1

class GhettoGuesserViewModel(
    private val photoRepository: CommonsPhotoRepository,
    preferenceManager: Preferencemanager,
) : ViewModel() {

    private val _state = MutableStateFlow(GhettoGuesserState(maps = DemoGuessMaps.all))
    val state = combine(_state, preferenceManager.getMapStyleUrlFlow()) { state, mapStyleUrl ->
        state.copy(mapStyleUrl = mapStyleUrl)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /** Every photo loaded in this game (shown, skipped or queued), never offered twice. */
    private val loadedPageIds = mutableSetOf<Long>()
    private val loadedLocations = mutableListOf<LatLong>()

    /** Filled in the background so the next photo is already downloaded when a round starts. */
    private var photoQueue: Channel<PhotoLookup>? = null
    private var preloadJob: Job? = null
    private var loadJob: Job? = null
    private var roundStart: TimeSource.Monotonic.ValueTimeMark? = null

    fun onAction(action: GhettoGuesserAction) {
        when (action) {
            is GhettoGuesserAction.SelectMap -> startGame(action.map)
            GhettoGuesserAction.BackToMaps -> backToMaps()
            is GhettoGuesserAction.ShowGuessMap -> _state.update { it.copy(isGuessMapVisible = action.visible) }
            is GhettoGuesserAction.PlaceGuess -> placeGuess(action.location)
            GhettoGuesserAction.SubmitGuess -> submitGuess()
            GhettoGuesserAction.SkipPhoto -> showNextPhoto()
            GhettoGuesserAction.NextRound -> nextRound()
            GhettoGuesserAction.PlayAgain -> _state.value.selectedMap?.let(::startGame)
            GhettoGuesserAction.RetryLoad -> retryLoad()
            is GhettoGuesserAction.ShowCredits -> _state.update { it.copy(isCreditsVisible = action.visible) }
        }
    }

    private fun startGame(map: GuessMap) {
        stopLoading()
        loadedPageIds.clear()
        loadedLocations.clear()
        _state.update {
            GhettoGuesserState(maps = it.maps, selectedMap = map)
        }
        startPreloading(map)
        showNextPhoto()
    }

    private fun backToMaps() {
        stopLoading()
        _state.update { GhettoGuesserState(maps = it.maps) }
    }

    private fun stopLoading() {
        loadJob?.cancel()
        preloadJob?.cancel()
        photoQueue?.cancel()
        photoQueue = null
        roundStart = null
    }

    private fun retryLoad() {
        val map = _state.value.selectedMap ?: return
        startPreloading(map)
        showNextPhoto()
    }

    /** Loads photos one after another into [photoQueue]; stops at the first failure until a retry. */
    private fun startPreloading(map: GuessMap) {
        preloadJob?.cancel()
        photoQueue?.cancel()
        val queue = Channel<PhotoLookup>(capacity = QUEUED_PHOTOS)
        photoQueue = queue
        preloadJob = viewModelScope.launch {
            while (true) {
                val lookup = photoRepository.findRandomPhoto(map, loadedPageIds.toSet(), loadedLocations.toList())
                if (lookup is PhotoLookup.Found) {
                    loadedPageIds += lookup.photo.pageId
                    loadedLocations += lookup.photo.location
                }
                queue.send(lookup)
                if (lookup !is PhotoLookup.Found) break
            }
        }
    }

    private fun showNextPhoto() {
        val queue = photoQueue ?: return
        loadJob?.cancel()

        val ready = queue.tryReceive().getOrNull()
        if (ready != null) {
            showLookup(ready)
            return
        }

        _state.update {
            it.copy(
                phase = GhettoGuesserPhase.LOADING,
                loadError = null,
                photo = null,
                imageBytes = null,
                guess = null,
                isGuessMapVisible = false,
                isCreditsVisible = false,
            )
        }
        loadJob = viewModelScope.launch {
            showLookup(queue.receive())
        }
    }

    private fun showLookup(lookup: PhotoLookup) {
        when (lookup) {
            is PhotoLookup.Found -> {
                _state.update {
                    it.copy(
                        phase = GhettoGuesserPhase.GUESSING,
                        loadError = null,
                        photo = lookup.photo,
                        imageBytes = lookup.imageBytes,
                        guess = null,
                        isGuessMapVisible = false,
                        isCreditsVisible = false,
                    )
                }
                roundStart = TimeSource.Monotonic.markNow()
            }
            PhotoLookup.NoPhotos -> showLoadError(PhotoLoadError.NO_PHOTOS)
            PhotoLookup.NetworkError -> showLoadError(PhotoLoadError.NO_NETWORK)
        }
    }

    private fun showLoadError(error: PhotoLoadError) {
        _state.update { it.copy(phase = GhettoGuesserPhase.LOAD_FAILED, loadError = error) }
    }

    private fun placeGuess(location: LatLong) {
        if (_state.value.phase != GhettoGuesserPhase.GUESSING) return
        _state.update { it.copy(guess = location) }
    }

    private fun submitGuess() {
        val current = _state.value
        val map = current.selectedMap ?: return
        val photo = current.photo ?: return
        val guess = current.guess ?: return
        if (current.phase != GhettoGuesserPhase.GUESSING) return

        val distance = distanceMeters(guess, photo.location)
        val result = GuessRoundResult(
            photo = photo,
            guess = guess,
            distanceMeters = distance,
            points = calculateGuessPoints(distance, map.maxDistanceFrom(photo.location)),
            timeMillis = roundStart?.elapsedNow()?.inWholeMilliseconds ?: 0L,
        )
        _state.update {
            it.copy(
                phase = GhettoGuesserPhase.ROUND_RESULT,
                results = it.results + result,
                isGuessMapVisible = true,
            )
        }
    }

    private fun nextRound() {
        val current = _state.value
        if (current.phase != GhettoGuesserPhase.ROUND_RESULT) return
        if (current.round >= GHETTO_GUESSER_ROUNDS) {
            stopLoading()
            _state.update { it.copy(phase = GhettoGuesserPhase.GAME_OVER) }
            return
        }
        _state.update { it.copy(round = it.round + 1) }
        showNextPhoto()
    }
}
