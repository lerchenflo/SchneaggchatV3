package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_asian
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_austrian
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_burger
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_chinese
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_greek
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_kebab
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_other
import schneaggchatv3mp.composeapp.generated.resources.location_food_type_pizza
import schneaggchatv3mp.composeapp.generated.resources.location_radar_type_mobile
import schneaggchatv3mp.composeapp.generated.resources.location_radar_type_police
import schneaggchatv3mp.composeapp.generated.resources.location_radar_type_redlight
import schneaggchatv3mp.composeapp.generated.resources.location_radar_type_speed
import schneaggchatv3mp.composeapp.generated.resources.location_type_camping
import schneaggchatv3mp.composeapp.generated.resources.location_type_food
import schneaggchatv3mp.composeapp.generated.resources.location_type_party
import schneaggchatv3mp.composeapp.generated.resources.location_type_radar
import schneaggchatv3mp.composeapp.generated.resources.location_type_sightseeing
import schneaggchatv3mp.composeapp.generated.resources.location_type_street
import schneaggchatv3mp.composeapp.generated.resources.location_type_swimming


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
    enum class RadarType {
        REDLIGHT,
        SPEED,
        MOBILE,
        POLICE;

        fun toUiText() : UiText {
            return when (this) {
                REDLIGHT -> UiText.StringResourceText(Res.string.location_radar_type_redlight)
                SPEED -> UiText.StringResourceText(Res.string.location_radar_type_speed)
                MOBILE -> UiText.StringResourceText(Res.string.location_radar_type_mobile)
                POLICE -> UiText.StringResourceText(Res.string.location_radar_type_police)
            }
        }
    }



    @Serializable
    @SerialName("radar")
    data class Radar(

        val speedLimit: AttributeValue.IntValue,
        val radarType: RadarType,


        ): LocationData {
            override fun schema() = listOf(
                AttributeDefinition.EnumDef(
                    key = "radarType",
                    required = true,
                    options = RadarType.entries.map { it.name }
                ),
                AttributeDefinition.IntDef(key = "speedLimit", required = true, min = 0, max = 300),
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
    enum class FoodType { KEBAB,
        PIZZA,
        GREEK,
        CHINESE,
        ASIAN,
        AUSTRIAN,
        BURGER,
        OTHER;

        fun toUiText() : UiText {
            return when (this) {
                KEBAB -> UiText.StringResourceText(Res.string.location_food_type_kebab)
                PIZZA -> UiText.StringResourceText(Res.string.location_food_type_pizza)
                GREEK -> UiText.StringResourceText(Res.string.location_food_type_greek)
                CHINESE -> UiText.StringResourceText(Res.string.location_food_type_chinese)
                ASIAN -> UiText.StringResourceText(Res.string.location_food_type_asian)
                AUSTRIAN -> UiText.StringResourceText(Res.string.location_food_type_austrian)
                BURGER -> UiText.StringResourceText(Res.string.location_food_type_burger)
                OTHER -> UiText.StringResourceText(Res.string.location_food_type_other)
            }
        }
    }

    @Serializable
    @SerialName("food")
    data class Food(

        val foodType: FoodType,
        val allYouCanEat: AttributeValue.BoolValue?,

    ): LocationData {
        override fun schema() = listOf(
            AttributeDefinition.EnumDef(
                key = "foodType",
                required = true,
                options = FoodType.entries.map { it.name }
            ),
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

@Composable
fun LocationData.stringRes() = when (this) {
    is LocationData.Camping ->
        Res.string.location_type_camping

    is LocationData.Food ->
        Res.string.location_type_food

    is LocationData.PartyLocation ->
        Res.string.location_type_party

    is LocationData.Radar ->
        Res.string.location_type_radar

    is LocationData.SightSeeing ->
        Res.string.location_type_sightseeing

    is LocationData.Street ->
        Res.string.location_type_street

    is LocationData.SwimmingLocation ->
        Res.string.location_type_swimming
}

//Horror aber was sunsch
@Composable
fun uiTextFromEnumValue(value: String): UiText{
    if (LocationData.RadarType.entries.map { it.toString() }.contains(value)){
        return LocationData.RadarType.valueOf(value).toUiText()
    }
    if (LocationData.FoodType.entries.map { it.toString() }.contains(value)){
        return LocationData.FoodType.valueOf(value).toUiText()
    }
    return UiText.DynamicString("Unresolved")

}

