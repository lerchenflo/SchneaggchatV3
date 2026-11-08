import UserNotifications

private let appGroupId = "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtension"
private let languageKey = "shared_language_iso"
// Must match SharedNotificationDefaults.KEY_PENDING_PUSH_MESSAGES on the Kotlin side.
private let pendingPushMessagesKey = "pending_push_messages"
private let maxPendingPushMessages = 100

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

        // Queue the raw payload for the main app to upsert into Room next time it activates -
        // a background push never reaches Kotlin otherwise, since this extension is a separate
        // process. Queued regardless of decode success below so a future app version that
        // understands more types can still process it.
        if payload["type"] == "message" {
            enqueuePendingPushMessage(payload)
        }

        guard let decoded = NotificationPayloadDecoder.decode(payload) else {
            contentHandler(bestAttemptContent)
            return
        }

        let resolved = resolve(decoded: decoded, language: language)
        if let title = resolved.title { bestAttemptContent.title = title }
        if let body  = resolved.body  { bestAttemptContent.body  = body }
        contentHandler(bestAttemptContent)
    }

    /// Appends the flattened push payload to the shared App Group queue, capped so a burst of
    /// undelivered pushes can't grow it unboundedly. The `aps` entry is dropped - its stringified
    /// value is a Swift dictionary description, not usable data.
    private func enqueuePendingPushMessage(_ payload: [String: String]) {
        var entry = payload
        entry.removeValue(forKey: "aps")

        let defaults = UserDefaults(suiteName: appGroupId)
        var queue: [[String: String]] = []
        if let raw = defaults?.string(forKey: pendingPushMessagesKey),
           let data = raw.data(using: .utf8),
           let decoded = try? JSONDecoder().decode([[String: String]].self, from: data) {
            queue = decoded
        }

        queue.append(entry)
        if queue.count > maxPendingPushMessages {
            queue.removeFirst(queue.count - maxPendingPushMessages)
        }

        if let data = try? JSONEncoder().encode(queue), let json = String(data: data, encoding: .utf8) {
            defaults?.set(json, forKey: pendingPushMessagesKey)
            defaults?.synchronize()
        }
    }

    override func serviceExtensionTimeWillExpire() {
        if let contentHandler = contentHandler, let bestAttemptContent = bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }

    private func resolve(
        decoded: DecodedNotification,
        language: String?
    ) -> (title: String?, body: String?) {
        switch decoded {
        case let .message(_, senderName, groupName, messageType, groupMessage, content, reaction):
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
                return (reactionTitle, content)
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
                // Empty when the push came from a server that predates the plaintext `content` field.
                body = content.isEmpty ? NotificationStrings.get(.youHaveNewMessages, language: language) : content
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

        case let .event(eventTitle, creatorName):
            return (
                NotificationStrings.get(.newEventTitle, language: language, creatorName),
                eventTitle
            )

        case let .mapSighting(entryName, sightingType):
            let titleKey: NotificationStringKey = sightingType == .POLICE ? .mapSightingPoliceTitle : .mapSightingRadarTitle
            return (NotificationStrings.get(titleKey, language: language), entryName)

        case let .eventReminder(eventTitle, startDate, leadMinutes):
            // Without a lead time the start is named instead, so the reminder is never empty
            let body: String
            if leadMinutes > 0 {
                body = NotificationStrings.get(.eventReminderBody, language: language, EventTexts.leadTime(leadMinutes, language: language))
            } else if startDate > 0 {
                body = NotificationStrings.get(.eventReminderBodyAt, language: language, EventTexts.date(startDate))
            } else {
                body = NotificationStrings.get(.youHaveNewMessages, language: language)
            }
            return (NotificationStrings.get(.eventReminderTitle, language: language, eventTitle), body)

        case let .eventUpdate(eventTitle, changes, startDate, actorName):
            let start = EventTexts.date(startDate)
            let body: String
            if changes.contains("CANCELLED") {
                body = NotificationStrings.get(.eventCancelledBody, language: language, actorName)
            } else if changes.contains("START") && changes.contains("LOCATION") {
                body = NotificationStrings.get(.eventStartAndLocationBody, language: language, actorName, start)
            } else if changes.contains("START") {
                body = NotificationStrings.get(.eventStartBody, language: language, actorName, start)
            } else if changes.contains("LOCATION") {
                body = NotificationStrings.get(.eventLocationBody, language: language, actorName)
            } else {
                // Unknown or unreadable changes still show the alert
                body = NotificationStrings.get(.eventChangedBody, language: language, actorName)
            }
            return (eventTitle.isEmpty ? "Schneaggchat" : eventTitle, body)
        }
    }
}

/// Texts shared by the event reminder and change alert.
private enum EventTexts {
    /// "1 day 2 h 30 min"; zero parts are left out.
    static func leadTime(_ totalMinutes: Int, language: String?) -> String {
        let days = totalMinutes / (24 * 60)
        let hours = (totalMinutes % (24 * 60)) / 60
        let minutes = totalMinutes % 60
        var parts: [String] = []
        if days > 0 {
            parts.append(NotificationStrings.get(days == 1 ? .leadDay : .leadDays, language: language, days))
        }
        if hours > 0 { parts.append(NotificationStrings.get(.leadHours, language: language, hours)) }
        if minutes > 0 || parts.isEmpty { parts.append(NotificationStrings.get(.leadMinutes, language: language, minutes)) }
        return parts.joined(separator: " ")
    }

    /// Epoch milliseconds in the device's time zone, like the app's `dd.MM.yyyy HH:mm`.
    static func date(_ epochMillis: Int64) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy HH:mm"
        return formatter.string(from: Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000))
    }
}
