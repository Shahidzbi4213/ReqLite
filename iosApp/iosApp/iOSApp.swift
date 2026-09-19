import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        DiHelper.shared.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            WorkspaceView()
        }
    }
}
