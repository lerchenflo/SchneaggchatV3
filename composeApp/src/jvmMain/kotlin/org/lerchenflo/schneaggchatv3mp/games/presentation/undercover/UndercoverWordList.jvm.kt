package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import java.util.Locale

/**
 * Gets the current system language code (e.g., "de", "en")
 */
actual fun getCurrentSystemLanguage(): String {
    return Locale.getDefault().language
}
