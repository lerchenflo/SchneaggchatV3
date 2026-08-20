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