package org.lerchenflo.schneaggchatv3mp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val DarkColorTheme = darkColorScheme(
    primary = Primary,
    onPrimary = onPrimary,
    background = BackgroundDark
)

val LightColorTheme = lightColorScheme(
    background = BackgroundLight
)

@Composable
fun SchneaggchatTheme(
    content: @Composable () -> Unit
){
    val theme = if(isSystemInDarkTheme()){
        DarkColorTheme
    }else{
        LightColorTheme
    }

    MaterialTheme(
        colorScheme = theme,
        content = content
    )
}