package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import org.maplibre.compose.map.MapState

//Shared between the map chrome (BaseMap: zoom-slider snapping, follow mode) and the map layers
//(MapContentLayers: friend clustering). The distance math is approximateDistanceMeters, with the other geo helpers.

//The camera only knows its scale once the map has reported a viewport; until then everything that
//converts dp to meters falls back to zero, which reads as "no snapping, no clustering yet".
val MapState.metersPerDpAtTarget: Double
    get() = viewport?.metersPerDpAtCenter ?: 0.0

