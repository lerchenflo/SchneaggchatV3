package org.lerchenflo.schneaggchatv3mp.app

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.database.User


sealed interface Route {

    @Serializable
    data object ChatGraph: Route

    @Serializable
    data object ChatSelector: Route

    @Serializable
    data object Chat: Route
    @Serializable
    data object newChat: Route

}