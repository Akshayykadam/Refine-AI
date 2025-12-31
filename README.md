<h1 align="center">Refine.AI</h1>

<p align="center">
  <strong>Your AI Writing Assistant, Everywhere</strong><br>
  <em>Premium Writing Refinement Powered by Google Gemini</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square" />
  <img src="https://img.shields.io/badge/AI-Gemini%20Flash-blue?style=flat-square" />
  <img src="https://img.shields.io/badge/Theme-Material%20You-purple?style=flat-square" />
</p>

---

## What is Refine.AI?

Refine.AI is a **system-wide writing assistant** that works in any Android app. Select text, tap the floating bubble, and instantly transform your writing with AI-powered suggestions.

### Key Features

| Feature | Description |
|:---|:---|
| **System-Wide** | Works in WhatsApp, Gmail, Slack, Notes, and more. |
| **🎨 Material You** | Adaptive color palette based on your system wallpaper (Android 12+). |
| **🔄 In-App Updates** | Seamless version checking and installation directly within the app. |
| **✨ One-Tap Refine** | Select text → Tap bubble → Done. |
| **🛡️ Privacy First** | Only processes text you explicitly select. |

---

## Intelligent Tones

Choose the perfect voice for every message:

- **Refine** — Fix grammar and enhance clarity.
- **Professional** — Executive-ready communication.
- **Casual** — Friendly and relaxed.
- **Hinglish** — Natural mix of Hindi and English. 🇮🇳
- **Warm** — Kind and approachable.
- **Love** — Affectionate language.
- **Emojify** — Add relevant emojis without changing your words.

---

## What's New in v0.0.2

- **Material You Dynamic Theming**: Beautiful, adaptive UI that follows your wallpaper.
- **Enhanced Update System**: "Check for Updates" button and more reliable downloads.
- **Stability Fixes**: Resolved crashes on newer Android versions and lock screen behavior.
- **Improved Emojify**: Stricter control over AI output to preserve your original intent.
- **Clipboard Fallback**: Better "Insert" button compatibility across different apps.

---

## Tech Stack

```
Frontend UI      →  React Native (Expo)
Core Engine      →  Kotlin AccessibilityService
Intelligence     →  Google Gemini Flash API
Style Engine     →  Material You (Dynamic Colors)
```

---

## Quick Start

```bash
# Clone
git clone https://github.com/Akshayykadam/Refine-AI-Your-writing--elevated-by-intelligence.git
cd ai-writing-assistant

# Install
npm install

# Configure (create android/local.properties)
echo "GEMINI_API_KEY=your_key_here" >> android/local.properties

# Build & Run
npx expo prebuild
npx expo run:android
```

---

## Privacy

- **On-Demand**: Only processes text you explicitly send to the AI.
- **Secure**: Password fields are automatically ignored by the overlay.
- **Transparent**: All cloud transmissions are encrypted.

---

<p align="center">
  <strong>Refine your world, one word at a time.</strong>
</p>

<p align="center">
  Made by <a href="https://github.com/Akshayykadam">Akshay Kadam</a>
</p>
