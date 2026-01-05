package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

data class SchneaggmapState(
    val nawd: Boolean = false
)

sealed interface SchneaggmapAction {
    data object OnBackClicked : SchneaggmapAction
}