package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/**
 * The one path from a user id to the shown name and picture: the users table first (the own
 * nickname for them, else their name), otherwise the looked-up users, otherwise a lookup on the
 * server. Looked-up users are fetched again after a day and deleted five days after their last
 * fetch.
 */
interface UserIdentityRepository {

    /**
     * Name and picture of [userId]. Emits again when the user's stored row changes. A user who is
     * missing or was looked up more than a day ago is looked up in the background; a visible
     * picture that is not loaded yet is downloaded.
     */
    fun observeUser(userId: String): Flow<UserIdentity>

    /**
     * Name and picture of [userId] right now, for one-off texts such as notifications. A user who is
     * neither stored nor looked up within the last day is looked up first, but the call waits at
     * most [maxWait] for it. After that, or when the lookup fails, it returns the stored row even
     * if outdated (an old name beats "Unknown user"), and the lookup finishes in the background.
     */
    suspend fun getUser(userId: String, maxWait: Duration): UserIdentity

    /** [observeUser] for many ids at once, keyed by id; the missing ones go out as one batched lookup. */
    fun observeUsers(userIds: Collection<String>): Flow<Map<String, UserIdentity>>

    /**
     * Names of every user this device knows (users table first, then the looked-up users), for
     * texts that may name anyone, such as mentions. Looks nobody up.
     */
    fun observeKnownNames(): Flow<Map<String, UserIdentityName>>

    /**
     * Looks up the ids that are neither in the users table nor looked up within the last day, in
     * the background (for example all members after a group sync).
     */
    fun prefetch(userIds: Collection<String>)

    /**
     * Deletes looked-up users (and their pictures) whose last lookup is older than five days, and
     * forgets picture files that are gone from the device so they load again.
     */
    suspend fun cleanUp()
}
