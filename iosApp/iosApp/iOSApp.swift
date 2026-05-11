import FirebaseCore
import SwiftUI

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
    }

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
