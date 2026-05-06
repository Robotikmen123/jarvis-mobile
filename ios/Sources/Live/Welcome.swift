import Foundation

enum Welcome {
    static func build() -> String {
        let address = MemoryStore.shared.value(category: "preferences", key: "address_preference") ?? "efendim"
        return "Hosgeldiniz \(address)."
    }
}
