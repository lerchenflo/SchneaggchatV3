package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.database.MessageWithReaders

class GetAllMessagesWithReadersUseCase(
    private val userRepository: AppDatabaseRepository
) {
    operator fun invoke(): Flow<List<MessageWithReaders>> {
        return userRepository.getAllMessagesWithReaders()
    }
}