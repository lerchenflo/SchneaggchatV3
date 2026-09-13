@file:OptIn(ExperimentalComposeUiApi::class)

package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import kotlinx.datetime.LocalDate
import org.jetbrains.skia.EncodedImageFormat
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.EventsViewMode
import org.lerchenflo.schneaggchatv3mp.events.presentation.CalendarBirthday
import org.lerchenflo.schneaggchatv3mp.events.presentation.EventsScreen
import org.lerchenflo.schneaggchatv3mp.events.presentation.EventsState
import org.lerchenflo.schneaggchatv3mp.games.domain.EmojiCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.GameRecapUi
import org.lerchenflo.schneaggchatv3mp.games.domain.GroupActivityUi
import org.lerchenflo.schneaggchatv3mp.games.domain.LongestMessageUi
import org.lerchenflo.schneaggchatv3mp.games.domain.MonthCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.MostReactedMessageUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RankedRowUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapPartnerUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapBetaTesterPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapGamesPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapGroupsPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapIntroPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapLeaderboardPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMapLeaderboardPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMapPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMessagesReceivedPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMessagesSentPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapOutroPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapPasswordResetPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapReactionsPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapRhythmPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapSocialPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapTopContactsPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapTypingPage
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders every recap story page (and the events day view) headless on the JVM and writes PNGs
 * to `build/recap-renders/`. Guards against layout crashes and lets the pages be eyeballed
 * without a device. Set `RECAP_RENDER_DIR` to write somewhere else.
 */
class RecapRenderTest {

    private val outDir = File(System.getenv("RECAP_RENDER_DIR") ?: "build/recap-renders").apply { mkdirs() }

    private val recap = RecapUi(
        year = 2026,
        username = "flo",
        memberSinceFormatted = "14.02.2024",
        accountAgeDays = 938,
        loginCountThisYear = 41,
        friendRequestsSentThisYear = 7,
        messagesSent = 18_432,
        messagesReceived = 27_915,
        messagesSentAllTime = 61_204,
        messagesReceivedAllTime = 88_310,
        sentByType = listOf(
            MessageTypeCountUiFixture.text(16_800),
            MessageTypeCountUiFixture.image(1_210),
            MessageTypeCountUiFixture.audio(302),
            MessageTypeCountUiFixture.poll(120),
        ),
        charactersTyped = 1_284_501,
        wordsTyped = 241_872,
        averageMessageLength = 69,
        longestMessage = LongestMessageUi(
            preview = "Also i hon mr denkt mir gond hüt obed no schnell ufd Hütta und denn morn früah witer zum See, wenn s Wetter passt und alle Zit hond",
            length = 1_204,
            toName = "Bergfexe 2026"
        ),
        busiestDayFormatted = "24.12.2026",
        busiestDayCount = 412,
        busiestHourOfDay = 22,
        mostActiveMonth = MonthCountUi(7, UiText.DynamicString("July"), 3_120),
        perMonth = (1..12).map { MonthCountUi(it, UiText.DynamicString("M$it"), (it * 217L) % 3_121L + 200) },
        longestStreakDays = 143,
        firstMessageEverFormatted = "14.02.2024",
        reactionsGiven = 4_812,
        reactionsReceived = 3_977,
        topEmojiGiven = listOf("😂", "❤️", "👍", "🔥", "😭").map { EmojiCountUi(it, 900L) },
        topEmojiReceived = listOf("😂", "😍", "👀").map { EmojiCountUi(it, 400L) },
        mostReactedMessage = MostReactedMessageUi(preview = "Wer kummt morn mit zum Jassa?", reactionCount = 23),
        pollsCreated = 14,
        pollVotesCast = 88,
        friendsCount = 37,
        newFriendsThisYear = 9,
        topPartners = listOf(
            RecapPartnerUi("Bergfexe 2026", true, 9_210, 3_100, 6_110, ""),
            RecapPartnerUi("Fabi", false, 4_402, 2_201, 2_201, ""),
            RecapPartnerUi("Jeh", false, 3_120, 1_500, 1_620, ""),
            RecapPartnerUi("Schneaggchat Devs", true, 2_890, 1_990, 900, ""),
            RecapPartnerUi("Mama", false, 1_204, 800, 404, ""),
        ),
        leaderboardTop = listOf(
            RankedRowUi(1, "fabi", 24_100, false),
            RankedRowUi(2, "flo", 18_432, true),
            RankedRowUi(3, "jeh", 17_002, false),
            RankedRowUi(4, "averyveryverylongusernameindeed", 12_000, false),
            RankedRowUi(5, "mama", 9_000, false),
        ),
        myRank = 2,
        myLeaderboardMessageCount = 18_432,
        groupsMemberOf = 12,
        groupsCreated = 4,
        mostActiveGroup = GroupActivityUi("Bergfexe 2026", 9_210),
        mapEntriesCreated = 23,
        mapEntriesEdited = 61,
        mapEntriesCreatedAllTime = 58,
        mapLeaderboardTop = listOf(
            RankedRowUi(1, "flo", 84, true),
            RankedRowUi(2, "fabi", 51, false),
            RankedRowUi(3, "jeh", 12, false),
        ),
        myMapRank = 1,
        myMapContributions = 84,
        games = listOf(
            GameRecapUi("Tetris", "HARD", 48_200, 1),
            GameRecapUi("Crossword", "MEDIUM", 1_200, 4),
            GameRecapUi("Gridrush", "EASY", 980, 12),
        ),
        betaTesterRows = (1..9).map { RankedRowUi(it, if (it == 3) "flo" else "user$it", 200L - it * 15, it == 3) },
        myBetaTesterRank = 3,
        myExceptionCount = 155,
        passwordResetEmailsSentThisYear = 3,
        passwordResetEmailsSentAllTime = 11,
    )

    private val pages: List<Pair<String, @Composable () -> Unit>> = listOf(
        "01_intro" to { RecapIntroPage(recap, true) },
        "02_sent" to { RecapMessagesSentPage(recap, true) },
        "03_typing" to { RecapTypingPage(recap, true) },
        "04_rhythm" to { RecapRhythmPage(recap, true) },
        "05_received" to { RecapMessagesReceivedPage(recap, true) },
        "06_top_contacts" to { RecapTopContactsPage(recap, true) },
        "07_reactions" to { RecapReactionsPage(recap, true) },
        "08_social" to { RecapSocialPage(recap, true) },
        "09_groups" to { RecapGroupsPage(recap, true) },
        "10_leaderboard" to { RecapLeaderboardPage(recap, true) },
        "11_map" to { RecapMapPage(recap, true) },
        "12_map_leaderboard" to { RecapMapLeaderboardPage(recap, true) },
        "13_games" to { RecapGamesPage(recap, true) },
        "14_betatester" to { RecapBetaTesterPage(recap, true) },
        "15_password" to { RecapPasswordResetPage(recap, true) },
        "16_outro" to { RecapOutroPage(recap, true) },
    )

    @Test
    fun `every recap page renders on a phone sized canvas`() {
        pages.forEach { (name, content) ->
            render("phone_$name", width = 412, height = 915, density = 2f, content = content)
        }
    }

    @Test
    fun `full recap story renders with chrome on phone and desktop`() {
        val state = RecapState(isLoading = false, recap = recap)
        render("story_phone", 412, 915, 2f) { RecapScreen(state = state, onAction = {}, onClose = {}) }
        render("story_desktop", 1280, 800, 1f) { RecapScreen(state = state, onAction = {}, onClose = {}) }
        render("story_small_phone", 360, 640, 2f) { RecapScreen(state = state, onAction = {}, onClose = {}) }
    }

    @Test
    fun `events day view renders with an event and a birthday`() {
        val day = LocalDate(2026, 9, 10)
        val startMillis = 1_789_000_000_000L // arbitrary, only the anchor date drives the day view lookup
        val event = Event(
            id = "e1",
            creatorId = "u2",
            type = EventType.FOOD,
            title = "Pizza bei Fabi",
            description = "Bring Getränke mit",
            groupId = "g1",
            location = null,
            startDate = startMillis,
            closeDate = null,
            invitedUsers = listOf("u1"),
            visibility = EventVisibility.FRIENDS_ONLY,
            createdAt = startMillis,
            updatedAt = startMillis,
            creatorName = "Fabi",
        )
        val fabi = User(
            id = "u2",
            name = "Fabi",
            description = null,
            status = null,
            friendshipStatus = NetworkUtils.FriendshipStatus.ACCEPTED,
            birthDate = "1999-09-10",
            emailVerifiedAt = null,
            createdAt = null,
            profilePicUpdatedAt = 0L,
        )
        val state = EventsState(
            events = listOf(event),
            eventsByDate = mapOf(day to listOf(event)),
            ownUserId = "u1",
            friendsById = mapOf(fabi.id to fabi),
            viewMode = EventsViewMode.DAY,
            calendarAnchorDate = day,
            birthdaysByMonthDay = mapOf(910 to listOf(CalendarBirthday("u2", "Fabi", null, 910, 1999, false))),
        )
        render("events_day_phone", 412, 915, 2f) {
            MaterialTheme { EventsScreen(state = state, onAction = {}) }
        }
        render("events_day_empty_phone", 412, 915, 2f) {
            MaterialTheme { EventsScreen(state = state.copy(calendarAnchorDate = LocalDate(2026, 9, 11)), onAction = {}) }
        }
    }

    /** [width]/[height] are in dp; the scene itself is sized in pixels, hence the density scaling. */
    private fun render(name: String, width: Int, height: Int, density: Float, content: @Composable () -> Unit) {
        ImageComposeScene(
            width = (width * density).toInt(),
            height = (height * density).toInt(),
            density = Density(density),
            content = content
        ).use { scene ->
            // Frame 0 composes and launches the entrance animations; each Animatable stamps its
            // start time on the *next* frame it sees, so a couple of early frames are needed before
            // jumping 8s ahead (past the longest reveal stagger + count-up). The count-up text
            // commits its final value during that frame, hence one more frame before capturing.
            listOf(0L, 16_000_000L, 32_000_000L, 8_000_000_000L).forEach { scene.render(it) }
            val image = scene.render(8_100_000_000L)
            val png = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG encode failed for $name")
            val file = File(outDir, "$name.png")
            file.writeBytes(png.bytes)
            assertTrue(file.length() > 0, "Empty render for $name")
        }
    }
}

/** Builds the four message type rows without touching string resources. */
private object MessageTypeCountUiFixture {
    fun text(count: Long) = org.lerchenflo.schneaggchatv3mp.games.domain.MessageTypeCountUi(UiText.DynamicString("Texts"), count)
    fun image(count: Long) = org.lerchenflo.schneaggchatv3mp.games.domain.MessageTypeCountUi(UiText.DynamicString("Images"), count)
    fun audio(count: Long) = org.lerchenflo.schneaggchatv3mp.games.domain.MessageTypeCountUi(UiText.DynamicString("Voice messages"), count)
    fun poll(count: Long) = org.lerchenflo.schneaggchatv3mp.games.domain.MessageTypeCountUi(UiText.DynamicString("Polls"), count)
}
