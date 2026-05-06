import SwiftUI

/// Phase 1 placeholder. The full HUD (rings, core, sparks, waveform) lands in Phase 2.
struct MainView: View {
    @StateObject private var session = LiveSession()
    @State private var input: String = ""
    @FocusState private var inputFocused: Bool

    var body: some View {
        ZStack {
            Color(red: 0, green: 0.024, blue: 0.04).ignoresSafeArea()

            VStack(spacing: 0) {
                // Top "HUD" placeholder
                ZStack {
                    Circle()
                        .stroke(Color.cyan.opacity(0.4), lineWidth: 2)
                        .frame(width: 220, height: 220)
                    Circle()
                        .fill(coreColor.opacity(0.18))
                        .frame(width: 160, height: 160)
                    Text(label)
                        .font(.system(size: 18, weight: .bold, design: .monospaced))
                        .foregroundColor(coreColor)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                bottomPanel
            }
        }
        .onAppear { session.start() }
        .onDisappear { session.stop() }
    }

    private var label: String {
        switch session.state {
        case .idle:       return "STANDBY"
        case .connecting: return "CONNECTING…"
        case .listening:  return "LISTENING"
        case .thinking:   return "PROCESSING"
        case .speaking:   return "RESPONDING"
        case .muted:      return "MUTED"
        case .error:      return "ERROR"
        }
    }

    private var coreColor: Color {
        switch session.state {
        case .idle:       return Color(red: 0.23, green: 0.54, blue: 0.60)
        case .connecting: return Color(red: 1.0,  green: 0.80, blue: 0.0)
        case .listening:  return Color(red: 0,    green: 0.83, blue: 1.0)
        case .thinking:   return Color(red: 1.0,  green: 0.42, blue: 0.0)
        case .speaking:   return Color(red: 0,    green: 1.0,  blue: 0.53)
        case .muted:      return Color(red: 1.0,  green: 0.20, blue: 0.40)
        case .error:      return Color(red: 1.0,  green: 0.20, blue: 0.33)
        }
    }

    private var bottomPanel: some View {
        VStack(spacing: 10) {
            // Log
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 2) {
                        ForEach(Array(session.log.enumerated()), id: \.offset) { i, line in
                            Text(line)
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(Color(red: 0.62, green: 0.91, blue: 1.0))
                                .id(i)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                }
                .frame(height: 130)
                .background(
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color(red: 0, green: 0.03, blue: 0.06).opacity(0.85))
                        .overlay(RoundedRectangle(cornerRadius: 14)
                            .stroke(Color.cyan.opacity(0.20), lineWidth: 1))
                )
                .onChange(of: session.log.count) { _ in
                    if let last = session.log.indices.last {
                        withAnimation { proxy.scrollTo(last, anchor: .bottom) }
                    }
                }
            }

            // Input row
            HStack(spacing: 8) {
                TextField("// konuş…", text: $input)
                    .focused($inputFocused)
                    .submitLabel(.send)
                    .onSubmit(send)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .font(.system(size: 14, design: .monospaced))
                    .foregroundColor(Color(red: 0.75, green: 0.96, blue: 1.0))
                    .padding(.horizontal, 16)
                    .frame(height: 44)
                    .background(
                        Capsule()
                            .fill(Color.cyan.opacity(0.10))
                            .overlay(Capsule().stroke(Color.cyan.opacity(0.33), lineWidth: 1))
                    )
                Button(action: send) {
                    Text("▶")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color(red: 0, green: 0.07, blue: 0.09))
                        .frame(width: 44, height: 44)
                        .background(
                            Circle().fill(LinearGradient(
                                colors: [Color(red: 0, green: 0.90, blue: 1.0),
                                         Color(red: 0, green: 0.60, blue: 0.80)],
                                startPoint: .topLeading, endPoint: .bottomTrailing))
                        )
                }
            }

            // Control row
            HStack(spacing: 8) {
                ctrlButton(session.isMuted ? "🔇  SES KAPALI" : "🎙  MIC", color: .cyan) {
                    session.toggleMute()
                }
                ctrlButton("⚙  SETTINGS", color: Color(red: 0.5, green: 0.75, blue: 0.82)) {
                    // Phase 2'de ayarlar overlay'i. Şu an dokunulmuyor.
                }
            }
        }
        .padding(12)
        .background(Color(red: 0, green: 0.024, blue: 0.04).opacity(0.95))
        .overlay(Rectangle().frame(height: 1)
            .foregroundColor(Color.cyan.opacity(0.33)), alignment: .top)
    }

    private func ctrlButton(_ title: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .semibold, design: .monospaced))
                .foregroundColor(color)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.cyan.opacity(0.04))
                        .overlay(RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.cyan.opacity(0.20), lineWidth: 1))
                )
        }
    }

    private func send() {
        let t = input.trimmingCharacters(in: .whitespaces)
        guard !t.isEmpty else { return }
        session.sendText(t)
        input = ""
    }
}
