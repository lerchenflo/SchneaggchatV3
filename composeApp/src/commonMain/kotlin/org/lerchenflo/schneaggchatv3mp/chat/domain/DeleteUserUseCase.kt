package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.User

class DeleteUserUseCase(
    private val userRepository: AppDatabaseRepository
) {
    suspend operator fun invoke(userid: Long){
        userRepository.deleteUser(userid)
    }
}