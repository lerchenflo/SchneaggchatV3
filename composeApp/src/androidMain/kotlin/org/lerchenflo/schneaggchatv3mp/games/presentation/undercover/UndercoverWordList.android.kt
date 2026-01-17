package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import android.content.Context
import java.util.Locale

/**
 * Gets the current system language code (e.g., "de", "en")
 */
actual fun getCurrentSystemLanguage(): String {
    // This function needs a Context to get the proper system locale
    // For now, we'll use the default locale, but this could be enhanced
    // to use the application context if needed
    return Locale.getDefault().language
}
