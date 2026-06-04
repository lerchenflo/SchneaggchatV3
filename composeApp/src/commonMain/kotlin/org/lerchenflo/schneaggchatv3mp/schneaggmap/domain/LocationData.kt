package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.ASIANFOOD
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.CAMPING
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FASTFOOD
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.GENERICFOOD
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.PARTY
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.RADAR
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.SIGHTSEEING
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.STREET
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.SWIMMING
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_asian
import schneaggchatv3mp.composeapp.generated.resources.location_type_asian_food
import schneaggchatv3mp.composeapp.generated.resources.location_type_camping
import schneaggchatv3mp.composeapp.generated.resources.location_type_fast_food
import schneaggchatv3mp.composeapp.generated.resources.location_type_generic_food
import schneaggchatv3mp.composeapp.generated.resources.location_type_party
import schneaggchatv3mp.composeapp.generated.resources.location_type_radar
import schneaggchatv3mp.composeapp.generated.resources.location_type_sightseeing
import schneaggchatv3mp.composeapp.generated.resources.location_type_street
import schneaggchatv3mp.composeapp.generated.resources.location_type_swimming


enum class LocationType {

    //Street
    RADAR,
    STREET,
    CAMPING,

    //Activity
    SIGHTSEEING,
    SWIMMING,
    PARTY,

    //Food
    FASTFOOD,
    ASIANFOOD,
    GENERICFOOD;
}



@Serializable
sealed interface LocationData {

    fun schema(): List<AttributeDefinition>


    @Serializable
    @SerialName("radar")
    data class Radar(
        val speedLimit: AttributeValue.IntValue,
        val mobile: AttributeValue.BoolValue?,
        val redLight: AttributeValue.BoolValue
    ) : LocationData {
        override fun schema() = listOf(
            AttributeDefinition.IntDef(key = "speedLimit", required = true, min = 0),
            AttributeDefinition.BoolDef(key = "mobile", required = false),
            AttributeDefinition.BoolDef(key = "redLight", required = true)
        )
    }

    @Serializable
    @SerialName("street")
    data class Street(

        val mautFee: AttributeValue.DoubleValue?,
        val heightLimit: AttributeValue.DoubleValue?,
        val closedInWinter: AttributeValue.BoolValue?,

        val wheeliesAllowed: AttributeValue.BoolValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "mautFee",        required = false, min = 0.0),
            AttributeDefinition.DoubleDef(key = "heightLimit",    required = false, min = 0.0),
            AttributeDefinition.BoolDef  (key = "closedInWinter", required = false),
            AttributeDefinition.BoolDef  (key = "wheeliesAllowed",required = false),
        )
    }

    @Serializable
    @SerialName("camping")
    data class Camping(

        val official: AttributeValue.BoolValue

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "official", required = true),
        )
    }


    //ACtivities

    @Serializable
    @SerialName("sightseeing")
    data class SightSeeing(

        val entryFee: AttributeValue.DoubleValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("swimming")
    data class SwimmingLocation(
        val indoor: AttributeValue.BoolValue?
    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "indoor", required = false),
        )
    }

    @Serializable
    @SerialName("party")
    data class PartyLocation(

        val entryFee: AttributeValue.DoubleValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }


    @Serializable
    @SerialName("fast_food")
    data class FastFood(
        val burger: AttributeValue.BoolValue?,
        val kebab: AttributeValue.BoolValue?,
        val pizza: AttributeValue.BoolValue?,
        val allYouCanEat: AttributeValue.BoolValue?,
    ) : LocationData {
        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "burger", required = false),
            AttributeDefinition.BoolDef(key = "kebab", required = false),
            AttributeDefinition.BoolDef(key = "pizza", required = false),
            AttributeDefinition.BoolDef(key = "allYouCanEat", required = false),
        )
    }

    @Serializable
    @SerialName("asian_food")
    data class AsianFood(
        val chinese: AttributeValue.BoolValue?,
        val japanese: AttributeValue.BoolValue?,
        val thai: AttributeValue.BoolValue?,
        val allYouCanEat: AttributeValue.BoolValue?,
    ) : LocationData {
        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "chinese", required = false),
            AttributeDefinition.BoolDef(key = "japanese", required = false),
            AttributeDefinition.BoolDef(key = "thai", required = false),
            AttributeDefinition.BoolDef(key = "allYouCanEat", required = false),
        )
    }

    @Serializable
    @SerialName("generic_food")
    data class GenericFood(
        val cuisine: AttributeValue.StringValue,  // Free text field for any cuisine
        val allYouCanEat: AttributeValue.BoolValue?,
    ) : LocationData {
        override fun schema() = listOf(
            AttributeDefinition.StringDef(key = "cuisine", required = true),
            AttributeDefinition.BoolDef(key = "allYouCanEat", required = false),
        )
    }
}


val LocationData.toEnum: LocationType
    get() = when (this) {
        is LocationData.Radar -> RADAR
        is LocationData.Street -> STREET
        is LocationData.Camping -> CAMPING
        is LocationData.SightSeeing -> SIGHTSEEING
        is LocationData.SwimmingLocation -> SWIMMING
        is LocationData.PartyLocation -> PARTY
        is LocationData.AsianFood -> ASIANFOOD
        is LocationData.FastFood -> FASTFOOD
        is LocationData.GenericFood -> GENERICFOOD
    }

fun LocationType.toSimpleLocationData(): LocationData {
    return when(this) {
        RADAR -> LocationData.Radar(
            speedLimit = AttributeValue.IntValue(0),
            mobile = AttributeValue.BoolValue(false),
            redLight = AttributeValue.BoolValue(false)
        )
        STREET -> LocationData.Street(mautFee = null, heightLimit = null, closedInWinter = null, wheeliesAllowed = null)
        CAMPING -> LocationData.Camping(official = AttributeValue.BoolValue(true))
        SIGHTSEEING -> LocationData.SightSeeing(entryFee = null)
        SWIMMING -> LocationData.SwimmingLocation(indoor = null)
        PARTY -> LocationData.PartyLocation(entryFee = null)
        FASTFOOD -> LocationData.FastFood(burger = AttributeValue.BoolValue(false), kebab = AttributeValue.BoolValue(false), pizza = AttributeValue.BoolValue(false), allYouCanEat = AttributeValue.BoolValue(false))
        ASIANFOOD -> LocationData.AsianFood(chinese = AttributeValue.BoolValue(false), japanese = AttributeValue.BoolValue(false), thai = AttributeValue.BoolValue(false), allYouCanEat = AttributeValue.BoolValue(false))
        GENERICFOOD -> LocationData.GenericFood(cuisine = AttributeValue.StringValue(""), allYouCanEat = AttributeValue.BoolValue(false))
    }
}


@Composable
fun locationDataStringResFromKey(type: LocationType): StringResource {
    return when (type) {
        RADAR -> Res.string.location_type_radar
        STREET -> Res.string.location_type_street
        CAMPING -> Res.string.location_type_camping
        SIGHTSEEING -> Res.string.location_type_sightseeing
        SWIMMING -> Res.string.location_type_swimming
        PARTY -> Res.string.location_type_party
        FASTFOOD -> Res.string.location_type_fast_food
        ASIANFOOD -> Res.string.location_food_type_asian
        GENERICFOOD -> Res.string.location_type_generic_food
    }

}


@Composable
fun LocationData.stringRes() = when (this) {
    is LocationData.Camping ->
        Res.string.location_type_camping

    is LocationData.PartyLocation ->
        Res.string.location_type_party

    is LocationData.Radar ->
        Res.string.location_type_radar

    is LocationData.SightSeeing ->
        Res.string.location_type_sightseeing

    is LocationData.Street ->
        Res.string.location_type_street

    is LocationData.SwimmingLocation ->
        Res.string.location_type_swimming

    is LocationData.AsianFood ->
        Res.string.location_type_asian_food


    is LocationData.FastFood ->
        Res.string.location_type_fast_food

    is LocationData.GenericFood ->
        Res.string.location_type_generic_food

}
