package org.lerchenflo.schneaggchatv3mp.chat.data.local

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.LookedUpUserEntity

/** Local (Room) storage for users who are not in the users table but were looked up by id. */
interface LookedUpUserLocalDataSource {

    suspend fun upsertAll(users: List<LookedUpUserEntity>)

    suspend fun getByIds(ids: List<String>): List<LookedUpUserEntity>

    fun getByIdsFlow(ids: List<String>): Flow<List<LookedUpUserEntity>>

    fun getAllFlow(): Flow<List<LookedUpUserEntity>>

    /** Rows whose last lookup is older than [fetchedBefore] (epoch millis). */
    suspend fun getFetchedBefore(fetchedBefore: Long): List<LookedUpUserEntity>

    /** Rows that point to a stored picture file. */
    suspend fun getWithProfilePicture(): List<LookedUpUserEntity>

    /**
     * Stores the file of a loaded picture, but only while the row still names the picture version
     * [pictureUpdatedAt] it was loaded for. @return false when the row changed meanwhile
     */
    suspend fun updateProfilePictureUrl(id: String, url: String, pictureUpdatedAt: Long): Boolean

    suspend fun clearProfilePictureUrls(ids: List<String>)

    suspend fun deleteByIds(ids: List<String>)
}
