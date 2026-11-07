import Foundation

enum DecodedNotification {
    case message(msgId: String, senderName: String, groupName: String, messageType: MessageType, groupMessage: Bool, content: String, reaction: Bool)
    case friendRequest(requesterName: String, accepted: Bool)
    case system(title: String, message: String)
    case birthday(birthdayUserName: String, ownBirthday: Bool)
    case event(eventTitle: String, creatorName: String)
    case mapSighting(entryName: String, sightingType: MapSightingType)
}

/// Raw values are wire names and must match the server.
enum MapSightingType: String {
    case POLICE
    case RADAR
}

enum MessageType: String {
    case TEXT
    case IMAGE
    case AUDIO
    case POLL

    static func parse(_ raw: String?) -> MessageType {
        guard let raw, let value = MessageType(rawValue: raw) else { return .TEXT }
        return value
    }
}

enum NotificationPayloadDecoder {
    static func decode(_ payload: [String: String]) -> DecodedNotification? {
        switch payload["type"] {
        case "message":
            guard let msgId = payload["msgId"] else { return nil }
            return .message(
                msgId: msgId,
                senderName: payload["senderName"] ?? "",
                groupName: payload["groupName"] ?? "",
                messageType: MessageType.parse(payload["messageType"]),
                groupMessage: parseBool(payload["groupMessage"]),
                content: payload["content"] ?? "",
                reaction: parseBool(payload["reaction"])
            )
        case "friend_request":
            return .friendRequest(
                requesterName: payload["requesterName"] ?? "",
                accepted: parseBool(payload["accepted"])
            )
        case "system":
            return .system(
                title: payload["title"] ?? "Schneaggchat",
                message: payload["message"] ?? ""
            )
        case "birthday":
            return .birthday(
                birthdayUserName: payload["birthdayUserName"] ?? "",
                ownBirthday: parseBool(payload["ownBirthday"])
            )
        case "event":
            return .event(
                eventTitle: payload["eventTitle"] ?? "",
                creatorName: payload["creatorName"] ?? ""
            )
        case "map_sighting":
            guard let sightingType = MapSightingType(rawValue: payload["sightingType"] ?? "") else { return nil }
            return .mapSighting(
                entryName: payload["entryName"] ?? "",
                sightingType: sightingType
            )
        default:
            return nil
        }
    }

    private static func parseBool(_ raw: String?) -> Bool {
        guard let raw else { return false }
        return raw.lowercased() == "true" || raw == "1"
    }
}
