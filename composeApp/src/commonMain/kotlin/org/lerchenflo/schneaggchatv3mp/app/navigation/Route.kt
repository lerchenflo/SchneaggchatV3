package org.lerchenflo.schneaggchatv3mp.app.navigation

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
    @Serializable
    data object GroupCreator: Route
    @Serializable
    data object AutoLoginCredChecker: Route

    @Serializable
    data object Login: Route
    @Serializable
    data object SignUp: Route

    @Serializable
    data object Settings: Route

    @Serializable
    data object ChatDetails: Route

    @Serializable
    data object Todolist: Route

    @Serializable
    data object UnderConstruction: Route

    @Serializable
    data object DeveloperSettings: Route

}