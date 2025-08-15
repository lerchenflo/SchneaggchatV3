package org.lerchenflo.schneaggchatv3mp.chatauswahl.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.User
import org.lerchenflo.schneaggchatv3mp.database.UserDatabaseRepository

class GetAllUserUseCase(
    private val userRepository: UserDatabaseRepository
) {
    operator fun invoke(): Flow<List<User>> {
        return userRepository.getallusers()
    }
}