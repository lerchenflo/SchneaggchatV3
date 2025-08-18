package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.database.User
import org.lerchenflo.schneaggchatv3mp.database.UserDatabaseRepository

class UpsertUserUseCase(
    private val userRepository: UserDatabaseRepository
) {
    suspend operator fun invoke(user: User){
        userRepository.upsertUser(user)
    }
}