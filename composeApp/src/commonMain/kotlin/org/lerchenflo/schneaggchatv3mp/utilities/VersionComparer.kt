package org.lerchenflo.schneaggchatv3mp.utilities

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

    // 6. Handle pre-release tags (beta, alpha, rc, etc.)
    if (preCurrent == null && preNew != null) return false
    if (preCurrent != null && preNew == null) return true

    if (preCurrent != null && preNew != null) {
        return preNew > preCurrent
    }

    return false
}