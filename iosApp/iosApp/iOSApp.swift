import SwiftUI
import UIKit
import UserNotifications

private enum APNsTokenStore {
    static let tokenKey = "zugspitz.apnsDeviceToken"
    static let registerNotificationName = Notification.Name("zugspitz.registerForRemoteNotifications")
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private var remoteRegistrationObserver: NSObjectProtocol?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        remoteRegistrationObserver = NotificationCenter.default.addObserver(
            forName: APNsTokenStore.registerNotificationName,
            object: nil,
            queue: .main
        ) { _ in
            UIApplication.shared.registerForRemoteNotifications()
        }
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        UserDefaults.standard.set(token, forKey: APNsTokenStore.tokenKey)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        NSLog("APNs registration failed: \(error.localizedDescription)")
    }

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
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ZStack {
                Color(red: 245.0 / 255.0, green: 247.0 / 255.0, blue: 243.0 / 255.0)
                    .ignoresSafeArea()
                ContentView()
            }
        }
    }
}
