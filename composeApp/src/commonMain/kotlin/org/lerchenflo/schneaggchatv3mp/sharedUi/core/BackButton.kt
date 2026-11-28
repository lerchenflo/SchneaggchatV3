package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.utilities.device.AppVersion
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.go_back

@Composable
fun BackButton(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Offset for free-standing back buttons; the ActivityTitle bar places the button itself
    contentPadding: PaddingValues = PaddingValues(top = 5.dp, start = 5.dp)
) {

    // Previews and render tests run without Koin, so the platform is only asked for at runtime
    val isIOS = if (LocalInspectionMode.current) false else koinInject<AppVersion>().isIOS()

    IconButton(
        onClick = onBackClick,
        modifier = modifier
            .padding(contentPadding),
        // No own background: the button always shows the background of whatever is behind it (user, 2026-11-18)
        colors = IconButtonDefaults.iconButtonColors().copy(
            containerColor = Color.Transparent
        )
    ) {
        Icon(
            imageVector = if (isIOS) Icons.AutoMirrored.Filled.ArrowBackIos else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.go_back),
        )
    }
}