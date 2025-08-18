package org.lerchenflo.schneaggchatv3mp.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.util.network.UnresolvedAddressException
import util.NetworkError
import util.Result

class NetworkUtils(
    private val httpClient: HttpClient
) {
    public val SERVERURL = "https://schneaggchat.lerchenflo.eu"


    //An login beim server, es git an string ("DU bisc higloggt" oda so) und an error wenns ned goht
    suspend fun login(username: String, password: String): Result<String, NetworkError> {
        val response = try {
            httpClient.get (
                urlString = SERVERURL
            ){
                header("msgtype", 7) //TODO: Richtia msgtypes
                header("username", username)
                header("password", password)

                //TODO: No a main funktion die ma ufrufa kann des isch nur dass mol code do isch i heb mi no


            }
        }catch (e: UnresolvedAddressException){
            return Result.Error(NetworkError.NO_INTERNET)
        }

        return when(response.status.value) {
            in 200..299 -> {
                Result.Success(response.body())
            }
            else -> Result.Error(NetworkError.UNKNOWN)
        }
    }
}