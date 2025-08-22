package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.database.AppRepository

class DeleteUserUseCase(
    private val userRepository: AppRepository
) {
    suspend operator fun invoke(userid: Long){
        userRepository.deleteUser(userid)
    }
}