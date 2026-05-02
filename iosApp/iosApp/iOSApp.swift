import SwiftUI
import ComposeApp
import UserNotifications
import os


class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        // NotificationManager.initialize() (permission + registerForRemoteNotifications) is called
        // from MainViewController.configure after Koin is started.
        return true
    }

    // Raw APNs device token → hex string into Kotlin
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let hex = deviceToken.map { String(format: "%02x", $0) }.joined()
        IosPushTokenStore.shared.onApnsToken(hexToken: hex)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("[APNs] Failed to register: \(error.localizedDescription)")
    }

    // Show banner even when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}


@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) var scenePhase
    let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "sharing")

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
                    if url.scheme == "schneaggchat" {
                        logger.info("DEBUG onOpenURL Log")
                        handleIncomingShare()
                    }
                }
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                logger.info("DEBUG onChange Log")
                handleIncomingShare()
                UIApplication.shared.applicationIconBadgeNumber = 0
            }
        }
    }

    private func handleIncomingShare() {
        let suiteName = "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtention"
        logger.info("DEBUG: App side suite name: \(suiteName, privacy: .public)")

        guard let userDefaults = UserDefaults(suiteName: suiteName) else {
            logger.info("DEBUG: App side - UserDefaults suite is NIL")
            return
        }

        if let containerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: suiteName) {
            logger.info("DEBUG: App side container URL: \(containerURL.path, privacy: .public)")
        } else {
            logger.error("DEBUG: App side container URL is NIL (App Group is invalid/not provisioned correctly)")
        }

        userDefaults.synchronize()
        if let sharedText = userDefaults.string(forKey: "sharedTextKey") {
            logger.info("DEBUG: App side - Found data immediately: \(sharedText, privacy: .public)")
            IncomingDataManager.shared.updateText(text: sharedText)
            userDefaults.removeObject(forKey: "sharedTextKey")
            userDefaults.synchronize()
            return
        }

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
