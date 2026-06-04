package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapData
import org.lerchenflo.schneaggchatv3mp.games.domain.TopContact
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong
import org.lerchenflo.schneaggchatv3mp.utilities.getStartOfYearMillis
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

class RecapViewModel(
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
): ViewModel() {

    private val ownId = SessionCache.requireLoggedIn()?.userId

    fun getstats(): Flow<RecapData> {
        //val currentYear = 1780575702995L
        val currentYear = getCurrentYear()

        return messageRepository.getAllMessages().map { allMessages ->
            // 1. Single filter pass for the targeted year
            val yearlyMessages = allMessages.filter { message ->
                val messageTime = message.sendDate.toLongOrNull() ?: 0L
                messageTime > currentYear
            }

            // Compute metrics in memory without hitting the DB again
            val sentCount = yearlyMessages.count { it.senderId == ownId }
            val receivedCount = yearlyMessages.count { it.senderId != ownId }

            // Find top contacts
            val topContactsList = yearlyMessages
                .groupBy { message ->
                    if (message.isGroupMessage()) {
                        // For group messages, the receiverId is typically the Group ID
                        message.receiverId
                    } else {
                        // For DMs, pick the ID that belongs to the other person (not you)
                        if (message.senderId == ownId) message.receiverId else message.senderId
                    }
                }
                // Safeguard: Drop any groups where the key accidentally matches ownId
                .filterKeys { chatId -> chatId != ownId }
                .map { (chatId, messageList) ->
                    // Since groupBy guarantees at least one item, .first() is safe
                    val isGroup = messageList.first().isGroupMessage()

                    val selectedChat = if (isGroup) {
                        groupRepository.getGroupById(chatId)?.toSelectedChat(0, 0, null)
                    } else {
                        userRepository.getUserById(chatId)?.toSelectedChat(0, 0, null)
                    }

                    TopContact(
                        selectedChat = selectedChat,
                        msgCount = messageList.size
                    )
                }
                .sortedByDescending { it.msgCount }
                .take(5) // Limit to top 5 contacts for clean presentation

            RecapData(
                year = currentYear,
                totalMessagesSent = sentCount,
                totalMessagesReceived = receivedCount,
                topContacts = topContactsList
            )
        }
    }

    private fun getCurrentYear(): Int{
        val millis = getCurrentTimeMillisLong()
        return millisToString(
            millis = millis,
            format = "yyyy"
        ).toInt()
    }

    private fun getTotalMessagesAfter(startTimeMillis: Long): Flow<Int> {
        return messageRepository.getAllMessages()
            .map { list ->
                list.count { message ->
                    // Safely convert the string millis to Long, default to 0L if it fails
                    val messageTime = message.sendDate.toLongOrNull() ?: 0L
                    val senderId = message.senderId
                    messageTime > startTimeMillis && senderId == ownId
                }
            }
    }
    
    

}