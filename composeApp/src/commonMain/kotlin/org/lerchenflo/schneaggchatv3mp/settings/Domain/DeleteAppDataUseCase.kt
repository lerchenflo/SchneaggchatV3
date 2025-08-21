package org.lerchenflo.schneaggchatv3mp.settings.Domain

import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository

class DeleteAppDataUseCase(
    private val appDatabaseRepository: AppDatabaseRepository
) {
    suspend operator fun invoke(){
        appDatabaseRepository.deleteAllAppData()
    }
}