package org.lerchenflo.schneaggchatv3mp.games.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.games.domain.WORDLE_WORD_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.WordlePuzzle
import org.lerchenflo.schneaggchatv3mp.utilities.today
import kotlin.random.Random

/** Public solution endpoint, no API key: .../v2/2026-09-21.json */
private const val NYT_WORDLE_URL = "https://www.nytimes.com/svc/wordle/v2"

/** Public list of words Wordle accepts as a guess (one per line, lowercase). */
private const val GUESS_LIST_URL = "https://raw.githubusercontent.com/tabatkins/wordle-list/main/words"

/** Wordle #1, the first date the endpoint answers for. Everything up to today is fair game. */
private val WORDLE_FIRST_DATE = LocalDate(2021, 6, 19)

/** A date can come back empty or malformed; try a few before giving up. */
private const val MAX_FETCH_ATTEMPTS = 4

@Serializable
private data class NytWordleDto(
    val solution: String,
    @SerialName("days_since_launch") val daysSinceLaunch: Int? = null,
    val editor: String? = null,
)

/**
 * Fetches English words from the public New York Times Wordle endpoint and,
 * separately, the public list of accepted guesses. Both are plain HTTP GETs
 * without authentication, like the crossword archive in [CrosswordRepository].
 */
class WordleRepository(
    private val httpClient: HttpClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val guessListMutex = Mutex()
    /** In-memory only: ~15k words, cheap to re-fetch once per app start. */
    private var cachedGuessList: Set<String>? = null

    /**
     * A real Wordle solution from a random past date — the endpoint answers for
     * every day since [WORDLE_FIRST_DATE], which is a pool of ~1900 words that
     * grows by one a day. null on failure (offline) — caller shows retry.
     */
    suspend fun getRandomEnglishPuzzle(): WordlePuzzle? {
        val firstDay = WORDLE_FIRST_DATE.toEpochDays()
        val lastDay = today().toEpochDays()
        if (lastDay < firstDay) return null

        repeat(MAX_FETCH_ATTEMPTS) {
            val day = Random.nextLong(firstDay, lastDay + 1)
            fetchPuzzle(LocalDate.fromEpochDays(day))?.let { return it }
        }
        return null
    }

    /** null when that date is missing or the payload is not a plain five-letter word. */
    private suspend fun fetchPuzzle(date: LocalDate): WordlePuzzle? {
        val dto = try {
            val response = httpClient.get("$NYT_WORDLE_URL/$date.json")
            if (!response.status.isSuccess()) return null
            json.decodeFromString<NytWordleDto>(response.bodyAsText())
        } catch (_: Exception) {
            return null
        }

        val solution = dto.solution.uppercase()
        if (solution.length != WORDLE_WORD_LENGTH || !solution.all { it in 'A'..'Z' }) return null

        return WordlePuzzle(
            language = WordleLanguage.ENGLISH,
            solution = solution,
            sourceInfo = listOfNotNull(
                dto.daysSinceLaunch?.let { "Wordle #$it" },
                dto.editor,
            ).joinToString(" · ").ifEmpty { null },
        )
    }

    /**
     * The words accepted as a guess, uppercase. null when the list could not be
     * fetched — the caller then accepts any five letters instead of locking the
     * player out of the game.
     */
    suspend fun getEnglishGuessList(): Set<String>? = guessListMutex.withLock {
        cachedGuessList?.let { return@withLock it }

        val words = try {
            val response = httpClient.get(GUESS_LIST_URL)
            if (!response.status.isSuccess()) return@withLock null
            response.bodyAsText()
                .lineSequence()
                .map { it.trim().uppercase() }
                .filterTo(mutableSetOf()) { it.length == WORDLE_WORD_LENGTH && it.all { c -> c in 'A'..'Z' } }
        } catch (_: Exception) {
            return@withLock null
        }

        if (words.isEmpty()) return@withLock null
        cachedGuessList = words
        words
    }
}
