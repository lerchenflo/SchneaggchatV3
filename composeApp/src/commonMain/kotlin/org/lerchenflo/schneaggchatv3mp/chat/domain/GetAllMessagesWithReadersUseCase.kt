package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders

class GetAllMessagesWithReadersUseCase(
    private val userRepository: AppRepository
) {
    operator fun invoke(): Flow<List<MessageWithReaders>> {
        return userRepository.getAllMessagesWithReaders()
    }
}