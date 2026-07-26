package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

fun LocationData.getValueByKey(key: String): AttributeValue? = when (this) {

    // Traffic & Hazards
    is LocationData.Radar -> when (key) {
        "speedLimit" -> speedLimit
        "mobile"     -> mobile
        "redLight"   -> redLight
        else         -> null
    }

    is LocationData.Police           -> when (key) {
        "lastSeen"         -> lastSeen
        else               -> null
    }

    // Rider Spots
    is LocationData.MountainStreet   -> when (key) {
        "mautFee"          -> mautFee
        "heightLimit"      -> heightLimit
        "closedInWinter"   -> closedInWinter
        else               -> null
    }
    is LocationData.Wheeliespot      -> when (key) {
        "onlyOnWeekends"   -> onlyOnWeekends
        else               -> null
    }
    is LocationData.OffroadMotorcycle -> when (key) {
        "legal"            -> legal
        "motocross"        -> motocross
        "enduro"           -> enduro
        else               -> null
    }
    is LocationData.Viewpoint        -> when (key) {
        "lieDownFriendly"  -> lieDownFriendly
        else               -> null
    }

    // Nature & Activities
    is LocationData.Camping          -> when (key) {
        "official"            -> official
        "waterDistance"        -> waterDistance
        "sittingPossibility"   -> sittingPossibility
        "grillPossibility"     -> grillPossibility
        else                  -> null
    }
    is LocationData.SwimmingLocation -> when (key) {
        "indoor"            -> indoor
        "jumpSpot"           -> jumpSpot
        "lieDownFriendly"   -> lieDownFriendly
        "price"             -> price
        else                -> null
    }
    is LocationData.Climbingspot -> when (key) {
        "viaFerrata"        -> viaFerrata
        "outdoor"           -> outdoor
        "price"             -> price
        else                -> null
    }

    // Sport
    is LocationData.Volleyball       -> when (key) {
        "goodNet"           -> goodNet
        "goodField"         -> goodField
        "outdoor"           -> outdoor
        else                -> null
    }
    is LocationData.Bicycle          -> when (key) {
        "legal"             -> legal
        "difficulty"        -> difficulty
        "undergroundType"   -> undergroundType
        else                -> null
    }
    is LocationData.OutdoorFitness   -> when (key) {
        "shadow"            -> shadow
        else                -> null
    }
    is LocationData.TableTennis      -> when (key) {
        "private"           -> `private`
        else                -> null
    }
    is LocationData.Tennis           -> when (key) {
        "paddle"            -> paddle
        else                -> null
    }

    // Social & Entertainment
    is LocationData.SightSeeing      -> when (key) {
        "entryFee"         -> entryFee
        else               -> null
    }
    is LocationData.PartyLocation    -> when (key) {
        "entryFee"         -> entryFee
        else               -> null
    }

    // Fast Food & Snacks
    is LocationData.FoodKebab        -> when (key) {
        "kebabPrice"       -> kebabPrice
        else               -> null
    }
    is LocationData.FoodPizza        -> when (key) {
        "margaritaPrice"   -> margaritaPrice
        else               -> null
    }
    is LocationData.FoodBurger       -> when (key) {
        "cheeseburgerPrice" -> cheeseburgerPrice
        else               -> null
    }
    is LocationData.FoodBeer         -> when (key) {
        "beerPrice"        -> beerPrice
        else               -> null
    }
    is LocationData.FoodCafeBakery   -> when (key) {
        "outdoorSeating"    -> outdoorSeating
        "alcohol"           -> alcohol
        "coffee"            -> coffee
        "breakfast"         -> breakfast
        else                -> null
    }

    // Restaurant
    is LocationData.FoodAsian        -> when (key) {
        "allYouCanEat"     -> allYouCanEat
        else               -> null
    }
    is LocationData.FoodGreek        -> null
    is LocationData.FoodOther        -> when (key) {
        "cuisine"          -> cuisine
        else               -> null
    }
}

fun LocationData.withValueForKey(key: String, value: AttributeValue): LocationData = when (this) {

    // Traffic & Hazards
    is LocationData.Radar -> when (key) {
        "speedLimit" -> copy(speedLimit = value)
        "mobile"     -> copy(mobile = value)
        "redLight"   -> copy(redLight = value)
        else         -> this
    }

    is LocationData.Police           -> when (key) {
        "lastSeen"          -> copy(lastSeen = value)
        else                -> this
    }

    // Rider Spots
    is LocationData.MountainStreet   -> when (key) {
        "mautFee"           -> copy(mautFee = value)
        "heightLimit"       -> copy(heightLimit = value)
        "closedInWinter"    -> copy(closedInWinter = value)
        else                -> this
    }
    is LocationData.Wheeliespot      -> when (key) {
        "onlyOnWeekends"    -> copy(onlyOnWeekends = value)
        else                -> this
    }
    is LocationData.OffroadMotorcycle -> when (key) {
        "legal"             -> copy(legal = value)
        "motocross"         -> copy(motocross = value)
        "enduro"            -> copy(enduro = value)
        else                -> this
    }
    is LocationData.Viewpoint        -> when (key) {
        "lieDownFriendly"   -> copy(lieDownFriendly = value)
        else                -> this
    }

    // Nature & Activities
    is LocationData.Camping          -> when (key) {
        "official"             -> copy(official = value)
        "waterDistance"        -> copy(waterDistance = value)
        "sittingPossibility"   -> copy(sittingPossibility = value)
        "grillPossibility"     -> copy(grillPossibility = value)
        else                   -> this
    }
    is LocationData.SwimmingLocation -> when (key) {
        "indoor"            -> copy(indoor = value)
        "jumpSpot"           -> copy(jumpSpot = value)
        "lieDownFriendly"   -> copy(lieDownFriendly = value)
        "price"             -> copy(price = value)
        else                -> this
    }
    is LocationData.Climbingspot -> when (key) {
        "viaFerrata"        -> copy(viaFerrata = value)
        "outdoor"           -> copy(outdoor = value)
        "price"             -> copy(price = value)
        else                -> this
    }

    // Sport
    is LocationData.Volleyball       -> when (key) {
        "goodNet"           -> copy(goodNet = value)
        "goodField"         -> copy(goodField = value)
        "outdoor"           -> copy(outdoor = value)
        else                -> this
    }
    is LocationData.Bicycle          -> when (key) {
        "legal"             -> copy(legal = value)
        "difficulty"        -> copy(difficulty = value)
        "undergroundType"   -> copy(undergroundType = value)
        else                -> this
    }
    is LocationData.OutdoorFitness   -> when (key) {
        "shadow"            -> copy(shadow = value)
        else                -> this
    }
    is LocationData.TableTennis      -> when (key) {
        "private"           -> copy(`private` = value)
        else                -> this
    }
    is LocationData.Tennis           -> when (key) {
        "paddle"            -> copy(paddle = value)
        else                -> this
    }

    // Social & Entertainment
    is LocationData.SightSeeing      -> when (key) {
        "entryFee"          -> copy(entryFee = value)
        else                -> this
    }
    is LocationData.PartyLocation    -> when (key) {
        "entryFee"          -> copy(entryFee = value)
        else                -> this
    }

    // Fast Food & Snacks
    is LocationData.FoodKebab        -> when (key) {
        "kebabPrice"        -> copy(kebabPrice = value)
        else                -> this
    }
    is LocationData.FoodPizza        -> when (key) {
        "margaritaPrice"    -> copy(margaritaPrice = value)
        else                -> this
    }
    is LocationData.FoodBurger       -> when (key) {
        "cheeseburgerPrice" -> copy(cheeseburgerPrice = value)
        else                -> this
    }
    is LocationData.FoodBeer         -> when (key) {
        "beerPrice"         -> copy(beerPrice = value)
        else                -> this
    }
    is LocationData.FoodCafeBakery   -> when (key) {
        "outdoorSeating"    -> copy(outdoorSeating = value)
        "alcohol"           -> copy(alcohol = value)
        "coffee"            -> copy(coffee = value)
        "breakfast"         -> copy(breakfast = value)
        else                -> this
    }

    // Restaurant
    is LocationData.FoodAsian        -> when (key) {
        "allYouCanEat"      -> copy(allYouCanEat = value)
        else                -> this
    }
    is LocationData.FoodGreek        -> this
    is LocationData.FoodOther        -> when (key) {
        "cuisine"           -> copy(cuisine = value)
        else                -> this
    }
}