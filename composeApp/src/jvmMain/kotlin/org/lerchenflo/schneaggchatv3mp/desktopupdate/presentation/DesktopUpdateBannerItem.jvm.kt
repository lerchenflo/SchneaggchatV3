package org.lerchenflo.schneaggchatv3mp.desktopupdate.presentation

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.ChatSelectorDismissableInfo
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.beta_version_available
import schneaggchatv3mp.composeapp.generated.resources.beta_version_optional
import schneaggchatv3mp.composeapp.generated.resources.download
import schneaggchatv3mp.composeapp.generated.resources.download_from_github
import schneaggchatv3mp.composeapp.generated.resources.new_version_available

actual fun LazyListScope.desktopUpdateBannerItem() {
    item {
        DesktopUpdateBanner()
    }
}

@Composable
private fun DesktopUpdateBanner(viewModel: DesktopUpdateViewModel = koinViewModel()) {
    val update = viewModel.update.collectAsStateWithLifecycle().value ?: return
    val uriHandler = LocalUriHandler.current

    ChatSelectorDismissableInfo(
        title = stringResource(
            if (update.isBeta) Res.string.beta_version_available else Res.string.new_version_available,
            update.tagName,
        ),
        description = stringResource(
            if (update.isBeta) Res.string.beta_version_optional else Res.string.download_from_github
        ),
        onClick = {
            uriHandler.openUri(update.pageUrl)
        },
        onDismiss = viewModel::onDismiss,
        activateText = stringResource(Res.string.download),
    )
}
