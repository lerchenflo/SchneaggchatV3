import Foundation

enum NotificationStringKey: String {
    case image
    case audio
    case poll
    case message
    case youHaveNewMessages
    case newMessageSingleTitle
    case newMessageGroupTitle
    case newMessageReaction
    case newFriendRequestTitle
    case newFriendRequestBody
    case newFriendAcceptedTitle
    case newFriendAcceptedBody
    case ownBirthdayTitle
    case ownBirthdayBody
    case friendBirthdayTitle
    case friendBirthdayBody
}

enum NotificationStrings {

    static func get(_ key: NotificationStringKey, language: String?, _ args: CVarArg...) -> String {
        let bucket = bucket(for: language)
        let template = bucket[key] ?? englishStrings[key] ?? key.rawValue
        return args.isEmpty ? template : String(format: template, arguments: args)
    }

    private static func bucket(for language: String?) -> [NotificationStringKey: String] {
        guard let raw = language?.lowercased() else { return englishStrings }
        let parts = raw.replacingOccurrences(of: "_", with: "-").split(separator: "-").map(String.init)
        guard parts.first == "de" else { return englishStrings }
        if parts.count > 1, parts[1] == "at" || parts[1] == "rat" {
            return austrianGermanStrings
        }
        return germanStrings
    }

    private static let englishStrings: [NotificationStringKey: String] = [
        .image: "image",
        .audio: "audio",
        .poll: "poll",
        .message: "message",
        .youHaveNewMessages: "You have new messages",
        .newMessageSingleTitle: "New chat from %1$@",
        .newMessageGroupTitle: "New chat from %1$@ in %2$@",
        .newMessageReaction: "%1$@ reacted to your %2$@",
        .newFriendRequestTitle: "Friend request from %1$@",
        .newFriendRequestBody: "%1$@ wants to be your friend",
        .newFriendAcceptedTitle: "Friend request accepted",
        .newFriendAcceptedBody: "%1$@ is now your friend",
        .ownBirthdayTitle: "Happy Birthday! 🎂",
        .ownBirthdayBody: "Wishing you a wonderful day!",
        .friendBirthdayTitle: "%1$@ has birthday today! 🎂",
        .friendBirthdayBody: "Don't forget to congratulate them!",
    ]

    private static let germanStrings: [NotificationStringKey: String] = [
        .image: "Bild",
        .audio: "Audio",
        .poll: "Umfrage",
        .message: "Nachricht",
        .youHaveNewMessages: "Du hast neue Nachrichten",
        .newMessageSingleTitle: "Neuer Chat von %1$@",
        .newMessageGroupTitle: "Neuer Chat von %1$@ in %2$@",
        .newMessageReaction: "%1$@ hat auf deine %2$@ reagiert",
        .newFriendRequestTitle: "Freundschaftsanfrage von %1$@",
        .newFriendRequestBody: "%1$@ will dein Freund sein",
        .newFriendAcceptedTitle: "Freundschaftsanfrage akzeptiert",
        .newFriendAcceptedBody: "%1$@ ist jetzt dein Freund",
        .ownBirthdayTitle: "Alles Gute zum Geburtstag! 🎂",
        .ownBirthdayBody: "Alles Liebe und einen schönen Tag!",
        .friendBirthdayTitle: "%1$@ hat heute Geburtstag! 🎂",
        .friendBirthdayBody: "Vergiss nicht zu gratulieren!",
    ]

    private static let austrianGermanStrings: [NotificationStringKey: String] = [
        .image: "Bild",
        .audio: "Audio",
        .poll: "Umfrog",
        .message: "Nochricht",
        .youHaveNewMessages: "Du hosch neue Nachrichta",
        .newMessageSingleTitle: "Neuer Chat vo %1$@",
        .newMessageGroupTitle: "Neuer Chat vo %1$@ in %2$@",
        .newMessageReaction: "%1$@ hot uf dine %2$@ reagiert",
        .newFriendRequestTitle: "Freundschaftsanfrog von %1$@",
        .newFriendRequestBody: "%1$@ will din Freund sei",
        .newFriendAcceptedTitle: "Freundschaftafrog agno",
        .newFriendAcceptedBody: "%1$@ isch jetzt din Freund",
        .ownBirthdayTitle: "Alles Gute zum Geburtstag! 🎂",
        .ownBirthdayBody: "Viel Spaß und an schöna Tag!",
        .friendBirthdayTitle: "%1$@ hot hüt Geburtstag! 🎂",
        .friendBirthdayBody: "Vergiss ned zum gratuliera!",
    ]
}
