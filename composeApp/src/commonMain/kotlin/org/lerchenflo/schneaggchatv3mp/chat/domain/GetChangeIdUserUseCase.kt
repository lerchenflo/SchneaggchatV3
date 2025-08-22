package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate

class GetChangeIdUserUseCase(
    private val userRepository: AppRepository
) {
    suspend operator fun invoke(): List<IdChangeDate> {
        return withContext(Dispatchers.IO){
            userRepository.getuserchangeid()
        }
    }
}