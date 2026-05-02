import UserNotifications
import ComposeApp

class NotificationService: UNNotificationServiceExtension {

    private let appGroup = "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtention"
    private let encryptionKeyKey = "noti_encryption_key"

    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        guard let bestAttemptContent else {
            contentHandler(request.content)
            return
        }

        let encryptionKey = UserDefaults(suiteName: appGroup)?
            .string(forKey: encryptionKeyKey) ?? ""

        let userInfoStrings = request.content.userInfo.reduce(into: [String: Any]()) { acc, pair in
            if let key = pair.key as? String {
                acc[key] = pair.value
            }
        }

        if let content = NotificationContentBuilder.shared.fromMap(
            data: userInfoStrings,
            encryptionKey: encryptionKey
        ) {
            bestAttemptContent.title = content.title
            bestAttemptContent.body = content.body
        }

        contentHandler(bestAttemptContent)
    }

    override func serviceExtensionTimeWillExpire() {
        if let contentHandler, let bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }
}
