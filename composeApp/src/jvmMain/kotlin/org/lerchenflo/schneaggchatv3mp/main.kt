package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.lerchenflo.schneaggchatv3mp.app.App
import java.awt.Dimension

fun main() = application {

    onAppStart()


    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3 Desktop",

    ) {
        window.setSize(1600, 1000)
        window.minimumSize = Dimension(400, 400)
        App()
    }
}