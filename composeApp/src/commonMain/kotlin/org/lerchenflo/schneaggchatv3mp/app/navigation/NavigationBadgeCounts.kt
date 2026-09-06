package org.lerchenflo.schneaggchatv3mp.app.navigation

/**
 * Live badge numbers for the bottom navigation bar, produced by
 * [org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel] and read per destination through
 * [NavigationBarItemTemplate.badgeCount].
 */
data class NavigationBadgeCounts(
    val unreadChats: Int = 0,
    val unseenEvents: Int = 0,
)
