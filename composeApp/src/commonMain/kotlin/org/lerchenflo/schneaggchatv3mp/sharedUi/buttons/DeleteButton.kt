package org.lerchenflo.schneaggchatv3mp.sharedUi.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeleteButton(
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
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFDC3545), // Red color for destructive action
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color(0xFFDC3545).copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ){
        if (isLoading) {
            CircularProgressIndicator(
                color = Color(0xFFDC3545),
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun Miscbuttonpreview() {
    DeleteButton(
        text = "Delete Account",
        onClick = {},
        modifier = Modifier.padding(vertical = 60.dp)
    )
}