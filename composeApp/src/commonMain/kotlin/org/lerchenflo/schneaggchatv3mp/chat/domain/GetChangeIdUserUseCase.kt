package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.database.tables.User

class GetChangeIdUserUseCase(
    private val userRepository: AppDatabaseRepository
) {
    suspend operator fun invoke(): List<IdChangeDate> {
        return withContext(Dispatchers.IO){
            userRepository.getuserchangeid()
        }
    }
}