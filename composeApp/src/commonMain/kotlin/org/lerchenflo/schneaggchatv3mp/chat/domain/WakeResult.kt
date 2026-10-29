package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * Why a wake did or did not reach anybody. Domain copy of the network enum
 * `NetworkWakeOutcome`, entry for entry. Waking a non-friend is not represented here:
 * the server rejects it with 403.
 */
enum class WakeOutcome {
    WOKEN,
    NOT_ALLOWED,
    NO_DEVICES,
}

/**
 * Server answer to [org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository.sendWake].
 * Domain copy of the network DTO `WakeResponse`, field for field.
 */
data class WakeResult(
    val outcome: WakeOutcome,
    val userCount: Int = 0,
    val deviceCount: Int = 0,
    val skippedCount: Int = 0,
)
