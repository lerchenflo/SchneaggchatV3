package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.User

class UpsertUserUseCase(
    private val userRepository: AppRepository
) {
    suspend operator fun invoke(user: User) {
        userRepository.upsertUser(user)
    }
}