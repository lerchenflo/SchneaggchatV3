package org.lerchenflo.schneaggchatv3mp.events.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType.*
import schneaggchatv3mp.composeapp.generated.resources.Res


import schneaggchatv3mp.composeapp.generated.resources.event_type_beer
import schneaggchatv3mp.composeapp.generated.resources.event_type_driving
import schneaggchatv3mp.composeapp.generated.resources.event_type_food
import schneaggchatv3mp.composeapp.generated.resources.event_type_gaming
import schneaggchatv3mp.composeapp.generated.resources.event_type_meetup
import schneaggchatv3mp.composeapp.generated.resources.event_type_movie
import schneaggchatv3mp.composeapp.generated.resources.event_type_other
import schneaggchatv3mp.composeapp.generated.resources.event_type_outdoor
import schneaggchatv3mp.composeapp.generated.resources.event_type_party
import schneaggchatv3mp.composeapp.generated.resources.event_type_riding
import schneaggchatv3mp.composeapp.generated.resources.event_type_sport
import schneaggchatv3mp.composeapp.generated.resources.event_type_trip

@Serializable
enum class EventType {
    // Existing
    SPORT,
    FOOD,

    BEER,
    DRIVING,

    HORSE_RIDING,

    // New
    PARTY,       // birthdays, get-togethers, celebrations
    GAMING,      // board games, video games, LAN
    MOVIE,       // cinema or movie night
    TRIP,        // day trips, weekend trips, vacations
    MEETUP,      // casual hangout with no specific activity
    OUTDOOR,     // hiking, camping, swimming, nature stuff
    OTHER        // fallback / catch-all
}

fun EventType.icon(): ImageVector = when (this) {
    SPORT -> Icons.Default.FitnessCenter
    FOOD -> Icons.Default.Restaurant
    DRIVING -> Icons.Default.DirectionsCar
    PARTY -> Icons.Default.Celebration
    GAMING -> Icons.Default.SportsEsports
    MOVIE -> Icons.Default.Movie
    TRIP -> Icons.Default.Luggage
    MEETUP -> Icons.Default.Groups
    OUTDOOR -> Icons.Default.Terrain
    OTHER -> Icons.Default.Event
    BEER -> Icons.Default.SportsBar
    HORSE_RIDING -> Icons.Default.Pets
}

fun EventType.labelRes(): StringResource = when (this) {
    SPORT -> Res.string.event_type_sport
    FOOD -> Res.string.event_type_food
    DRIVING -> Res.string.event_type_driving
    PARTY -> Res.string.event_type_party
    GAMING -> Res.string.event_type_gaming
    MOVIE -> Res.string.event_type_movie
    TRIP -> Res.string.event_type_trip
    MEETUP -> Res.string.event_type_meetup
    OUTDOOR -> Res.string.event_type_outdoor
    OTHER -> Res.string.event_type_other
    BEER -> Res.string.event_type_beer
    HORSE_RIDING -> Res.string.event_type_riding
}