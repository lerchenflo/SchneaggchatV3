package org.lerchenflo.schneaggchatv3mp.events.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_type_cooking
import schneaggchatv3mp.composeapp.generated.resources.event_type_driving
import schneaggchatv3mp.composeapp.generated.resources.event_type_food
import schneaggchatv3mp.composeapp.generated.resources.event_type_gaming
import schneaggchatv3mp.composeapp.generated.resources.event_type_meetup
import schneaggchatv3mp.composeapp.generated.resources.event_type_movie
import schneaggchatv3mp.composeapp.generated.resources.event_type_other
import schneaggchatv3mp.composeapp.generated.resources.event_type_outdoor
import schneaggchatv3mp.composeapp.generated.resources.event_type_party
import schneaggchatv3mp.composeapp.generated.resources.event_type_shopping
import schneaggchatv3mp.composeapp.generated.resources.event_type_sport
import schneaggchatv3mp.composeapp.generated.resources.event_type_trip

@Serializable
enum class EventType {
    // Existing
    SPORT,
    FOOD,
    COOKING,
    SHOPPING,
    DRIVING,

    // New
    PARTY,       // birthdays, get-togethers, celebrations
    GAMING,      // board games, video games, LAN
    MOVIE,       // cinema or movie night
    TRIP,        // day trips, weekend trips, vacations
    MEETUP,      // casual hangout with no specific activity
    OUTDOOR,     // hiking, camping, swimming, nature stuff
    OTHER        // fallback / catch-all
}

fun EventType.stringRes(): StringResource = when (this) {
    EventType.SPORT -> Res.string.event_type_sport
    EventType.FOOD -> Res.string.event_type_food
    EventType.COOKING -> Res.string.event_type_cooking
    EventType.SHOPPING -> Res.string.event_type_shopping
    EventType.DRIVING -> Res.string.event_type_driving
    EventType.PARTY -> Res.string.event_type_party
    EventType.GAMING -> Res.string.event_type_gaming
    EventType.MOVIE -> Res.string.event_type_movie
    EventType.TRIP -> Res.string.event_type_trip
    EventType.MEETUP -> Res.string.event_type_meetup
    EventType.OUTDOOR -> Res.string.event_type_outdoor
    EventType.OTHER -> Res.string.event_type_other
}

fun EventType.icon(): ImageVector = when (this) {
    EventType.SPORT -> Icons.Default.FitnessCenter
    EventType.FOOD -> Icons.Default.Restaurant
    EventType.COOKING -> Icons.Default.Kitchen
    EventType.SHOPPING -> Icons.Default.ShoppingCart
    EventType.DRIVING -> Icons.Default.DirectionsCar
    EventType.PARTY -> Icons.Default.Celebration
    EventType.GAMING -> Icons.Default.SportsEsports
    EventType.MOVIE -> Icons.Default.Movie
    EventType.TRIP -> Icons.Default.Luggage
    EventType.MEETUP -> Icons.Default.Groups
    EventType.OUTDOOR -> Icons.Default.Terrain
    EventType.OTHER -> Icons.Default.Event
}