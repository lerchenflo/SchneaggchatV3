package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

//DIe variabla sind static und ma muss ned uf se höra falls se sich ändrand (Ned alle funktiona mol)
var OWNID: Long? = -1;
var SESSIONID: String? = null


class SessionCache {

    var sessionId: String? by mutableStateOf(null)
        private set
    fun updateSessionId(newValue: String?) {
        sessionId = newValue
        SESSIONID = newValue
    }

    var ownId: Long? by mutableStateOf(null)
        private set
    fun updateOwnId(newValue: Long?) {
        ownId = newValue
        OWNID = newValue
    }

    var username: String by mutableStateOf("")
        private set
    fun updateUsername(newValue: String) {
        username = newValue
    }

    var passwordDonotprint: String by mutableStateOf("")
        private set
    fun updatePassword(newValue: String) {
        passwordDonotprint = newValue
    }

    var loggedIn: Boolean by mutableStateOf(false)
        private set
    fun updateLoggedIn(newValue: Boolean) {
        loggedIn = newValue
    }

    var online: Boolean by mutableStateOf(true)
        private set
    fun updateOnline(newValue: Boolean) {
        online = newValue
    }

    override fun toString(): String {
        val sid = sessionId?.let {
            if (it.length <= 8) it else it.substring(0, 6) + "..."
        } ?: "null"

        val own = ownId?.toString() ?: "null"
        val pwd = if (passwordDonotprint.isEmpty()) "<empty>" else "<redacted>"

        return "SessionCache(sessionId=$sid, ownId=$own, username=\"$username\", password=$pwd, loggedIn=$loggedIn, online=$online)"
    }

}
