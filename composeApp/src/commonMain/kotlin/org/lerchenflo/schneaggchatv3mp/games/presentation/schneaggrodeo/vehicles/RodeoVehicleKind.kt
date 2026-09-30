package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_balloon
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_balloon_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_balloon_controls_keys
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_cow
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_cow_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_rowboat
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_rowboat_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_uboat
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_uboat_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_uboat_controls_keys
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_oil_tanker
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_oil_tanker_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_flamingo
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_flamingo_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_flamingo_controls_keys
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_mine_cart
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_mine_cart_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_traffic_jam
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_traffic_jam_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_pirate_ship
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_pirate_ship_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_drill
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_golden_carriage
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_golden_carriage_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_drill_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_drill_controls_keys
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
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_pocket_bike
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_pocket_bike_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_rocket_name
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_shopping_cart
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_shopping_cart_controls
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
    FORD_ESCORT(
        title = Res.string.games_schneaggrodeo_car_wreck,
        rideHint = Res.string.games_schneaggrodeo_car_wreck_controls,
    ),
    CANDY_BUS(
        title = Res.string.games_schneaggrodeo_candy_bus,
        rideHint = Res.string.games_schneaggrodeo_candy_bus_controls,
        keepsButtons = true,
        lassoLabel = Res.string.games_schneaggrodeo_capture,
    ),
    POCKET_BIKE(
        title = Res.string.games_schneaggrodeo_pocket_bike,
        rideHint = Res.string.games_schneaggrodeo_pocket_bike_controls,
    ),
    SHOPPING_CART(
        title = Res.string.games_schneaggrodeo_shopping_cart,
        rideHint = Res.string.games_schneaggrodeo_shopping_cart_controls,
    ),
    BALLOON(
        title = Res.string.games_schneaggrodeo_balloon,
        rideHint = Res.string.games_schneaggrodeo_balloon_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_balloon_controls_keys,
    ),
    COW(
        title = Res.string.games_schneaggrodeo_cow,
        rideHint = Res.string.games_schneaggrodeo_cow_controls,
    ),
    /** The sea's vehicles (see RodeoTraffic). */
    ROWBOAT(
        title = Res.string.games_schneaggrodeo_rowboat,
        rideHint = Res.string.games_schneaggrodeo_rowboat_controls,
        keepsButtons = true,
    ),
    U_BOAT(
        title = Res.string.games_schneaggrodeo_uboat,
        rideHint = Res.string.games_schneaggrodeo_uboat_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_uboat_controls_keys,
    ),
    OIL_TANKER(
        title = Res.string.games_schneaggrodeo_oil_tanker,
        rideHint = Res.string.games_schneaggrodeo_oil_tanker_controls,
    ),
    FLAMINGO(
        title = Res.string.games_schneaggrodeo_flamingo,
        rideHint = Res.string.games_schneaggrodeo_flamingo_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_flamingo_controls_keys,
    ),
    /** The mine's vehicle. */
    MINE_CART(
        title = Res.string.games_schneaggrodeo_mine_cart,
        rideHint = Res.string.games_schneaggrodeo_mine_cart_controls,
    ),
    TRAFFIC_JAM(
        title = Res.string.games_schneaggrodeo_traffic_jam,
        rideHint = Res.string.games_schneaggrodeo_traffic_jam_controls,
    ),
    /** The sea's pirate ship. */
    PIRATE_SHIP(
        title = Res.string.games_schneaggrodeo_pirate_ship,
        rideHint = Res.string.games_schneaggrodeo_pirate_ship_controls,
    ),
    /** The mine's drill machine. */
    DRILL(
        title = Res.string.games_schneaggrodeo_drill,
        rideHint = Res.string.games_schneaggrodeo_drill_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_drill_controls_keys,
    ),
    /** Bought with snails, like the rocket. */
    GOLDEN_CARRIAGE(
        title = Res.string.games_schneaggrodeo_golden_carriage,
        rideHint = Res.string.games_schneaggrodeo_golden_carriage_controls,
    ),
    /** Comes along with a mountain (see RodeoLandscape). */
    CABLE_CAR(
        title = Res.string.games_schneaggrodeo_cable_car,
        rideHint = Res.string.games_schneaggrodeo_cable_car_controls,
    ),
    /** Bought with snails instead of coming along the track. */
    ROCKET(
        title = Res.string.games_schneaggrodeo_rocket_name,
        rideHint = Res.string.games_schneaggrodeo_plane_controls,
        rideHintKeys = Res.string.games_schneaggrodeo_plane_controls_keys,
        steers = true,
    ),
}
