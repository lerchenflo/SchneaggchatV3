package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map

val sampleSimplePlaceLocations = listOf(
    MapLocation.SimplePlaceLocation(LocationType.BAR, Coordinate(48.2082, 16.3738), "Zum Augustin", "Classic Viennese bar"),
    MapLocation.SimplePlaceLocation(LocationType.BAR, Coordinate(48.2100, 16.3700), "Das Loft Bar", "Rooftop bar with views"),
    MapLocation.SimplePlaceLocation(LocationType.RESTAURANT, Coordinate(48.2060, 16.3750), "Figlmüller", "Traditional Schnitzel house"),
    MapLocation.SimplePlaceLocation(LocationType.RESTAURANT, Coordinate(48.2090, 16.3760), "Steirereck", "Fine Austrian cuisine"),
    MapLocation.SimplePlaceLocation(LocationType.PARK, Coordinate(48.2185, 16.3590), "Volksgarten", "Historic rose garden"),
    MapLocation.SimplePlaceLocation(LocationType.MUSEUM, Coordinate(48.2030, 16.3605), "Kunsthistorisches Museum", "Art history museum"),
    MapLocation.SimplePlaceLocation(LocationType.MUSEUM, Coordinate(48.2047, 16.3597), "Naturhistorisches Museum", "Natural history museum"),
    MapLocation.SimplePlaceLocation(LocationType.SHOP, Coordinate(48.2090, 16.3720), "Naschmarkt", "Open-air market"),
    MapLocation.SimplePlaceLocation(LocationType.SPORT, Coordinate(48.1980, 16.3640), "Ernst Happel Stadion", "Main football stadium"),
)

val sampleUserLocations = listOf(
    MapLocation.UserLocation("schneagg", Coordinate(48.2100, 16.3780)),
    MapLocation.UserLocation("flo", Coordinate(48.2050, 16.3720)),
)
