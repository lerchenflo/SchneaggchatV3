package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziState
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import kotlin.math.abs

class SchneaggaHusViewmodel(

): ViewModel(

) {


    private val _state = MutableStateFlow(
        SchneaggaHusState(
            schneaggList = listOf(),
            schneagghusList = listOf(
                Schneaggahus(
                    position = Position(x = 1, y = 9),
                    color = Color(0xFFEF5350)
                ),
                Schneaggahus(
                    position = Position(x = 8, y = 8),
                    color = Color(0xFF42A5F5)
                )
            ),
            schneaggaPathList = listOf(
                SchneaggaPath(
                    startPoint = DIRECTION.NORTH,
                    endPoint = DIRECTION.SOUTH,
                    position = Position(x = 1, y = 2),
                    toggleOptions = null
                ),
                SchneaggaPath(
                    startPoint = DIRECTION.NORTH,
                    endPoint = DIRECTION.SOUTH,
                    position = Position(x = 1, y = 3),
                    toggleOptions = null
                ),
                SchneaggaPath(
                    startPoint = DIRECTION.NORTH,
                    endPoint = DIRECTION.EAST,
                    position = Position(x = 1, y = 4),
                    toggleOptions = Pair(DIRECTION.EAST, DIRECTION.SOUTH)
                ),


                SchneaggaPath(
                    startPoint = DIRECTION.NORTH,
                    endPoint = DIRECTION.SOUTH,
                    position = Position(x = 1, y = 5),
                    toggleOptions = null
                ),

                SchneaggaPath(
                    startPoint = DIRECTION.WEST,
                    endPoint = DIRECTION.EAST,
                    position = Position(x = 2, y = 4),
                    toggleOptions = null
                ),
            )
        )
    )

    val state: StateFlow<SchneaggaHusState> = _state.asStateFlow()



    fun onAction(action: SchneaggaHusAction) {
        when (action) {
            is SchneaggaHusAction.OnConsecrationClick -> {

            }
        }
    }

    fun getDirectionFromLocation(location: FloatPosition, oldDirection: DIRECTION) : DIRECTION {
        state.value.schneaggaPathList.forEach {
            val epsilon = 0.01f

            if (abs(it.position.x - location.x) < epsilon && abs(it.position.y - location.y) < epsilon) {
                println("Direction change")
                return it.endPoint
            }
        }

        return oldDirection
    }


    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(16L)

                _state.update { state ->

                    state.copy(
                        schneaggList = state.schneaggList.map {
                            val newDir = getDirectionFromLocation(it.position, it.direction)
                            it.move(newDir)
                        }
                    )

                }
            }
        }

        viewModelScope.launch {
            delay(3000)
            _state.update {
                it.copy(
                    schneaggList = it.schneaggList + Schneagg(
                        direction = DIRECTION.SOUTH,
                        position = FloatPosition(1f,1f),
                        color = Color.Red
                    )
                )
            }
        }
    }

}