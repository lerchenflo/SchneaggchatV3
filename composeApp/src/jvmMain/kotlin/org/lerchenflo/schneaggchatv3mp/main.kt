package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3mp",
    ) {
        App()
    }
}