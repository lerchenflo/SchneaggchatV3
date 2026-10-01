package org.lerchenflo.schneaggchatv3mp.games.domain

import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_bad_internet
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_bad_internet_detail
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_isp_switch
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_isp_switch_detail
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_lightning
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_lightning_detail
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_proxy
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_proxy_detail

/** The year [DowntimeReason] describes - the downtime page is only shown in that year's recap. */
const val DOWNTIME_RECAP_YEAR = 2026

/**
 * Hardcoded reasons for server downtime, in chaos order (most chaotic first).
 * [occurrences] null means "countless".
 *
 */
enum class DowntimeReason(
    val title: StringResource,
    val detail: StringResource,
    val occurrences: Int?,
) {
    LIGHTNING(Res.string.recap_downtime_lightning, Res.string.recap_downtime_lightning_detail, 1),
    ISP_SWITCH(Res.string.recap_downtime_isp_switch, Res.string.recap_downtime_isp_switch_detail, 1),
    BAD_INTERNET(Res.string.recap_downtime_bad_internet, Res.string.recap_downtime_bad_internet_detail, null),
    PROXY_CONFIG(Res.string.recap_downtime_proxy, Res.string.recap_downtime_proxy_detail, 3),
}
