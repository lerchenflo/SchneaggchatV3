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

            let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
            print("APNs Token: \(token)")

            Messaging.messaging().token { token, error in
                if let error = error {
                    print("Error fetching FCM token: \(error)")
                } else if let token = token {
                    print("FCM Token: \(token)")
                }
            }
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
        }
    }
}

