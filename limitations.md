# Limitations and open questions

Things touched but not finished, known issues, and doubts. Remove an entry once it is solved.

## Behaviour

- Ctrl+Shift+letter drops Shift: Ctrl+Shift+C arrives as Ctrl+C.
- Ctrl is ignored on digits, punctuation, the Sym page, Space and Backspace (Ctrl+1 types `1`).
- No Alt key, no F1–F12.
- QWERTY only. Long-press accents cover French plus a few extras (ñ ß á ú ó í ö ä); no other
  layouts or languages.
- No suggestions, autocorrect, voice input or clipboard history (by design).
- Emoji page: no skin tones, no flags, no search, nothing newer than Unicode 12 (Android 11's font).
  No category labels: the pages run in a fixed order (faces, people, hearts and symbols, nature,
  food, activities and travel, objects).

## Not verified on a device

- Holding Ctrl while tapping a letter: `adb input` can't hold one key while tapping another.
- The emoji page (Fn/emojis transitions, holding Fn, paging, recents) and the reset to letters when
  the keyboard closes: built and installed, not yet checked on screen.
- Only tested on one phone (Redmi Note 9T, Android 11, MIUI).

## Apps

- Ctrl combinations only do something in apps that handle `KEYCODE_x` + `META_CTRL_ON`. Many Android
  text fields don't. TUI-Launcher's support is in progress: Tab and the arrows work, Ctrl+letter, Esc,
  Home/End, PgUp/PgDn and Del don't yet.
