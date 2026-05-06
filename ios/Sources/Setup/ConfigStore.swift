import Foundation
import Security

/// Keychain-backed store for API keys + UserDefaults for non-secret prefs.
/// Mirrors the Android ConfigStore surface.
enum ConfigStore {
    private static let service = "com.jarvis.mobile"
    private static let geminiKeyAccount = "gemini_key"
    private static let anthropicKeyAccount = "anthropic_key"

    private static let voiceKey = "voice_name"

    static var geminiKey: String? {
        get { keychainRead(account: geminiKeyAccount) }
        set { keychainWrite(account: geminiKeyAccount, value: newValue) }
    }

    static var anthropicKey: String? {
        get { keychainRead(account: anthropicKeyAccount) }
        set { keychainWrite(account: anthropicKeyAccount, value: newValue) }
    }

    static var voiceName: String {
        get { UserDefaults.standard.string(forKey: voiceKey) ?? "Puck" }
        set { UserDefaults.standard.set(newValue, forKey: voiceKey) }
    }

    static var isConfigured: Bool {
        guard let k = geminiKey else { return false }
        return !k.isEmpty
    }

    // MARK: - Keychain helpers

    private static func keychainRead(account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String:         kSecClassGenericPassword,
            kSecAttrService as String:   service,
            kSecAttrAccount as String:   account,
            kSecReturnData as String:    true,
            kSecMatchLimit as String:    kSecMatchLimitOne,
        ]
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data,
              let s = String(data: data, encoding: .utf8)
        else { return nil }
        return s
    }

    private static func keychainWrite(account: String, value: String?) {
        let baseQuery: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(baseQuery as CFDictionary)
        guard let value, let data = value.data(using: .utf8) else { return }
        var addQuery = baseQuery
        addQuery[kSecValueData as String] = data
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(addQuery as CFDictionary, nil)
    }
}
