## Webseite
https://schneaggchatv3.lerchenflo.eu/

# Schneaggchat V3
Neue Version in CMP mit Multiplatform für Android, iOS und Desktop

## What is Schneaggchat?
Schneaggchat is an innovative chatting platform. 

# Changelog

### 3.0.7
#### Features
- 

#### Bugfixes
- Offline message sending fix


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