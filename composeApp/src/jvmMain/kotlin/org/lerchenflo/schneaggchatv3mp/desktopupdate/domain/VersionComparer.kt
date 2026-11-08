package org.lerchenflo.schneaggchatv3mp.desktopupdate.domain

private val BETA_TAG_REGEX = Regex("beta(\\d*)", RegexOption.IGNORE_CASE)

/** Build number of a beta: "beta" is 1, "beta2" is 2. Null for anything that is no beta. */
private fun betaNumber(versionTag: String?): Int? =
    versionTag?.let(BETA_TAG_REGEX::find)?.let { it.groupValues[1].toIntOrNull() ?: 1 }

fun isBetaVersion(version: String): Boolean = betaNumber(version) != null

fun isNewVersionHigher(currentVersion: String, newVersion: String): Boolean {
    // 1. Validate parameters using Regex
    // Now accepts both '-' and '_' before the tag, and within the tag itself
    val versionRegex = Regex("^v?\\d+(\\.\\d+)*([-_][a-zA-Z0-9._-]+)?$")

    require(currentVersion.isNotBlank() && versionRegex.matches(currentVersion)) {
        "Invalid currentVersion format: '$currentVersion'. Expected format like 'v1.2.3' or '3.0.16_beta'."
    }
    require(newVersion.isNotBlank() && versionRegex.matches(newVersion)) {
        "Invalid newVersion format: '$newVersion'. Expected format like 'v1.2.3' or '3.0.16_beta'."
    }

    // 2. Clean the strings by removing 'v' and normalizing '_' to '-'
    val current = currentVersion.removePrefix("v").replace("_", "-")
    val new = newVersion.removePrefix("v").replace("_", "-")

    // 3. Split into core version and pre-release tag
    val (coreCurrent, preCurrent) = current.split("-", limit = 2).let { it[0] to it.getOrNull(1) }
    val (coreNew, preNew) = new.split("-", limit = 2).let { it[0] to it.getOrNull(1) }

    // 4. Parse core components safely
    val currentParts = coreCurrent.split(".").map { it.toIntOrNull() ?: 0 }
    val newParts = coreNew.split(".").map { it.toIntOrNull() ?: 0 }

    // 5. Compare core versions (Major, Minor, Patch)
    val maxLength = maxOf(currentParts.size, newParts.size)
    for (i in 0 until maxLength) {
        val cPart = currentParts.getOrElse(i) { 0 }
        val nPart = newParts.getOrElse(i) { 0 }

        if (nPart > cPart) return true
        if (nPart < cPart) return false
    }

    // 6. Same numbers: the final version beats its betas, a later beta beats an earlier one
    val betaCurrent = betaNumber(preCurrent) ?: return false
    val betaNew = betaNumber(preNew) ?: return true
    return betaNew > betaCurrent
}