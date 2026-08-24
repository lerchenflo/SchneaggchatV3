package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

/**
 * Identifies one attribute of one [LocationData] subtype. Namespaced by location type
 * (`<TYPE>_<ATTRIBUTE>`) so the same attribute name reused across types (e.g. "outdoor", "price",
 * "legal") can never collide.
 *
 * No string payload on purpose - the constant name itself is the only identifier, and it must match
 * the Kotlin property it points to 1:1 (`RADAR_SPEED_LIMIT` <-> `radarSpeedLimit`). See
 * [getValueByKey] / [withValueForKey] and [labelRes].
 */
enum class AttributeKey {
    // Radar
    RADAR_SPEED_LIMIT, RADAR_MOBILE, RADAR_RED_LIGHT,

    // Police
    POLICE_LAST_SEEN,

    // Mountain street
    MOUNTAIN_STREET_MAUT_FEE, MOUNTAIN_STREET_HEIGHT_LIMIT, MOUNTAIN_STREET_CLOSED_IN_WINTER,

    // Wheeliespot
    WHEELIESPOT_ONLY_ON_WEEKENDS,

    // Offroad motorcycle
    OFFROAD_MOTORCYCLE_LEGAL, OFFROAD_MOTORCYCLE_MOTOCROSS, OFFROAD_MOTORCYCLE_ENDURO,

    // Viewpoint
    VIEWPOINT_LIE_DOWN_FRIENDLY,

    // Camping
    CAMPING_OFFICIAL, CAMPING_WATER_DISTANCE, CAMPING_SITTING_POSSIBILITY, CAMPING_GRILL_POSSIBILITY,

    // Swimming
    SWIMMING_INDOOR, SWIMMING_JUMP_SPOT, SWIMMING_LIE_DOWN_FRIENDLY, SWIMMING_PRICE,

    // Climbingspot
    CLIMBINGSPOT_VIA_FERRATA, CLIMBINGSPOT_OUTDOOR, CLIMBINGSPOT_PRICE,

    // Volleyball
    VOLLEYBALL_GOOD_NET, VOLLEYBALL_GOOD_FIELD, VOLLEYBALL_OUTDOOR,

    // Bicycle
    BICYCLE_LEGAL, BICYCLE_DIFFICULTY, BICYCLE_UNDERGROUND_TYPE,

    // Outdoor fitness
    OUTDOOR_FITNESS_SHADOW,

    // Table tennis
    TABLE_TENNIS_PRIVATE,

    // Tennis
    TENNIS_PADDLE,

    // Sightseeing / party
    SIGHTSEEING_ENTRY_FEE, PARTY_ENTRY_FEE,

    // Wifi
    WIFI_SSID, WIFI_PASSWORD,

    // Food
    FOOD_KEBAB_PRICE,
    FOOD_PIZZA_MARGARITA_PRICE,
    FOOD_BURGER_CHEESEBURGER_PRICE,
    FOOD_BEER_PRICE,
    FOOD_ICE_SCOOP_PRICE,
    FOOD_CAFE_BAKERY_OUTDOOR_SEATING,
    FOOD_CAFE_BAKERY_ALCOHOL,
    FOOD_CAFE_BAKERY_COFFEE,
    FOOD_CAFE_BAKERY_BREAKFAST,
    FOOD_ASIAN_ALL_YOU_CAN_EAT,
    FOOD_OTHER_CUISINE,
}
