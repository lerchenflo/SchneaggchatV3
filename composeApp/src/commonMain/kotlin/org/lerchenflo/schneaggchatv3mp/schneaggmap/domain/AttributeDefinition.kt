package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.location_camping_official
import schneaggchatv3mp.composeapp.generated.resources.location_food_all_you_can_eat
import schneaggchatv3mp.composeapp.generated.resources.location_food_type
import schneaggchatv3mp.composeapp.generated.resources.location_radar_speed_limit
import schneaggchatv3mp.composeapp.generated.resources.location_radar_type
import schneaggchatv3mp.composeapp.generated.resources.location_sightseeing_entry_fee
import schneaggchatv3mp.composeapp.generated.resources.location_street_closed_in_winter
import schneaggchatv3mp.composeapp.generated.resources.location_street_height_limit
import schneaggchatv3mp.composeapp.generated.resources.location_street_maut_fee
import schneaggchatv3mp.composeapp.generated.resources.location_street_wheelies_allowed
import schneaggchatv3mp.composeapp.generated.resources.location_swimming_indoor

@Serializable
sealed interface AttributeDefinition {
    val key: String
    val required: Boolean

    @Serializable @SerialName("string")
    data class StringDef(
        override val key: String,
        override val required: Boolean,
        val maxLength: Int? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("int")
    data class IntDef(
        override val key: String,
        override val required: Boolean,
        val min: Int? = null,
        val max: Int? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("double")
    data class DoubleDef(
        override val key: String,
        override val required: Boolean,
        val min: Double? = null,
        val max: Double? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("bool")
    data class BoolDef(
        override val key: String,
        override val required: Boolean,
    ) : AttributeDefinition
}
