package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

actual class AppIconManager(private val context: Context) {
    // Class names must use the manifest namespace (org.lerchenflo.androidApp), not
    // context.packageName: applicationId varies (debug adds a .debug suffix) and ComponentName's
    // (String, String) constructor does NOT resolve a leading-dot class name relative to the
    // package the way the manifest merger does, it fails with "Component class .DarkIcon does
    // not exist" instead. The package half of ComponentName is context.packageName, since that's
    // what PackageManager needs to identify the installed app.
    //
    // .DefaultIcon is the only alias declared enabled by default in the manifest, so it can only
    // ever be turned off via an explicit DISABLE, which always restarts the app (this happens at
    // most once, the first time the user ever leaves the default icon). .DefaultIconClone stands
    // in for it afterwards because its declared default is disabled, so resetting it to
    // COMPONENT_ENABLED_STATE_DEFAULT hides it without another restart.
    private val defaultAlias = ComponentName(context.packageName, "org.lerchenflo.androidApp.DefaultIcon")
    private val defaultCloneAlias = ComponentName(context.packageName, "org.lerchenflo.androidApp.DefaultIconClone")
    private val darkAlias = ComponentName(context.packageName, "org.lerchenflo.androidApp.DarkIcon")

    actual fun setIcon(icon: AppIcon) {
        val pm = context.packageManager
        val current = currentlyEnabledAlias(pm)

        if (iconFor(current) == icon) return

        val target = when (icon) {
            AppIcon.DEFAULT -> defaultCloneAlias
            AppIcon.DARK -> darkAlias
        }

        pm.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

        if (current == defaultAlias) {
            // One-time restart: this is the only alias that can't be hidden via COMPONENT_ENABLED_STATE_DEFAULT.
            pm.setComponentEnabledSetting(current, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        } else {
            pm.setComponentEnabledSetting(current, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
        }
    }

    private fun currentlyEnabledAlias(pm: PackageManager): ComponentName {
        if (pm.getComponentEnabledSetting(darkAlias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return darkAlias
        if (pm.getComponentEnabledSetting(defaultCloneAlias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return defaultCloneAlias
        return defaultAlias
    }

    private fun iconFor(alias: ComponentName): AppIcon = when (alias) {
        darkAlias -> AppIcon.DARK
        else -> AppIcon.DEFAULT
    }

    actual fun getCurrentIcon(): AppIcon = iconFor(currentlyEnabledAlias(context.packageManager))

    actual fun isSupported(): Boolean = true
}
