package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.go_back

@Composable
fun BackButton(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    // Previews and render tests run without Koin, so the platform is only asked for at runtime
    val isIOS = if (LocalInspectionMode.current) false else koinInject<AppVersion>().isIOS()

    IconButton(
        onClick = onBackClick,
        modifier = modifier
            .padding(top = 5.dp, start = 5.dp),
        colors = IconButtonDefaults.iconButtonColors().copy(
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Icon(
            imageVector = if (isIOS) Icons.AutoMirrored.Filled.ArrowBackIos else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.go_back),
        )
    }
}