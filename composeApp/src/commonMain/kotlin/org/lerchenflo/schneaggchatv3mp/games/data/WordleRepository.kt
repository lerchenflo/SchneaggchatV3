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

/** Public daily-solution endpoint, no API key: .../v2/2026-09-21.json */
private const val NYT_WORDLE_URL = "https://www.nytimes.com/svc/wordle/v2"

/** Public list of words Wordle accepts as a guess (one per line, lowercase). */
private const val GUESS_LIST_URL = "https://raw.githubusercontent.com/tabatkins/wordle-list/main/words"

@Serializable
private data class NytWordleDto(
    val solution: String,
    @SerialName("days_since_launch") val daysSinceLaunch: Int? = null,
    val editor: String? = null,
)

/**
 * Fetches the English daily word from the public New York Times Wordle endpoint
 * and, separately, the public list of accepted guesses. Both are plain HTTP GETs
 * without authentication, like the crossword archive in [CrosswordRepository].
 */
class WordleRepository(
    private val httpClient: HttpClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val guessListMutex = Mutex()
    /** In-memory only: ~15k words, cheap to re-fetch once per app start. */
    private var cachedGuessList: Set<String>? = null

    /** null on failure (offline / unexpected payload) — caller shows retry. */
    suspend fun getEnglishDailyPuzzle(date: LocalDate): WordlePuzzle? {
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
