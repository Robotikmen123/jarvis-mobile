import Foundation
import Combine

/// Owns the WebSocket client + audio I/O. Mirrors live/LiveSession.kt.
/// Phase 1: no tools yet — tool calls are acknowledged with a stub message.
@MainActor
final class LiveSession: ObservableObject {

    enum State { case idle, connecting, listening, thinking, speaking, muted, error }

    @Published var state: State = .idle
    @Published private(set) var log: [String] = []

    private var client: GeminiLiveClient?
    private var audio: AudioIO?
    private var bridge: Bridge?
    private var welcomeSent = false
    private var reconnectTask: Task<Void, Never>?
    private var stopped = false

    func start() {
        stopped = false
        state = .connecting
        appendLog("SYS: Baglaniliyor...")
        connect()
    }

    func stop() {
        stopped = true
        reconnectTask?.cancel()
        client?.close()
        client = nil
        audio?.stop()
        audio = nil
        state = .idle
    }

    func toggleMute() {
        guard let a = audio else { return }
        a.muted.toggle()
        state = a.muted ? .muted : .listening
        appendLog(a.muted ? "SYS: Mikrofon kapali." : "SYS: Mikrofon acik.")
    }

    var isMuted: Bool { audio?.muted ?? false }

    func sendText(_ t: String) {
        appendLog("Sen: \(t)")
        client?.sendUserText(t)
        state = .thinking
    }

    private func connect() {
        guard let key = ConfigStore.geminiKey, !key.isEmpty else {
            state = .error
            appendLog("ERR: Gemini API key yok.")
            return
        }
        let voice = ConfigStore.voiceName
        let sysPrompt = buildSystemInstruction()
        let tools = ToolDeclarations.empty   // Faz 3'te dolacak

        let c = GeminiLiveClient(apiKey: key, voiceName: voice)
        let a = AudioIO(onMicChunk: { [weak c] chunk in c?.sendMicChunk(chunk) })
        let b = Bridge(owner: self)
        self.client = c
        self.audio = a
        self.bridge = b
        c.connect(systemInstruction: sysPrompt, tools: tools, listener: b)
    }

    fileprivate func appendLog(_ line: String) {
        log.append(line)
        if log.count > 120 { log.removeFirst(log.count - 120) }
    }

    private func buildSystemInstruction() -> String {
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US")
        df.dateFormat = "EEEE, MMMM d, yyyy — h:mm a"
        let timeCtx = """
        [CURRENT DATE & TIME]
        Right now it is: \(df.string(from: Date()))
        Use this to calculate exact times for reminders.

        """
        let mem = MemoryStore.shared.formatForPrompt()
        let core: String = {
            if let url = Bundle.main.url(forResource: "prompt", withExtension: "txt"),
               let s = try? String(contentsOf: url) { return s }
            return "You are JARVIS, an efficient assistant. Use tools, no fluff."
        }()
        return timeCtx + (mem.isEmpty ? "" : mem + "\n") + core
    }

    private func scheduleReconnect() {
        guard !stopped, reconnectTask?.isCancelled ?? true else { return }
        reconnectTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            await MainActor.run {
                guard let self, !self.stopped else { return }
                self.audio?.stop()
                self.audio = nil
                self.client = nil
                self.appendLog("SYS: Yeniden baglaniliyor...")
                self.connect()
            }
        }
    }

    private func handleSetupComplete() {
        do {
            try audio?.start()
            state = .listening
            appendLog("SYS: JARVIS online.")
            speakWelcomeIfNeeded()
        } catch {
            state = .error
            appendLog("ERR: audio start: \(error.localizedDescription)")
        }
    }

    private func speakWelcomeIfNeeded() {
        guard !welcomeSent else { return }
        welcomeSent = true
        Task { [weak self] in
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            await MainActor.run {
                guard let self else { return }
                let msg = Welcome.build()
                self.appendLog("SYS: Welcome → \(msg)")
                let instr = "Asagidaki cumleyi aynen, hicbir ekleme yapmadan, sicak bir tonla seslendir:\n\"\(msg)\""
                self.client?.sendUserText(instr)
            }
        }
    }

    // Bridge to forward GeminiLiveClient.Listener callbacks (which arrive on a
    // background queue) onto the main actor.
    private final class Bridge: GeminiLiveClient.Listener {
        weak var owner: LiveSession?
        init(owner: LiveSession) { self.owner = owner }

        func onSetupComplete() {
            Task { @MainActor in self.owner?.handleSetupComplete() }
        }
        func onAudioChunk(_ pcm: Data) {
            Task { @MainActor in
                self.owner?.audio?.enqueuePlayback(pcm)
                if self.owner?.audio?.jarvisSpeaking == true { self.owner?.state = .speaking }
            }
        }
        func onInputTranscript(_ text: String) {
            Task { @MainActor in self.owner?.appendLog("Sen: \(text)") }
        }
        func onOutputTranscript(_ text: String) {
            Task { @MainActor in self.owner?.appendLog("Jarvis: \(text)") }
        }
        func onTurnComplete() {
            Task { @MainActor in
                guard let o = self.owner else { return }
                o.audio?.endOfTurn()
                if o.audio?.muted != true { o.state = .listening }
            }
        }
        func onToolCall(_ calls: [GeminiLiveClient.ToolCall]) {
            // Faz 3'te ToolDispatcher devreye girecek. Şimdilik boş cevap.
            Task { @MainActor in
                guard let o = self.owner else { return }
                let results: [(GeminiLiveClient.ToolCall, String)] = calls.map {
                    o.appendLog("TOOL: \($0.name) (Phase 3'te aktif olacak)")
                    return ($0, "tool not yet implemented on iOS")
                }
                o.client?.sendToolResponses(results)
            }
        }
        func onError(_ reason: String) {
            Task { @MainActor in
                guard let o = self.owner else { return }
                o.state = .error
                o.appendLog("ERR: \(reason)")
                o.scheduleReconnect()
            }
        }
        func onClosed() {
            Task { @MainActor in
                guard let o = self.owner else { return }
                o.appendLog("SYS: Baglanti kapandi.")
                o.scheduleReconnect()
            }
        }
    }
}

/// Tool declarations placeholder — gets fully populated in Phase 3.
enum ToolDeclarations {
    static let empty: [String: Any] = ["function_declarations": [Any]()]
}
