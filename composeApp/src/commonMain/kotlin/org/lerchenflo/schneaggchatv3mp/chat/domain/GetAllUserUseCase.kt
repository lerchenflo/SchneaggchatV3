package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.User

class GetAllUserUseCase(
    private val userRepository: AppRepository
) {
    operator fun invoke(searchterm: String = ""): Flow<List<User>> {
        return userRepository.getallusers(searchterm)
    }
}