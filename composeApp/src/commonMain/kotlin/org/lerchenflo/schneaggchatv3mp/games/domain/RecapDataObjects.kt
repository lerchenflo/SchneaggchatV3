package org.lerchenflo.schneaggchatv3mp.games.domain

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

// All recap data classes live in this single file on purpose:
// network DTOs (mirroring the server /recap response) first, UI models below.

// ─── Network DTOs (server /recap response) ────────────────────────────────

@Serializable
data class RecapResponse(
    val year: Int,
    val generatedAt: Long,
    val account: AccountRecapDto,
    val messaging: MessagingRecapDto,
    val reactions: ReactionsRecapDto,
    val polls: PollsRecapDto,
    val social: SocialRecapDto,
    val topPartners: List<PartnerRecapDto>,
    val globalLeaderboard: LeaderboardRecapDto,
    val groups: GroupsRecapDto,
    val map: MapRecapDto,
    val games: List<GameRecapEntryDto>,
    val betaTester: BetaTesterRecapDto = BetaTesterRecapDto(),
    val passwordResets: PasswordResetRecapDto = PasswordResetRecapDto(),
)

@Serializable
data class AccountRecapDto(
    val userId: String,
    val username: String,
    val memberSince: Long,
    val accountAgeDays: Int,
    val emailVerified: Boolean,
    val loginCountThisYear: Long, //Note: These are actual logins with username and password, this doesnt tell how often the app was opened
    val loginCountAllTime: Long,
    val friendRequestsSentThisYear: Long,
    val friendRequestsSentAllTime: Long,
)

@Serializable
data class MessagingRecapDto(
    val messagesSentThisYear: Long,
    val messagesReceivedThisYear: Long,
    val messagesSentAllTime: Long,
    val messagesReceivedAllTime: Long,
    val sentByType: Map<String, Long> = emptyMap(),
    val totalCharactersTypedThisYear: Long,
    val totalWordsTypedThisYear: Long,
    val averageMessageLength: Double,
    val longestMessage: LongestMessageRecapDto? = null,
    val busiestDay: DayCountDto? = null,
    val busiestHourOfDay: Int? = null,
    val mostActiveMonth: MonthCountDto? = null,
    val perMonth: List<MonthCountDto> = emptyList(),
    val longestStreakDays: Int,
    val firstMessageEverAt: Long? = null,
)

@Serializable
data class LongestMessageRecapDto(
    val content: String,
    val length: Int,
    val sentAt: Long,
    val toId: String,
    val toName: String,
    val group: Boolean,
)

@Serializable
data class DayCountDto(val date: String, val count: Long)

@Serializable
data class MonthCountDto(val month: Int, val count: Long)

@Serializable
data class ReactionsRecapDto(
    val reactionsGivenThisYear: Long,
    val reactionsReceivedThisYear: Long,
    val reactionsGivenAllTime: Long,
    val reactionsReceivedAllTime: Long,
    val topEmojiGiven: List<EmojiCountDto> = emptyList(),
    val topEmojiReceived: List<EmojiCountDto> = emptyList(),
    val mostReactedMessage: MostReactedMessageDto? = null,
)

@Serializable
data class EmojiCountDto(val emoji: String, val count: Long)

@Serializable
data class MostReactedMessageDto(val content: String, val reactionCount: Int, val sentAt: Long)

@Serializable
data class PollsRecapDto(
    val pollsCreated: Long,
    val pollVotesCast: Long,
)

@Serializable
data class SocialRecapDto(
    val friendsCount: Int,
    val pendingRequestsReceived: Int,
    val pendingRequestsSent: Int,
    val newFriendsThisYear: Int,
)

@Serializable
data class PartnerRecapDto(
    val id: String,
    val name: String,
    val group: Boolean,
    val messagesExchanged: Long,
    val messagesFromMe: Long,
    val messagesFromThem: Long,
)

@Serializable
data class LeaderboardRowDto(
    val rank: Int,
    val userId: String,
    val username: String,
    val messageCount: Long,
)

@Serializable
data class LeaderboardRecapDto(
    val top: List<LeaderboardRowDto> = emptyList(),
    val myRank: Int? = null,
    val myMessageCount: Long,
)

@Serializable
data class GroupActivityDto(
    val groupId: String,
    val groupName: String,
    val messageCount: Long,
)

@Serializable
data class GroupsRecapDto(
    val memberOfCount: Int,
    val createdCount: Int,
    val mostActiveGroup: GroupActivityDto? = null,
)

@Serializable
data class MapRecapDto(
    val entriesCreatedThisYear: Long,
    val entriesCreatedAllTime: Long,
    val entriesEditedThisYear: Long,
    val entriesEditedAllTime: Long,
)

@Serializable
data class GameRecapEntryDto(
    val game: String,
    val difficulty: String,
    val bestScore: Long,
    val bestTimeMillis: Long,
    val rank: Int,
    val achievedAt: Long,
)

@Serializable
data class BetaTesterRowDto(
    val rank: Int,
    val userId: String,
    val username: String,
    val exceptionCount: Long,
)

@Serializable
data class BetaTesterRecapDto(
    val all: List<BetaTesterRowDto> = emptyList(),
    val myRank: Int? = null,
    val myExceptionCount: Long = 0,
)

@Serializable
data class PasswordResetRecapDto(
    val passwordResetEmailsSentThisYear: Long = 0,
    val passwordResetEmailsSentAllTime: Long = 0,
)

// ─── UI models (formatted, enriched with local data) ──────────────────────

data class RecapUi(
    val year: Int,
    val username: String,
    val memberSinceFormatted: String,
    val accountAgeDays: Int,
    val loginCountThisYear: Long,
    val friendRequestsSentThisYear: Long,

    val messagesSent: Long,
    val messagesReceived: Long,
    val messagesSentAllTime: Long,
    val messagesReceivedAllTime: Long,
    val sentByType: List<MessageTypeCountUi>,
    val charactersTyped: Long,
    val wordsTyped: Long,
    val averageMessageLength: Int,
    val longestMessage: LongestMessageUi?,
    val busiestDayFormatted: String?,
    val busiestDayCount: Long,
    val busiestHourOfDay: Int?,
    val mostActiveMonth: MonthCountUi?,
    val perMonth: List<MonthCountUi>,
    val longestStreakDays: Int,
    val firstMessageEverFormatted: String?,

    val reactionsGiven: Long,
    val reactionsReceived: Long,
    val topEmojiGiven: List<EmojiCountUi>,
    val topEmojiReceived: List<EmojiCountUi>,
    val mostReactedMessage: MostReactedMessageUi?,

    val pollsCreated: Long,
    val pollVotesCast: Long,

    val friendsCount: Int,
    val newFriendsThisYear: Int,

    val topPartners: List<RecapPartnerUi>,

    val leaderboardTop: List<LeaderboardRowUi>,
    val myRank: Int?,
    val myLeaderboardMessageCount: Long,

    val groupsMemberOf: Int,
    val groupsCreated: Int,
    val mostActiveGroup: GroupActivityUi?,

    val mapEntriesCreated: Long,
    val mapEntriesEdited: Long,
    val mapEntriesCreatedAllTime: Long,

    val games: List<GameRecapUi>,

    val betaTesterRows: List<BetaTesterRowUi>,
    val myBetaTesterRank: Int?,
    val myExceptionCount: Long,

    val passwordResetEmailsSentThisYear: Long,
    val passwordResetEmailsSentAllTime: Long,
)

data class MessageTypeCountUi(
    val label: UiText,
    val count: Long,
)

data class MonthCountUi(
    val month: Int,
    val monthName: UiText,
    val count: Long,
)

data class EmojiCountUi(
    val emoji: String,
    val count: Long,
)

data class LongestMessageUi(
    val preview: String,
    val length: Int,
    val toName: String,
)

data class MostReactedMessageUi(
    val preview: String,
    val reactionCount: Int,
)

data class RecapPartnerUi(
    val name: String,
    val group: Boolean,
    val messagesExchanged: Long,
    val messagesFromMe: Long,
    val messagesFromThem: Long,
    val profilePictureFilePath: String,
)

data class LeaderboardRowUi(
    val rank: Int,
    val username: String,
    val messageCount: Long,
    val isMe: Boolean,
)

data class GroupActivityUi(
    val name: String,
    val messageCount: Long,
)

data class GameRecapUi(
    val gameName: String,
    val difficulty: String,
    val bestScore: Long,
    val rank: Int,
)

data class BetaTesterRowUi(
    val rank: Int,
    val username: String,
    val exceptionCount: Long,
    val isMe: Boolean,
)
