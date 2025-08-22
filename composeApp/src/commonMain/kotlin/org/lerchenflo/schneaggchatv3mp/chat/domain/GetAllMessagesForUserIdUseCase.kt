package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders

class GetAllMessagesForUserIdUseCase(
    private val userRepository: AppRepository
) {
    operator fun invoke(userid: Long): Flow<List<MessageWithReaders>> {
        return userRepository.getMessagesByUserId(userid)
    }
}