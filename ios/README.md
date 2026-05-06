# JARVIS iOS

iPhone 15 Pro Max + Single App Mode kurulumu için Swift portu. Android sürümünden bağımsız ama aynı Gemini Live API ve aynı tool şemalarını kullanır.

## Önkoşullar

- macOS + Xcode 15+ (iOS 17 SDK)
- [XcodeGen](https://github.com/yonaskolb/XcodeGen): `brew install xcodegen`
- Apple ID (free tier yeterli; $99/yıl Apple Developer alırsan imzayı 7 günde bir yenilemen gerekmez)
- iPhone 15 Pro Max + USB-C kablo
- Single App Mode için: [Apple Configurator 2](https://apps.apple.com/us/app/apple-configurator-2/id1037126344) (Mac App Store, ücretsiz)

## Build & Run

```bash
cd ios
xcodegen generate
open Jarvis.xcodeproj
```

Xcode'da:
1. Sol panelde **Jarvis** target → **Signing & Capabilities**.
2. **Team** → kendi Apple ID'ni seç.
3. **Bundle Identifier**'ı eşsiz yap: `com.jarvis.mobile.<seninkullanıcıadın>` (free tier için zorunlu).
4. iPhone'u kabloyla bağla, üst barda hedef cihaz olarak seç.
5. ⌘R → uygulama telefona yüklenir, ilk açılışta:
   - **Settings → General → VPN & Device Management → Developer App** altında kendi Apple ID'ne **Trust** demen gerekir.
6. Setup ekranı açılır → Gemini API key gir.

> **Free tier uyarısı:** İmza 7 günde bir uçar. Mac'i bağla, ⌘R, devam.

## Single App Mode (kiosk) kurulumu

> ⚠️ **Telefon silinir.** Önce iCloud yedeği al.

1. Mac App Store'dan **Apple Configurator 2** kur.
2. iPhone'u kabloyla bağla. Configurator açılınca cihaz görünür.
3. Cihazı seç → **Prepare** → "Supervise" işaretle, organizasyon ekle (kendi adın yeterli).
4. Telefon sıfırlanıp tekrar kurulur (iCloud restore yapma — temiz başla, sadece JARVIS'i yükle).
5. JARVIS'i yukarıdaki adımlarla tekrar build & run.
6. Configurator'da cihaz seçili → **Actions → Advanced → Start Single App Mode → Jarvis**.
7. Telefon kilitli kiosk: yalnızca JARVIS açılır, swipe up / Home çalışmaz.

Modu kaldırmak: **Actions → Advanced → Stop Single App Mode**.

## Klasör yapısı

```
ios/
  project.yml                # XcodeGen
  Resources/
    Info.plist
    Jarvis.entitlements
    prompt.txt               # PR #1'deki Android sürümünün aynısı
  Sources/
    JarvisApp.swift
    Setup/
      SetupView.swift
      ConfigStore.swift
    Live/
      LiveSession.swift
      GeminiLiveClient.swift
      AudioIO.swift
      Welcome.swift
    HUD/                     # Faz 2'de doldurulacak
    Tools/                   # Faz 3'te doldurulacak
    Memory/                  # Faz 4'te doldurulacak
```

## Faz durumu

- [x] **Faz 1** — İskelet, Setup ekranı, Gemini Live bağlantısı, sesli sohbet
- [ ] **Faz 2** — HUD (SwiftUI Canvas, Android'deki tüm çizimler)
- [ ] **Faz 3** — Tools (open_app, weather, reminder, …)
- [ ] **Faz 4** — Memory + welcome parite
- [ ] **Faz 5** — Single App Mode konfigürasyonu (kullanıcı tarafında)
