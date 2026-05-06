import Foundation
import AVFoundation

/// Mic capture at 16kHz mono Int16 PCM + scheduled playback of 24kHz mono Int16 PCM chunks.
/// Mirrors live/AudioIO.kt in the Android module.
final class AudioIO {

    static let micRate:  Double = 16_000
    static let playRate: Double = 24_000

    var muted: Bool = false
    private(set) var jarvisSpeaking: Bool = false

    private let onMicChunk: (Data) -> Void

    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let mixer = AVAudioMixerNode()

    private var converter: AVAudioConverter?
    private var lastScheduledEnd: AVAudioTime?
    private let queue = DispatchQueue(label: "audio.io")

    init(onMicChunk: @escaping (Data) -> Void) {
        self.onMicChunk = onMicChunk
    }

    func start() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playAndRecord,
                                mode: .voiceChat,
                                options: [.defaultToSpeaker, .allowBluetooth, .mixWithOthers])
        try session.setPreferredSampleRate(48_000)
        try session.setActive(true, options: .notifyOthersOnDeactivation)

        let input = engine.inputNode
        let inputFormat = input.outputFormat(forBus: 0)

        guard let outFormat = AVAudioFormat(commonFormat: .pcmFormatInt16,
                                            sampleRate: Self.micRate,
                                            channels: 1,
                                            interleaved: true)
        else { throw NSError(domain: "audio", code: -1) }

        converter = AVAudioConverter(from: inputFormat, to: outFormat)

        // Mic tap → resample → 16k Int16 PCM → onMicChunk
        input.installTap(onBus: 0, bufferSize: 2048, format: inputFormat) { [weak self] buf, _ in
            guard let self else { return }
            if self.muted || self.jarvisSpeaking { return }
            self.encodeAndEmit(buffer: buf, to: outFormat)
        }

        // Playback chain: player → mixer (24k mono) → output
        let playFormat = AVAudioFormat(commonFormat: .pcmFormatInt16,
                                       sampleRate: Self.playRate,
                                       channels: 1,
                                       interleaved: true)!
        engine.attach(playerNode)
        engine.attach(mixer)
        engine.connect(playerNode, to: mixer, format: playFormat)
        engine.connect(mixer, to: engine.mainMixerNode, format: nil)

        engine.prepare()
        try engine.start()
        playerNode.play()
    }

    func stop() {
        playerNode.stop()
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        jarvisSpeaking = false
        lastScheduledEnd = nil
    }

    /// Schedule a 24kHz Int16 PCM chunk for playback.
    func enqueuePlayback(_ pcm: Data) {
        guard !pcm.isEmpty else { return }
        guard let format = AVAudioFormat(commonFormat: .pcmFormatInt16,
                                         sampleRate: Self.playRate,
                                         channels: 1,
                                         interleaved: true) else { return }
        let frames = AVAudioFrameCount(pcm.count / 2) // Int16 = 2 bytes
        guard let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames) else { return }
        buf.frameLength = frames
        pcm.withUnsafeBytes { raw in
            guard let src = raw.baseAddress, let dst = buf.int16ChannelData?[0] else { return }
            memcpy(dst, src, pcm.count)
        }
        jarvisSpeaking = true
        playerNode.scheduleBuffer(buf, completionHandler: nil)
    }

    /// Marks end-of-turn so the mic tap re-engages.
    func endOfTurn() {
        // Wait briefly for the queued audio to drain; if more arrives, this resets to true.
        queue.asyncAfter(deadline: .now() + 0.05) { [weak self] in
            self?.jarvisSpeaking = false
        }
    }

    private func encodeAndEmit(buffer: AVAudioPCMBuffer, to outFormat: AVAudioFormat) {
        guard let converter else { return }
        let ratio = outFormat.sampleRate / buffer.format.sampleRate
        let outFrames = AVAudioFrameCount(Double(buffer.frameLength) * ratio + 1024)
        guard let outBuf = AVAudioPCMBuffer(pcmFormat: outFormat, frameCapacity: outFrames) else { return }

        var error: NSError?
        var didProvide = false
        let status = converter.convert(to: outBuf, error: &error) { _, inputStatus in
            if didProvide {
                inputStatus.pointee = .noDataNow
                return nil
            }
            didProvide = true
            inputStatus.pointee = .haveData
            return buffer
        }

        guard status != .error, error == nil, outBuf.frameLength > 0 else { return }

        let byteCount = Int(outBuf.frameLength) * 2
        var data = Data(count: byteCount)
        data.withUnsafeMutableBytes { raw in
            guard let dst = raw.baseAddress, let src = outBuf.int16ChannelData?[0] else { return }
            memcpy(dst, src, byteCount)
        }
        onMicChunk(data)
    }
}
