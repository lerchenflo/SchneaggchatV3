package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.*

/**
 * One selectable option of an [AttributeDefinition.EnumDef]. Implemented by enums - [name] is the
 * enum constant name, which is the stored/wire value and must match the server's enum exactly.
 * Never rename a constant without a server migration, only add new ones.
 */
interface AttributeOption {
    val name: String
    val labelRes: StringResource
}

enum class BicycleUndergroundType(override val labelRes: StringResource) : AttributeOption {
    ASPHALT(Res.string.location_bicycle_underground_type_asphalt),
    GRAVEL(Res.string.location_bicycle_underground_type_gravel),
    DIRT(Res.string.location_bicycle_underground_type_dirt),
    TRAIL(Res.string.location_bicycle_underground_type_trail),
    OTHER(Res.string.location_bicycle_underground_type_other),
}

/** Whether a sports/activity venue is indoors, outdoors or has both. */
enum class VenueSetting(override val labelRes: StringResource) : AttributeOption {
    INDOOR(Res.string.location_option_venue_setting_indoor),
    OUTDOOR(Res.string.location_option_venue_setting_outdoor),
    BOTH(Res.string.location_option_venue_setting_both),
}

enum class OffroadDiscipline(override val labelRes: StringResource) : AttributeOption {
    MOTOCROSS(Res.string.location_option_offroad_discipline_motocross),
    ENDURO(Res.string.location_option_offroad_discipline_enduro),
    BOTH(Res.string.location_option_offroad_discipline_both),
}

enum class CampingKind(override val labelRes: StringResource) : AttributeOption {
    OFFICIAL_SITE(Res.string.location_option_camping_kind_official_site),
    WILD_CAMPING(Res.string.location_option_camping_kind_wild_camping),
    TOLERATED(Res.string.location_option_camping_kind_tolerated),
}
