package org.lerchenflo.schneaggchatv3mp.desktopupdate.presentation

import androidx.compose.foundation.lazy.LazyListScope

/** Desktop: the banner for a newer GitHub release, see the jvm source set. Adds no item on Android and iOS. */
expect fun LazyListScope.desktopUpdateBannerItem()
