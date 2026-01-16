package org.lerchenflo.schneaggchatv3mp.games.dartcounter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class DartCounterViewmodel() : ViewModel(){

    var count by mutableStateOf(0)
        private set

    fun updateCount(pointsToAdd : Int) {
        count += pointsToAdd
    }

    fun clearCount() {
        count = 0
    }

}