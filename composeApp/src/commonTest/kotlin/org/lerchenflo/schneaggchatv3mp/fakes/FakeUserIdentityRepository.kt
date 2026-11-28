package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import kotlin.time.Duration

/** In-memory [UserIdentityRepository]. Serves [identities]; ids without an entry are unknown. */
class FakeUserIdentityRepository : UserIdentityRepository {

    val identities = MutableStateFlow<Map<String, UserIdentity>>(emptyMap())

    val prefetchCalls = CallLog<Collection<String>>()
    val cleanUpCalls = CallLog<Unit>()

    override fun observeUser(userId: String): Flow<UserIdentity> =
        identities.map { it[userId] ?: UserIdentity(userId, UserIdentityName.Unknown) }

    override suspend fun getUser(userId: String, maxWait: Duration): UserIdentity =
        identities.value[userId] ?: UserIdentity(userId, UserIdentityName.Unknown)

    override fun observeUsers(userIds: Collection<String>): Flow<Map<String, UserIdentity>> =
        identities.map { known -> userIds.associateWith { known[it] ?: UserIdentity(it, UserIdentityName.Unknown) } }

    override fun observeKnownNames(): Flow<Map<String, UserIdentityName>> =
        identities.map { known -> known.mapValues { (_, identity) -> identity.name } }

    override fun prefetch(userIds: Collection<String>) {
        prefetchCalls.record(userIds)
    }

    override suspend fun cleanUp() {
        cleanUpCalls.record(Unit)
    }
}
