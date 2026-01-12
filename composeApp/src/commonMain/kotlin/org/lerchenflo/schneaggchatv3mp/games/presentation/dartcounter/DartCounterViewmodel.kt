package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
class DartCounterViewModel() : ViewModel() {
    class Player(val name: String, var score: Int) {

        //var id;

    }

    class GameGanager(val doubleout: Boolean = false, val countdown: Int, val namelist: List<String>){
        val playerList: MutableList<Player> = mutableListOf()
        init {

            for(name in namelist){
                playerList.add(Player(name, countdown))
            }
        }
        fun subtract(index: Int, score: Int, double: Boolean){
            if ((playerList[index].score>=score && !doubleout) || (playerList[index].score>score || (playerList[index].score==score || doubleout))){
                playerList[index].score -= score;
            }
        }
    }




    var count by mutableStateOf(0)
        public set
    fun updateCount(pointsToAdd : Int) {
        count += pointsToAdd
    }

    fun clearCount() {
        count = 0
    }
}