package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.*
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.*


enum class LocationType {

    // Street
    RADAR,
    POLICE,
    MOUNTAIN_STREET,
    WHEELIESPOT,

    // Nature & Activities

    SIGHTSEEING,

    VIEWPOINT,
    CAMPING,
    SWIMMING,

    // Social & Entertainment
    PARTY,

    // Food
    FOOD_KEBAB,
    FOOD_PIZZA,
    FOOD_BURGER,
    FOOD_BEER,
    FOOD_ASIAN,
    FOOD_GREEK,
    FOOD_OTHER,
}


enum class LocationGroup(val types: List<LocationType>) {
    STREET(listOf(RADAR, POLICE, MOUNTAIN_STREET, WHEELIESPOT)),
    NATURE_ACTIVITIES(listOf(SIGHTSEEING, VIEWPOINT, CAMPING, SWIMMING)),
    SOCIAL_ENTERTAINMENT(listOf(PARTY)),
    FOOD(listOf(FOOD_KEBAB, FOOD_PIZZA, FOOD_BURGER, FOOD_BEER, FOOD_ASIAN, FOOD_GREEK, FOOD_OTHER)),
}


@Serializable
sealed class LocationData {

    abstract fun schema(): List<AttributeDefinition>
    abstract val locationtype: LocationType

    // Traffic & Hazards

    @Serializable
    @SerialName("radar")
    data class Radar(
        val speedLimit: AttributeValue,
        val mobile: AttributeValue?,
        val redLight: AttributeValue,
    ) : LocationData() {
        override val locationtype = RADAR

        val speedLimitValue get() = speedLimit.asInt
        val mobileValue     get() = mobile?.asBool
        val redLightValue   get() = redLight.asBool

        override fun schema() = listOf(
            AttributeDefinition.IntDef(key = "speedLimit", required = true, min = 0),
            AttributeDefinition.BoolDef(key = "mobile",    required = false),
            AttributeDefinition.BoolDef(key = "redLight",  required = true),
        )
    }

    @Serializable
    @SerialName("police")
    data class Police(
        val lastSeen: AttributeValue?,
    ) : LocationData() {
        override val locationtype = POLICE

        val lastSeenValue get() = lastSeen?.asLong

        override fun schema() = listOf(
            AttributeDefinition.LongDef(key = "lastSeen", required = false),
        )
    }


    // Rider Spots

    @Serializable
    @SerialName("mountain_street")
    data class MountainStreet(
        val mautFee: AttributeValue?,
        val heightLimit: AttributeValue?,
        val closedInWinter: AttributeValue?,
    ) : LocationData() {
        override val locationtype = MOUNTAIN_STREET

        val mautFeeValue        get() = mautFee?.asDouble
        val heightLimitValue    get() = heightLimit?.asDouble
        val closedInWinterValue get() = closedInWinter?.asBool

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "mautFee",        required = false, min = 0.0),
            AttributeDefinition.DoubleDef(key = "heightLimit",    required = false, min = 0.0),
            AttributeDefinition.BoolDef  (key = "closedInWinter", required = false),
        )
    }

    @Serializable
    @SerialName("wheeliespot")
    data class Wheeliespot(
        val onlyOnWeekends: AttributeValue?,
    ) : LocationData() {
        override val locationtype = WHEELIESPOT

        val onlyOnWeekendsValue get() = onlyOnWeekends?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "onlyOnWeekends", required = false),
        )
    }

    @Serializable
    @SerialName("viewpoint")
    class Viewpoint : LocationData() {
        override val locationtype = VIEWPOINT

        override fun schema() = emptyList<AttributeDefinition>()
    }


    // Nature & Activities

    @Serializable
    @SerialName("camping")
    data class Camping(
        val official: AttributeValue,
        val waterDistance: AttributeValue?,
        val sittingPossibility: AttributeValue?,
        val grillPossibility: AttributeValue?,
    ) : LocationData() {
        override val locationtype = CAMPING

        val officialValue           get() = official.asBool
        val waterDistanceValue       get() = waterDistance?.asInt
        val sittingPossibilityValue get() = sittingPossibility?.asBool
        val grillPossibilityValue   get() = grillPossibility?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "official",           required = true),
            AttributeDefinition.IntDef (key = "waterDistance",      required = false, min = 0),
            AttributeDefinition.BoolDef(key = "sittingPossibility", required = false),
            AttributeDefinition.BoolDef(key = "grillPossibility",   required = false),
        )
    }

    @Serializable
    @SerialName("swimming")
    data class SwimmingLocation(
        val indoor: AttributeValue?,
        val jumpSpot: AttributeValue?,
    ) : LocationData() {
        override val locationtype = SWIMMING

        val indoorValue   get() = indoor?.asBool
        val jumpSpotValue get() = jumpSpot?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "indoor",   required = false),
            AttributeDefinition.BoolDef(key = "jumpSpot", required = false),
        )
    }


    // Social & Entertainment

    @Serializable
    @SerialName("sightseeing")
    data class SightSeeing(
        val entryFee: AttributeValue?,
    ) : LocationData() {
        override val locationtype = SIGHTSEEING

        val entryFeeValue get() = entryFee?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("party")
    data class PartyLocation(
        val entryFee: AttributeValue?,
    ) : LocationData() {
        override val locationtype = PARTY

        val entryFeeValue get() = entryFee?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }


    // Fast Food & Snacks

    @Serializable
    @SerialName("food_kebab")
    data class FoodKebab(
        val kebabPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_KEBAB

        val kebabPriceValue get() = kebabPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "kebabPrice", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_pizza")
    data class FoodPizza(
        val margaritaPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_PIZZA

        val margaritaPriceValue get() = margaritaPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "margaritaPrice", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_burger")
    data class FoodBurger(
        val cheeseburgerPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_BURGER

        val cheeseburgerPriceValue get() = cheeseburgerPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "cheeseburgerPrice", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_beer")
    data class FoodBeer(
        val beerPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_BEER

        val beerPriceValue get() = beerPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "beerPrice", required = false, min = 0.0),
        )
    }


    // Restaurant

    @Serializable
    @SerialName("food_asian")
    data class FoodAsian(
        val allYouCanEat: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_ASIAN

        val allYouCanEatValue get() = allYouCanEat?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "allYouCanEat", required = false),
        )
    }

    @Serializable
    @SerialName("food_greek")
    class FoodGreek : LocationData() {
        override val locationtype = FOOD_GREEK

        override fun schema() = emptyList<AttributeDefinition>()
    }

    @Serializable
    @SerialName("food_other")
    data class FoodOther(
        val cuisine: AttributeValue,
    ) : LocationData() {
        override val locationtype = FOOD_OTHER

        val cuisineValue get() = cuisine.asString

        override fun schema() = listOf(
            AttributeDefinition.StringDef(key = "cuisine", required = true),
        )
    }
}


fun LocationType.toSimpleLocationData(): LocationData = when (this) {
    RADAR           -> LocationData.Radar(speedLimit = AttributeValue.IntValue(0), mobile = AttributeValue.BoolValue(false), redLight = AttributeValue.BoolValue(false)
    )
    POLICE          -> LocationData.Police(lastSeen = null)
    MOUNTAIN_STREET -> LocationData.MountainStreet(null, null, null)
    WHEELIESPOT     -> LocationData.Wheeliespot(onlyOnWeekends = null)
    VIEWPOINT       -> LocationData.Viewpoint()
    CAMPING         -> LocationData.Camping(official = AttributeValue.BoolValue(true), waterDistance = null, sittingPossibility = null, grillPossibility = null)
    SWIMMING        -> LocationData.SwimmingLocation(indoor = null, jumpSpot = null)
    SIGHTSEEING     -> LocationData.SightSeeing(entryFee = null)
    PARTY           -> LocationData.PartyLocation(entryFee = null)
    FOOD_KEBAB      -> LocationData.FoodKebab(kebabPrice = null)
    FOOD_PIZZA      -> LocationData.FoodPizza(margaritaPrice = null)
    FOOD_BURGER     -> LocationData.FoodBurger(cheeseburgerPrice = null)
    FOOD_BEER       -> LocationData.FoodBeer(beerPrice = null)
    FOOD_ASIAN      -> LocationData.FoodAsian(allYouCanEat = null)
    FOOD_GREEK      -> LocationData.FoodGreek()
    FOOD_OTHER      -> LocationData.FoodOther(cuisine = AttributeValue.StringValue(""))
}


@Composable
fun AttributeDefinition.label(): String {
    return when (this.key) {
        // Radar
        "speedLimit"        -> stringResource(Res.string.location_radar_speed_limit)
        "mobile"            -> stringResource(Res.string.location_radar_mobile)
        "redLight"          -> stringResource(Res.string.location_radar_red_light)

        // Police
        "lastSeen"          -> stringResource(Res.string.location_police_last_seen)

        // Mountain Street
        "mautFee"           -> stringResource(Res.string.location_street_maut_fee)
        "heightLimit"       -> stringResource(Res.string.location_street_height_limit)
        "closedInWinter"    -> stringResource(Res.string.location_street_closed_in_winter)

        // Wheelie Spot
        "onlyOnWeekends"    -> stringResource(Res.string.location_wheeliespot_only_on_weekends)

        // Camping
        "official"           -> stringResource(Res.string.location_camping_official)
        "waterDistance"       -> stringResource(Res.string.location_camping_water_distance)
        "sittingPossibility"  -> stringResource(Res.string.location_camping_sitting_possibility)
        "grillPossibility"    -> stringResource(Res.string.location_camping_grill_possibility)

        // Swimming
        "indoor"             -> stringResource(Res.string.location_swimming_indoor)
        "jumpSpot"            -> stringResource(Res.string.location_swimming_jump_spot)

        // Sightseeing & Party
        "entryFee"          -> stringResource(Res.string.location_sightseeing_entry_fee)

        // Food
        "allYouCanEat"      -> stringResource(Res.string.location_food_all_you_can_eat)
        "kebabPrice"        -> stringResource(Res.string.location_food_kebab_price)
        "margaritaPrice"    -> stringResource(Res.string.location_food_margarita_price)
        "cheeseburgerPrice" -> stringResource(Res.string.location_food_cheeseburger_price)
        "beerPrice"         -> stringResource(Res.string.location_food_beer_price)
        "cuisine"           -> stringResource(Res.string.location_food_cuisine)

        else -> {
            println("ERROR: SCHNEAGGMAP: KEY NOT RESOLVED: $key")
            this.key
        }
    }
}


@Composable
fun LocationType.stringRes(): StringResource = when (this) {
    RADAR           -> Res.string.location_type_radar
    POLICE          -> Res.string.location_type_police
    MOUNTAIN_STREET -> Res.string.location_type_mountain_street
    WHEELIESPOT     -> Res.string.location_type_wheeliespot
    VIEWPOINT       -> Res.string.location_type_viewpoint
    CAMPING         -> Res.string.location_type_camping
    SWIMMING        -> Res.string.location_type_swimming
    SIGHTSEEING     -> Res.string.location_type_sightseeing
    PARTY           -> Res.string.location_type_party
    FOOD_KEBAB      -> Res.string.location_type_food_kebab
    FOOD_PIZZA      -> Res.string.location_type_food_pizza
    FOOD_BURGER     -> Res.string.location_type_food_burger
    FOOD_BEER       -> Res.string.location_type_food_beer
    FOOD_ASIAN      -> Res.string.location_type_food_asian
    FOOD_GREEK      -> Res.string.location_type_food_greek
    FOOD_OTHER      -> Res.string.location_type_food_other
}

@Composable
fun LocationGroup.stringRes(): StringResource = when (this) {
    LocationGroup.STREET               -> Res.string.location_group_street
    LocationGroup.NATURE_ACTIVITIES    -> Res.string.location_group_nature_activities
    LocationGroup.SOCIAL_ENTERTAINMENT -> Res.string.location_group_social_entertainment
    LocationGroup.FOOD                 -> Res.string.location_group_food
}