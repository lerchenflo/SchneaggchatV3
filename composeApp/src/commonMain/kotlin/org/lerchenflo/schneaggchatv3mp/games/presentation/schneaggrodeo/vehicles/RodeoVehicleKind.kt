package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_bear
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_bear_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_bear_controls_keys
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_bull
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_bull_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_bull_controls_keys
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_cable_car
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_cable_car_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_candy_bus
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_candy_bus_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_capture
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_car_wreck
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_car_wreck_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_crash_pilot
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lawn_tractor
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lawn_tractor_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_milk_truck
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_milk_truck_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_plane_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_plane_controls_keys
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_rocket_name
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_train
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_train_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_train_controls_keys

/**
 * The vehicles of the game, plus everything the screen needs to know about riding one. Adding a
 * vehicle starts here (see RodeoVehicle for the other steps).
 */
enum class RodeoVehicleKind(
    /** Banner shown over the track when the rider boards it. */
    val title: StringResource,
    /** Shown in place of the buttons while riding: how to ride it. */
    val rideHint: StringResource,
    /** Same as [rideHint], with the keys to press on desktop. */
    val rideHintKeys: StringResource = rideHint,
    /**
     * Flown like the plane: the left half of the play area steers up, the right half down (the bull
     * uses the same halves to lean back and forward).
     */
    val steers: Boolean = false,
    /** The buttons stay (next to the hint) while riding, e.g. for the lasso. */
    val keepsButtons: Boolean = false,
    /** The lasso button's text while riding, if the vehicle uses it for something else. */
    val lassoLabel: StringResource? = null,
) {
    PLANE(
        title = Res.string.games_schneaggrodeo_crash_pilot,
        rideHint = Res.string.games_schneaggrodeo_plane_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_plane_controls_keys,
        steers = true,
    ),
    LAWN_TRACTOR(
        title = Res.string.games_schneaggrodeo_lawn_tractor,
        rideHint = Res.string.games_schneaggrodeo_lawn_tractor_controls,
    ),
    TRAIN(
        title = Res.string.games_schneaggrodeo_train,
        rideHint = Res.string.games_schneaggrodeo_train_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_train_controls_keys,
    ),
    MILK_TRUCK(
        title = Res.string.games_schneaggrodeo_milk_truck,
        rideHint = Res.string.games_schneaggrodeo_milk_truck_controls,
        keepsButtons = true,
    ),
    BULL(
        title = Res.string.games_schneaggrodeo_bull,
        rideHint = Res.string.games_schneaggrodeo_bull_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_bull_controls_keys,
        steers = true,
    ),
    CAR_WRECK(
        title = Res.string.games_schneaggrodeo_car_wreck,
        rideHint = Res.string.games_schneaggrodeo_car_wreck_controls,
    ),
    CANDY_BUS(
        title = Res.string.games_schneaggrodeo_candy_bus,
        rideHint = Res.string.games_schneaggrodeo_candy_bus_controls,
        keepsButtons = true,
        lassoLabel = Res.string.games_schneaggrodeo_capture,
    ),
    /** Comes along with a mountain (see RodeoLandscape). */
    CABLE_CAR(
        title = Res.string.games_schneaggrodeo_cable_car,
        rideHint = Res.string.games_schneaggrodeo_cable_car_controls,
    ),
    /** Comes along with a forest (see RodeoLandscape). */
    BEAR(
        title = Res.string.games_schneaggrodeo_bear,
        rideHint = Res.string.games_schneaggrodeo_bear_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_bear_controls_keys,
    ),
    /** Bought with snails instead of coming along the track. */
    ROCKET(
        title = Res.string.games_schneaggrodeo_rocket_name,
        rideHint = Res.string.games_schneaggrodeo_plane_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_plane_controls_keys,
        steers = true,
    ),
}
