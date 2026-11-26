package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.appearance_settings
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices
import schneaggchatv3mp.composeapp.generated.resources.misc_settings
import schneaggchatv3mp.composeapp.generated.resources.notifications
import schneaggchatv3mp.composeapp.generated.resources.privacy_and_security
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_settings
import schneaggchatv3mp.composeapp.generated.resources.user_settings

/**
 * Settings pages a `@settings/<key>` link can open. [key] is stored in message texts, so it must
 * never change. The developer settings stay out: a link must not open them without unlocking.
 */
internal enum class SettingsAnnotationPage(val key: String, val title: StringResource, val route: Route) {
    ACCOUNT("account", Res.string.user_settings, Route.UserSettings),
    PRIVACY("privacy", Res.string.privacy_and_security, Route.PrivacyAndSecuritySettings),
    DEVICES("devices", Res.string.logged_in_devices, Route.LoggedInDevices),
    NOTIFICATIONS("notifications", Res.string.notifications, Route.NotificationSettings),
    APPEARANCE("appearance", Res.string.appearance_settings, Route.AppearanceSettings),
    MAP("map", Res.string.schneaggmap_settings, Route.SchneaggmapSettings),
    MISC("misc", Res.string.misc_settings, Route.MiscSettings);

    companion object {
        fun of(key: String): SettingsAnnotationPage? = entries.firstOrNull { it.key == key }
    }
}
