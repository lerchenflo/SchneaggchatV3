package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.errorCodeToMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.onError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.onSuccess
import org.lerchenflo.schneaggchatv3mp.games.domain.EmojiCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.GameRecapUi
import org.lerchenflo.schneaggchatv3mp.games.domain.GroupActivityUi
import org.lerchenflo.schneaggchatv3mp.games.domain.LeaderboardRowUi
import org.lerchenflo.schneaggchatv3mp.games.domain.LongestMessageUi
import org.lerchenflo.schneaggchatv3mp.games.domain.MessageTypeCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.MonthCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.MostReactedMessageUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapPartnerUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapResponse
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.iso8601DateFormatter
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.month_april
import schneaggchatv3mp.composeapp.generated.resources.month_august
import schneaggchatv3mp.composeapp.generated.resources.month_december
import schneaggchatv3mp.composeapp.generated.resources.month_february
import schneaggchatv3mp.composeapp.generated.resources.month_january
import schneaggchatv3mp.composeapp.generated.resources.month_july
import schneaggchatv3mp.composeapp.generated.resources.month_june
import schneaggchatv3mp.composeapp.generated.resources.month_march
import schneaggchatv3mp.composeapp.generated.resources.month_may
import schneaggchatv3mp.composeapp.generated.resources.month_november
import schneaggchatv3mp.composeapp.generated.resources.month_october
import schneaggchatv3mp.composeapp.generated.resources.month_september
import schneaggchatv3mp.composeapp.generated.resources.recap_load_failed
import schneaggchatv3mp.composeapp.generated.resources.recap_type_audio
import schneaggchatv3mp.composeapp.generated.resources.recap_type_image
import schneaggchatv3mp.composeapp.generated.resources.recap_type_poll
import schneaggchatv3mp.composeapp.generated.resources.recap_type_text
import kotlin.math.roundToInt

data class RecapState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val recap: RecapUi? = null,
)

sealed interface RecapAction {
    data object OnRetryClick : RecapAction
}

class RecapViewModel(
    private val networkUtils: NetworkUtils,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
): ViewModel() {

    private val _state = MutableStateFlow(RecapState())
    val state = _state.asStateFlow()

    init {
        loadRecap()
    }

    fun onAction(action: RecapAction) {
        when (action) {
            RecapAction.OnRetryClick -> loadRecap()
        }
    }

    private fun loadRecap() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            networkUtils.getRecap()
                .onSuccess { response ->
                    _state.update { it.copy(isLoading = false, recap = response.toRecapUi()) }
                }
                .onError { error ->
                    val message = error.message ?: errorCodeToMessage(error.errorCode)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (message.isNotBlank()) UiText.DynamicString(message)
                            else UiText.StringResourceText(Res.string.recap_load_failed)
                        )
                    }
                }
        }
    }

    private suspend fun RecapResponse.toRecapUi(): RecapUi {
        val partners = topPartners.map { partner ->
            val profilePicture = if (partner.group) {
                groupRepository.getGroupById(partner.id)?.profilePictureUrl
            } else {
                userRepository.getUserById(partner.id)?.profilePictureUrl
            }
            RecapPartnerUi(
                name = partner.name,
                group = partner.group,
                messagesExchanged = partner.messagesExchanged,
                messagesFromMe = partner.messagesFromMe,
                messagesFromThem = partner.messagesFromThem,
                profilePictureFilePath = profilePicture ?: ""
            )
        }

        val monthCounts = (1..12).map { month ->
            MonthCountUi(
                month = month,
                monthName = monthName(month),
                count = messaging.perMonth.find { it.month == month }?.count ?: 0L
            )
        }

        return RecapUi(
            year = year,
            username = account.username,
            memberSinceFormatted = millisToString(account.memberSince, "dd.MM.yyyy"),
            accountAgeDays = account.accountAgeDays,
            loginCountThisYear = account.loginCountThisYear,
            friendRequestsSentThisYear = account.friendRequestsSentThisYear,

            messagesSent = messaging.messagesSentThisYear,
            messagesReceived = messaging.messagesReceivedThisYear,
            messagesSentAllTime = messaging.messagesSentAllTime,
            messagesReceivedAllTime = messaging.messagesReceivedAllTime,
            sentByType = messaging.sentByType.entries
                .sortedByDescending { it.value }
                .mapNotNull { (type, count) ->
                    messageTypeLabel(type)?.let { MessageTypeCountUi(it, count) }
                },
            charactersTyped = messaging.totalCharactersTypedThisYear,
            wordsTyped = messaging.totalWordsTypedThisYear,
            averageMessageLength = messaging.averageMessageLength.roundToInt(),
            longestMessage = messaging.longestMessage?.let {
                LongestMessageUi(
                    preview = it.content.take(120),
                    length = it.length,
                    toName = it.toName
                )
            },
            // Server sends ISO yyyy-MM-dd; fall back to the raw value if it ever fails to parse
            busiestDayFormatted = messaging.busiestDay?.date?.let { iso ->
                iso8601DateFormatter(iso).ifEmpty { iso }
            },
            busiestDayCount = messaging.busiestDay?.count ?: 0L,
            busiestHourOfDay = messaging.busiestHourOfDay,
            mostActiveMonth = messaging.mostActiveMonth?.let { peak ->
                MonthCountUi(peak.month, monthName(peak.month), peak.count)
            },
            perMonth = monthCounts,
            longestStreakDays = messaging.longestStreakDays,
            firstMessageEverFormatted = messaging.firstMessageEverAt?.let {
                millisToString(it, "dd.MM.yyyy")
            },

            reactionsGiven = reactions.reactionsGivenThisYear,
            reactionsReceived = reactions.reactionsReceivedThisYear,
            topEmojiGiven = reactions.topEmojiGiven.take(5).map { EmojiCountUi(it.emoji, it.count) },
            topEmojiReceived = reactions.topEmojiReceived.take(5).map { EmojiCountUi(it.emoji, it.count) },
            mostReactedMessage = reactions.mostReactedMessage?.let {
                MostReactedMessageUi(preview = it.content.take(120), reactionCount = it.reactionCount)
            },

            pollsCreated = polls.pollsCreated,
            pollVotesCast = polls.pollVotesCast,

            friendsCount = social.friendsCount,
            newFriendsThisYear = social.newFriendsThisYear,

            topPartners = partners,

            leaderboardTop = globalLeaderboard.top.take(5).map {
                LeaderboardRowUi(
                    rank = it.rank,
                    username = it.username,
                    messageCount = it.messageCount,
                    isMe = it.userId == account.userId
                )
            },
            myRank = globalLeaderboard.myRank,
            myLeaderboardMessageCount = globalLeaderboard.myMessageCount,

            groupsMemberOf = groups.memberOfCount,
            groupsCreated = groups.createdCount,
            mostActiveGroup = groups.mostActiveGroup?.let {
                GroupActivityUi(name = it.groupName, messageCount = it.messageCount)
            },

            mapEntriesCreated = map.entriesCreatedThisYear,
            mapEntriesEdited = map.entriesEditedThisYear,
            mapEntriesCreatedAllTime = map.entriesCreatedAllTime,

            games = games
                .sortedBy { it.rank }
                .map {
                    GameRecapUi(
                        gameName = it.game.lowercase().replaceFirstChar { c -> c.uppercase() },
                        difficulty = it.difficulty,
                        bestScore = it.bestScore,
                        rank = it.rank
                    )
                },
        )
    }

    private fun messageTypeLabel(type: String): UiText? = when (type) {
        "TEXT" -> UiText.StringResourceText(Res.string.recap_type_text)
        "IMAGE" -> UiText.StringResourceText(Res.string.recap_type_image)
        "AUDIO" -> UiText.StringResourceText(Res.string.recap_type_audio)
        "POLL" -> UiText.StringResourceText(Res.string.recap_type_poll)
        else -> null
    }

    private fun monthName(month: Int): UiText {
        val resource = when (month) {
            1 -> Res.string.month_january
            2 -> Res.string.month_february
            3 -> Res.string.month_march
            4 -> Res.string.month_april
            5 -> Res.string.month_may
            6 -> Res.string.month_june
            7 -> Res.string.month_july
            8 -> Res.string.month_august
            9 -> Res.string.month_september
            10 -> Res.string.month_october
            11 -> Res.string.month_november
            else -> Res.string.month_december
        }
        return UiText.StringResourceText(resource)
    }
}
