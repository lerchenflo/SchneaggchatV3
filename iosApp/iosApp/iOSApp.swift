import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseMessaging
import os


class AppDelegate: NSObject, UIApplicationDelegate {

  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

      FirebaseApp.configure() //important

      //By default showPushNotification value is true.
      //When set showPushNotification to false foreground push  notification will not be shown.
      //You can still get notification content using #onPushNotification listener method.
      NotifierManager.shared.initialize(configuration: NotificationPlatformConfigurationIos(
            showPushNotification: true,
            askNotificationPermissionOnStart: true,
            notificationSoundName: nil
          )
      )

      // Initialize custom notification manager for encrypted payload processing
      NotificationManager.shared.initialize()

    return true
  }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
            Messaging.messaging().apnsToken = deviceToken
    }
        
        
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) async -> UIBackgroundFetchResult {
        print("IOS Notification received")

        UIApplication.shared.applicationIconBadgeNumber += 1

        NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
            return UIBackgroundFetchResult.newData
        }

}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) var scenePhase  // ← Add this
    let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "sharing");

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    logger.info("DEBUG onAppear Log")
                
                    handleIncomingShare()
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                        AppUpdateChecker.checkForUpdate()
                    }
                    UIApplication.shared.applicationIconBadgeNumber = 0
                }
                .onOpenURL { url in
                                    // 2. Handle the Redirection from the Share Extension
                                    // This triggers when 'schneaggchat://share' is called
                                    if url.scheme == "schneaggchat" {
                                        logger.info("DEBUG onOpenURL Log")
                                        handleIncomingShare()
                                    }
                                }
        }
        .onChange(of: scenePhase) { phase in  // ← Note: .onChange should be on WindowGroup, not ContentView
            if phase == .active {
                logger.info("DEBUG onChange Log")
                handleIncomingShare()
                UIApplication.shared.applicationIconBadgeNumber = 0
            }
        }
    }
    
    // Helper to bridge data from App Group to Kotlin
    private func handleIncomingShare() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            let bundleId = Bundle.main.bundleIdentifier ?? "org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp"
            // REPLACE with your actual App Group ID from Xcode
            let suiteName = "group." + bundleId
            NSLog("DEBUG: App side suite name: " + suiteName)
            logger.info("DEBUG: App side suite name: \(suiteName, privacy: .public)")
            let userDefaults = UserDefaults(suiteName: suiteName)
            
            if userDefaults == nil {
                NSLog("DEBUG: App side - UserDefaults suite is NIL")
                logger.info("DEBUG: App side - UserDefaults suite is NIL")
            }
            
            // Check container URL to verify App Group is actually valid
            if let containerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: suiteName) {
                logger.info("DEBUG: App side container URL: \(containerURL.path, privacy: .public)")
            } else {
                logger.error("DEBUG: App side container URL is NIL (App Group is invalid/not provisioned correctly)")
            }
            
            // Force read from disk (since cfprefsd might be delayed)
            userDefaults?.synchronize()
            
            if let sharedText = userDefaults?.string(forKey: "sharedTextKey") {
                NSLog("DEBUG: App side - Found data: \(sharedText)")
                logger.info("DEBUG: App side - Found data: \(sharedText, privacy: .public)")
                
                // Update your Kotlin Common code
                IncomingDataManager.shared.updateText(text: sharedText)
                
                // CRITICAL: Clear the data so it doesn't trigger again on next app open
                userDefaults?.removeObject(forKey: "sharedTextKey")
                userDefaults?.synchronize()
            }else {
                NSLog("DEBUG: App side - No data found for key 'sharedTextKey'")
                logger.info("DEBUG: App side - No data found for key 'sharedTextKey'")
            }
        }
    }
}
