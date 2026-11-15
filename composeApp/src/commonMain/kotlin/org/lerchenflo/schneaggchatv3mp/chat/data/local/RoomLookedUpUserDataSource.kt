package org.lerchenflo.schneaggchatv3mp.chat.data.local

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.LookedUpUserEntity
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase

class RoomLookedUpUserDataSource(
    private val database: AppDatabase,
) : LookedUpUserLocalDataSource {

    override suspend fun upsertAll(users: List<LookedUpUserEntity>) {
        database.lookedUpUserDao().upsertAll(users)
    }

    override suspend fun getByIds(ids: List<String>): List<LookedUpUserEntity> =
        database.lookedUpUserDao().getByIds(ids)

    override fun getByIdsFlow(ids: List<String>): Flow<List<LookedUpUserEntity>> =
        database.lookedUpUserDao().getByIdsAsFlow(ids)

    override fun getAllFlow(): Flow<List<LookedUpUserEntity>> =
        database.lookedUpUserDao().getAllAsFlow()

    override suspend fun getFetchedBefore(fetchedBefore: Long): List<LookedUpUserEntity> =
        database.lookedUpUserDao().getFetchedBefore(fetchedBefore)

    override suspend fun getWithProfilePicture(): List<LookedUpUserEntity> =
        database.lookedUpUserDao().getWithProfilePicture()

    override suspend fun updateProfilePictureUrl(id: String, url: String, pictureUpdatedAt: Long): Boolean =
        database.lookedUpUserDao().updateProfilePictureUrl(id, url, pictureUpdatedAt) > 0

    override suspend fun clearProfilePictureUrls(ids: List<String>) {
        database.lookedUpUserDao().clearProfilePictureUrls(ids)
    }

    override suspend fun deleteByIds(ids: List<String>) {
        database.lookedUpUserDao().deleteByIds(ids)
    }
}
