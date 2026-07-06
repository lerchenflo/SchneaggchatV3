import SwiftUI
import ComposeApp
import UserNotifications
import os


class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        UNUserNotificationCenter.current().delegate = self

        // Always register — APNs token is needed regardless of display permission
        application.registerForRemoteNotifications()

        // Ask for display permission separately (alert, sound, badge)
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }

        NotificationManager.shared.initialize()

        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let hexToken = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("[APNs] Token received: \(hexToken)")
        IosPushDelegateBridge().onTokenReceived(hexToken: hexToken)
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[APNs] Failed to register: \(error.localizedDescription)")
    }

    // Foreground: show notification as banner
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let data = apnsUserInfoToStringMap(notification.request.content.userInfo)
        IosPushDelegateBridge().onForegroundPayload(data: data)
        completionHandler([.banner, .sound, .badge])
    }

    // Tap on notification
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = apnsUserInfoToStringMap(response.notification.request.content.userInfo)
        IosPushDelegateBridge().onNotificationTap(data: data)
        completionHandler()
    }

    private func apnsUserInfoToStringMap(_ userInfo: [AnyHashable: Any]) -> [String: String] {
        var result = [String: String]()
        for (key, value) in userInfo {
            if let k = key as? String {
                result[k] = "\(value)"
            }
        }
        return result
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
        let suiteName = "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtension"
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

        // Check for shared images
        if let imageKeys = userDefaults.array(forKey: "sharedImageKeys") as? [String], !imageKeys.isEmpty {
            guard let containerURL = FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: suiteName
            ) else { return }

            let imagesDir = containerURL.appendingPathComponent("SharedImages")
            var imageDataList: [KotlinByteArray] = []

            for key in imageKeys {
                let fileURL = imagesDir.appendingPathComponent(key)
                if let data = try? Data(contentsOf: fileURL) {
                    let kotlinArray = KotlinByteArray(size: Int32(data.count))
                    data.withUnsafeBytes { ptr in
                        for i in 0..<data.count {
                            kotlinArray.set(index: Int32(i), value: ptr[i])
                        }
                    }
                    imageDataList.append(kotlinArray)
                }
                try? FileManager.default.removeItem(at: fileURL)
            }

            if !imageDataList.isEmpty {
                IncomingDataManager.shared.updateImages(images: imageDataList)
            }

            userDefaults.removeObject(forKey: "sharedImageKeys")
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
            } else if let imageKeys = userDefaults.array(forKey: "sharedImageKeys") as? [String], !imageKeys.isEmpty {
                guard let containerURL = FileManager.default.containerURL(
                    forSecurityApplicationGroupIdentifier: suiteName
                ) else { return }

                let imagesDir = containerURL.appendingPathComponent("SharedImages")
                var imageDataList: [KotlinByteArray] = []

                for key in imageKeys {
                    let fileURL = imagesDir.appendingPathComponent(key)
                    if let data = try? Data(contentsOf: fileURL) {
                        let kotlinArray = KotlinByteArray(size: Int32(data.count))
                        data.withUnsafeBytes { ptr in
                            for i in 0..<data.count {
                                kotlinArray.set(index: Int32(i), value: ptr[i])
                            }
                        }
                        imageDataList.append(kotlinArray)
                    }
                    try? FileManager.default.removeItem(at: fileURL)
                }

                if !imageDataList.isEmpty {
                    IncomingDataManager.shared.updateImages(images: imageDataList)
                }

                userDefaults.removeObject(forKey: "sharedImageKeys")
                userDefaults.synchronize()
            } else {
                logger.info("DEBUG: App side - No data found for key 'sharedTextKey'")
            }
        }
    }
}
