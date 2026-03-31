package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// commonMain
object IncomingDataManager {
    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText: StateFlow<String?> = _sharedText


    fun updateText(text: String?) {
        _sharedText.value = text
        println("new intent text: $text")
    }

    fun isNewDataAvailable(): Boolean {
        return _sharedText.value?.isNotEmpty() ?: false
    }
}