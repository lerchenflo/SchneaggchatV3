package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData

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
    is LocationData.Viewpoint        -> null

    // Nature & Activities
    is LocationData.Camping          -> when (key) {
        "official"         -> official
        else               -> null
    }
    is LocationData.SwimmingLocation -> when (key) {
        "indoor"           -> indoor
        else               -> null
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
    is LocationData.Viewpoint        -> this

    // Nature & Activities
    is LocationData.Camping          -> when (key) {
        "official"          -> copy(official = value)
        else                -> this
    }
    is LocationData.SwimmingLocation -> when (key) {
        "indoor"            -> copy(indoor = value)
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