# JARVIS MOBILE — Android (APK)

PC versiyonunun mobile portu. Gemini Live API ile sesli sohbet + Android-native tool calling.

## Yetenekler

- Sesli sohbet (Gemini 2.5 Flash native audio, "Puck" sesi default)
- Tool calling:
  - `open_app` — telefondaki uygulamalari acar (WhatsApp, Spotify, vb.)
  - `weather_report` — wttr.in ile anlik hava
  - `web_search` — varsayilan tarayici/arama
  - `send_message` — WhatsApp/Telegram/SMS taslagi acar
  - `reminder` — AlarmManager ile bildirimli hatirlatma
  - `youtube_video` — YouTube videosu/aramasi acar
  - `save_memory` — kullanici hakkinda bilgi kaydi
  - `shutdown_jarvis` — oturumu kapatir
- Hosgeldin mesaji (saat + hava + yatma saati)
- Animasyonlu HUD UI
- Foreground service (ekran kapaliyken de calisir)
- PC'deki gibi `prompt.txt` ve `long_term.json` formatlari

## APK build

### Yontem 1 — Android Studio (yerel)
1. Android Studio kur
2. "Open" → bu klasor (`JarvisMobile/`)
3. Build → Build APK(s)
4. APK'yi telefona kopyala, kur

### Yontem 2 — Komut satiri
```bash
cd JarvisMobile
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

### Yontem 3 — GitHub Actions
1. Repo'ya push
2. Actions → Build APK workflow otomatik calisir
3. Artifacts'tan `jarvis-mobile-debug.apk` indir

## Kurulum (telefonda)

1. APK'yi telefona aktar
2. Bilinmeyen kaynaklara izin ver
3. Kur ve ac
4. Setup ekranindan **Gemini API key** gir (anthropic ve voice opsiyonel)
5. Mikrofon iznini ver
6. JARVIS dinler — konusmaya basla

## Notlar

- Internet baglantisi gerek (Gemini Live)
- Pil tasarrufu agresif cihazlarda Settings → Apps → Jarvis → Battery → Unrestricted yap
- Memory dosyasi `Internal Storage/Android/data/com.jarvis.mobile/files/long_term.json` ya da app icinde
- PC'deki prompt.txt assets/'e gomuldu; degistirmek icin yeniden build

## Yol haritasi (Faz 2)

- Screen capture (MediaProjection)
- Camera (CameraX)
- File processing (SAF)
- Browser control (WebView shell)
- Code helper (Termux intent)
