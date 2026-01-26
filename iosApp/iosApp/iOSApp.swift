import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseMessaging

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

        NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
            return UIBackgroundFetchResult.newData
        }

}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    // 🔹 Delay the update check slightly to allow Firebase and the app environment to finish initializing.
                    // Without this delay, calling `AppUpdateChecker.checkForUpdate()` too early may trigger the error:
                    // "Attempted to call Firebase before it has been configured."
                    // This happens because FirebaseApp.configure() runs asynchronously at startup, and the App Store
                    // configuration check requires Firebase to be fully initialized.
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                        AppUpdateChecker.checkForUpdate()
                    }
                }
        }
    }
}

