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
    case newEventTitle
    case mapSightingPoliceTitle
    case mapSightingRadarTitle
    case eventReminderTitle
    case eventReminderBody
    case eventReminderBodyAt
    case leadDay
    case leadDays
    case leadHours
    case leadMinutes
    case eventCancelledBody
    case eventStartAndLocationBody
    case eventStartBody
    case eventLocationBody
    case eventChangedBody
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
        switch parts.first {
        case "de":
            if parts.count > 1, parts[1] == "at" || parts[1] == "rat" {
                return austrianGermanStrings
            }
            return germanStrings
        case "it":
            return italianStrings
        default:
            return englishStrings
        }
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
        .newEventTitle: "%1$@ created a new event",
        .mapSightingPoliceTitle: "Police seen",
        .mapSightingRadarTitle: "Mobile radar seen",
        .eventReminderTitle: "Reminder: %1$@",
        .eventReminderBody: "Starts in %1$@",
        .eventReminderBodyAt: "Starts at %1$@",
        .leadDay: "%1$ld day",
        .leadDays: "%1$ld days",
        .leadHours: "%1$ld h",
        .leadMinutes: "%1$ld min",
        .eventCancelledBody: "%1$@ cancelled the event",
        .eventStartAndLocationBody: "%1$@ moved the start to %2$@ and changed the location",
        .eventStartBody: "%1$@ moved the start to %2$@",
        .eventLocationBody: "%1$@ changed the location",
        .eventChangedBody: "%1$@ updated the event",
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
        .newEventTitle: "%1$@ hat ein neues Event erstellt",
        .mapSightingPoliceTitle: "Polizei gesichtet",
        .mapSightingRadarTitle: "Mobiles Radar gesichtet",
        .eventReminderTitle: "Erinnerung: %1$@",
        .eventReminderBody: "Beginnt in %1$@",
        .eventReminderBodyAt: "Beginnt am %1$@",
        .leadDay: "%1$ld Tag",
        .leadDays: "%1$ld Tage",
        .leadHours: "%1$ld Std.",
        .leadMinutes: "%1$ld Min.",
        .eventCancelledBody: "%1$@ hat das Event abgesagt",
        .eventStartAndLocationBody: "%1$@ hat den Start auf %2$@ verschoben und den Ort geändert",
        .eventStartBody: "%1$@ hat den Start auf %2$@ verschoben",
        .eventLocationBody: "%1$@ hat den Ort geändert",
        .eventChangedBody: "%1$@ hat das Event aktualisiert",
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
        .newEventTitle: "%1$@ hot a neus Event erstellt",
    ]

    private static let italianStrings: [NotificationStringKey: String] = [
        .image: "immagine",
        .audio: "audio",
        .poll: "sondaggio",
        .message: "messaggio",
        .youHaveNewMessages: "Hai nuovi messaggi",
        .newMessageSingleTitle: "Nuova chat da %1$@",
        .newMessageGroupTitle: "Nuova chat da %1$@ in %2$@",
        .newMessageReaction: "%1$@ ha reagito al tuo %2$@",
        .newFriendRequestTitle: "Richiesta di amicizia da %1$@",
        .newFriendRequestBody: "%1$@ vuole essere tuo amico",
        .newFriendAcceptedTitle: "Richiesta di amicizia accettata",
        .newFriendAcceptedBody: "%1$@ è ora tuo amico",
        .ownBirthdayTitle: "Buon compleanno! 🎂",
        .ownBirthdayBody: "Ti auguriamo una splendida giornata!",
        .friendBirthdayTitle: "%1$@ oggi compie gli anni! 🎂",
        .friendBirthdayBody: "Non dimenticare di fargli gli auguri!",
        .newEventTitle: "%1$@ ha creato un nuovo evento",
        .mapSightingPoliceTitle: "Polizia avvistata",
        .mapSightingRadarTitle: "Autovelox mobile avvistato",
        .eventReminderTitle: "Promemoria: %1$@",
        .eventReminderBody: "Inizia tra %1$@",
        .eventReminderBodyAt: "Inizia il %1$@",
        .leadDay: "%1$ld giorno",
        .leadDays: "%1$ld giorni",
        .leadHours: "%1$ld h",
        .leadMinutes: "%1$ld min",
        .eventCancelledBody: "%1$@ ha annullato l'evento",
        .eventStartAndLocationBody: "%1$@ ha spostato l'inizio a %2$@ e cambiato il luogo",
        .eventStartBody: "%1$@ ha spostato l'inizio a %2$@",
        .eventLocationBody: "%1$@ ha cambiato il luogo",
        .eventChangedBody: "%1$@ ha aggiornato l'evento",
    ]
}
