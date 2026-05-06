import SwiftUI

struct SetupView: View {
    @State private var geminiKey: String = ConfigStore.geminiKey ?? ""
    @State private var anthropicKey: String = ConfigStore.anthropicKey ?? ""
    @State private var voice: String = ConfigStore.voiceName

    private static let voices = ["Puck", "Charon", "Kore", "Fenrir", "Aoede"]

    var onSave: () -> Void

    var body: some View {
        ZStack {
            Color(red: 0, green: 0.024, blue: 0.04).ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("J.A.R.V.I.S.")
                        .font(.system(size: 26, weight: .bold, design: .monospaced))
                        .foregroundColor(.cyan)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Text("Initialisation")
                        .font(.system(size: 13, design: .monospaced))
                        .foregroundColor(Color(white: 0.4))
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.bottom, 16)

                    label("GEMINI API KEY")
                    secureField("AIza…", text: $geminiKey)

                    label("ANTHROPIC API KEY  (optional)").padding(.top, 8)
                    secureField("sk-ant-…", text: $anthropicKey)

                    label("VOICE").padding(.top, 8)
                    Picker("Voice", selection: $voice) {
                        ForEach(Self.voices, id: \.self) { Text($0).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .colorScheme(.dark)

                    Button(action: save) {
                        Text("INITIALISE")
                            .font(.system(size: 14, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 0, green: 0.05, blue: 0.08))
                            .frame(maxWidth: .infinity, minHeight: 52)
                            .background(Color.cyan)
                            .cornerRadius(8)
                    }
                    .padding(.top, 24)
                    .disabled(geminiKey.trimmingCharacters(in: .whitespaces).isEmpty)
                }
                .padding(24)
            }
        }
        .preferredColorScheme(.dark)
    }

    private func save() {
        ConfigStore.geminiKey   = geminiKey.trimmingCharacters(in: .whitespaces)
        ConfigStore.anthropicKey = anthropicKey.trimmingCharacters(in: .whitespaces).isEmpty ? nil : anthropicKey
        ConfigStore.voiceName   = voice
        onSave()
    }

    private func label(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, design: .monospaced))
            .foregroundColor(Color(white: 0.4))
    }

    private func secureField(_ placeholder: String, text: Binding<String>) -> some View {
        SecureField(placeholder, text: text)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .font(.system(size: 14, design: .monospaced))
            .foregroundColor(.cyan)
            .padding(12)
            .background(Color.cyan.opacity(0.08))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.cyan.opacity(0.3), lineWidth: 1))
            .cornerRadius(8)
    }
}
