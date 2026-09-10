package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils

/** [AuthSessionStore] on top of the encrypted preferences (KSafe). */
class PreferenceAuthSessionStore(
    private val preferencemanager: Preferencemanager,
) : AuthSessionStore {

    override suspend fun read(): TokenPair? =
        preferencemanager.getTokens().takeIf { it.refreshToken.isNotBlank() }

    override suspend fun write(tokens: TokenPair) {
        preferencemanager.saveTokens(tokens)
        preferencemanager.saveOWNID(JwtUtils.getUserIdFromToken(tokens.refreshToken))
    }

    override suspend fun clear() {
        preferencemanager.clearTokens()
    }
}
