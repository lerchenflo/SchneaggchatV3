package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map

data class Coordinate(val lat: Double, val lon: Double)

sealed class MapLocation {
    abstract val coordinate: Coordinate //Koordinaten (Override in allen klassen)

    //Location für eassa (Döner, Pizza, Mci, Restaurant etc)
    data class FoodPlaceLocation(
        override val coordinate: Coordinate,

        //Vorhanden nur für essensplätze
        val name: String,
        val description: String,

        //MEHR???

    ) : MapLocation()

    //Location für Radar (Subtypen Speed, Ampelblitzer, Mobiler Radar, Großkontrolle spot
    data class RadarPlaceLocation(
        override val coordinate: Coordinate,

        //Vorhanden nur für Radarplätze
        val lastSeen: Long, //Zuletzt gesehen zeit

        //MEHR???

    ) : MapLocation()

    //Location für Auto/Moped (Motorradstrecke, Wheeliespot, etc)
    data class DrivePlaceLocation(
        override val coordinate: Coordinate,

        val maut: Boolean, //Gits do maut

    ) : MapLocation()

    //Machand custom andre sacha



}
