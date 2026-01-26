import Foundation
import UIKit

public class AppUpdateChecker {
    public static func checkForUpdate() {
        guard let info = Bundle.main.infoDictionary,
              let currentVersion = info["CFBundleShortVersionString"] as? String,
              let bundleId = Bundle.main.bundleIdentifier,
              let url = URL(string: "https://itunes.apple.com/lookup?bundleId=\(bundleId)") else { return }
        URLSession.shared.dataTask(with: url) { data, _, _ in
            guard let data = data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let results = (json["results"] as? [[String: Any]])?.first,
                  let latestVersion = results["version"] as? String,
                  let appStoreURL = results["trackViewUrl"] as? String else { return }
            if latestVersion.compare(currentVersion, options: .numeric) == .orderedDescending {
                showUpdateSuggestionPopUp(appStoreURL: appStoreURL)
            }
        }.resume()
    }
    private static func showUpdateSuggestionPopUp(appStoreURL: String) {
        DispatchQueue.main.async {
            let alert = UIAlertController(
                title: "Update Available",
                message: "A new version of the app is available. Would you like to update now?",
                preferredStyle: .alert
            )
            alert.addAction(UIAlertAction(title: "Update", style: .default) { _ in
                openAppStore(appStoreURL: appStoreURL)
            })
            alert.addAction(UIAlertAction(title: "Later", style: .cancel))
            if let scene = UIApplication.shared.connectedScenes
                .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
               let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController {
                rootVC.present(alert, animated: true)
            }
        }
    }
    private static func openAppStore(appStoreURL: String) {
        if let url = URL(string: appStoreURL) {
            UIApplication.shared.open(url)
        }
    }
}
