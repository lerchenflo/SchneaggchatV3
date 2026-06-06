package org.lerchenflo.schneaggchatv3mp.datasource.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.PollResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionMessage
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue

/**
 * Deserializer for all polymorphic JSON objects
 */
object AppJson {
    val instance = Json {
        classDiscriminator = "_class"
        ignoreUnknownKeys = true
        //https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/

        serializersModule = SerializersModule {
            polymorphic(NetworkUtils.UserResponse::class) {
                subclass(NetworkUtils.UserResponse.SimpleUserResponse::class)
                subclass(NetworkUtils.UserResponse.FriendUserResponse::class)
                subclass(NetworkUtils.UserResponse.SelfUserResponse::class)
            }

            polymorphic(NetworkUtils.NotificationResponse::class) {
                subclass(NetworkUtils.NotificationResponse.MessageNotificationResponse::class)
                subclass(NetworkUtils.NotificationResponse.FriendRequestNotificationResponse::class)
                subclass(NetworkUtils.NotificationResponse.SystemNotificationResponse::class)
            }

            polymorphic(SocketConnectionMessage::class) {
                subclass(SocketConnectionMessage.MessageChange::class)
                subclass(SocketConnectionMessage.UserChange::class)
                subclass(SocketConnectionMessage.FriendRequest::class)
                subclass(SocketConnectionMessage.MapChange::class)
            }

            polymorphic(PollResponse::class) {
                subclass(PollResponse.PublicPollResponse::class)
                subclass(PollResponse.AnonymousPollResponse::class)
            }

            polymorphic(AttributeValue::class) {
                subclass(AttributeValue.StringValue::class)
                subclass(AttributeValue.IntValue::class)
                subclass(AttributeValue.DoubleValue::class)
                subclass(AttributeValue.BoolValue::class)
            }

            /*
            polymorphic(AttributeDefinition::class) {
                subclass(AttributeDefinition.StringDef::class)
                subclass(AttributeDefinition.IntDef::class)
                subclass(AttributeDefinition.DoubleDef::class)
                subclass(AttributeDefinition.BoolDef::class)
                subclass()
            }

             */
        }
    }
}