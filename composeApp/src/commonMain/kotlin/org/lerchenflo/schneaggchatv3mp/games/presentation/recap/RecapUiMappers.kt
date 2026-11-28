package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import org.lerchenflo.schneaggchatv3mp.games.domain.ChatHabitsRecapDto
import org.lerchenflo.schneaggchatv3mp.games.domain.ChatHabitsRecapUi
import org.lerchenflo.schneaggchatv3mp.games.domain.EmojiCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.FastestReaderUi
import org.lerchenflo.schneaggchatv3mp.games.domain.FeedbackRecapDto
import org.lerchenflo.schneaggchatv3mp.games.domain.FeedbackRecapUi
import org.lerchenflo.schneaggchatv3mp.games.domain.FeedbackTopEntryUi
import org.lerchenflo.schneaggchatv3mp.games.domain.FeedbackVotesUi
import org.lerchenflo.schneaggchatv3mp.games.domain.MonologueUi
import org.lerchenflo.schneaggchatv3mp.games.domain.NamedCountDto
import org.lerchenflo.schneaggchatv3mp.games.domain.NamedCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.StarterPartnerUi
import org.lerchenflo.schneaggchatv3mp.games.domain.TextCountDto
import org.lerchenflo.schneaggchatv3mp.games.domain.TextCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.WakeRecapDto
import org.lerchenflo.schneaggchatv3mp.games.domain.WakeRecapUi
import org.lerchenflo.schneaggchatv3mp.utilities.format.millisToString

// Mappers from the /recap topics to their UI models (pure, no local data needed).

private const val RECAP_DATE_FORMAT = "dd.MM.yyyy"

fun TextCountDto.toUi(): TextCountUi = TextCountUi(text = text, count = count)

fun NamedCountDto.toUi(): NamedCountUi = NamedCountUi(name = name, group = group, count = count)

fun recapDate(millis: Long): String = millisToString(millis, RECAP_DATE_FORMAT)

fun FeedbackRecapDto.toUi(): FeedbackRecapUi = FeedbackRecapUi(
    bugsThisYear = bugsReportedThisYear,
    bugsAllTime = bugsReportedAllTime,
    ideasThisYear = featuresRequestedThisYear,
    ideasAllTime = featuresRequestedAllTime,
    ideasPlanned = myEntriesByStatus["PLANNED"] ?: 0L,
    ideasImplemented = myEntriesByStatus["IMPLEMENTED"] ?: 0L,
    bugsConfirmed = myEntriesByStatus["CONFIRMED"] ?: 0L,
    bugsFixed = myEntriesByStatus["FIXED"] ?: 0L,
    topEntry = topEntry?.let {
        FeedbackTopEntryUi(title = it.title, isBug = it.type == "BUG", status = it.status, upVotes = it.upVotes)
    },
    votesCast = votesCast.toVotesUi(),
    votesReceived = votesReceived.toVotesUi(),
    commentsThisYear = commentsWrittenThisYear,
    commentsAllTime = commentsWrittenAllTime,
    devRepliesThisYear = devRepliesReceivedThisYear,
    devRepliesAllTime = devRepliesReceivedAllTime,
)

private fun Map<String, Long>.toVotesUi(): FeedbackVotesUi = FeedbackVotesUi(
    up = this["UP"] ?: 0L,
    down = this["DOWN"] ?: 0L,
    didntKnow = this["DIDNT_KNOW"] ?: 0L,
    notNeeded = this["NOT_NEEDED"] ?: 0L,
)

fun ChatHabitsRecapDto.toUi(): ChatHabitsRecapUi = ChatHabitsRecapUi(
    medianReadMillis = medianReadMillis,
    medianReplyMillis = medianReplyMillis,
    fastestReader = fastestReader?.let { FastestReaderUi(name = it.name, medianMillis = it.medianMillis) },
    longestMonologue = longestMonologue?.let {
        MonologueUi(messageCount = it.messageCount, partnerName = it.partnerName, startedAtFormatted = recapDate(it.startedAt))
    },
    conversations = conversations,
    conversationsStartedByMe = conversationsStartedByMe,
    starterPerPartner = starterPerPartner
        .filter { it.conversations > 0 }
        .map { StarterPartnerUi(name = it.name, conversations = it.conversations, startedByMe = it.startedByMe) },
    topOpener = topOpener?.toUi(),
    topEmojiTyped = topEmojiTyped.take(5).map { EmojiCountUi(it.emoji, it.count) },
)

fun WakeRecapDto.toUi(): WakeRecapUi = WakeRecapUi(
    sentThisYear = wakesSentThisYear,
    sentAllTime = wakesSentAllTime,
    mostWoken = mostWoken?.toUi(),
    topReason = topReason?.toUi(),
    receivedThisYear = wakesReceivedThisYear,
    receivedAllTime = wakesReceivedAllTime,
    topWaker = topWaker?.toUi(),
    groupWakesThisYear = groupWakesThisYear,
)
