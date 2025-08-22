package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.Message
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders

class UpsertMessageUseCase (
    private val repository: AppRepository
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