package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import platform.Foundation.NSLocale

/**
 * Gets the current system language code (e.g., "de", "en")
 */
actual fun getCurrentSystemLanguage(): String {
    val currentLocale = NSLocale.currentLocale
    return currentLocale.languageCode ?: "en"
}
