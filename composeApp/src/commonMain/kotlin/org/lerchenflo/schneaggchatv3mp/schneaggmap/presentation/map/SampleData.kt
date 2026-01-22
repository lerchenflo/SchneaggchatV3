package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map

// Sample user locations around Dornbirn, Vorarlberg area
val sampleUserLocations = listOf(
    MapLocation.UserLocation(
        id = "user_001",
        coordinate = Coordinate(47.4125, 9.7437),
        username = "flo",
        profilePicture = null,
        lastSeen = "2 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_002",
        coordinate = Coordinate(47.4200, 9.7500),
        username = "anna_m",
        profilePicture = null,
        lastSeen = "5 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_003",
        coordinate = Coordinate(47.4100, 9.7300),
        username = "max_power",
        profilePicture = null,
        lastSeen = "10 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_004",
        coordinate = Coordinate(47.4150, 9.7380),
        username = "sarah_k",
        profilePicture = null,
        lastSeen = "15 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_005",
        coordinate = Coordinate(47.4180, 9.7420),
        username = "tom_h",
        profilePicture = null,
        lastSeen = "20 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_006",
        coordinate = Coordinate(47.4130, 9.7350),
        username = "julia_s",
        profilePicture = null,
        lastSeen = "25 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_007",
        coordinate = Coordinate(47.4160, 9.7480),
        username = "david_w",
        profilePicture = null,
        lastSeen = "30 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_008",
        coordinate = Coordinate(47.4140, 9.7320),
        username = "lisa_b",
        profilePicture = null,
        lastSeen = "35 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_009",
        coordinate = Coordinate(47.4110, 9.7460),
        username = "mike_r",
        profilePicture = null,
        lastSeen = "40 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_010",
        coordinate = Coordinate(47.4190, 9.7390),
        username = "emma_l",
        profilePicture = null,
        lastSeen = "45 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_011",
        coordinate = Coordinate(47.4170, 9.7340),
        username = "chris_m",
        profilePicture = null,
        lastSeen = "50 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_012",
        coordinate = Coordinate(47.4120, 9.7510),
        username = "nina_f",
        profilePicture = null,
        lastSeen = "55 min ago"
    ),
    MapLocation.UserLocation(
        id = "user_013",
        coordinate = Coordinate(47.4115, 9.7290),
        username = "paul_g",
        profilePicture = null,
        lastSeen = "1 hour ago"
    ),
    MapLocation.UserLocation(
        id = "user_014",
        coordinate = Coordinate(47.4145, 9.7450),
        username = "sophia_t",
        profilePicture = null,
        lastSeen = "1 hour ago"
    ),
    MapLocation.UserLocation(
        id = "user_015",
        coordinate = Coordinate(47.4105, 9.7370),
        username = "james_d",
        profilePicture = null,
        lastSeen = "2 hours ago"
    ),
    MapLocation.UserLocation(
        id = "user_016",
        coordinate = Coordinate(47.4185, 9.7490),
        username = "olivia_c",
        profilePicture = null,
        lastSeen = "2 hours ago"
    ),
    MapLocation.UserLocation(
        id = "user_017",
        coordinate = Coordinate(47.4125, 9.7310),
        username = "ben_k",
        profilePicture = null,
        lastSeen = "3 hours ago"
    ),
    MapLocation.UserLocation(
        id = "user_018",
        coordinate = Coordinate(47.4135, 9.7470),
        username = "mia_p",
        profilePicture = null,
        lastSeen = "3 hours ago"
    ),
    MapLocation.UserLocation(
        id = "user_019",
        coordinate = Coordinate(47.4195, 9.7360),
        username = "lucas_v",
        profilePicture = null,
        lastSeen = "4 hours ago"
    ),
    MapLocation.UserLocation(
        id = "user_020",
        coordinate = Coordinate(47.4108, 9.7410),
        username = "ava_n",
        profilePicture = null,
        lastSeen = "5 hours ago"
    ),
    MapLocation.UserLocation(
        id = "user_021",
        coordinate = Coordinate(47.4155, 9.7430),
        username = "noah_j",
        profilePicture = null,
        lastSeen = "6 hours ago"
    ),
    MapLocation.UserLocation(
        id = "user_022",
        coordinate = Coordinate(47.4175, 9.7365),
        username = "isabella_h",
        profilePicture = null,
        lastSeen = "Yesterday"
    ),
    MapLocation.UserLocation(
        id = "user_023",
        coordinate = Coordinate(47.4092, 9.7445),
        username = "ethan_r",
        profilePicture = null,
        lastSeen = "Yesterday"
    ),
    MapLocation.UserLocation(
        id = "user_024",
        coordinate = Coordinate(47.4165, 9.7325),
        username = "charlotte_w",
        profilePicture = null,
        lastSeen = "Yesterday"
    ),
    MapLocation.UserLocation(
        id = "user_025",
        coordinate = Coordinate(47.4138, 9.7505),
        username = "liam_b",
        profilePicture = null,
        lastSeen = "2 days ago"
    )
)

// Sample place locations around Dornbirn, Vorarlberg area
val samplePlaceLocations = listOf(
    MapLocation.PlaceLocation(
        id = "place_001",
        coordinate = Coordinate(47.4125, 9.7440),
        name = "Radarbox Dornbirn",
        locationType = LocationType.RADARBOX,
        description = "Main radarbox location in city center",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_002",
        coordinate = Coordinate(47.4095, 9.7380),
        name = "Radarbox Süd",
        locationType = LocationType.RADARBOX,
        description = "Southern radarbox location",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_003",
        coordinate = Coordinate(47.4180, 9.7520),
        name = "Radarbox Nord",
        locationType = LocationType.RADARBOX,
        description = "Northern radarbox location",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_004",
        coordinate = Coordinate(47.4150, 9.7390),
        name = "Kebab House Dornbirn",
        locationType = LocationType.DOENER,
        description = "Authentic Turkish döner kebab",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_005",
        coordinate = Coordinate(47.4130, 9.7460),
        name = "Istanbul Grill",
        locationType = LocationType.DOENER,
        description = "Fast food döner and wraps",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_006",
        coordinate = Coordinate(47.4170, 9.7350),
        name = "Sultan's Döner",
        locationType = LocationType.DOENER,
        description = "Family-run döner restaurant",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_007",
        coordinate = Coordinate(47.4110, 9.7420),
        name = "Kebab Express",
        locationType = LocationType.DOENER,
        description = "Quick service döner spot",
        rating = 3
    ),
    MapLocation.PlaceLocation(
        id = "place_008",
        coordinate = Coordinate(47.4160, 9.7490),
        name = "Anatolien Kebab",
        locationType = LocationType.DOENER,
        description = "Traditional Anatolian cuisine",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_009",
        coordinate = Coordinate(47.4140, 9.7330),
        name = "Turkish Delight",
        locationType = LocationType.DOENER,
        description = "Döner and Turkish specialties",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_010",
        coordinate = Coordinate(47.4185, 9.7410),
        name = "Döneria",
        locationType = LocationType.DOENER,
        description = "Modern döner restaurant",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_011",
        coordinate = Coordinate(47.4120, 9.7370),
        name = "Pizzeria Bella Italia",
        locationType = LocationType.PIZZA,
        description = "Authentic Italian pizza",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_012",
        coordinate = Coordinate(47.4145, 9.7455),
        name = "Pizza Pronto",
        locationType = LocationType.PIZZA,
        description = "Fast pizza delivery and dine-in",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_013",
        coordinate = Coordinate(47.4105, 9.7395),
        name = "Napoli Pizza",
        locationType = LocationType.PIZZA,
        description = "Neapolitan style pizza",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_014",
        coordinate = Coordinate(47.4175, 9.7475),
        name = "Da Mario",
        locationType = LocationType.PIZZA,
        description = "Family pizza restaurant",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_015",
        coordinate = Coordinate(47.4135, 9.7340),
        name = "Pizza Rustica",
        locationType = LocationType.PIZZA,
        description = "Wood-fired pizza",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_016",
        coordinate = Coordinate(47.4155, 9.7515),
        name = "La Strada Pizza",
        locationType = LocationType.PIZZA,
        description = "Street-style Italian pizza",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_017",
        coordinate = Coordinate(47.4098, 9.7425),
        name = "Pizza Palace",
        locationType = LocationType.PIZZA,
        description = "Large variety of pizzas",
        rating = 3
    ),
    MapLocation.PlaceLocation(
        id = "place_018",
        coordinate = Coordinate(47.4168, 9.7385),
        name = "Ristorante Roma",
        locationType = LocationType.PIZZA,
        description = "Fine dining pizza restaurant",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_019",
        coordinate = Coordinate(47.4142, 9.7500),
        name = "Pizza Express",
        locationType = LocationType.PIZZA,
        description = "Quick service pizza",
        rating = 3
    ),
    MapLocation.PlaceLocation(
        id = "place_020",
        coordinate = Coordinate(47.4112, 9.7358),
        name = "Venezia Pizza",
        locationType = LocationType.PIZZA,
        description = "Venetian style pizza",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_021",
        coordinate = Coordinate(47.4188, 9.7442),
        name = "Pizzeria Toscana",
        locationType = LocationType.PIZZA,
        description = "Tuscan pizza specialties",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_022",
        coordinate = Coordinate(47.4102, 9.7468),
        name = "Adana Kebab",
        locationType = LocationType.DOENER,
        description = "Spicy Adana kebab specialty",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_023",
        coordinate = Coordinate(47.4178, 9.7325),
        name = "Radarbox West",
        locationType = LocationType.RADARBOX,
        description = "Western district radarbox",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_024",
        coordinate = Coordinate(47.4128, 9.7495),
        name = "Kebab Corner",
        locationType = LocationType.DOENER,
        description = "Corner döner shop",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_025",
        coordinate = Coordinate(47.4152, 9.7368),
        name = "Pizza Margherita",
        locationType = LocationType.PIZZA,
        description = "Classic Margherita pizza",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_026",
        coordinate = Coordinate(47.4092, 9.7405),
        name = "Radarbox Ost",
        locationType = LocationType.RADARBOX,
        description = "Eastern radarbox location",
        rating = 3
    ),
    MapLocation.PlaceLocation(
        id = "place_027",
        coordinate = Coordinate(47.4165, 9.7448),
        name = "Mediterranean Grill",
        locationType = LocationType.DOENER,
        description = "Mediterranean style döner",
        rating = 5
    ),
    MapLocation.PlaceLocation(
        id = "place_028",
        coordinate = Coordinate(47.4118, 9.7385),
        name = "Pizza Italia",
        locationType = LocationType.PIZZA,
        description = "Traditional Italian pizza",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_029",
        coordinate = Coordinate(47.4148, 9.7510),
        name = "Döner Haus",
        locationType = LocationType.DOENER,
        description = "Large portions, good value",
        rating = 4
    ),
    MapLocation.PlaceLocation(
        id = "place_030",
        coordinate = Coordinate(47.4195, 9.7395),
        name = "Radarbox Premium",
        locationType = LocationType.RADARBOX,
        description = "Premium radarbox services",
        rating = 5
    )
)