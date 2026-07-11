## Webseite
https://schneaggchatv3.lerchenflo.eu/

# Schneaggchat V3
CMP Multiplatform Chat - App für Android, iOS und Desktop

## Was macht Schneaggchat besonders
- Vorarlbergerisch als Sprachoption
- Umfragen mit custom Antworten
- Userbeschreibungen (Freunde können gemeinsam einen Text über dich verfassen)
- Nachrichten - Reaktionen (Auch mit Text)
- Geburtstagsanzeige + Benachrichtigungen
- App kaputt Button
- Standort mit Verlauf teilen


# Changelog

### 3.0.14

#### Features
- Roadmap in den Misc Einstellungen mit Github issues

#### Bugfixes
- Überlappende Schaltflächen auf der Schneaggmap behoben

### 3.0.13

#### Features
- Freundstandorte-Preview mit Profilbildern auf der Schneaggmap
- Highscores für Spiele + Spiele unified + Verbessert + Neue Spiele
- Finger Picker Spiel
- Map Style Einstellungen
- Open in Maps Button
- Echtzeit Online-Status von Freunden

#### Bugfixes
- Nicknames werden jetzt überall verwendet
- Standortteilen Dialog vereinfacht



### 3.0.12

#### Features
- Standorttypen-Filter in Gruppen organisiert auf der Schneaggmap
- Standorttypen beim Hinzufügen gruppiert
- Standort teilen auf der Schneaggmap
- Datensync Detail Popup für mehr Transparenz
- Message highlight bei beantworteten Nachrichten
- Bilder aus externen Apps teilen

#### Bugfixes
- Umfragen Textinput verbessert + Reorderable
- Snackbar custom UI
- Email verification bugfixes


### 3.0.11

#### Announcements
- Bitte Bugreports / Featurerequests einreichen, falls etwas nicht passt / nicht funktioniert. Es wird nicht alles von alleine besser!

#### Features
- Reaktionenanzeige in Nachrichtendetails
- Datensync beim Öffnen der App über Benachrichtigungen
- Email Provider Warnung bei Registrierung und Einstellungen
- Geburtstags - Anzeige in Chatauswahl
- Neue-Nachrichten-Trennlinie im Chat
- Schneaggmap mit Orten (Noch ohne Benutzerstandorte)

#### Bugfixes
- Zufällige Logouts behoben (hoffentlich endgültig)
- Image Picker verbessert
- Benachrichtigungen werden beim Öffnen eines Chats wieder automatisch entfernt


### 3.0.10
#### Features
- Native Benachrichtigungen (iOS Priorität wie bei WhatsApp, Snapchat, etc)
- Nachrichten Reaktionen
- Geburtstags Benachrichtigung

#### Bugfixes
- Entwicklerstatus direkt übernehmen


### 3.0.9
#### Features
- Nicknames
- Umfrage Markdown Support
- Gruppe umbenennen
- Sprachnachrichten public
- Teilen mit Schneaggchat
- Neue Themes

#### Bugfixes
- Umfragen Profilbilder
- vieles mehr

### 3.0.8
#### Features
- Beta-Tester Knopf in Einstellungen
- Changelog-Anzeige beim Start
- Sprachnachrichten

#### Bugfixes
- Token sync fix (Auto logout)
- Nachrichten senden Input validation fix

### 3.0.7
#### Features
- 

#### Bugfixes
- Offline message sending fix
- Access token validity fix
- Infinite loading screen on notification click fix
- Login crash fix


### 3.0.6
#### Features
- Show birthdate of others

#### Bugfixes
- Fix for my messages showing up as sent by other user
- Fix for login but no data sync
- Auto logout on invalid tokens
- Fix for notifications not showing when app in background
- Fix for navigating out of chat (unselected chat)

- iOS notification badge fix
- iOS update Checker fix


### 3.0.5
#### Features
- change Group Profile Pictures
- save Message as Draft when leaving Chat (only for Text Messages)
- Names on replies
- Bugreport / Feature request form
- Poll show answers
- Password reset button
- show readers in Groups
- Image messages
- Register update with swipeablecards

#### Bugfixes
- Navigate to chat selector when no user is selected in chat (bug when swiping back)
- User limit of 2 warning removed for adding users to Groups
- Poll ui fixes

### 3.0.4
#### Features
- polls
- Secure data store on all platforms
- Improved sync for profile pictures

#### Bugfixes




### 3.0.3 - bugfix 
#### Features
- Games

#### Bugfixes
- Register Screen fix
- Profile picture downscaling before sending to server
- Changed text color in chat


### 3.0.2

#### Features
- Status / Description Settings
- Group member add / Admin status change
- Socket connection updates
- Birthdate change setting

#### Bugfixes
- Android Gradle Plugin 9.0.0 refactoring
- Auto Scroll for Chat + Chat selection + Signup
- Chatdetails + Chatselection UI fixes
- Socketconnection crash fix

### 3.0.1
- Quick bugfixes for iOS

### 3.0.0
- Initial release (User, Groups, Notifications, ProfilePicture)


# Doku
## Networktask
The networktask is very simple structurized: There are two networkclients(ktor), one with JWT Auth (For all authenticated entpoints), and one without Auth (For login, register and refresh).
