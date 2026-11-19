package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.sharedtext

/**
 * Puts the own unsaved edits ([local], based on [base]) on top of a newer server version
 * ([remote], also based on [base]), so the user keeps typing while others write, like in
 * Etherpad. Each side is reduced to one changed part ([textChange]): parts that do not overlap
 * are both kept, where they overlap the own edit wins, because that is what the user sees on
 * screen. Unchanged text takes its writers from [remote], the newest server state.
 */
fun mergeTextEdits(base: AuthoredText, local: AuthoredText, remote: AuthoredText): AuthoredText {
    if (local.text == remote.text) return remote
    val localChange = textChange(base.text, local.text) ?: return remote
    val remoteChange = textChange(base.text, remote.text) ?: TextChange(remote.length, remote.length, remote.length)

    return when {
        // Own edit before the other one: its positions are the same in remote
        localChange.oldEnd <= remoteChange.start -> remote.replace(
            start = localChange.start,
            end = localChange.oldEnd,
            replacement = local.slice(localChange.start, localChange.newEnd),
        )
        // Own edit after the other one: shifted by what the other edit added or removed
        remoteChange.oldEnd <= localChange.start -> remote.replace(
            start = localChange.start + remoteChange.lengthDelta,
            end = localChange.oldEnd + remoteChange.lengthDelta,
            replacement = local.slice(localChange.start, localChange.newEnd),
        )
        // Both changed the same area: the own version of the whole area wins
        else -> {
            val start = minOf(localChange.start, remoteChange.start)
            val oldEnd = maxOf(localChange.oldEnd, remoteChange.oldEnd)
            remote.replace(
                start = start,
                end = oldEnd + remoteChange.lengthDelta,
                replacement = local.slice(start, oldEnd + localChange.lengthDelta),
            )
        }
    }
}
