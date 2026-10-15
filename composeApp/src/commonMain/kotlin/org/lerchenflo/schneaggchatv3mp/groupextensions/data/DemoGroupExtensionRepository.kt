package org.lerchenflo.schneaggchatv3mp.groupextensions.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType

/**
 * In-memory stand-in until the server and Room parts exist (GROUP_EXTENSIONS_PLAN.md phase 2).
 * Every group gets two demo extensions the first time its list is observed.
 * todo: replace with the Room-backed repository
 */
class DemoGroupExtensionRepository : GroupExtensionRepository {

    private val extensions = MutableStateFlow<Map<String, GroupExtension>>(emptyMap())

    override fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>> = extensions
        .onStart { seedDemoExtensions(ownerId) }
        .map { all -> all.values.filter { it.ownerId == ownerId } }

    override fun getExtensionFlow(id: String): Flow<GroupExtension?> = extensions.map { it[id] }

    private fun seedDemoExtensions(ownerId: String) {
        extensions.update { current ->
            if (current.values.any { it.ownerId == ownerId }) return@update current

            val demo = listOf(
                demoExtension(ownerId, GroupExtensionType.SPLIT, "Kassa"),
                demoExtension(ownerId, GroupExtensionType.QUOTES, "Zitate"),
            )
            current + demo.associateBy { it.id }
        }
    }

    private fun demoExtension(ownerId: String, type: GroupExtensionType, title: String) = GroupExtension(
        id = "demo-${type.name}-$ownerId",
        ownerType = ExtensionOwnerType.GROUP,
        ownerId = ownerId,
        type = type,
        title = title,
        createdBy = "",
        createdAt = 0L,
        updatedBy = "",
        updatedAt = 0L,
    )
}
