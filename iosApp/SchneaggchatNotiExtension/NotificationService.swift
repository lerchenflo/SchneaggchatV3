import UserNotifications
import Foundation

class NotificationService: UNNotificationServiceExtension {

    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        guard let best = bestAttemptContent else {
            contentHandler(request.content)
            return
        }

        let userInfo = request.content.userInfo

        if let (title, body) = buildContent(userInfo: userInfo) {
            best.title = title
            best.body = body
        }

        contentHandler(best)
    }

    override func serviceExtensionTimeWillExpire() {
        if let contentHandler, let bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }

    // MARK: - Content building

    private func buildContent(userInfo: [AnyHashable: Any]) -> (String, String)? {
        guard let type = userInfo["type"] as? String else { return nil }

        switch type {
        case "message":
            return buildMessage(userInfo: userInfo)
        case "friend_request":
            return buildFriendRequest(userInfo: userInfo)
        case "system":
            let title = userInfo["title"] as? String ?? "Schneaggchat"
            let body = userInfo["message"] as? String ?? ""
            return (title, body)
        default:
            return nil
        }
    }

    private func buildMessage(userInfo: [AnyHashable: Any]) -> (String, String)? {
        let senderName = userInfo["senderName"] as? String ?? "Schneaggchat"
        let groupName = userInfo["groupName"] as? String ?? ""
        let isGroup = (userInfo["groupMessage"] as? String) == "true"
        let messageType = userInfo["messageType"] as? String ?? "TEXT"

        let title = isGroup ? "\(senderName) → \(groupName)" : senderName

        let body: String
        switch messageType {
        case "IMAGE":
            body = loc("image", fallback: "Image")
        case "AUDIO":
            body = loc("audio", fallback: "Audio message")
        case "POLL":
            body = loc("poll", fallback: "Poll")
        default:
            body = ""
        }

        return (title, body)
    }

    private func buildFriendRequest(userInfo: [AnyHashable: Any]) -> (String, String)? {
        let requesterName = userInfo["requesterName"] as? String ?? ""
        let accepted = (userInfo["accepted"] as? String) == "true"

        if accepted {
            return (
                loc("new_friend_accepted_noti", fallback: "Friend request accepted"),
                String(format: loc("new_friend_accepted_noti_body", fallback: "%@ is now your friend"), requesterName)
            )
        } else {
            return (
                String(format: loc("new_friend_request_noti", fallback: "Friend request from %@"), requesterName),
                String(format: loc("new_friend_request_noti_body", fallback: "%@ wants to be your friend"), requesterName)
            )
        }
    }

    private func loc(_ key: String, fallback: String) -> String {
        let value = Bundle.main.localizedString(forKey: key, value: nil, table: nil)
        return value == key ? fallback : value
    }
}
