package org.lerchenflo.schneaggchatv3mp.games.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordClue
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordDirection
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordPuzzle
import org.lerchenflo.schneaggchatv3mp.games.domain.SplitMix64

/** The archive holds every NYT puzzle 1977–2017; stay one year clear of the end. */
private const val ARCHIVE_FIRST_YEAR = 1977
private const val ARCHIVE_LAST_YEAR = 2016

/** Sunday puzzles are 21x21 — too big for a phone grid, so those dates are skipped. */
private const val MAX_GRID_SIZE = 15

private const val MAX_FETCH_ATTEMPTS = 8

@Serializable
private data class NytSizeDto(
    val rows: Int,
    val cols: Int,
)

@Serializable
private data class NytCluesDto(
    val across: List<String>,
    val down: List<String>,
)

@Serializable
private data class NytCrosswordDto(
    val size: NytSizeDto,
    val grid: List<String>,
    val gridnums: List<Int>,
    val clues: NytCluesDto,
    val author: String? = null,
    val date: String? = null,
)

/**
 * Fetches the daily English crossword from the public NYT archive mirror
 * (github.com/doshea/nyt_crosswords, raw JSON, no API key). The puzzle for a
 * given day is picked deterministically: everyone gets the same historic
 * puzzle (same month/day, seeded year) on the same date.
 */
class CrosswordRepository(
    private val httpClient: HttpClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** null on failure (offline / all candidate dates missing) — caller shows retry. */
    suspend fun getEnglishDailyPuzzle(date: LocalDate): CrosswordPuzzle? {
        val random = SplitMix64(date.toEpochDays() * 912_367L + 17L)
        val years = (ARCHIVE_FIRST_YEAR..ARCHIVE_LAST_YEAR).toMutableList()
        random.shuffle(years)

        val candidates = years.filter { year ->
            // Feb 29 only exists in leap years of the archive
            !(date.month.number == 2 && date.day == 29 && !isLeapYear(year))
        }.take(MAX_FETCH_ATTEMPTS)

        for (year in candidates) {
            val puzzle = fetchPuzzle(year, date.month.number, date.day) ?: continue
            return puzzle
        }
        return null
    }

    private suspend fun fetchPuzzle(year: Int, month: Int, day: Int): CrosswordPuzzle? {
        val url = "https://raw.githubusercontent.com/doshea/nyt_crosswords/master/" +
                "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}.json"
        return try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) return null
            val dto = json.decodeFromString<NytCrosswordDto>(response.bodyAsText())
            if (dto.size.rows > MAX_GRID_SIZE || dto.size.cols > MAX_GRID_SIZE) return null
            dto.toPuzzle()
        } catch (_: Exception) {
            null
        }
    }
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun NytCrosswordDto.toPuzzle(): CrosswordPuzzle? {
    val rows = size.rows
    val cols = size.cols
    if (grid.size != rows * cols || gridnums.size != rows * cols) return null

    // "." = block; rebus cells (multi-letter) are reduced to their first letter
    val solution = grid.map { cell ->
        if (cell == "." || cell.isEmpty()) null else cell.first().uppercaseChar()
    }

    val acrossTexts = parseClueTexts(clues.across)
    val downTexts = parseClueTexts(clues.down)

    val parsedClues = mutableListOf<CrosswordClue>()
    for (index in solution.indices) {
        val number = gridnums[index]
        if (number == 0 || solution[index] == null) continue
        val row = index / cols
        val col = index % cols

        val startsAcross = (col == 0 || solution[index - 1] == null) &&
                col + 1 < cols && solution[index + 1] != null
        if (startsAcross) {
            val cells = mutableListOf<Int>()
            var c = col
            while (c < cols && solution[row * cols + c] != null) {
                cells.add(row * cols + c)
                c++
            }
            acrossTexts[number]?.let {
                parsedClues.add(CrosswordClue(number, CrosswordDirection.ACROSS, it, cells))
            }
        }

        val startsDown = (row == 0 || solution[index - cols] == null) &&
                row + 1 < rows && solution[index + cols] != null
        if (startsDown) {
            val cells = mutableListOf<Int>()
            var r = row
            while (r < rows && solution[r * cols + col] != null) {
                cells.add(r * cols + col)
                r++
            }
            downTexts[number]?.let {
                parsedClues.add(CrosswordClue(number, CrosswordDirection.DOWN, it, cells))
            }
        }
    }
    if (parsedClues.isEmpty()) return null

    return CrosswordPuzzle(
        rows = rows,
        cols = cols,
        solution = solution,
        cellNumbers = gridnums,
        clues = parsedClues,
        sourceInfo = listOfNotNull(date?.let { "NYT $it" }, author).joinToString(" · ").ifEmpty { null },
    )
}

/** Clues arrive as "12. Clue text" — split off the leading number. */
private fun parseClueTexts(rawClues: List<String>): Map<Int, String> {
    val result = mutableMapOf<Int, String>()
    for (raw in rawClues) {
        val numberPart = raw.takeWhile { it.isDigit() }
        val number = numberPart.toIntOrNull() ?: continue
        val text = raw.drop(numberPart.length).removePrefix(".").trim()
        result[number] = decodeHtmlEntities(text)
    }
    return result
}

private fun decodeHtmlEntities(text: String): String = text
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&rsquo;", "'")
    .replace("&lsquo;", "'")
    .replace("&rdquo;", "\"")
    .replace("&ldquo;", "\"")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
