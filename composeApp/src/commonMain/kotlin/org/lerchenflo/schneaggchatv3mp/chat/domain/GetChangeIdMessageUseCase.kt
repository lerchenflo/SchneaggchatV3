package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate

class GetChangeIdMessageUseCase(
    private val repository: AppRepository
) {
    suspend operator fun invoke(): List<IdChangeDate> {
        return withContext(Dispatchers.IO){
            repository.getmessagechangeid()
        }
    }
}