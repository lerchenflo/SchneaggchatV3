package org.lerchenflo.schneaggchatv3mp.events.domain

import kotlinx.serialization.Serializable

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