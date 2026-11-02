# TUI Keyboard

A PC-style Android keyboard for TUILauncher, with a number row, Shift symbols, held Ctrl combos, Esc,
Tab, arrows and an Fn layer. French accents on long press. Kotlin, no network access.

## Layers

| Layer | How | What |
|---|---|---|
| Base | — | QWERTY with a number row. Long press a, c, e, i, n, o, s, u, y for accents (é è ê ë à â ç ù û ü î ï ô œ …). |
| Shift | tap, one-shot | Capitals and the PC symbols over the digits. |
| Fn | tap to lock, or hold | Esc, Home, End, PgUp, PgDn, Tab, Del, and arrows on H J K L. |
| Sym | tap to toggle | PC punctuation. |
| Ctrl | hold | Letters (and Fn keys) are sent as key events with Ctrl, so apps get real Ctrl+C, Ctrl+R, … |

Backspace repeats while held. The exact key events and text each key sends are specified in
[`TUIKBfnsignals.md`](https://github.com/Boudbois2271/TUI-Launcher/blob/master/TUIKBfnsignals.md).

Dark and flat, in [TUI-Launcher](https://github.com/Boudbois2271/TUI-Launcher)'s colours. **No
permissions at all**: no INTERNET, no ads, no analytics, nothing typed is stored or logged.

## Build and install

Requirements: Android Studio (its bundled JDK works for Gradle), and a phone with USB debugging on.
minSdk 26 (Android 8.0). Developed and tested on a Redmi Note 9T with Android 11 / MIUI.

```sh
./gradlew installDebug
adb shell ime enable com.ilanmarchand.tui_keyboard/.TuiKeyboardService
adb shell ime set com.ilanmarchand.tui_keyboard/.TuiKeyboardService
```

Or open the TUI Keyboard app, tap "Open keyboard settings", enable it, then pick it as the current
keyboard.

## Works best with

[TUI-Launcher](https://github.com/Boudbois2271/TUI-Launcher), a terminal-style home launcher. Ctrl
combinations only work in apps that handle hardware-style Ctrl key events. Many Android text fields
ignore them. See [`limitations.md`](limitations.md).

## License

[GPL-3.0-or-later](LICENSE).
