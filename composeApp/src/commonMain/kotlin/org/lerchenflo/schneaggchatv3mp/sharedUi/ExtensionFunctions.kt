package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun Modifier.clearFocusOnTap() : Modifier {
    val focusManager = LocalFocusManager.current
    // IOSKEYBOARDFIX: on iOS clearFocus() alone does not always resign the first responder,
    // so the keyboard stays up. Every dialog in this app already pairs it with hide().
    val keyboardController = LocalSoftwareKeyboardController.current
    return this.pointerInput(Unit) {
        detectTapGestures {
            keyboardController?.hide() // IOSKEYBOARDFIX
            focusManager.clearFocus()
        }
    }
}
