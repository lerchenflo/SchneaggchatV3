package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// commonMain
object IncomingDataManager {
    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText: StateFlow<String?> = _sharedText

    private val _sharedImages = MutableStateFlow<List<ByteArray>?>(null)
    val sharedImages: StateFlow<List<ByteArray>?> = _sharedImages

    fun updateText(text: String?) {
        _sharedText.value = text
        println("new intent text: $text")
    }

    fun updateImages(images: List<ByteArray>?) {
        _sharedImages.value = images
        println("new intent images: ${images?.size} images")
    }

    fun isNewDataAvailable(): Boolean {
        return (_sharedText.value?.isNotEmpty() == true) || isNewImageDataAvailable()
    }

    fun isNewImageDataAvailable(): Boolean {
        return _sharedImages.value?.isNotEmpty() == true
    }

    fun clearAllData() {
        _sharedText.value = null
        _sharedImages.value = null
    }
}