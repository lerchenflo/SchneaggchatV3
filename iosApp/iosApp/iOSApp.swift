import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

//Des isch alles abgschrieba vo nam yt tutorial ohne autokorrektur
class AppDelegate: NSObject, UIApplicationDelegate{
    func application(_ application; UIApplication, didFinishLaunchingWithOptions launchOptions:
        [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        InitKoinKt.doInitKoin()

        return true
    }

}