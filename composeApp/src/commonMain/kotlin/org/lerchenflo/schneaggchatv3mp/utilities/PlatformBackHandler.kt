package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.Composable

// für custom backpress logic
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)