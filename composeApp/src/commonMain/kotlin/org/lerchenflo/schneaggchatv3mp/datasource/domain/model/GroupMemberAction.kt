package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

/**
 * What [org.lerchenflo.schneaggchatv3mp.datasource.domain.GroupRepository.changeGroupMembers]
 * does with a group member. Domain copy of the network enum `NetworkUtils.GroupMemberAction`,
 * entry for entry.
 */
enum class GroupMemberAction {
    ADD_USER,
    REMOVE_USER,
    MAKE_ADMIN,
    REMOVE_ADMIN
}
