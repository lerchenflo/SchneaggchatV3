package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res

@Composable
fun NormalButton(
    text: String,
    onClick: () -> Unit,
    disabled: Boolean = false,
    isLoading: Boolean = false,
    focusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    primary: Boolean = true,
    modifier: Modifier = Modifier
){
    var newModifier = modifier
    if (focusRequester != null) {
        newModifier = modifier.focusRequester(focusRequester)
        
        if (nextFocusRequester != null) {
            newModifier = newModifier.onPreviewKeyEvent { event ->
                // Detect TAB key press
                if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                    nextFocusRequester.requestFocus()
                    true // we handled it
                } else false
            }
        }
    }
    Button(
        onClick = onClick,
        enabled = !disabled && !isLoading,
        modifier = newModifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (disabled){
                MaterialTheme.colorScheme.onBackground
            }else {
                if (!primary) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
            },
            contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(12.dp)
    ){
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(text)
        }
    }
}