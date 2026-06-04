package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData

fun LocationData.getValueByKey(key: String): AttributeValue? = when (this) {
    is LocationData.Camping         -> when (key) {
        "official"        -> official
        else              -> null
    }
    is LocationData.Radar -> when (key) {
        "radarType"  -> AttributeValue.EnumValue(radarType.value)
        "speedLimit" -> speedLimit
        else         -> null
    }

    is LocationData.Food -> when (key) {
        "foodType"    -> AttributeValue.EnumValue(foodType.value)
        "allYouCanEat" -> allYouCanEat
        else          -> null
    }
    is LocationData.PartyLocation   -> when (key) {
        "entryFee"        -> entryFee
        else              -> null
    }
    is LocationData.SightSeeing     -> when (key) {
        "entryFee"        -> entryFee
        else              -> null
    }
    is LocationData.Street          -> when (key) {
        "mautFee"         -> mautFee
        "heightLimit"     -> heightLimit
        "closedInWinter"  -> closedInWinter
        "wheeliesAllowed" -> wheeliesAllowed
        else              -> null
    }
    is LocationData.SwimmingLocation -> when (key) {
        "indoor"          -> indoor
        else              -> null
    }
}

fun LocationData.withValueForKey(key: String, value: AttributeValue): LocationData = when (this) {
    is LocationData.Camping         -> when (key) {
        "official"        -> copy(official = value as AttributeValue.BoolValue)
        else              -> this
    }
    is LocationData.Radar -> when (key) {
        "radarType"  -> copy(radarType = (value as AttributeValue.EnumValue))
        "speedLimit" -> copy(speedLimit = value as AttributeValue.IntValue)
        else         -> this
    }

    is LocationData.Food -> when (key) {
        "foodType"    -> copy(foodType = (value as AttributeValue.EnumValue))
        "allYouCanEat" -> copy(allYouCanEat = value as? AttributeValue.BoolValue)
        else          -> this
    }
    is LocationData.PartyLocation   -> when (key) {
        "entryFee"        -> copy(entryFee = value as? AttributeValue.DoubleValue)
        else              -> this
    }

    is LocationData.SightSeeing     -> when (key) {
        "entryFee"        -> copy(entryFee = value as? AttributeValue.DoubleValue)
        else              -> this
    }
    is LocationData.Street          -> when (key) {
        "mautFee"         -> copy(mautFee = value as? AttributeValue.DoubleValue)
        "heightLimit"     -> copy(heightLimit = value as? AttributeValue.DoubleValue)
        "closedInWinter"  -> copy(closedInWinter = value as? AttributeValue.BoolValue)
        "wheeliesAllowed" -> copy(wheeliesAllowed = value as? AttributeValue.BoolValue)
        else              -> this
    }
    is LocationData.SwimmingLocation -> when (key) {
        "indoor"          -> copy(indoor = value as? AttributeValue.BoolValue)
        else              -> this
    }
}