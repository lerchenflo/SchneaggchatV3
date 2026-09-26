## Webseite
https://schneaggchatv3.lerchenflo.eu/

# Schneaggchat V3
CMP Multiplatform Chat - App für Android, iOS und Desktop

## Was macht Schneaggchat besonders
- Vorarlbergerisch als Sprachoption
- Umfragen mit eigenen Antworten
- Userbeschreibungen (Freunde können gemeinsam einen Text über dich verfassen)
- Nachrichten - Reaktionen (Auch mit Text)
- Geburtstagsanzeige + Benachrichtigungen
- Standort mit Verlauf teilen
- Bearbeitbare Karte mit öffentlichen Orten
- @Annotations für Standorte + User + Spiele + Events
- Weckerfeature (Wecker-ton bei Freunden abspielen, auch wenn das Gerät auf lautlos ist)
- App kaputt Button


# Changelog

### 3.0.18

#### Features
- Spiele: Spielstand wird gespeichert und beim Zurückkehren wiederhergestellt (Tetris, Tower Stack, 2048, SchneaggaHus, Grid Rush, Odd One Out, Morse Challenge, Kreuzworträtsel, Yatzi, Dart Counter, Undercover), Undercover zeigt beim Fortsetzen nie ein geheimes Wort erneut, Yatzi und Dart Counter haben Leaderboards (Trophy-Knopf im Spielselektor und Highscores-Knopf), Undercover hat ein Wins-Leaderboard (Trophy-Knopf im Spielselektor, Bestätigungsdialog beim Spielende), Bestätigungsdialog zum Hochladen von Spielergebnissen (Yatzi: Endscores, Dart Counter: Drei-Dart-Durchschnitte für 301/501), Spiele zählen nicht zum globalen Ranking aber erscheinen im Recap, Yatzi und Undercover: Zurücksetzen-Knopf in der Titelleiste zum Verwerfen des Spielstands (mit Bestätigung)
- Neue Spiele: SchneaggaHus, Wordle + Kreuzworträtsel-Update
- SchneaggaHus: Wellen-Spielmechanik mit Farb-Vorschau und Wellenfortschritt, gleitende Schneaggs auf gekrümmten Schienen, Porträt-Spielfeld mit größeren Tiles, neues visuelles Design im Train-of-Thought-Stil, Spawn-Tempo passt sich der Leistung an (schneller bei richtigen Zustellungen, langsamer nach Fehlern)
- Tower Stack: Schneagg reitet auf jedem neuen sich bewegenden Balken, farblich dem Balken angepasst, schaut in Fahrtrichtung und springt ab (mit Hop, Fall und Fade-out), wenn der Balken platziert wird
- Dartcounter: Checkout-Karte entfernt, Player-Anzeige mit Score/Darts/Checkout-Pfad, Scoreboard kompakt, Text-Clipping behoben
- Undercover: Restart mit gleichen Spielern, Mr. White Hinweis-Setting, viele neue Wortpaare, "Sniff"-Knopf um sein eigenes Wort bei Auswahl, Diskussion und Abstimmung anzusehen
- Spiele: Spiele-Menü in drei Tabs aufgeteilt - "Einzel", "Mehrspieler" (Yahtzee, Dartcounter, Undercover) und "Werkzeuge" (Coin Flip, Finger Picker, Gemischrechner); "Spiele ohne Bestenliste" Sektion entfernt, die Schwierigkeits-Chips erscheinen nur noch im Einzel-Tab
- Werkzeug: Gemischrechner für 2-Takt-Gemisch mit Mischungsverhältnissen 1:25, 1:32, 1:40, 1:50, 1:60, 1:100 oder frei von 1:1 bis 1:200
- Spiele: Schwierigkeits-Symbol auf jeder Spielkarte im Spiele-Menü, links vom Bestenlisten-Knopf (Balken für die App-weite Schwierigkeit, Regler-Symbol wenn die Einstellung im Spiel selbst gewählt wird)
- Spiele: Kurzbeschreibung auf jeder Spiel- und Werkzeugkarte im Spiele-Menü
- Events: Gewählte Ansicht bleibt erhalten, Upcomming Events, Ungelesen Indikator
- Chat: Geteilte Inhalte (Bilder und Links) in Chat-Details
- Schneaggmap: Daten Picker update, neue Types, neu sortiert
- Freundschaftsanfragen: Tippen auf Profilbilder öffnet Bildvorschau
- Quick reactions anpassen in Einstellungen
- Android Auto Support
- Doppelte Nachrichten (Ungesendete) Behoben


#### Bugfixes
- Spiele: extrem viele Bugfixes in fast allen Spielen, Spielstände für ungespielte Durchläufe werden nicht mehr gespeichert, Geräterotation führt nicht mehr zu einem Reset beim Spielen, Tower Stack Lifecycle-Fehler behoben
- Tower Stack: Balken-Platzierung wird jetzt beim Tippen ausgelöst statt beim Loslassen, wahrgenommene Input-Verzögerung behoben
- Dart Counter: Rückgängig nach einem überworfenen Dart (Bust) korrigiert - der Bust-Dart wurde übersprungen, danach stimmten Punktestand und Dart-Zähler nicht mehr; alte Spielstände werden beim Update verworfen, Mehrspieler-Spielfeld friert nicht mehr ein, wenn ein Spieler mit dem ersten oder zweiten Dart auscheckt (der fertige Spieler blieb aktiv und es konnte kein Dart mehr geworfen werden; eingefrorene Spielstände reparieren sich beim Fortsetzen), Rückgängig ist deaktiviert, sobald das Ergebnis auf der Bestenliste ist
- Kreuzworträtsel: Buchstaben beim schnellen Tippen gehen nicht mehr verloren, Bestenliste behoben (zeigt Deutsch/English Chips statt Schwierigkeits-Chips, öffnet auf existierendem Board)
- Undercover: Crash + Logs viewer, Start-Knopf beim Öffnen deaktiviert bis Wortliste geladen ist, Geheimwort bleibt nicht mehr sichtbar nach dem Hintergrund der App (geht hinter „Telefon weitergeben“-Bildschirm), automatischer Hide-Timer läuft nicht mehr im Hintergrund
- Tetris: Laufendes Spiel wird nicht mehr gelöscht, wenn es vor Abräumen der ersten Linie in den Hintergrund geht
- SchneaggaHus: Durchgang mit 0 Punkten wird nicht mehr verworfen, wenn die App in den Hintergrund geht - das war ein kostenloser Neustart mit allen Leben, sobald man auf dem letzten Leben war
- Grid Rush: Das Ergebnis der täglichen Herausforderung wird nicht mehr gelöscht, wenn man sie über den „Verlassen“-Knopf im Spielende-Bildschirm verlässt - das gab einen zweiten Versuch am selben Rätsel
- Morse Challenge: Zeichen-Countdown kann nicht mehr durch ungültige Codes unbegrenzt pausiert werden
- Kreuzworträtsel und Wordle: Spielzeit läuft nicht mehr weiter, wenn das Spiel nicht im Fokus ist (stoppt bei Benachrichtigung, wird fortgesetzt bei Rückkehr)
- Anmeldung: Token-Refresh rebuild (5. mol??), Leerer Bildschirm nach Hintergrundbeendigung behoben (Session wird wiederhergestellt)
- Chat: Nachrichtenentwürfe werden beim Öffnen eines Chats wiederhergestellt, Antwort-Vorschau reagiert responsiv auf Bildschirmhöhe, Nachrichten werden korrekt als gelesen markiert, wenn der Chat über eine Benachrichtigung geöffnet wird
- Events: Crash bei mehrtägigen Events behoben, Eigenes Profilbild und Name werden überall als Event-Ersteller/-teilnehmer angezeigt
- Android Noti-Fehler behoben
- Dialog-Popups: ModalBottomSheet zu Dialog konvertiert um Fehler bei schneller Navigation zu beheben
- Bestenlisten: keine leere Zeit-Spalte mehr bei Spielen ohne Timer (Yatzi, Dart Counter, Undercover), Tages-Zeitraum nur noch bei Spielen mit täglich neuem Board (Kreuzworträtsel, Grid Rush), Ladeindikator beim Öffnen statt "Keine Bestenlisten"-Blinken, Retry-Knopf bei Fehlern, Dialog-Inhalt bleibt beim Schwierigkeitswechsel erhalten, Überschrift über Filter-Chips, Shared-Device-Hochladen: keine leeren Submissions, Upload-Dialog wird nicht durch verspätete Server-Antworten reaktiviert, Hard-Timeout gegen eingefrorene Upload-Dialogs, Kreuzworträtsel/Wordle/Dartcounter öffnen jetzt die richtige Bestenliste (verwendeten vorher fälschlicherweise die App-weite Schwierigkeit), Hinweis am Spielende, wenn kein Spieler ein Konto hat und deshalb nichts hochgeladen wurde (Yatzi, Dartcounter, Undercover)
- Wordle: Bestenliste funktioniert wieder (das Spiel war nach einem Merge nicht mehr mit der Bestenliste verbunden), das Ergebnis wird als Anzahl Versuche angezeigt statt als interner Punktewert
- Bugreport-E-Mail (iOS): Umlaute, Sonderzeichen und Emojis werden nicht mehr verstümmelt (mailto-Link wird korrekt UTF-8-kodiert)
- Spieler-Auswahl: Alle Texte sind jetzt übersetzt (waren fest auf Englisch)
- Gesamtwertung: Erklärungstext korrigiert (Yatzi, Dartcounter und Undercover sind vom globalen Ranking ausgeschlossen)

### 3.0.17

##### Highlights
- Schneaggmap-Kompass
- Bottom Nav Badges
- Events: Beigetretene User anzeigen

#### Features
- Chat-Nachrichtenliste: Sticky Date Chip beim Scrollen
- Events: Nach Tagen gruppiert mit Sticky Header
- Chat-Nachrichtenliste: Zu neuen Nachrichten springen Pfeil
- Umfragen: Option Checkboxen anzeigen & Option löschen
- Geburtstagsliste
- Beta Tests für iOS
- Italienisch als Sprache
- Schneaggmap-Kompass: Livekompass mit Richtungs- und Entfernungsanzeige für Freunde
- Kreuzworträtsel-Daily-Challenge
- Neue Nachrichten über Benachrichtigungen direkt im Chat anzeigen
- Doppelte Nachrichten fix
- Events: Woche und Monat Kalenderansicht mit Geburtstagen, zum Kalender hinzufügen
- Morse-Spiel: Hörbarer Piepton beim Tastendruck
- Events: Beigetretene / Abgelehnte User anzeigen
- Bottom Nav: Badge Icons

#### Bugfixes
- Chat-Auswahl lädt beim App-Start schneller (optimierte Datenbankabfragen und Profilbilder-Caching)
- Chat-Nachrichten: Zeilenumbrüche im Markdown-Modus behoben
- Schneaggmap: Suche verbessert
- Event Farbe auf der Map repariert
- Event bearbeiten/erstellen Bug behoben
- iOS Tastatur-Verhalten und Anzeige unter Chat-Eingabe behoben
- Schneaggmap: Überlappende Marker-Icons bei Orten mit mehreren Typen behoben
- Umfragen: App - Crash bei ungeschickten Umfragen behoben, UI verbessert
- Gruppenmitglieder doppelt anzeigen
- Sprachnachrichten: Audio-Routing für Bluetooth-Geräte behoben (AirPods, Auto, Lautsprecher)
- Dartcounter: Funktion und UI verbessert
- Undercover-Spiel: Spielerliste bleibt nach einer Runde erhalten
- Bilder rotiert anzeigen fix

### 3.0.16

##### Highlights
- Events
- Telefonnummer teilen
- Systemnachrichten im Chat (Mitglied hinzugefügt etc.)
- Bottom Navigation

#### Features
- Einführung in die Features
- Events
- Telefonnummer teilen (Optional)
- Gruppen - lösch - Countdown
- Bottom Navigation Bar mit Swipen
- Systemnachrichten im Chat (Gruppenereignisse wie Mitglieder hinzugefügt, Gruppenname geändert, Admin-Status vergeben, Freundschaft angenommen, etc.)
- Chat öffnen aus Benachrichtigungen
- Annas Theme & Flos Theme
- Reaktionszeitpunkt in Nachrichtendetails anzeigen
- @Game und @Event annotations im Chat
- Ungelöste @Annotations zeigen Platzhalter statt ungültiger IDs
- 2048 Spiel
- Schneaggmap: Anzahl zusammengefasster Freunde im Hock-Marker in der Mitte anzeigen
- Schneaggmap: Eigenen Standort im Hock-Marker anzeigen (wenn im Radius) und eigenen Standort-Marker ausblenden 
- Neue Schneaggmap-Standorttypen: Eis (Preis pro Kugel) und WiFi (SSID und Passwort)
- Zurückknopf auf IOS verwendet nun das IOS - Icon
- Versionsabfrage Desktop
- Pausetaste für Spiele
- Ganze Gruppen adden bei Userauswahl
- Einstellungen auf Server synchen

#### Bugfixes
- Fingerpicker resetet nicht direkt
- Infinite loading screen beim Öffnen der App über Benachrichtigungen behoben
- Navigation Rebuild (Viele Bugs behoben)
- Einstellungen neu strukturiert
- Morse Code verbessert
- Kurz den richtigen Tile anzeigen bei odd one out
- Einstellung für farbige Zeit bei heutigen Nachrichten (Standard aus)
- Umfragen Titelformatierung
- Antworten auf große Nachrichten
- Umfragen mit leeren optionen wenn eigene Antworten erlaubt sind
- Bilder Zoom fix
- Nachricht bearbeiten, neue Nachricht bei Enter (Desktop)
- Register Bug bei schlechtem Internet behoben
- Website link icon in der Chatauswahl
- Nachrichtensync beschleunigt
- Token Refresh verbessert

### 3.0.15
##### Highlights
- Weckerfeature

#### Features
- Weckerfeature
- Standorteinträge zeigen letzten Bearbeiter an
- Benachrichtigungen "Als gelesen markieren" button
- Umfragen Custom optionen mit Abstimmungslimiteinstellung
- Map Suchfeld + neue UI
- Map Richtungsanzeige für Freunde
- Heutige Nachrichten in der Chatauswahl mit farbigem Datum
- Kleinerer abstand zwischen Nachrichten die weniger als 30sec hintereinander gesendet wurden

#### Bugfixes
- Profilbilder neu laden repariert
- Map Popup scroll fix
- Morse code tree voll angezeigt
- Tägliche Spiele Timer + Änderung um Mitternacht
- Fingerpicker fix

### 3.0.14

#### Highlights
- @User & @Map Annotations
- Nachrichtensuche in der Chatauswahl
- Umfragen mit Stimmenbegrenzung
- Neue Standorttypen

#### Features
- Nachrichtensuche in der Chat-Auswahl - durchsucht Nachrichteninhalte, Bildunterschriften und Umfragen
- @Map & @User Annotations in Nachrichten + Umfragen
- Roadmap in den Misc Einstellungen mit GitHub issues
- Neue Schneaggmap-Standorttypen (Volleyball, Trainingspark, Tischtennis, Tennis, Fahrrad und Enduro)
- Freunde-Zusammenfassung beim Zoomen - Einstellung auf der Schneaggmap
- Polls mit Stimmen-Begrenzung pro Option
- Gesamtranking über alle Spiele mit Zeitraumselektor
- Tetris nächster Block Vorschau in der oberen linken Ecke
- Schneaggahus Spiel verbessert

#### Bugfixes
- Schneaggmap UI fixes
- Chat-Navigation neu aufgebaut
- Chat öffnen bei Freundschaftsanfrage annehmen / Gruppe erstellen
- Tetris Steuerung repariert

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
