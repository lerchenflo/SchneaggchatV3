package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * What [org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository.changeGroupMembers]
 * does with a group member. Domain copy of the network enum `NetworkUtils.GroupMemberAction`,
 * entry for entry.
 */
enum class GroupMemberAction {
    ADD_USER,
    REMOVE_USER,
    MAKE_ADMIN,
    REMOVE_ADMIN
}
