# CLAUDE.md

## Project

TUI Keyboard is an Android input method that types like a PC keyboard: a number row, Shift symbols,
Ctrl combinations, Esc, Tab and arrow keys. It is built for
[TUI-Launcher](https://github.com/Boudbois2271/TUI-Launcher), a terminal-style home launcher, but
works in any app.

It is developed and tested on a Xiaomi Redmi Note 9T (Android 11 / API 30, MIUI).

## Files

| Path | What it is |
|---|---|
| `README.md` | Public overview: layers, install, enabling the keyboard. Keep it in sync. |
| `app/src/main/java/.../tui_keyboard/Layout.kt` | The layers (base, Shift, Fn, Sym) as data: rows of keys and their actions. |
| `app/src/main/java/.../tui_keyboard/TuiKeyboardService.kt` | The `InputMethodService`: draws the keys, handles touch, Ctrl hold, key repeat, the accent popup, and sends key events or text. |
| `app/src/main/java/.../tui_keyboard/MainActivity.kt` | Launcher activity with setup instructions. |
| `app/src/main/res/xml/method.xml` | Input method declaration, required by Android. |
| `limitations.md` | Known gaps and things not verified on a device. Read it before starting work, update it before finishing. |
| `LICENSE` | GPL-3.0. Every source file carries `SPDX-License-Identifier: GPL-3.0-or-later`. |
| `CLAUDE.local.md` | Private, gitignored: machine paths, current state, handoff notes (may not exist on your machine). |

## Contract with TUI-Launcher

What each key sends (key event or text, with or without Ctrl) is specified in
[`TUIKBfnsignals.md`](https://github.com/Boudbois2271/TUI-Launcher/blob/master/TUIKBfnsignals.md) in
the launcher repo. Change that file first when the contract changes, then update both projects.

## Decisions

- QWERTY base layout, French accents on long press (é è ê ë à â ç ù û ü î ï ô œ …).
- Layers: base, Shift (one-shot), Fn (Esc, Home/End, PgUp/PgDn, Tab, Del, arrows; toggle or hold),
  Sym (PC punctuation, toggle). The layers are data in `Layout.kt`.
- Ctrl is hold-to-combine. While it is held, a letter is sent as a key event with `META_CTRL_ON`, so
  the app gets Ctrl codes, not text.
- No Alt. Esc and Tab are keys. The space bar is a normal space bar, labelled `space`.
- Built with Views, not Compose: input method services don't fit Compose well. This is the one
  exception to the launcher's Compose-only rule.
- Colours match the launcher: background `#000000`, foreground `#aeb6f4`, accent `#ff9a00`, dark keys.

## Hard rules

- Dark only, flat look, no animations.
- No network access. No INTERNET permission and no networking library. A keyboard sees everything the
  user types: it must never be able to send it anywhere.
- No ads, analytics or tracking. No logging of typed text.
- Kotlin only. No XML layouts for screens (`method.xml` is required by Android).
- Do not change the build setup (AGP, Kotlin, SDK levels) unless the maintainer asks.
- New source files start with `// SPDX-License-Identifier: GPL-3.0-or-later`.
- Nothing personal in tracked files: no home paths, usernames or machine names. That goes in
  `CLAUDE.local.md`.

## Build and run

- `./gradlew installDebug` with a phone connected over USB debugging. If the shell has no JDK on
  `PATH`, point `JAVA_HOME` at Android Studio's bundled JBR.
- Always install after a change, then test on the phone.
- Enable: `adb shell ime enable com.ilanmarchand.tui_keyboard/.TuiKeyboardService`, then
  `adb shell ime set com.ilanmarchand.tui_keyboard/.TuiKeyboardService`.
- Back to Gboard: `adb shell ime set com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME`.

## Git

- Branch `master`, remote `origin` = https://github.com/Boudbois2271/TUI-Keyboard.
- Small commits with clear messages, once `installDebug` succeeds.
- Never commit `local.properties`, build output, `debug.keystore` or `CLAUDE.local.md`.

## End of session

1. Update the launcher's `TUIKBfnsignals.md` if what a key sends changed.
2. Move unfinished or doubtful items to `limitations.md`.
3. Write the handoff (where we are, what's next) in `CLAUDE.local.md`.
