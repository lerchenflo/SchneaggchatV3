package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.database.Message
import org.lerchenflo.schneaggchatv3mp.database.MessageReader
import org.lerchenflo.schneaggchatv3mp.database.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.User

class UpsertMessageUseCase (
    private val repository: AppDatabaseRepository
) {
    suspend operator fun invoke(message: Message) {
        repository.upsertMessage(message)
    }

    suspend operator fun invoke(message: MessageWithReaders){
        repository.upsertMessageWithReaders(message)
    }

    /*
    suspend operator fun invoke(messages: List<Message>) {
        repository.upsertMessages(messages)
    }

     */

    suspend operator fun invoke(message: List<MessageWithReaders>){
        repository.upsertMessagesWithReaders(message)
    }


}