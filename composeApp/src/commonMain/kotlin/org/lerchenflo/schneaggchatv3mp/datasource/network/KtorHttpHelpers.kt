package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerializationStrategy
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.safeNetworkCall
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/**
 * [client] bound to the server the user configured: every call takes a server endpoint, builds
 * the full URL with [Preferencemanager.buildServerUrl] and maps the outcome through
 * [safeNetworkCall]. Shared by the classes that call the Schneaggchat server.
 */
class ServerClient(
    @PublishedApi internal val client: HttpClient,
    @PublishedApi internal val preferenceManager: Preferencemanager,
    @PublishedApi internal val logger: Logger,
) {

    suspend inline fun <reified T> safeGet(endpoint: String): NetworkResult<T, NetworkingError> {
        return safeNetworkCall(logger) {
            client.get(preferenceManager.buildServerUrl(endpoint))
        }
    }

    /** POSTs [body] as JSON. */
    suspend inline fun <reified T, reified R> safePost(
        endpoint: String,
        body: T
    ): NetworkResult<R, NetworkingError> {
        return safeNetworkCall(logger) {
            client.post(preferenceManager.buildServerUrl(endpoint)) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    /** POSTs a multipart form built by [parts]. */
    suspend inline fun <reified R> safePostMultipart(
        endpoint: String,
        crossinline parts: FormBuilder.() -> Unit
    ): NetworkResult<R, NetworkingError> {
        return safeNetworkCall(logger) {
            client.post(preferenceManager.buildServerUrl(endpoint)) {
                setBody(MultiPartFormDataContent(formData { parts() }))
            }
        }
    }

    suspend inline fun <reified T> safeDelete(endpoint: String): NetworkResult<T, NetworkingError> {
        return safeNetworkCall(logger) {
            client.delete(preferenceManager.buildServerUrl(endpoint))
        }
    }
}

/** Appends a file part under [key], with its content type and file name. */
fun FormBuilder.appendFile(
    key: String,
    bytes: ByteArray,
    fileName: String,
    contentType: String,
) {
    append(key, bytes, Headers.build {
        append(HttpHeaders.ContentType, contentType)
        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
    })
}

/** Appends the `profilepic` file part the server reads profile and group pictures from. */
fun FormBuilder.appendProfilePic(
    bytes: ByteArray,
    fileName: String = "profile.jpg",
    contentType: String = "image/jpeg"
) {
    appendFile("profilepic", bytes, fileName, contentType)
}

/** Appends [value] as a JSON part under [key], encoded with [AppJson]. */
fun <T> FormBuilder.appendJsonPart(
    key: String,
    serializer: SerializationStrategy<T>,
    value: T,
) {
    append(
        key = key,
        value = AppJson.instance.encodeToString(serializer, value),
        headers = Headers.build {
            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
    )
}
