import UserNotifications
import ComposeApp

class NotificationService: UNNotificationServiceExtension {

    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(_ request: UNNotificationRequest,
                             withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        self.bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        guard let bestAttemptContent = bestAttemptContent else {
            contentHandler(request.content)
            return
        }

        var payload = [String: String]()
        for (key, value) in request.content.userInfo {
            if let k = key as? String {
                payload[k] = "\(value)"
            }
        }

        IosNotificationServiceBridge().resolveLocalized(payload: payload) { result in
            if let title = result.title { bestAttemptContent.title = title }
            if let body = result.body { bestAttemptContent.body = body }
            contentHandler(bestAttemptContent)
        }
    }

    override func serviceExtensionTimeWillExpire() {
        // iOS will fall back to the original aps.alert text the server sent.
        if let contentHandler = contentHandler, let bestAttemptContent = bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }
}
