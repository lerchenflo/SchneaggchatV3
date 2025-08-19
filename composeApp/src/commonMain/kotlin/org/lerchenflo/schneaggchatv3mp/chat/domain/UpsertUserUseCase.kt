package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.database.User

class UpsertUserUseCase(
    private val userRepository: AppDatabaseRepository
) {
    suspend operator fun invoke(user: User){
        userRepository.upsertUser(user)
    }
}