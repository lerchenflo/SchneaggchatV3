import UserNotifications

private let appGroupId = "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtension"
private let languageKey = "shared_language_iso"
private let encryptionKeyKey = "shared_encryption_key"

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

        var payload: [String: String] = [:]
        for (key, value) in request.content.userInfo {
            if let k = key as? String {
                payload[k] = "\(value)"
            }
        }

        let defaults = UserDefaults(suiteName: appGroupId)
        let language = defaults?.string(forKey: languageKey)
        let encryptionKey = defaults?.string(forKey: encryptionKeyKey)

        guard let decoded = NotificationPayloadDecoder.decode(payload) else {
            contentHandler(bestAttemptContent)
            return
        }

        let resolved = resolve(decoded: decoded, language: language, encryptionKey: encryptionKey)
        if let title = resolved.title { bestAttemptContent.title = title }
        if let body  = resolved.body  { bestAttemptContent.body  = body }
        contentHandler(bestAttemptContent)
    }

    override func serviceExtensionTimeWillExpire() {
        if let contentHandler = contentHandler, let bestAttemptContent = bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }

    private func resolve(
        decoded: DecodedNotification,
        language: String?,
        encryptionKey: String?
    ) -> (title: String?, body: String?) {
        switch decoded {
        case let .message(_, senderName, groupName, messageType, groupMessage, encodedContent, reaction):
            if reaction {
                let typeKey: NotificationStringKey
                switch messageType {
                case .TEXT:  typeKey = .message
                case .IMAGE: typeKey = .image
                case .AUDIO: typeKey = .audio
                case .POLL:  typeKey = .poll
                }
                let typeWord = NotificationStrings.get(typeKey, language: language)
                let reactionTitle = NotificationStrings.get(.newMessageReaction, language: language, senderName, typeWord)
                let reactionBody: String
                if let key = encryptionKey, !key.isEmpty,
                   let plaintext = NotificationCrypto.decrypt(base64Ciphertext: encodedContent, key: key),
                   !plaintext.isEmpty {
                    reactionBody = plaintext
                } else {
                    reactionBody = ""
                }
                return (reactionTitle, reactionBody)
            }

            let title: String
            if groupMessage && !groupName.isEmpty {
                title = NotificationStrings.get(.newMessageGroupTitle, language: language, senderName, groupName)
            } else {
                title = NotificationStrings.get(.newMessageSingleTitle, language: language, senderName)
            }
            let body: String
            switch messageType {
            case .TEXT:
                if let key = encryptionKey, !key.isEmpty,
                   let plaintext = NotificationCrypto.decrypt(base64Ciphertext: encodedContent, key: key),
                   !plaintext.isEmpty {
                    body = plaintext
                } else {
                    body = NotificationStrings.get(.youHaveNewMessages, language: language)
                }
            case .IMAGE:
                body = NotificationStrings.get(.image, language: language)
            case .AUDIO:
                body = NotificationStrings.get(.audio, language: language)
            case .POLL:
                body = NotificationStrings.get(.poll, language: language)
            }
            return (title, body)

        case let .friendRequest(requesterName, accepted):
            if accepted {
                return (
                    NotificationStrings.get(.newFriendAcceptedTitle, language: language),
                    NotificationStrings.get(.newFriendAcceptedBody, language: language, requesterName)
                )
            }
            return (
                NotificationStrings.get(.newFriendRequestTitle, language: language, requesterName),
                NotificationStrings.get(.newFriendRequestBody, language: language, requesterName)
            )

        case let .system(title, message):
            return (title, message)

        case let .birthday(birthdayUserName, ownBirthday):
            if ownBirthday {
                return (
                    NotificationStrings.get(.ownBirthdayTitle, language: language),
                    NotificationStrings.get(.ownBirthdayBody, language: language)
                )
            }
            return (
                NotificationStrings.get(.friendBirthdayTitle, language: language, birthdayUserName),
                NotificationStrings.get(.friendBirthdayBody, language: language)
            )
        }
    }
}
