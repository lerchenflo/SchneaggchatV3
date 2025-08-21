package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.User

class GetAllUserUseCase(
    private val userRepository: AppDatabaseRepository
) {
    operator fun invoke(searchterm: String = ""): Flow<List<User>> {
        return userRepository.getallusers(searchterm)
    }
}