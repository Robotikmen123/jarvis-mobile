import Foundation

/// JSON file in Documents/long_term.json. Mirrors Android MemoryStore schema.
final class MemoryStore {
    static let shared = MemoryStore()

    private let url: URL
    private let queue = DispatchQueue(label: "memory.store")
    private var cache: [String: [String: String]] = [:]

    init() {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        self.url = docs.appendingPathComponent("long_term.json")
        load()
    }

    func value(category: String, key: String) -> String? {
        queue.sync { cache[category]?[key] }
    }

    func set(category: String, key: String, value: String) {
        queue.sync {
            var cat = cache[category] ?? [:]
            cat[key] = value
            cache[category] = cat
            persist()
        }
    }

    func formatForPrompt() -> String {
        queue.sync {
            guard !cache.isEmpty else { return "" }
            var lines = ["[USER MEMORY]"]
            for (cat, entries) in cache.sorted(by: { $0.key < $1.key }) {
                lines.append("• \(cat):")
                for (k, v) in entries.sorted(by: { $0.key < $1.key }) {
                    lines.append("    - \(k): \(v)")
                }
            }
            return lines.joined(separator: "\n")
        }
    }

    private func load() {
        guard let data = try? Data(contentsOf: url),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: [String: String]]
        else { return }
        cache = obj
    }

    private func persist() {
        guard let data = try? JSONSerialization.data(withJSONObject: cache, options: [.prettyPrinted]) else { return }
        try? data.write(to: url, options: .atomic)
    }
}
