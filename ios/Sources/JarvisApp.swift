import SwiftUI
import AVFAudio

@main
struct JarvisApp: App {
    @State private var configured: Bool = ConfigStore.isConfigured

    var body: some Scene {
        WindowGroup {
            Group {
                if configured {
                    MainView()
                } else {
                    SetupView { configured = ConfigStore.isConfigured }
                }
            }
            .preferredColorScheme(.dark)
            .statusBar(hidden: true)
            .onAppear { requestMicrophone() }
        }
    }

    private func requestMicrophone() {
        let session = AVAudioSession.sharedInstance()
        if #available(iOS 17.0, *) {
            AVAudioApplication.requestRecordPermission { _ in }
        } else {
            session.requestRecordPermission { _ in }
        }
    }
}
