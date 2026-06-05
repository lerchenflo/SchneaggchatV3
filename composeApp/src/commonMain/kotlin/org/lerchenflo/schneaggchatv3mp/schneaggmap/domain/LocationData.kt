package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.*
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.*


enum class LocationType {
    RADAR, STREET, CAMPING,
    SIGHTSEEING, SWIMMING, PARTY,
    FASTFOOD, ASIANFOOD, GENERICFOOD
}


@Serializable
sealed interface LocationData {

    fun schema(): List<AttributeDefinition>

    @Serializable
    @SerialName("radar")
    data class Radar(
        val speedLimit: AttributeValue,
        val mobile: AttributeValue?,
        val redLight: AttributeValue
    ) : LocationData {
        val speedLimitValue get() = speedLimit.asInt
        val mobileValue     get() = mobile?.asBool
        val redLightValue   get() = redLight.asBool

        override fun schema() = listOf(
            AttributeDefinition.IntDef(key = "speedLimit", required = true, min = 0),
            AttributeDefinition.BoolDef(key = "mobile", required = false),
            AttributeDefinition.BoolDef(key = "redLight", required = true)
        )
    }

    @Serializable
    @SerialName("street")
    data class Street(
        val mautFee: AttributeValue?,
        val heightLimit: AttributeValue?,
        val closedInWinter: AttributeValue?,
        val wheeliesAllowed: AttributeValue?,
    ) : LocationData {
        val mautFeeValue        get() = mautFee?.asDouble
        val heightLimitValue    get() = heightLimit?.asDouble
        val closedInWinterValue get() = closedInWinter?.asBool
        val wheeliesAllowedValue get() = wheeliesAllowed?.asBool

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "mautFee",         required = false, min = 0.0),
            AttributeDefinition.DoubleDef(key = "heightLimit",     required = false, min = 0.0),
            AttributeDefinition.BoolDef  (key = "closedInWinter",  required = false),
            AttributeDefinition.BoolDef  (key = "wheeliesAllowed", required = false),
        )
    }

    @Serializable
    @SerialName("camping")
    data class Camping(
        val official: AttributeValue
    ) : LocationData {
        val officialValue get() = official.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "official", required = true),
        )
    }

    @Serializable
    @SerialName("sightseeing")
    data class SightSeeing(
        val entryFee: AttributeValue?,
    ) : LocationData {
        val entryFeeValue get() = entryFee?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("swimming")
    data class SwimmingLocation(
        val indoor: AttributeValue?
    ) : LocationData {
        val indoorValue get() = indoor?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "indoor", required = false),
        )
    }

    @Serializable
    @SerialName("party")
    data class PartyLocation(
        val entryFee: AttributeValue?,
    ) : LocationData {
        val entryFeeValue get() = entryFee?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("fast_food")
    data class FastFood(
        val burger: AttributeValue?,
        val kebab: AttributeValue?,
        val pizza: AttributeValue?,
        val allYouCanEat: AttributeValue?,
    ) : LocationData {
        val burgerValue      get() = burger?.asBool
        val kebabValue       get() = kebab?.asBool
        val pizzaValue       get() = pizza?.asBool
        val allYouCanEatValue get() = allYouCanEat?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "burger",      required = false),
            AttributeDefinition.BoolDef(key = "kebab",       required = false),
            AttributeDefinition.BoolDef(key = "pizza",       required = false),
            AttributeDefinition.BoolDef(key = "allYouCanEat",required = false),
        )
    }

    @Serializable
    @SerialName("asian_food")
    data class AsianFood(
        val chinese: AttributeValue?,
        val japanese: AttributeValue?,
        val thai: AttributeValue?,
        val allYouCanEat: AttributeValue?,
    ) : LocationData {
        val chineseValue      get() = chinese?.asBool
        val japaneseValue     get() = japanese?.asBool
        val thaiValue         get() = thai?.asBool
        val allYouCanEatValue get() = allYouCanEat?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "chinese",     required = false),
            AttributeDefinition.BoolDef(key = "japanese",    required = false),
            AttributeDefinition.BoolDef(key = "thai",        required = false),
            AttributeDefinition.BoolDef(key = "allYouCanEat",required = false),
        )
    }

    @Serializable
    @SerialName("generic_food")
    data class GenericFood(
        val cuisine: AttributeValue,
        val allYouCanEat: AttributeValue?,
    ) : LocationData {
        val cuisineValue      get() = cuisine.asString
        val allYouCanEatValue get() = allYouCanEat?.asBool

        override fun schema() = listOf(
            AttributeDefinition.StringDef(key = "cuisine",      required = true),
            AttributeDefinition.BoolDef  (key = "allYouCanEat", required = false),
        )
    }
}


val LocationData.toEnum: LocationType
    get() = when (this) {
        is LocationData.Radar           -> RADAR
        is LocationData.Street          -> STREET
        is LocationData.Camping         -> CAMPING
        is LocationData.SightSeeing     -> SIGHTSEEING
        is LocationData.SwimmingLocation -> SWIMMING
        is LocationData.PartyLocation   -> PARTY
        is LocationData.AsianFood       -> ASIANFOOD
        is LocationData.FastFood        -> FASTFOOD
        is LocationData.GenericFood     -> GENERICFOOD
    }

fun LocationType.toSimpleLocationData(): LocationData = when (this) {
    RADAR      -> LocationData.Radar(
        speedLimit = AttributeValue.IntValue(0),
        mobile     = AttributeValue.BoolValue(false),
        redLight   = AttributeValue.BoolValue(false)
    )
    STREET     -> LocationData.Street(null, null, null, null)
    CAMPING    -> LocationData.Camping(official = AttributeValue.BoolValue(true))
    SIGHTSEEING -> LocationData.SightSeeing(entryFee = null)
    SWIMMING   -> LocationData.SwimmingLocation(indoor = null)
    PARTY      -> LocationData.PartyLocation(entryFee = null)
    FASTFOOD   -> LocationData.FastFood(
        burger       = AttributeValue.BoolValue(false),
        kebab        = AttributeValue.BoolValue(false),
        pizza        = AttributeValue.BoolValue(false),
        allYouCanEat = AttributeValue.BoolValue(false)
    )
    ASIANFOOD  -> LocationData.AsianFood(
        chinese      = AttributeValue.BoolValue(false),
        japanese     = AttributeValue.BoolValue(false),
        thai         = AttributeValue.BoolValue(false),
        allYouCanEat = AttributeValue.BoolValue(false)
    )
    GENERICFOOD -> LocationData.GenericFood(
        cuisine      = AttributeValue.StringValue(""),
        allYouCanEat = AttributeValue.BoolValue(false)
    )
}


@Composable
fun locationDataStringResFromKey(type: LocationType): StringResource = when (type) {
    RADAR       -> Res.string.location_type_radar
    STREET      -> Res.string.location_type_street
    CAMPING     -> Res.string.location_type_camping
    SIGHTSEEING -> Res.string.location_type_sightseeing
    SWIMMING    -> Res.string.location_type_swimming
    PARTY       -> Res.string.location_type_party
    FASTFOOD    -> Res.string.location_type_fast_food
    ASIANFOOD   -> Res.string.location_food_type_asian
    GENERICFOOD -> Res.string.location_type_generic_food
}

@Composable
fun LocationData.stringRes(): StringResource = when (this) {
    is LocationData.Camping         -> Res.string.location_type_camping
    is LocationData.PartyLocation   -> Res.string.location_type_party
    is LocationData.Radar           -> Res.string.location_type_radar
    is LocationData.SightSeeing     -> Res.string.location_type_sightseeing
    is LocationData.Street          -> Res.string.location_type_street
    is LocationData.SwimmingLocation -> Res.string.location_type_swimming
    is LocationData.AsianFood       -> Res.string.location_type_asian_food
    is LocationData.FastFood        -> Res.string.location_type_fast_food
    is LocationData.GenericFood     -> Res.string.location_type_generic_food
}