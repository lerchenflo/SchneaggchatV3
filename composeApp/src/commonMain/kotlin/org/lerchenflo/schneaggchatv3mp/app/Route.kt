package org.lerchenflo.schneaggchatv3mp.app

import kotlinx.serialization.Serializable


sealed interface Route {

    // Chat - Feature
    @Serializable
    data object ChatGraph: Route
    @Serializable
    data object ChatSelector: Route
    @Serializable
    data object Chat: Route
    @Serializable
    data object NewChat: Route

    // Login feature
    @Serializable
    data object LoginGraph: Route

    @Serializable
    data object AutoLoginCredChecker: Route

    @Serializable
    data object Login: Route
    @Serializable
    data object SignUp: Route

    @Serializable
    data object Settings: Route

}