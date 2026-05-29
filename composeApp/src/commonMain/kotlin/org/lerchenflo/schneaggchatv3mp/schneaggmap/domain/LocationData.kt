package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



enum class LocationType {
    RADAR, STREET, CAMPING, SIGHTSEEING, SWIMMING, PARTY, FOOD;

    val typeKey: String get() = when (this) {
        RADAR       -> "radar"
        STREET      -> "street"
        CAMPING     -> "camping"
        SIGHTSEEING -> "sightseeing"
        SWIMMING    -> "swimming"
        PARTY       -> "party"
        FOOD        -> "food"
    }
}

val allLocationTypeKeys: List<String> = LocationType.entries.map { it.typeKey }


@Serializable
sealed interface LocationData {

    fun schema(): List<AttributeDefinition>


    //STREET LOCATION TYPES
    enum class RadarType { REDLIGHT, SPEED, MOBILE, POLICE }

    @Serializable
    @SerialName("radar")
    data class Radar(

        val speedLimit: AttributeValue.IntValue,
        val radarType: RadarType,


        ): LocationData {
            override fun schema() = listOf(
                AttributeDefinition.IntDef(key = "speedLimit", required = true, min = 0, max = 300)
            )
    }

    @Serializable
    @SerialName("street")
    data class Street(

        val mautFee: AttributeValue.DoubleValue?,
        val heightLimit: AttributeValue.DoubleValue?,
        val closedInWinter: AttributeValue.BoolValue?,

        val wheeliesAllowed: AttributeValue.BoolValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "mautFee",        required = false, min = 0.0),
            AttributeDefinition.DoubleDef(key = "heightLimit",    required = false, min = 0.0),
            AttributeDefinition.BoolDef  (key = "closedInWinter", required = false),
            AttributeDefinition.BoolDef  (key = "wheeliesAllowed",required = false),
        )
    }

    @Serializable
    @SerialName("camping")
    data class Camping(

        val official: AttributeValue.BoolValue

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "official", required = true),
        )
    }


    //ACtivities

    @Serializable
    @SerialName("sightseeing")
    data class SightSeeing(

        val entryFee: AttributeValue.DoubleValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }

    @Serializable
    @SerialName("swimming")
    data class SwimmingLocation(
        val indoor: AttributeValue.BoolValue?
    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "indoor", required = false),
        )
    }

    @Serializable
    @SerialName("party")
    data class PartyLocation(

        val entryFee: AttributeValue.DoubleValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.DoubleDef(key = "entryFee", required = false, min = 0.0),
        )
    }


    //Food
    enum class FoodType { KEBAB, PIZZA, GREEK, CHINESE, ASIAN, AUSTRIAN, BURGER, OTHER}

    @Serializable
    @SerialName("food")
    data class Food(

        val foodType: FoodType,
        val allYouCanEat: AttributeValue.BoolValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.BoolDef(key = "allYouCanEat", required = false),
        )
    }

}


val LocationData.typeKey: String
    get() = when (this) {
        is LocationData.Radar -> "radar"
        is LocationData.Street -> "street"
        is LocationData.Camping -> "camping"
        is LocationData.SightSeeing -> "sightseeing"
        is LocationData.SwimmingLocation -> "swimming"
        is LocationData.PartyLocation -> "party"
        is LocationData.Food -> "food"
    }



