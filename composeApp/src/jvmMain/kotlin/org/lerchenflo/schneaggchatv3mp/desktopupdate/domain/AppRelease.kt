package org.lerchenflo.schneaggchatv3mp.desktopupdate.domain

/** A release on GitHub. A beta is an optional test version of the next final version. */
data class AppRelease(
    val tagName: String,
    val isBeta: Boolean,
    // GitHub page of this release, with its download files
    val pageUrl: String,
)

private fun List<AppRelease>.newest(): AppRelease? = reduceOrNull { newest, release ->
    if (isNewVersionHigher(newest.tagName, release.tagName)) release else newest
}

/**
 * The release to offer on [installedVersion]: the newest final version, otherwise the newest beta,
 * null when nothing is newer.
 *
 * @throws IllegalArgumentException for an invalid version, like "Desktop" in a dev run
 */
fun List<AppRelease>.findUpdateFor(installedVersion: String): AppRelease? {
    val (betas, finals) = filter { isNewVersionHigher(installedVersion, it.tagName) }
        .partition { it.isBeta }
    return finals.newest() ?: betas.newest()
}
