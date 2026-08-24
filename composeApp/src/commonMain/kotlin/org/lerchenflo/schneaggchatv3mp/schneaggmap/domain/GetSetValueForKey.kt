package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

fun LocationData.getValueByKey(key: AttributeKey): AttributeValue? = when (this) {

    // Traffic & Hazards
    is LocationData.Radar -> when (key) {
        AttributeKey.RADAR_SPEED_LIMIT -> radarSpeedLimit
        AttributeKey.RADAR_MOBILE      -> radarMobile
        AttributeKey.RADAR_RED_LIGHT   -> radarRedLight
        else                           -> null
    }

    is LocationData.Police -> when (key) {
        AttributeKey.POLICE_LAST_SEEN -> policeLastSeen
        else                          -> null
    }

    // Rider Spots
    is LocationData.MountainStreet -> when (key) {
        AttributeKey.MOUNTAIN_STREET_MAUT_FEE         -> mountainStreetMautFee
        AttributeKey.MOUNTAIN_STREET_HEIGHT_LIMIT     -> mountainStreetHeightLimit
        AttributeKey.MOUNTAIN_STREET_CLOSED_IN_WINTER -> mountainStreetClosedInWinter
        else                                          -> null
    }
    is LocationData.Wheeliespot -> when (key) {
        AttributeKey.WHEELIESPOT_ONLY_ON_WEEKENDS -> wheeliespotOnlyOnWeekends
        else                                       -> null
    }
    is LocationData.OffroadMotorcycle -> when (key) {
        AttributeKey.OFFROAD_MOTORCYCLE_LEGAL     -> offroadMotorcycleLegal
        AttributeKey.OFFROAD_MOTORCYCLE_MOTOCROSS -> offroadMotorcycleMotocross
        AttributeKey.OFFROAD_MOTORCYCLE_ENDURO    -> offroadMotorcycleEnduro
        else                                       -> null
    }
    is LocationData.Viewpoint -> when (key) {
        AttributeKey.VIEWPOINT_LIE_DOWN_FRIENDLY -> viewpointLieDownFriendly
        else                                      -> null
    }

    // Nature & Activities
    is LocationData.Camping -> when (key) {
        AttributeKey.CAMPING_OFFICIAL            -> campingOfficial
        AttributeKey.CAMPING_WATER_DISTANCE       -> campingWaterDistance
        AttributeKey.CAMPING_SITTING_POSSIBILITY  -> campingSittingPossibility
        AttributeKey.CAMPING_GRILL_POSSIBILITY    -> campingGrillPossibility
        else                                      -> null
    }
    is LocationData.SwimmingLocation -> when (key) {
        AttributeKey.SWIMMING_INDOOR            -> swimmingIndoor
        AttributeKey.SWIMMING_JUMP_SPOT         -> swimmingJumpSpot
        AttributeKey.SWIMMING_LIE_DOWN_FRIENDLY -> swimmingLieDownFriendly
        AttributeKey.SWIMMING_PRICE             -> swimmingPrice
        else                                     -> null
    }
    is LocationData.Climbingspot -> when (key) {
        AttributeKey.CLIMBINGSPOT_VIA_FERRATA -> climbingspotViaFerrata
        AttributeKey.CLIMBINGSPOT_OUTDOOR     -> climbingspotOutdoor
        AttributeKey.CLIMBINGSPOT_PRICE       -> climbingspotPrice
        else                                   -> null
    }

    // Sport
    is LocationData.Volleyball -> when (key) {
        AttributeKey.VOLLEYBALL_GOOD_NET   -> volleyballGoodNet
        AttributeKey.VOLLEYBALL_GOOD_FIELD -> volleyballGoodField
        AttributeKey.VOLLEYBALL_OUTDOOR    -> volleyballOutdoor
        else                                -> null
    }
    is LocationData.Bicycle -> when (key) {
        AttributeKey.BICYCLE_LEGAL            -> bicycleLegal
        AttributeKey.BICYCLE_DIFFICULTY       -> bicycleDifficulty
        AttributeKey.BICYCLE_UNDERGROUND_TYPE -> bicycleUndergroundType
        else                                   -> null
    }
    is LocationData.OutdoorFitness -> when (key) {
        AttributeKey.OUTDOOR_FITNESS_SHADOW -> outdoorFitnessShadow
        else                                 -> null
    }
    is LocationData.TableTennis -> when (key) {
        AttributeKey.TABLE_TENNIS_PRIVATE -> tableTennisPrivate
        else                               -> null
    }
    is LocationData.Tennis -> when (key) {
        AttributeKey.TENNIS_PADDLE -> tennisPaddle
        else                        -> null
    }

    // Social & Entertainment
    is LocationData.SightSeeing -> when (key) {
        AttributeKey.SIGHTSEEING_ENTRY_FEE -> sightseeingEntryFee
        else                                -> null
    }
    is LocationData.PartyLocation -> when (key) {
        AttributeKey.PARTY_ENTRY_FEE -> partyEntryFee
        else                          -> null
    }
    is LocationData.Wifi -> when (key) {
        AttributeKey.WIFI_SSID     -> wifiSsid
        AttributeKey.WIFI_PASSWORD -> wifiPassword
        else                        -> null
    }

    // Fast Food & Snacks
    is LocationData.FoodKebab -> when (key) {
        AttributeKey.FOOD_KEBAB_PRICE -> foodKebabPrice
        else                           -> null
    }
    is LocationData.FoodPizza -> when (key) {
        AttributeKey.FOOD_PIZZA_MARGARITA_PRICE -> foodPizzaMargaritaPrice
        else                                     -> null
    }
    is LocationData.FoodBurger -> when (key) {
        AttributeKey.FOOD_BURGER_CHEESEBURGER_PRICE -> foodBurgerCheeseburgerPrice
        else                                          -> null
    }
    is LocationData.FoodBeer -> when (key) {
        AttributeKey.FOOD_BEER_PRICE -> foodBeerPrice
        else                          -> null
    }
    is LocationData.FoodIce -> when (key) {
        AttributeKey.FOOD_ICE_SCOOP_PRICE -> foodIceScoopPrice
        else                                -> null
    }
    is LocationData.FoodCafeBakery -> when (key) {
        AttributeKey.FOOD_CAFE_BAKERY_OUTDOOR_SEATING -> foodCafeBakeryOutdoorSeating
        AttributeKey.FOOD_CAFE_BAKERY_ALCOHOL         -> foodCafeBakeryAlcohol
        AttributeKey.FOOD_CAFE_BAKERY_COFFEE          -> foodCafeBakeryCoffee
        AttributeKey.FOOD_CAFE_BAKERY_BREAKFAST       -> foodCafeBakeryBreakfast
        else                                           -> null
    }

    // Restaurant
    is LocationData.FoodAsian -> when (key) {
        AttributeKey.FOOD_ASIAN_ALL_YOU_CAN_EAT -> foodAsianAllYouCanEat
        else                                     -> null
    }
    is LocationData.FoodGreek -> null
    is LocationData.FoodOther -> when (key) {
        AttributeKey.FOOD_OTHER_CUISINE -> foodOtherCuisine
        else                             -> null
    }
}

fun LocationData.withValueForKey(key: AttributeKey, value: AttributeValue): LocationData = when (this) {

    // Traffic & Hazards
    is LocationData.Radar -> when (key) {
        AttributeKey.RADAR_SPEED_LIMIT -> copy(radarSpeedLimit = value)
        AttributeKey.RADAR_MOBILE      -> copy(radarMobile = value)
        AttributeKey.RADAR_RED_LIGHT   -> copy(radarRedLight = value)
        else                           -> this
    }

    is LocationData.Police -> when (key) {
        AttributeKey.POLICE_LAST_SEEN -> copy(policeLastSeen = value)
        else                          -> this
    }

    // Rider Spots
    is LocationData.MountainStreet -> when (key) {
        AttributeKey.MOUNTAIN_STREET_MAUT_FEE         -> copy(mountainStreetMautFee = value)
        AttributeKey.MOUNTAIN_STREET_HEIGHT_LIMIT     -> copy(mountainStreetHeightLimit = value)
        AttributeKey.MOUNTAIN_STREET_CLOSED_IN_WINTER -> copy(mountainStreetClosedInWinter = value)
        else                                          -> this
    }
    is LocationData.Wheeliespot -> when (key) {
        AttributeKey.WHEELIESPOT_ONLY_ON_WEEKENDS -> copy(wheeliespotOnlyOnWeekends = value)
        else                                       -> this
    }
    is LocationData.OffroadMotorcycle -> when (key) {
        AttributeKey.OFFROAD_MOTORCYCLE_LEGAL     -> copy(offroadMotorcycleLegal = value)
        AttributeKey.OFFROAD_MOTORCYCLE_MOTOCROSS -> copy(offroadMotorcycleMotocross = value)
        AttributeKey.OFFROAD_MOTORCYCLE_ENDURO    -> copy(offroadMotorcycleEnduro = value)
        else                                       -> this
    }
    is LocationData.Viewpoint -> when (key) {
        AttributeKey.VIEWPOINT_LIE_DOWN_FRIENDLY -> copy(viewpointLieDownFriendly = value)
        else                                      -> this
    }

    // Nature & Activities
    is LocationData.Camping -> when (key) {
        AttributeKey.CAMPING_OFFICIAL            -> copy(campingOfficial = value)
        AttributeKey.CAMPING_WATER_DISTANCE       -> copy(campingWaterDistance = value)
        AttributeKey.CAMPING_SITTING_POSSIBILITY  -> copy(campingSittingPossibility = value)
        AttributeKey.CAMPING_GRILL_POSSIBILITY    -> copy(campingGrillPossibility = value)
        else                                      -> this
    }
    is LocationData.SwimmingLocation -> when (key) {
        AttributeKey.SWIMMING_INDOOR            -> copy(swimmingIndoor = value)
        AttributeKey.SWIMMING_JUMP_SPOT         -> copy(swimmingJumpSpot = value)
        AttributeKey.SWIMMING_LIE_DOWN_FRIENDLY -> copy(swimmingLieDownFriendly = value)
        AttributeKey.SWIMMING_PRICE             -> copy(swimmingPrice = value)
        else                                     -> this
    }
    is LocationData.Climbingspot -> when (key) {
        AttributeKey.CLIMBINGSPOT_VIA_FERRATA -> copy(climbingspotViaFerrata = value)
        AttributeKey.CLIMBINGSPOT_OUTDOOR     -> copy(climbingspotOutdoor = value)
        AttributeKey.CLIMBINGSPOT_PRICE       -> copy(climbingspotPrice = value)
        else                                   -> this
    }

    // Sport
    is LocationData.Volleyball -> when (key) {
        AttributeKey.VOLLEYBALL_GOOD_NET   -> copy(volleyballGoodNet = value)
        AttributeKey.VOLLEYBALL_GOOD_FIELD -> copy(volleyballGoodField = value)
        AttributeKey.VOLLEYBALL_OUTDOOR    -> copy(volleyballOutdoor = value)
        else                                -> this
    }
    is LocationData.Bicycle -> when (key) {
        AttributeKey.BICYCLE_LEGAL            -> copy(bicycleLegal = value)
        AttributeKey.BICYCLE_DIFFICULTY       -> copy(bicycleDifficulty = value)
        AttributeKey.BICYCLE_UNDERGROUND_TYPE -> copy(bicycleUndergroundType = value)
        else                                   -> this
    }
    is LocationData.OutdoorFitness -> when (key) {
        AttributeKey.OUTDOOR_FITNESS_SHADOW -> copy(outdoorFitnessShadow = value)
        else                                 -> this
    }
    is LocationData.TableTennis -> when (key) {
        AttributeKey.TABLE_TENNIS_PRIVATE -> copy(tableTennisPrivate = value)
        else                               -> this
    }
    is LocationData.Tennis -> when (key) {
        AttributeKey.TENNIS_PADDLE -> copy(tennisPaddle = value)
        else                        -> this
    }

    // Social & Entertainment
    is LocationData.SightSeeing -> when (key) {
        AttributeKey.SIGHTSEEING_ENTRY_FEE -> copy(sightseeingEntryFee = value)
        else                                -> this
    }
    is LocationData.PartyLocation -> when (key) {
        AttributeKey.PARTY_ENTRY_FEE -> copy(partyEntryFee = value)
        else                          -> this
    }
    is LocationData.Wifi -> when (key) {
        AttributeKey.WIFI_SSID     -> copy(wifiSsid = value)
        AttributeKey.WIFI_PASSWORD -> copy(wifiPassword = value)
        else                        -> this
    }

    // Fast Food & Snacks
    is LocationData.FoodKebab -> when (key) {
        AttributeKey.FOOD_KEBAB_PRICE -> copy(foodKebabPrice = value)
        else                           -> this
    }
    is LocationData.FoodPizza -> when (key) {
        AttributeKey.FOOD_PIZZA_MARGARITA_PRICE -> copy(foodPizzaMargaritaPrice = value)
        else                                     -> this
    }
    is LocationData.FoodBurger -> when (key) {
        AttributeKey.FOOD_BURGER_CHEESEBURGER_PRICE -> copy(foodBurgerCheeseburgerPrice = value)
        else                                          -> this
    }
    is LocationData.FoodBeer -> when (key) {
        AttributeKey.FOOD_BEER_PRICE -> copy(foodBeerPrice = value)
        else                          -> this
    }
    is LocationData.FoodIce -> when (key) {
        AttributeKey.FOOD_ICE_SCOOP_PRICE -> copy(foodIceScoopPrice = value)
        else                                -> this
    }
    is LocationData.FoodCafeBakery -> when (key) {
        AttributeKey.FOOD_CAFE_BAKERY_OUTDOOR_SEATING -> copy(foodCafeBakeryOutdoorSeating = value)
        AttributeKey.FOOD_CAFE_BAKERY_ALCOHOL         -> copy(foodCafeBakeryAlcohol = value)
        AttributeKey.FOOD_CAFE_BAKERY_COFFEE          -> copy(foodCafeBakeryCoffee = value)
        AttributeKey.FOOD_CAFE_BAKERY_BREAKFAST       -> copy(foodCafeBakeryBreakfast = value)
        else                                           -> this
    }

    // Restaurant
    is LocationData.FoodAsian -> when (key) {
        AttributeKey.FOOD_ASIAN_ALL_YOU_CAN_EAT -> copy(foodAsianAllYouCanEat = value)
        else                                     -> this
    }
    is LocationData.FoodGreek -> this
    is LocationData.FoodOther -> when (key) {
        AttributeKey.FOOD_OTHER_CUISINE -> copy(foodOtherCuisine = value)
        else                             -> this
    }
}
