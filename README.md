# TYPE X Keyboard — AI keyboard for Android

**Tell Type X what to do. Type X gets it done — in every app.**

TYPE X is a custom Android keyboard (IME) with a built-in on-device AI compose bar.
The website (zenith — typex.vercel.app) is the showcase; **this keyboard is the product**.

## Features (v1.0)

- **Full QWERTY keyboard** — letters, shift / caps-lock, backspace with long-press
  repeat, space (double-tap = period), enter, `?123` symbols + second symbols page,
  key preview popups, haptic feedback. Canvas-drawn: zero View inflation per key.
- **AI compose bar** — tap it, type an instruction ("write a professional reply",
  "translate to urdu", "make it short"), hit RUN. On-device rule-based AI
  generates the text and inserts it into the current app via `commitText()`.
  Works on selected text or surrounding text too (rewrite / shorten / expand).
- **Per-app keypad themes** — the keypad skin follows the app you're typing in:
  WhatsApp (green), Instagram (pink/purple), Facebook (blue), Messenger,
  TikTok, Snapchat, YouTube, default TYPE X purple/orange everywhere else.
- **Languages** — EN + Roman Urdu + Urdu (phonetic layout) toggle on the keyboard;
  AI outputs in all three.
- **Setup wizard** — 2 steps: enable in system settings, switch to TYPE X.
- **Settings** — theme mode, AI tone, AI language, about.
- **Password fields** — AI bar auto-hides; plain fast typing only.
- **No fullscreen extract view** — the keyboard keeps its own AI bar.

## Anti-hang architecture (the 1M-user design)

- Typing path is **100% on-device and synchronous**: touch → `InputConnection`.
  No network, no waiting, no queues — typing can never hang, at any user count.
- AI runs on a **throwaway background thread** and never blocks typing.
- No network permission is even requested: the APK cannot phone home.

## Build

GitHub Actions builds a debug APK on every push to `master`
(`.github/workflows/build-apk.yml`). Pushing a `v*` tag additionally creates a
GitHub Release with the APK attached.

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

## v2 roadmap (not in v1)

- **Agent mode** (user-requested): "LinkedIn par in clients ko outreach bhejo" →
  on-device agent navigates + sends with approval-gating. v1 is assisted-only
  (TYPE X prepares/types, user confirms) which is ban-safe. Autonomous
  navigate-and-send needs an AccessibilityService + strict approval gates —
  never sends secretly. Honest warning: LinkedIn bans automation aggressively.
- Cloud AI provider option (modular `TypeXAi` interface keeps the door open).
- iOS keyboard extension.
- Glide/swipe typing, clipboard history, custom themes.
