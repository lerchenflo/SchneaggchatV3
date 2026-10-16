package org.lerchenflo.schneaggchatv3mp.datasource.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.PollResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionMessage
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.NotificationResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserResponse

/**
 * Deserializer for all polymorphic JSON objects
 */
object AppJson {
    val instance = Json {
        classDiscriminator = "_class"
        ignoreUnknownKeys = true
        encodeDefaults = true //Send values which have defaults explicit
        //https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/

        serializersModule = SerializersModule {
            polymorphic(UserResponse::class) {
                subclass(UserResponse.SimpleUserResponse::class)
                subclass(UserResponse.FriendUserResponse::class)
                subclass(UserResponse.SelfUserResponse::class)
            }

            polymorphic(NotificationResponse::class) {
                subclass(NotificationResponse.MessageNotificationResponse::class)
                subclass(NotificationResponse.FriendRequestNotificationResponse::class)
                subclass(NotificationResponse.SystemNotificationResponse::class)
            }

            polymorphic(SocketConnectionMessage::class) {
                subclass(SocketConnectionMessage.MessageChange::class)
                subclass(SocketConnectionMessage.UserChange::class)
                subclass(SocketConnectionMessage.EventChange::class)
                subclass(SocketConnectionMessage.FriendRequest::class)
                subclass(SocketConnectionMessage.MapChange::class)
                subclass(SocketConnectionMessage.LocationUpdate::class)
                subclass(SocketConnectionMessage.MessageAck::class)
                subclass(SocketConnectionMessage.FriendLocationChange::class)
                subclass(SocketConnectionMessage.FriendLocationsSnapshot::class)
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