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
        let suiteName = "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtention"
        logger.info("DEBUG: App side suite name: \(suiteName, privacy: .public)")
        
        guard let userDefaults = UserDefaults(suiteName: suiteName) else {
            logger.info("DEBUG: App side - UserDefaults suite is NIL")
            return
        }
        
        // Check container URL to verify App Group is actually valid
        if let containerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: suiteName) {
            logger.info("DEBUG: App side container URL: \(containerURL.path, privacy: .public)")
        } else {
            logger.error("DEBUG: App side container URL is NIL (App Group is invalid/not provisioned correctly)")
        }
        
        // Try synchronous read first (works when data is already synced)
        userDefaults.synchronize()
        if let sharedText = userDefaults.string(forKey: "sharedTextKey") {
            logger.info("DEBUG: App side - Found data immediately: \(sharedText, privacy: .public)")
            IncomingDataManager.shared.updateText(text: sharedText)
            userDefaults.removeObject(forKey: "sharedTextKey")
            userDefaults.synchronize()
            return
        }
        
        // Fallback: retry after delay (UserDefaults daemon may not have synced yet on cold start)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [self] in
            userDefaults.synchronize()
            if let sharedText = userDefaults.string(forKey: "sharedTextKey") {
                logger.info("DEBUG: App side - Found data after delay: \(sharedText, privacy: .public)")
                IncomingDataManager.shared.updateText(text: sharedText)
                userDefaults.removeObject(forKey: "sharedTextKey")
                userDefaults.synchronize()
            } else {
                logger.info("DEBUG: App side - No data found for key 'sharedTextKey'")
            }
        }
    }
}
