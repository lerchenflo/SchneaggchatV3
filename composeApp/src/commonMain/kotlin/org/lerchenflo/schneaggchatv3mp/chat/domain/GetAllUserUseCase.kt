package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.User
import org.lerchenflo.schneaggchatv3mp.database.UserDatabaseRepository

class GetAllUserUseCase(
    private val userRepository: UserDatabaseRepository
) {
    operator fun invoke(searchterm: String = ""): Flow<List<User>> {
        return userRepository.getallusers(searchterm)
    }
}