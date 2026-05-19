## Webseite
https://schneaggchatv3.lerchenflo.eu/

# Schneaggchat V3
Neue Version in CMP mit Multiplatform für Android, iOS und Desktop

## What is Schneaggchat?
Schneaggchat is an innovative chatting platform. 

# Changelog

### 3.0.11
#### Features
- Schneaggmap live sync: Room database for map entries, subtypes, and main types (DB version 57)
- MapRepository mit Delta-Sync für Kartendaten und Subtypen
- AppRepository.mapSync() für parallele Synchronisation von Einträgen, Subtypen und Haupttypen
- WebSocket live updates für mapchange und subtypechange Events (direkte Datenbank-Updates)
- Server: POST /map/subtypes/sync Delta-Sync Endpoint mit lastChangedAt Timestamp

#### Bugfixes
- Removed hardcoded sample data from map presentation layer
- Removed stale UserLocation overlay

### 3.0.10
#### Features
- Android push notifications now use Firebase Cloud Messaging (FCM) with native service integration
- iOS push notifications now use native APNs with UNUserNotificationCenter
- Shared Kotlin `Notifier` expect/actual for platform-specific token management and local notifications
- Added `Message.toNotificationContent()` for localized notification text generation
- Added `PayloadDecoder` for FCM data payload parsing
- Desktop (JVM) includes no-op Notifier implementation
- Android POST_NOTIFICATIONS permission now requested via ActivityResultContracts
- Server APNs payloads now include sender name and `interruption-level: time-sensitive` for iOS 15+

#### Bugfixes
- Removed kmpnotifier library entirely, replacing with platform-native implementations
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