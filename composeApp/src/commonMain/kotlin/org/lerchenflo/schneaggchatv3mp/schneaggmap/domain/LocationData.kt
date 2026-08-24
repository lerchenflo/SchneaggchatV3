package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
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
 *    - @SerialName annotation with snake_case name (the persisted/wire type discriminator)
 *    - Properties with AttributeValue types (nullable if optional), named
 *      lowerCamelCase(<TYPE>_<ATTRIBUTE>) to match their AttributeKey 1:1
 *    - Override val locationtype
 *    - Getter properties for each field (e.g., val fieldNameValue get() = field?.asType)
 *    - Override fun schema() returning list of AttributeDefinitions, keyed by AttributeKey
 * 4. Add the new type to toSimpleLocationData() function
 * 5. Add an AttributeKey entry per attribute in AttributeKey.kt, then wire it into
 *    AttributeKey.labelRes() below - the compiler enforces this (exhaustive `when`)
 * 6. Add the type to LocationType.stringRes() function
 * 7. Add string entries in strings.xml:
 *    - location_type_[name] for the type name
 *    - location_[type]_[attribute] for each attribute label
 * 8. Add icon mapping in SchneaggmapScreen.kt with TODO comment for proper icon
 * 9. Update GetSetValueForKey.kt - the compiler forces this too (exhaustive `when` over
 *    LocationData in both getValueByKey() and withValueForKey())
 *
 * SERVER SIDE (SchneaggchatV3server):
 * 1. Add the new type to @JsonSubTypes annotation in LocationData.kt
 * 2. Add the data class in LocationData sealed class with:
 *    - @TypeAlias annotation with snake_case name
 *    - Properties with AttributeValue types (nullable if optional), same names as the client
 *    - Override fun schema() returning list of AttributeDefinitions
 * 3. Add the new type to LocationDataWriteConverter in MongoConfig.kt
 * 4. Add an AttributeKey entry per attribute in model/AttributeKey.kt, then wire it into
 *    getValueByKey() in GetValueForKey.kt
 * 5. Update any service files that instantiate LocationData (e.g., SchneaggmapService.kt)
 *
 * IMPORTANT: Keep the serial/type-alias names ("radar", ...) and the property names
 * (lowerCamelCase, e.g. "radarSpeedLimit") consistent between client and server - property names
 * are also the Mongo field names and the wire JSON keys, so client and server must match exactly.
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
    WIFI,

    // Food
    FOOD_KEBAB,
    FOOD_PIZZA,
    FOOD_BURGER,
    FOOD_BEER,
    FOOD_ICE,
    FOOD_ASIAN,
    FOOD_GREEK,
    FOOD_CAFE_BAKERY,
    FOOD_OTHER,
}


enum class LocationGroup(val types: List<LocationType>) {
    DRIVING(listOf(RADAR, POLICE, MOUNTAIN_STREET, WHEELIESPOT, OFFROAD_MOTORCYCLE)),
    NATURE_ACTIVITIES(listOf(SIGHTSEEING, VIEWPOINT, CAMPING, SWIMMING, CLIMBINGSPOT)),
    SPORT(listOf(VOLLEYBALL, BICYCLE, OUTDOOR_FITNESS, TABLE_TENNIS, TENNIS)),
    SOCIAL_ENTERTAINMENT(listOf(PARTY, WIFI)),
    FOOD(listOf(FOOD_KEBAB, FOOD_PIZZA, FOOD_BURGER, FOOD_BEER, FOOD_ICE, FOOD_ASIAN, FOOD_GREEK, FOOD_CAFE_BAKERY, FOOD_OTHER)),
}


@Serializable
sealed class LocationData {

    abstract fun schema(): List<AttributeDefinition>
    abstract val locationtype: LocationType

    // Traffic & Hazards

    @Serializable
    @SerialName("radar")
    data class Radar(
        val radarSpeedLimit: AttributeValue,
        val radarMobile: AttributeValue?,
        val radarRedLight: AttributeValue,
    ) : LocationData() {
        override val locationtype = RADAR

        val radarSpeedLimitValue get() = radarSpeedLimit.asInt
        val radarMobileValue     get() = radarMobile?.asBool
        val radarRedLightValue   get() = radarRedLight.asBool

        override fun schema() = listOf(
            AttributeDefinition.IntDef(key = AttributeKey.RADAR_SPEED_LIMIT, required = true, min = 0),
            AttributeDefinition.BoolDef(key = AttributeKey.RADAR_MOBILE,    required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.RADAR_RED_LIGHT,  required = true),
        )
    }

    @Serializable
    @SerialName("police")
    data class Police(
        val policeLastSeen: AttributeValue?,
    ) : LocationData() {
        override val locationtype = POLICE

        val policeLastSeenValue get() = policeLastSeen?.asLong

        override fun schema() = listOf(
            AttributeDefinition.LongDef(key = AttributeKey.POLICE_LAST_SEEN, required = false),
        )
    }


    // Rider Spots

    @Serializable
    @SerialName("mountain_street")
    data class MountainStreet(
        val mountainStreetMautFee: AttributeValue?,
        val mountainStreetHeightLimit: AttributeValue?,
        val mountainStreetClosedInWinter: AttributeValue?,
    ) : LocationData() {
        override val locationtype = MOUNTAIN_STREET

        val mountainStreetMautFeeValue        get() = mountainStreetMautFee?.asDouble
        val mountainStreetHeightLimitValue    get() = mountainStreetHeightLimit?.asDouble
        val mountainStreetClosedInWinterValue get() = mountainStreetClosedInWinter?.asBool

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.MOUNTAIN_STREET_MAUT_FEE,        required = false, min = 0.0),
            AttributeDefinition.DoubleDef(key = AttributeKey.MOUNTAIN_STREET_HEIGHT_LIMIT,    required = false, min = 0.0),
            AttributeDefinition.BoolDef  (key = AttributeKey.MOUNTAIN_STREET_CLOSED_IN_WINTER, required = false),
        )
    }

    @Serializable
    @SerialName("wheeliespot")
    data class Wheeliespot(
        val wheeliespotOnlyOnWeekends: AttributeValue?,
    ) : LocationData() {
        override val locationtype = WHEELIESPOT

        val wheeliespotOnlyOnWeekendsValue get() = wheeliespotOnlyOnWeekends?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.WHEELIESPOT_ONLY_ON_WEEKENDS, required = false),
        )
    }

    @Serializable
    @SerialName("offroad_motorcycle")
    data class OffroadMotorcycle(
        val offroadMotorcycleLegal: AttributeValue,
        val offroadMotorcycleMotocross: AttributeValue? = null,
        val offroadMotorcycleEnduro: AttributeValue? = null,
    ) : LocationData() {
        override val locationtype = OFFROAD_MOTORCYCLE

        val offroadMotorcycleLegalValue     get() = offroadMotorcycleLegal.asBool
        val offroadMotorcycleMotocrossValue get() = offroadMotorcycleMotocross?.asBool
        val offroadMotorcycleEnduroValue    get() = offroadMotorcycleEnduro?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.OFFROAD_MOTORCYCLE_LEGAL,     required = true),
            AttributeDefinition.BoolDef(key = AttributeKey.OFFROAD_MOTORCYCLE_MOTOCROSS, required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.OFFROAD_MOTORCYCLE_ENDURO,    required = false),
        )
    }

    @Serializable
    @SerialName("viewpoint")
    data class Viewpoint(
        //Defaulted so map entries cached before this attribute existed still deserialize
        val viewpointLieDownFriendly: AttributeValue? = null,
    ) : LocationData() {
        override val locationtype = VIEWPOINT

        val viewpointLieDownFriendlyValue get() = viewpointLieDownFriendly?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.VIEWPOINT_LIE_DOWN_FRIENDLY, required = false),
        )
    }


    // Nature & Activities

    @Serializable
    @SerialName("camping")
    data class Camping(
        val campingOfficial: AttributeValue,
        val campingWaterDistance: AttributeValue?,
        val campingSittingPossibility: AttributeValue?,
        val campingGrillPossibility: AttributeValue?,
    ) : LocationData() {
        override val locationtype = CAMPING

        val campingOfficialValue           get() = campingOfficial.asBool
        val campingWaterDistanceValue       get() = campingWaterDistance?.asInt
        val campingSittingPossibilityValue get() = campingSittingPossibility?.asBool
        val campingGrillPossibilityValue   get() = campingGrillPossibility?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.CAMPING_OFFICIAL,           required = true),
            AttributeDefinition.IntDef (key = AttributeKey.CAMPING_WATER_DISTANCE,      required = false, min = 0),
            AttributeDefinition.BoolDef(key = AttributeKey.CAMPING_SITTING_POSSIBILITY, required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.CAMPING_GRILL_POSSIBILITY,   required = false),
        )
    }

    @Serializable
    @SerialName("swimming")
    data class SwimmingLocation(
        val swimmingIndoor: AttributeValue?,
        val swimmingJumpSpot: AttributeValue?,
        //Defaulted so map entries cached before this attribute existed still deserialize
        val swimmingLieDownFriendly: AttributeValue? = null,
        val swimmingPrice: AttributeValue? = null,
    ) : LocationData() {
        override val locationtype = SWIMMING

        val swimmingIndoorValue          get() = swimmingIndoor?.asBool
        val swimmingJumpSpotValue        get() = swimmingJumpSpot?.asBool
        val swimmingLieDownFriendlyValue get() = swimmingLieDownFriendly?.asBool
        val swimmingPriceValue           get() = swimmingPrice?.asInt

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.SWIMMING_INDOOR,          required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.SWIMMING_JUMP_SPOT,        required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.SWIMMING_LIE_DOWN_FRIENDLY, required = false),
            AttributeDefinition.IntDef (key = AttributeKey.SWIMMING_PRICE,           required = false, min = 0),
        )
    }

    @Serializable
    @SerialName("climbingspot")
    data class Climbingspot(
        val climbingspotViaFerrata: AttributeValue?,
        val climbingspotOutdoor: AttributeValue?,
        val climbingspotPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = CLIMBINGSPOT

        val climbingspotViaFerrataValue get() = climbingspotViaFerrata?.asBool
        val climbingspotOutdoorValue    get() = climbingspotOutdoor?.asBool
        val climbingspotPriceValue      get() = climbingspotPrice?.asInt

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.CLIMBINGSPOT_VIA_FERRATA, required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.CLIMBINGSPOT_OUTDOOR,     required = false),
            AttributeDefinition.IntDef (key = AttributeKey.CLIMBINGSPOT_PRICE,       required = false, min = 0),
        )
    }


    // Sport

    @Serializable
    @SerialName("volleyball")
    data class Volleyball(
        val volleyballGoodNet: AttributeValue?,
        val volleyballGoodField: AttributeValue?,
        val volleyballOutdoor: AttributeValue?,
    ) : LocationData() {
        override val locationtype = VOLLEYBALL

        val volleyballGoodNetValue   get() = volleyballGoodNet?.asBool
        val volleyballGoodFieldValue get() = volleyballGoodField?.asBool
        val volleyballOutdoorValue   get() = volleyballOutdoor?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.VOLLEYBALL_GOOD_NET,   required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.VOLLEYBALL_GOOD_FIELD, required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.VOLLEYBALL_OUTDOOR,    required = false),
        )
    }

    @Serializable
    @SerialName("bicycle")
    data class Bicycle(
        val bicycleLegal: AttributeValue,
        val bicycleDifficulty: AttributeValue,
        val bicycleUndergroundType: AttributeValue?,
    ) : LocationData() {
        override val locationtype = BICYCLE

        val bicycleLegalValue           get() = bicycleLegal.asBool
        val bicycleDifficultyValue      get() = bicycleDifficulty.asInt
        val bicycleUndergroundTypeValue get() = bicycleUndergroundType?.asString

        override fun schema() = listOf(
            AttributeDefinition.BoolDef  (key = AttributeKey.BICYCLE_LEGAL,            required = true),
            AttributeDefinition.IntDef   (key = AttributeKey.BICYCLE_DIFFICULTY,       required = true, min = 1, max = 10),
            AttributeDefinition.StringDef(key = AttributeKey.BICYCLE_UNDERGROUND_TYPE, required = false),
        )
    }

    @Serializable
    @SerialName("outdoor_fitness")
    data class OutdoorFitness(
        val outdoorFitnessShadow: AttributeValue?,
    ) : LocationData() {
        override val locationtype = OUTDOOR_FITNESS

        val outdoorFitnessShadowValue get() = outdoorFitnessShadow?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.OUTDOOR_FITNESS_SHADOW, required = false),
        )
    }

    @Serializable
    @SerialName("table_tennis")
    data class TableTennis(
        val tableTennisPrivate: AttributeValue?,
    ) : LocationData() {
        override val locationtype = TABLE_TENNIS

        val tableTennisPrivateValue get() = tableTennisPrivate?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.TABLE_TENNIS_PRIVATE, required = false),
        )
    }

    @Serializable
    @SerialName("tennis")
    data class Tennis(
        val tennisPaddle: AttributeValue?,
    ) : LocationData() {
        override val locationtype = TENNIS

        val tennisPaddleValue get() = tennisPaddle?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.TENNIS_PADDLE, required = false),
        )
    }


    // Social & Entertainment

    @Serializable
    @SerialName("sightseeing")
    data class SightSeeing(
        val sightseeingEntryFee: AttributeValue?,
    ) : LocationData() {
        override val locationtype = SIGHTSEEING

        val sightseeingEntryFeeValue get() = sightseeingEntryFee?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.SIGHTSEEING_ENTRY_FEE, required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("party")
    data class PartyLocation(
        val partyEntryFee: AttributeValue?,
    ) : LocationData() {
        override val locationtype = PARTY

        val partyEntryFeeValue get() = partyEntryFee?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.PARTY_ENTRY_FEE, required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("wifi")
    data class Wifi(
        val wifiSsid: AttributeValue? = null,
        val wifiPassword: AttributeValue? = null,
    ) : LocationData() {
        override val locationtype = WIFI

        val wifiSsidValue     get() = wifiSsid?.asString
        val wifiPasswordValue get() = wifiPassword?.asString

        override fun schema() = listOf(
            AttributeDefinition.StringDef(key = AttributeKey.WIFI_SSID,     required = false),
            AttributeDefinition.StringDef(key = AttributeKey.WIFI_PASSWORD, required = false),
        )
    }


    // Fast Food & Snacks

    @Serializable
    @SerialName("food_kebab")
    data class FoodKebab(
        val foodKebabPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_KEBAB

        val foodKebabPriceValue get() = foodKebabPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.FOOD_KEBAB_PRICE, required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_pizza")
    data class FoodPizza(
        val foodPizzaMargaritaPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_PIZZA

        val foodPizzaMargaritaPriceValue get() = foodPizzaMargaritaPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.FOOD_PIZZA_MARGARITA_PRICE, required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_burger")
    data class FoodBurger(
        val foodBurgerCheeseburgerPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_BURGER

        val foodBurgerCheeseburgerPriceValue get() = foodBurgerCheeseburgerPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.FOOD_BURGER_CHEESEBURGER_PRICE, required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_beer")
    data class FoodBeer(
        val foodBeerPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_BEER

        val foodBeerPriceValue get() = foodBeerPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.FOOD_BEER_PRICE, required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_ice")
    data class FoodIce(
        val foodIceScoopPrice: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_ICE

        val foodIceScoopPriceValue get() = foodIceScoopPrice?.asDouble

        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = AttributeKey.FOOD_ICE_SCOOP_PRICE, required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("food_cafe_bakery")
    data class FoodCafeBakery(
        val foodCafeBakeryOutdoorSeating: AttributeValue?,
        val foodCafeBakeryAlcohol: AttributeValue?,
        val foodCafeBakeryCoffee: AttributeValue?,
        val foodCafeBakeryBreakfast: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_CAFE_BAKERY

        val foodCafeBakeryOutdoorSeatingValue get() = foodCafeBakeryOutdoorSeating?.asBool
        val foodCafeBakeryAlcoholValue        get() = foodCafeBakeryAlcohol?.asBool
        val foodCafeBakeryCoffeeValue         get() = foodCafeBakeryCoffee?.asBool
        val foodCafeBakeryBreakfastValue      get() = foodCafeBakeryBreakfast?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.FOOD_CAFE_BAKERY_OUTDOOR_SEATING, required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.FOOD_CAFE_BAKERY_ALCOHOL,        required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.FOOD_CAFE_BAKERY_COFFEE,          required = false),
            AttributeDefinition.BoolDef(key = AttributeKey.FOOD_CAFE_BAKERY_BREAKFAST,       required = false),
        )
    }


    // Restaurant

    @Serializable
    @SerialName("food_asian")
    data class FoodAsian(
        val foodAsianAllYouCanEat: AttributeValue?,
    ) : LocationData() {
        override val locationtype = FOOD_ASIAN

        val foodAsianAllYouCanEatValue get() = foodAsianAllYouCanEat?.asBool

        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = AttributeKey.FOOD_ASIAN_ALL_YOU_CAN_EAT, required = false),
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
        val foodOtherCuisine: AttributeValue,
    ) : LocationData() {
        override val locationtype = FOOD_OTHER

        val foodOtherCuisineValue get() = foodOtherCuisine.asString

        override fun schema() = listOf(
            AttributeDefinition.StringDef(key = AttributeKey.FOOD_OTHER_CUISINE, required = true),
        )
    }
}


fun LocationType.toSimpleLocationData(): LocationData = when (this) {
    RADAR           -> LocationData.Radar(radarSpeedLimit = AttributeValue.IntValue(0), radarMobile = AttributeValue.BoolValue(false), radarRedLight = AttributeValue.BoolValue(false)
    )
    POLICE          -> LocationData.Police(policeLastSeen = null)
    MOUNTAIN_STREET -> LocationData.MountainStreet(null, null, null)
    WHEELIESPOT     -> LocationData.Wheeliespot(wheeliespotOnlyOnWeekends = null)
    OFFROAD_MOTORCYCLE -> LocationData.OffroadMotorcycle(offroadMotorcycleLegal = AttributeValue.BoolValue(false), offroadMotorcycleMotocross = null, offroadMotorcycleEnduro = null)
    VIEWPOINT       -> LocationData.Viewpoint(viewpointLieDownFriendly = null)
    CAMPING         -> LocationData.Camping(campingOfficial = AttributeValue.BoolValue(true), campingWaterDistance = null, campingSittingPossibility = null, campingGrillPossibility = null)
    SWIMMING        -> LocationData.SwimmingLocation(swimmingIndoor = null, swimmingJumpSpot = null, swimmingLieDownFriendly = null, swimmingPrice = null)
    CLIMBINGSPOT    -> LocationData.Climbingspot(climbingspotViaFerrata = null, climbingspotOutdoor = null, climbingspotPrice = null)
    VOLLEYBALL      -> LocationData.Volleyball(volleyballGoodNet = null, volleyballGoodField = null, volleyballOutdoor = null)
    BICYCLE         -> LocationData.Bicycle(bicycleLegal = AttributeValue.BoolValue(true), bicycleDifficulty = AttributeValue.IntValue(1), bicycleUndergroundType = null)
    OUTDOOR_FITNESS -> LocationData.OutdoorFitness(outdoorFitnessShadow = null)
    TABLE_TENNIS    -> LocationData.TableTennis(tableTennisPrivate = null)
    TENNIS          -> LocationData.Tennis(tennisPaddle = null)
    SIGHTSEEING     -> LocationData.SightSeeing(sightseeingEntryFee = null)
    PARTY           -> LocationData.PartyLocation(partyEntryFee = null)
    WIFI            -> LocationData.Wifi(wifiSsid = null, wifiPassword = null)
    FOOD_KEBAB      -> LocationData.FoodKebab(foodKebabPrice = null)
    FOOD_PIZZA      -> LocationData.FoodPizza(foodPizzaMargaritaPrice = null)
    FOOD_BURGER     -> LocationData.FoodBurger(foodBurgerCheeseburgerPrice = null)
    FOOD_BEER       -> LocationData.FoodBeer(foodBeerPrice = null)
    FOOD_ICE        -> LocationData.FoodIce(foodIceScoopPrice = null)
    FOOD_ASIAN      -> LocationData.FoodAsian(foodAsianAllYouCanEat = null)
    FOOD_GREEK      -> LocationData.FoodGreek()
    FOOD_CAFE_BAKERY -> LocationData.FoodCafeBakery(foodCafeBakeryOutdoorSeating = null, foodCafeBakeryAlcohol = null, foodCafeBakeryCoffee = null, foodCafeBakeryBreakfast = null)
    FOOD_OTHER      -> LocationData.FoodOther(foodOtherCuisine = AttributeValue.StringValue(""))
}


/**
 * Exhaustive - a missing entry is a compile error, unlike the old string `when` this replaces
 * (which had "outdoor" declared twice: Climbingspot's branch shadowed Volleyball's, so
 * VOLLEYBALL_OUTDOOR silently rendered Climbingspot's label until this enum made the collision
 * impossible to express).
 */
fun AttributeKey.labelRes(): StringResource = when (this) {
    // Radar
    AttributeKey.RADAR_SPEED_LIMIT -> Res.string.location_radar_speed_limit
    AttributeKey.RADAR_MOBILE      -> Res.string.location_radar_mobile
    AttributeKey.RADAR_RED_LIGHT   -> Res.string.location_radar_red_light

    // Police
    AttributeKey.POLICE_LAST_SEEN -> Res.string.location_police_last_seen

    // Mountain Street
    AttributeKey.MOUNTAIN_STREET_MAUT_FEE        -> Res.string.location_street_maut_fee
    AttributeKey.MOUNTAIN_STREET_HEIGHT_LIMIT    -> Res.string.location_street_height_limit
    AttributeKey.MOUNTAIN_STREET_CLOSED_IN_WINTER -> Res.string.location_street_closed_in_winter

    // Wheelie Spot
    AttributeKey.WHEELIESPOT_ONLY_ON_WEEKENDS -> Res.string.location_wheeliespot_only_on_weekends

    // Offroad Motorcycle
    AttributeKey.OFFROAD_MOTORCYCLE_LEGAL     -> Res.string.location_legal
    AttributeKey.OFFROAD_MOTORCYCLE_MOTOCROSS -> Res.string.location_offroad_motocross
    AttributeKey.OFFROAD_MOTORCYCLE_ENDURO    -> Res.string.location_offroad_enduro

    // Bicycle
    AttributeKey.BICYCLE_LEGAL            -> Res.string.location_legal
    AttributeKey.BICYCLE_DIFFICULTY       -> Res.string.location_bicycle_difficulty
    AttributeKey.BICYCLE_UNDERGROUND_TYPE -> Res.string.location_bicycle_underground_type

    // Viewpoint & Swimming
    AttributeKey.VIEWPOINT_LIE_DOWN_FRIENDLY -> Res.string.location_lie_down_friendly
    AttributeKey.SWIMMING_LIE_DOWN_FRIENDLY  -> Res.string.location_lie_down_friendly

    // Camping
    AttributeKey.CAMPING_OFFICIAL           -> Res.string.location_camping_official
    AttributeKey.CAMPING_WATER_DISTANCE      -> Res.string.location_camping_water_distance
    AttributeKey.CAMPING_SITTING_POSSIBILITY -> Res.string.location_camping_sitting_possibility
    AttributeKey.CAMPING_GRILL_POSSIBILITY   -> Res.string.location_camping_grill_possibility

    // Swimming
    AttributeKey.SWIMMING_INDOOR    -> Res.string.location_swimming_indoor
    AttributeKey.SWIMMING_JUMP_SPOT -> Res.string.location_swimming_jump_spot
    AttributeKey.SWIMMING_PRICE     -> Res.string.location_swimming_price

    // Climbingspot
    AttributeKey.CLIMBINGSPOT_VIA_FERRATA -> Res.string.location_climbingspot_via_ferrata
    AttributeKey.CLIMBINGSPOT_OUTDOOR     -> Res.string.location_climbingspot_outdoor
    AttributeKey.CLIMBINGSPOT_PRICE       -> Res.string.location_swimming_price

    // Volleyball
    AttributeKey.VOLLEYBALL_GOOD_NET   -> Res.string.location_volleyball_good_net
    AttributeKey.VOLLEYBALL_GOOD_FIELD -> Res.string.location_volleyball_good_field
    AttributeKey.VOLLEYBALL_OUTDOOR    -> Res.string.location_volleyball_outdoor

    // Outdoor Fitness
    AttributeKey.OUTDOOR_FITNESS_SHADOW -> Res.string.location_outdoor_fitness_shadow

    // Table Tennis
    AttributeKey.TABLE_TENNIS_PRIVATE -> Res.string.location_table_tennis_private

    // Tennis
    AttributeKey.TENNIS_PADDLE -> Res.string.location_tennis_paddle

    // Sightseeing & Party
    AttributeKey.SIGHTSEEING_ENTRY_FEE -> Res.string.location_sightseeing_entry_fee
    AttributeKey.PARTY_ENTRY_FEE       -> Res.string.location_sightseeing_entry_fee

    // Food
    AttributeKey.FOOD_ASIAN_ALL_YOU_CAN_EAT        -> Res.string.location_food_all_you_can_eat
    AttributeKey.FOOD_KEBAB_PRICE                  -> Res.string.location_food_kebab_price
    AttributeKey.FOOD_PIZZA_MARGARITA_PRICE        -> Res.string.location_food_margarita_price
    AttributeKey.FOOD_BURGER_CHEESEBURGER_PRICE    -> Res.string.location_food_cheeseburger_price
    AttributeKey.FOOD_BEER_PRICE                   -> Res.string.location_food_beer_price
    AttributeKey.FOOD_ICE_SCOOP_PRICE              -> Res.string.location_food_ice_scoop_price
    AttributeKey.FOOD_OTHER_CUISINE                -> Res.string.location_food_cuisine
    AttributeKey.FOOD_CAFE_BAKERY_OUTDOOR_SEATING  -> Res.string.location_food_cafe_bakery_outdoor_seating
    AttributeKey.FOOD_CAFE_BAKERY_ALCOHOL          -> Res.string.location_food_cafe_bakery_alcohol
    AttributeKey.FOOD_CAFE_BAKERY_COFFEE           -> Res.string.location_food_cafe_bakery_coffee
    AttributeKey.FOOD_CAFE_BAKERY_BREAKFAST        -> Res.string.location_food_cafe_bakery_breakfast

    // WiFi
    AttributeKey.WIFI_SSID     -> Res.string.location_wifi_ssid
    AttributeKey.WIFI_PASSWORD -> Res.string.location_wifi_password
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
    WIFI            -> Res.string.location_type_wifi
    FOOD_KEBAB      -> Res.string.location_type_food_kebab
    FOOD_PIZZA      -> Res.string.location_type_food_pizza
    FOOD_BURGER     -> Res.string.location_type_food_burger
    FOOD_BEER       -> Res.string.location_type_food_beer
    FOOD_ICE        -> Res.string.location_type_food_ice
    FOOD_ASIAN      -> Res.string.location_type_food_asian
    FOOD_GREEK      -> Res.string.location_type_food_greek
    FOOD_CAFE_BAKERY -> Res.string.location_type_food_cafe_bakery
    FOOD_OTHER      -> Res.string.location_type_food_other
}


fun LocationType.drawableRes(): DrawableResource = when (this) {
    RADAR -> Res.drawable.icon_radar_variant
    CAMPING -> Res.drawable.icon_camping
    SIGHTSEEING -> Res.drawable.icon_sightseeing
    SWIMMING -> Res.drawable.icon_badespot
    CLIMBINGSPOT -> Res.drawable.icon_badespot // TODO: Add proper icon for climbingspot
    PARTY -> Res.drawable.icon_partylocation
    WIFI -> Res.drawable.icon_wifi

    POLICE -> Res.drawable.icon_police
    MOUNTAIN_STREET -> Res.drawable.icon_street
    WHEELIESPOT -> Res.drawable.icon_wheeliespot
    OFFROAD_MOTORCYCLE -> Res.drawable.icon_offroad_motorcycle
    VIEWPOINT -> Res.drawable.icon_viewpoint
    FOOD_KEBAB -> Res.drawable.icon_doener
    FOOD_PIZZA -> Res.drawable.icon_pizza
    FOOD_BURGER -> Res.drawable.icon_burger
    FOOD_BEER -> Res.drawable.icon_beer
    FOOD_ICE -> Res.drawable.icon_ice
    FOOD_ASIAN -> Res.drawable.icon_chinese_food
    FOOD_GREEK -> Res.drawable.icon_food_greek
    FOOD_CAFE_BAKERY -> Res.drawable.icon_food // TODO: Add proper icon for cafe_bakery
    FOOD_OTHER -> Res.drawable.icon_food

    VOLLEYBALL -> Res.drawable.icon_volleyball
    BICYCLE -> Res.drawable.icon_bicycle
    OUTDOOR_FITNESS -> Res.drawable.icon_outdoor_fitness
    TABLE_TENNIS -> Res.drawable.icon_table_tennis
    TENNIS -> Res.drawable.icon_tennis
}

@Composable
fun LocationGroup.stringRes(): StringResource = when (this) {
    LocationGroup.DRIVING              -> Res.string.location_group_driving
    LocationGroup.NATURE_ACTIVITIES    -> Res.string.location_group_nature_activities
    LocationGroup.SPORT                -> Res.string.location_group_sport
    LocationGroup.SOCIAL_ENTERTAINMENT -> Res.string.location_group_social_entertainment
    LocationGroup.FOOD                 -> Res.string.location_group_food
}
