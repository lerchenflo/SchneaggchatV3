package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData

fun LocationData.getValueByKey(key: String): AttributeValue? = when (this) {
    is LocationData.Camping         -> when (key) {
        "official"        -> official
        else              -> null
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
    is LocationData.Radar           -> when (key) {
        "speedLimit"      -> speedLimit
        "mobile"          -> mobile
        "redLight"        -> redLight
        else              -> null
    }
    is LocationData.FastFood        -> when (key) {
        "burger"          -> burger
        "kebab"           -> kebab
        "pizza"           -> pizza
        "allYouCanEat"    -> allYouCanEat
        else              -> null
    }
    is LocationData.AsianFood       -> when (key) {
        "chinese"         -> chinese
        "japanese"        -> japanese
        "thai"            -> thai
        "allYouCanEat"    -> allYouCanEat
        else              -> null
    }
    is LocationData.GenericFood     -> when (key) {
        "cuisine"         -> cuisine
        "allYouCanEat"    -> allYouCanEat
        else              -> null
    }
}

fun LocationData.withValueForKey(key: String, value: AttributeValue): LocationData = when (this) {
    is LocationData.Camping         -> when (key) {
        "official"        -> copy(official = value as AttributeValue.BoolValue)
        else              -> this
    }
    is LocationData.Radar           -> when (key) {
        "speedLimit"      -> copy(speedLimit = value as AttributeValue.IntValue)
        "mobile"          -> copy(mobile = value as? AttributeValue.BoolValue)
        "redLight"        -> copy(redLight = value as AttributeValue.BoolValue)
        else              -> this
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
    is LocationData.FastFood        -> when (key) {
        "burger"          -> copy(burger = value as? AttributeValue.BoolValue)
        "kebab"           -> copy(kebab = value as? AttributeValue.BoolValue)
        "pizza"           -> copy(pizza = value as? AttributeValue.BoolValue)
        "allYouCanEat"    -> copy(allYouCanEat = value as? AttributeValue.BoolValue)
        else              -> this
    }
    is LocationData.AsianFood       -> when (key) {
        "chinese"         -> copy(chinese = value as? AttributeValue.BoolValue)
        "japanese"        -> copy(japanese = value as? AttributeValue.BoolValue)
        "thai"            -> copy(thai = value as? AttributeValue.BoolValue)
        "allYouCanEat"    -> copy(allYouCanEat = value as? AttributeValue.BoolValue)
        else              -> this
    }
    is LocationData.GenericFood     -> when (key) {
        "cuisine"         -> copy(cuisine = value as AttributeValue.StringValue)
        "allYouCanEat"    -> copy(allYouCanEat = value as? AttributeValue.BoolValue)
        else              -> this
    }
}