package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Shared Enter-key handling for the chat input field: Shift+Enter inserts a newline at the
 * cursor, plain Enter sends (or confirms an edit). Was previously copy-pasted once for the plain
 * text field and once for the field under an image attachment - the image-branch copy discarded
 * the attached images on Shift+Enter (it rebuilt a bare TextContent instead of writing the
 * newline back into its own content type). [onInsertNewline] fixes that: each caller writes the
 * new [TextFieldValue] back into whatever content type it owns.
 */
fun Modifier.sendOnEnter(
    currentText: TextFieldValue,
    onInsertNewline: (TextFieldValue) -> Unit,
    isEditing: Boolean,
    onSend: () -> Unit,
    onConfirmEdit: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
        if (event.isShiftPressed) {
            val text = currentText.text
            val cursorPos = currentText.selection.start
            val newText = text.substring(0, cursorPos) + "\n" + text.substring(cursorPos)
            onInsertNewline(
                TextFieldValue(
                    text = newText,
                    selection = TextRange(cursorPos + 1)
                )
            )
            true // consume to prevent double newline
        } else {
            if (isEditing) onConfirmEdit() else onSend()
            true
        }
    } else {
        false
    }
}
