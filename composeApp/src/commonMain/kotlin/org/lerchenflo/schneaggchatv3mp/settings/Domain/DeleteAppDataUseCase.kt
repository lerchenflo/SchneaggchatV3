package org.lerchenflo.schneaggchatv3mp.settings.Domain

import org.lerchenflo.schneaggchatv3mp.database.AppRepository

class DeleteAppDataUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(){
        appRepository.deleteAllAppData()
    }
}