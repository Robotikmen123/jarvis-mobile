import Foundation

/// WebSocket client for Gemini Live `BidiGenerateContent`.
/// Mirrors live/GeminiLiveClient.kt in the Android module.
final class GeminiLiveClient: NSObject {

    struct ToolCall {
        let id: String
        let name: String
        let args: [String: Any]
    }

    protocol Listener: AnyObject {
        func onSetupComplete()
        func onAudioChunk(_ pcm: Data)
        func onInputTranscript(_ text: String)
        func onOutputTranscript(_ text: String)
        func onTurnComplete()
        func onToolCall(_ calls: [ToolCall])
        func onError(_ reason: String)
        func onClosed()
    }

    private let apiKey: String
    private let model: String
    private let voiceName: String

    private var session: URLSession!
    private var task: URLSessionWebSocketTask?
    private weak var listener: Listener?
    private var pendingSystemInstruction: String?
    private var pendingTools: [String: Any]?
    private var isClosed = false

    init(
        apiKey: String,
        model: String = "models/gemini-2.5-flash-native-audio-preview-12-2025",
        voiceName: String = "Puck"
    ) {
        self.apiKey = apiKey
        self.model = model
        self.voiceName = voiceName
        super.init()
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 0   // long-lived
        cfg.waitsForConnectivity = true
        self.session = URLSession(configuration: cfg, delegate: self, delegateQueue: nil)
    }

    func connect(systemInstruction: String, tools: [String: Any], listener: Listener) {
        self.listener = listener
        self.pendingSystemInstruction = systemInstruction
        self.pendingTools = tools
        self.isClosed = false

        let urlStr = "wss://generativelanguage.googleapis.com/ws/" +
                     "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
                     "?key=\(apiKey)"
        guard let url = URL(string: urlStr) else {
            listener.onError("invalid ws url")
            return
        }
        let req = URLRequest(url: url)
        let t = session.webSocketTask(with: req)
        self.task = t
        t.resume()
        receiveLoop()
    }

    func close() {
        isClosed = true
        task?.cancel(with: .goingAway, reason: nil)
        task = nil
    }

    // MARK: - Outgoing

    func sendMicChunk(_ pcm: Data) {
        let payload: [String: Any] = [
            "realtime_input": [
                "media_chunks": [
                    [
                        "mime_type": "audio/pcm;rate=16000",
                        "data": pcm.base64EncodedString()
                    ]
                ]
            ]
        ]
        sendJSON(payload)
    }

    func sendUserText(_ text: String, turnComplete: Bool = true) {
        let payload: [String: Any] = [
            "client_content": [
                "turns": [
                    [
                        "role": "user",
                        "parts": [["text": text]]
                    ]
                ],
                "turn_complete": turnComplete
            ]
        ]
        sendJSON(payload)
    }

    func sendToolResponses(_ responses: [(ToolCall, String)]) {
        let arr: [[String: Any]] = responses.map { call, result in
            [
                "id":   call.id,
                "name": call.name,
                "response": ["result": result]
            ]
        }
        let payload: [String: Any] = [
            "tool_response": ["function_responses": arr]
        ]
        sendJSON(payload)
    }

    // MARK: - Incoming

    private func receiveLoop() {
        task?.receive { [weak self] result in
            guard let self else { return }
            switch result {
            case .failure(let err):
                if !self.isClosed {
                    self.listener?.onError(err.localizedDescription)
                }
                self.listener?.onClosed()
            case .success(let msg):
                switch msg {
                case .string(let s):
                    self.handle(json: s)
                case .data(let d):
                    if let s = String(data: d, encoding: .utf8) { self.handle(json: s) }
                @unknown default:
                    break
                }
                self.receiveLoop()
            }
        }
    }

    private func handle(json raw: String) {
        guard let l = listener,
              let data = raw.data(using: .utf8),
              let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
        else { return }

        if root["setupComplete"] != nil {
            l.onSetupComplete()
            return
        }

        if let sc = root["serverContent"] as? [String: Any] {
            if let mt = sc["modelTurn"] as? [String: Any],
               let parts = mt["parts"] as? [[String: Any]] {
                for p in parts {
                    if let inline = p["inlineData"] as? [String: Any],
                       let b64 = inline["data"] as? String,
                       let pcm = Data(base64Encoded: b64) {
                        l.onAudioChunk(pcm)
                    }
                }
            }
            if let it = sc["inputTranscription"] as? [String: Any],
               let t = it["text"] as? String, !t.trimmingCharacters(in: .whitespaces).isEmpty {
                l.onInputTranscript(t)
            }
            if let ot = sc["outputTranscription"] as? [String: Any],
               let t = ot["text"] as? String, !t.trimmingCharacters(in: .whitespaces).isEmpty {
                l.onOutputTranscript(t)
            }
            if let tc = sc["turnComplete"] as? Bool, tc {
                l.onTurnComplete()
            }
        }

        if let tc = root["toolCall"] as? [String: Any],
           let arr = tc["functionCalls"] as? [[String: Any]] {
            let calls: [ToolCall] = arr.compactMap { o in
                let args = (o["args"] as? [String: Any]) ?? [:]
                return ToolCall(
                    id:   (o["id"]   as? String) ?? "",
                    name: (o["name"] as? String) ?? "",
                    args: args
                )
            }
            if !calls.isEmpty { l.onToolCall(calls) }
        }
    }

    // MARK: - Setup

    fileprivate func sendSetup() {
        guard let sys = pendingSystemInstruction, let tools = pendingTools else { return }
        let setup: [String: Any] = [
            "setup": [
                "model": model,
                "generation_config": [
                    "response_modalities": ["AUDIO"],
                    "speech_config": [
                        "voice_config": [
                            "prebuilt_voice_config": ["voice_name": voiceName]
                        ]
                    ]
                ],
                "system_instruction": [
                    "role": "system",
                    "parts": [["text": sys]]
                ],
                "tools": [tools],
                "output_audio_transcription": [:],
                "input_audio_transcription": [:]
            ]
        ]
        sendJSON(setup)
    }

    private func sendJSON(_ obj: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: obj, options: []),
              let s = String(data: data, encoding: .utf8)
        else { return }
        task?.send(.string(s)) { [weak self] err in
            if let err, !(self?.isClosed ?? true) {
                self?.listener?.onError(err.localizedDescription)
            }
        }
    }
}

extension GeminiLiveClient: URLSessionWebSocketDelegate {
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask,
                    didOpenWithProtocol protocol: String?) {
        sendSetup()
    }
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask,
                    didCloseWith closeCode: URLSessionWebSocketTask.CloseCode,
                    reason: Data?) {
        listener?.onClosed()
    }
}
