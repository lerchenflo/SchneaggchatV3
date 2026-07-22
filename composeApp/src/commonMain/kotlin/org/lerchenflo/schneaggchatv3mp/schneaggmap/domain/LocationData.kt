package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.*
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.*

/**
 * HOW TO ADD A NEW LOCATION ENTRY
 * ================================
 *
 * CLIENT SIDE (SchneaggchatV3):
 * 1. Add the new enum value to LocationType enum (group it appropriately)
 * 2. Add the new type to the appropriate LocationGroup
 * 3. Add the data class in LocationData sealed class with:
 *    - @Serializable annotation
 *    - @SerialName annotation with snake_case name
 *    - Properties with AttributeValue types (nullable if optional)
 *    - Override val locationtype
 *    - Getter properties for each field (e.g., val fieldNameValue get() = field?.asType)
 *    - Override fun schema() returning list of AttributeDefinitions
 * 4. Add the new type to toSimpleLocationData() function
 * 5. Add string resolution for each attribute in AttributeDefinition.label() function
 * 6. Add the type to LocationType.stringRes() function
 * 7. Add string entries in strings.xml:
 *    - location_type_[name] for the type name
 *    - location_[type]_[attribute] for each attribute label
 * 8. Add icon mapping in SchneaggmapScreen.kt with TODO comment for proper icon
 * 9. Update GetSetValueForKey.kt:
 *    - Add case in getValueByKey() for each attribute
 *    - Add case in withValueForKey() for each attribute with copy()
 *
 * SERVER SIDE (SchneaggchatV3server):
 * 1. Add the new type to @JsonSubTypes annotation in LocationData.kt
 * 2. Add the data class in LocationData sealed class with:
 *    - @TypeAlias annotation with snake_case name
 *    - Properties with AttributeValue types (nullable if optional)
 *    - Override fun schema() returning list of AttributeDefinitions
 * 3. Add the new type to LocationDataWriteConverter in MongoConfig.kt
 * 4. Update any service files that instantiate LocationData (e.g., SchneaggmapService.kt)
 *
 * IMPORTANT: Keep the serial names consistent between client and server (snake_case)
 */


enum class LocationType {

    // Driving
    RADAR,
    POLICE,
    MOUNTAIN_STREET,
    WHEELIESPOT,
    OFFROAD_MOTORCYCLE,

    // Nature & Activities

    SIGHTSEEING,

    VIEWPOINT,
    CAMPING,
    SWIMMING,
    CLIMBINGSPOT,

    // Sport
    VOLLEYBALL,
    BICYCLE,
    OUTDOOR_FITNESS,
    TABLE_TENNIS,
    TENNIS,

    // Social & Entertainment
    PARTY,

    // Food
    FOOD_KEBAB,
    FOOD_PIZZA,
    FOOD_BURGER,
    FOOD_BEER,
    FOOD_ASIAN,
    FOOD_GREEK,
    FOOD_CAFE_BAKERY,
    FOOD_OTHER,
}


enum class LocationGroup(val types: List<LocationType>) {
    DRIVING(listOf(RADAR, POLICE, MOUNTAIN_STREET, WHEELIESPOT, OFFROAD_MOTORCYCLE)),
    NATURE_ACTIVITIES(listOf(SIGHTSEEING, VIEWPOINT, CAMPING, SWIMMING, CLIMBINGSPOT)),
    SPORT(listOf(VOLLEYBALL, BICYCLE, OUTDOOR_FITNESS, TABLE_TENNIS, TENNIS)),
    SOCIAL_ENTERTAINMENT(listOf(PARTY)),
    FOOD(listOf(FOOD_KEBAB, FOOD_PIZZA, FOOD_BURGER, FOOD_BEER, FOOD_ASIAN, FOOD_GREEK, FOOD_CAFE_BAKERY, FOOD_OTHER)),
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
    @SerialName("offroad_motorcycle")
    data class OffroadMotorcycle(
        val legal: AttributeValue,
        val motocross: AttributeValue? = null,
        val enduro: AttributeValue? = null,
    ) : LocationData() {
        override val locationtype = OFFROAD_MOTORCYCLE

        val legalValue     get() = legal.asBool
        val motocrossValue get() = motocross?.asBool
        val enduroValue    get() = enduro?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "legal",     required = true),
            AttributeDefinition.BoolDef(key = "motocross", required = false),
            AttributeDefinition.BoolDef(key = "enduro",    required = false),
        )
    }

    @Serializable
    @SerialName("viewpoint")
    data class Viewpoint(
        //Defaulted so map entries cached before this attribute existed still deserialize
        val lieDownFriendly: AttributeValue? = null,
    ) : LocationData() {
        override val locationtype = VIEWPOINT

        val lieDownFriendlyValue get() = lieDownFriendly?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "lieDownFriendly", required = false),
        )
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
        //Defaulted so map entries cached before this attribute existed still deserialize
        val lieDownFriendly: AttributeValue? = null,
        val price: AttributeValue? = null,
    ) : LocationData() {
        override val locationtype = SWIMMING

        val indoorValue          get() = indoor?.asBool
        val jumpSpotValue        get() = jumpSpot?.asBool
        val lieDownFriendlyValue get() = lieDownFriendly?.asBool
        val priceValue           get() = price?.asInt

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "indoor",          required = false),
            AttributeDefinition.BoolDef(key = "jumpSpot",        required = false),
            AttributeDefinition.BoolDef(key = "lieDownFriendly", required = false),
            AttributeDefinition.IntDef (key = "price",          required = false, min = 0),
        )
    }

    @Serializable
    @SerialName("climbingspot")
    data class Climbingspot(
        val viaFerrata: AttributeValue?,
        val outdoor: AttributeValue?,
        val price: AttributeValue?,
    ) : LocationData() {
        override val locationtype = CLIMBINGSPOT

        val viaFerrataValue get() = viaFerrata?.asBool
        val outdoorValue    get() = outdoor?.asBool
        val priceValue      get() = price?.asInt

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "viaFerrata", required = false),
            AttributeDefinition.BoolDef(key = "outdoor",    required = false),
            AttributeDefinition.IntDef (key = "price",      required = false, min = 0),
        )
    }


    // Sport

    @Serializable
    @SerialName("volleyball")
    data class Volleyball(
        val goodNet: AttributeValue?,
        val goodField: AttributeValue?,
        val outdoor: AttributeValue?,
    ) : LocationData() {
        override val locationtype = VOLLEYBALL

        val goodNetValue   get() = goodNet?.asBool
        val goodFieldValue get() = goodField?.asBool
        val outdoorValue   get() = outdoor?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "goodNet",   required = false),
            AttributeDefinition.BoolDef(key = "goodField", required = false),
            AttributeDefinition.BoolDef(key = "outdoor",   required = false),
        )
    }

    @Serializable
    @SerialName("bicycle")
    data class Bicycle(
        val legal: AttributeValue,
        val difficulty: AttributeValue,
        val undergroundType: AttributeValue?,
    ) : LocationData() {
        override val locationtype = BICYCLE

        val legalValue           get() = legal.asBool
        val difficultyValue      get() = difficulty.asInt
        val undergroundTypeValue get() = undergroundType?.asString

        override fun schema() = listOf(
            AttributeDefinition.BoolDef  (key = "legal",           required = true),
            AttributeDefinition.IntDef   (key = "difficulty",      required = true, min = 1, max = 10),
            AttributeDefinition.StringDef(key = "undergroundType", required = false),
        )
    }

    @Serializable
    @SerialName("outdoor_fitness")
    data class OutdoorFitness(
        val shadow: AttributeValue?,
    ) : LocationData() {
        override val locationtype = OUTDOOR_FITNESS

        val shadowValue get() = shadow?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "shadow", required = false),
        )
    }

    @Serializable
    @SerialName("table_tennis")
    data class TableTennis(
        val `private`: AttributeValue?,
    ) : LocationData() {
        override val locationtype = TABLE_TENNIS

        val privateValue get() = `private`?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "private", required = false),
        )
    }

    @Serializable
    @SerialName("tennis")
    data class Tennis(
        val paddle: AttributeValue?,
    ) : LocationData() {
        override val locationtype = TENNIS

        val paddleValue get() = paddle?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "paddle", required = false),
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

    @Serializable
    @SerialName("food_cafe_bakery")
    data class FoodCafeBakery(
        val outdoorSeating: AttributeValue?,
        val alcohol: AttributeValue?,
        val coffee: AttributeValue?,
        val breakfast: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_CAFE_BAKERY

        val outdoorSeatingValue get() = outdoorSeating?.asBool
        val alcoholValue       get() = alcohol?.asBool
        val coffeeValue        get() = coffee?.asBool
        val breakfastValue    get() = breakfast?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "outdoorSeating", required = false),
            AttributeDefinition.BoolDef(key = "alcohol",       required = false),
            AttributeDefinition.BoolDef(key = "coffee",        required = false),
            AttributeDefinition.BoolDef(key = "breakfast",     required = false),
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
    OFFROAD_MOTORCYCLE -> LocationData.OffroadMotorcycle(legal = AttributeValue.BoolValue(false), motocross = null, enduro = null)
    VIEWPOINT       -> LocationData.Viewpoint(lieDownFriendly = null)
    CAMPING         -> LocationData.Camping(official = AttributeValue.BoolValue(true), waterDistance = null, sittingPossibility = null, grillPossibility = null)
    SWIMMING        -> LocationData.SwimmingLocation(indoor = null, jumpSpot = null, lieDownFriendly = null, price = null)
    CLIMBINGSPOT    -> LocationData.Climbingspot(viaFerrata = null, outdoor = null, price = null)
    VOLLEYBALL      -> LocationData.Volleyball(goodNet = null, goodField = null, outdoor = null)
    BICYCLE         -> LocationData.Bicycle(legal = AttributeValue.BoolValue(true), difficulty = AttributeValue.IntValue(1), undergroundType = null)
    OUTDOOR_FITNESS -> LocationData.OutdoorFitness(shadow = null)
    TABLE_TENNIS    -> LocationData.TableTennis(`private` = null)
    TENNIS          -> LocationData.Tennis(paddle = null)
    SIGHTSEEING     -> LocationData.SightSeeing(entryFee = null)
    PARTY           -> LocationData.PartyLocation(entryFee = null)
    FOOD_KEBAB      -> LocationData.FoodKebab(kebabPrice = null)
    FOOD_PIZZA      -> LocationData.FoodPizza(margaritaPrice = null)
    FOOD_BURGER     -> LocationData.FoodBurger(cheeseburgerPrice = null)
    FOOD_BEER       -> LocationData.FoodBeer(beerPrice = null)
    FOOD_ASIAN      -> LocationData.FoodAsian(allYouCanEat = null)
    FOOD_GREEK      -> LocationData.FoodGreek()
    FOOD_CAFE_BAKERY -> LocationData.FoodCafeBakery(outdoorSeating = null, alcohol = null, coffee = null, breakfast = null)
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

        // Offroad Motorcycle
        "legal"             -> stringResource(Res.string.location_legal)
        "motocross"         -> stringResource(Res.string.location_offroad_motocross)
        "enduro"            -> stringResource(Res.string.location_offroad_enduro)

        // Bicycle
        "difficulty"        -> stringResource(Res.string.location_bicycle_difficulty)
        "undergroundType"   -> stringResource(Res.string.location_bicycle_underground_type)

        // Viewpoint & Swimming
        "lieDownFriendly"   -> stringResource(Res.string.location_lie_down_friendly)

        // Camping
        "official"           -> stringResource(Res.string.location_camping_official)
        "waterDistance"       -> stringResource(Res.string.location_camping_water_distance)
        "sittingPossibility"  -> stringResource(Res.string.location_camping_sitting_possibility)
        "grillPossibility"    -> stringResource(Res.string.location_camping_grill_possibility)

        // Swimming
        "indoor"             -> stringResource(Res.string.location_swimming_indoor)
        "jumpSpot"            -> stringResource(Res.string.location_swimming_jump_spot)
        "price"              -> stringResource(Res.string.location_swimming_price)

        // Climbingspot
        "viaFerrata"         -> stringResource(Res.string.location_climbingspot_via_ferrata)
        "outdoor"            -> stringResource(Res.string.location_climbingspot_outdoor)

        // Volleyball
        "goodNet"           -> stringResource(Res.string.location_volleyball_good_net)
        "goodField"         -> stringResource(Res.string.location_volleyball_good_field)
        "outdoor"           -> stringResource(Res.string.location_volleyball_outdoor)

        // Outdoor Fitness
        "shadow"            -> stringResource(Res.string.location_outdoor_fitness_shadow)

        // Table Tennis
        "private"           -> stringResource(Res.string.location_table_tennis_private)

        // Tennis
        "paddle"            -> stringResource(Res.string.location_tennis_paddle)

        // Sightseeing & Party
        "entryFee"          -> stringResource(Res.string.location_sightseeing_entry_fee)

        // Food
        "allYouCanEat"      -> stringResource(Res.string.location_food_all_you_can_eat)
        "kebabPrice"        -> stringResource(Res.string.location_food_kebab_price)
        "margaritaPrice"    -> stringResource(Res.string.location_food_margarita_price)
        "cheeseburgerPrice" -> stringResource(Res.string.location_food_cheeseburger_price)
        "beerPrice"         -> stringResource(Res.string.location_food_beer_price)
        "cuisine"           -> stringResource(Res.string.location_food_cuisine)
        "outdoorSeating"    -> stringResource(Res.string.location_food_cafe_bakery_outdoor_seating)
        "alcohol"           -> stringResource(Res.string.location_food_cafe_bakery_alcohol)
        "coffee"            -> stringResource(Res.string.location_food_cafe_bakery_coffee)
        "breakfast"         -> stringResource(Res.string.location_food_cafe_bakery_breakfast)

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
    OFFROAD_MOTORCYCLE -> Res.string.location_type_offroad_motorcycle
    VIEWPOINT       -> Res.string.location_type_viewpoint
    CAMPING         -> Res.string.location_type_camping
    SWIMMING        -> Res.string.location_type_swimming
    CLIMBINGSPOT    -> Res.string.location_type_climbingspot
    VOLLEYBALL      -> Res.string.location_type_volleyball
    BICYCLE         -> Res.string.location_type_bicycle
    OUTDOOR_FITNESS -> Res.string.location_type_outdoor_fitness
    TABLE_TENNIS    -> Res.string.location_type_table_tennis
    TENNIS          -> Res.string.location_type_tennis
    SIGHTSEEING     -> Res.string.location_type_sightseeing
    PARTY           -> Res.string.location_type_party
    FOOD_KEBAB      -> Res.string.location_type_food_kebab
    FOOD_PIZZA      -> Res.string.location_type_food_pizza
    FOOD_BURGER     -> Res.string.location_type_food_burger
    FOOD_BEER       -> Res.string.location_type_food_beer
    FOOD_ASIAN      -> Res.string.location_type_food_asian
    FOOD_GREEK      -> Res.string.location_type_food_greek
    FOOD_CAFE_BAKERY -> Res.string.location_type_food_cafe_bakery
    FOOD_OTHER      -> Res.string.location_type_food_other
}

@Composable
fun LocationGroup.stringRes(): StringResource = when (this) {
    LocationGroup.DRIVING              -> Res.string.location_group_driving
    LocationGroup.NATURE_ACTIVITIES    -> Res.string.location_group_nature_activities
    LocationGroup.SPORT                -> Res.string.location_group_sport
    LocationGroup.SOCIAL_ENTERTAINMENT -> Res.string.location_group_social_entertainment
    LocationGroup.FOOD                 -> Res.string.location_group_food
}