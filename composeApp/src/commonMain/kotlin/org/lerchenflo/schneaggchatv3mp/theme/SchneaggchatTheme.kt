package org.lerchenflo.schneaggchatv3mp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val DarkColorTheme = darkColorScheme(
    primary = Primary,
    onPrimary = onPrimary,
    background = BackgroundDark,
)

val LightColorTheme = lightColorScheme(
    primary = PrimaryPhil,
    surface = SurfacePhil,
    surfaceContainerLowest = SurfaceLowestPhil,
    background = BackgroundPhil,
    onSurface = OnSurfacePhil,
    onSurfaceVariant = OnSurfaceVariantPhil
)


val Shapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(15.dp)
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
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
