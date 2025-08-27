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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res

@Composable
fun NormalButton(
    text: String,
    onClick: () -> Unit,
    disabled: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
){
    Button(
        onClick = onClick,
        enabled = !disabled && !isLoading,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (disabled){
                MaterialTheme.colorScheme.onBackground
            }else{
                MaterialTheme.colorScheme.primary
            }
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