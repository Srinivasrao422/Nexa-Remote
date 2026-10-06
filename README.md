# Nexa Remote

> Your PC, remotely connected.

Nexa Remote is a secure local-network remote control system that lets you control a Windows PC from an Android device.

![Version](https://img.shields.io/badge/version-v1.0.1-blue)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Windows-brightgreen)
![License](https://img.shields.io/badge/license-MIT-orange)
[![GitHub Release](https://img.shields.io/badge/release-v1.0.1-green)](https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1)

---

## Overview

Nexa Remote connects your Android smartphone or tablet directly to your Windows PC over your local Wi-Fi or LAN network:

- **Android Device:** Acts as the handheld remote control client.
- **Windows PC:** Runs Nexa Remote Server in the background or system tray.
- **Direct Local Communication:** Both devices communicate directly over the local network.
- **No Cloud Relay:** Your screen, input, and data never pass through third-party servers.
- **No Subscriptions:** Free, open-source, and local-first.
- **Personal Ownership:** Designed specifically for controlling your own Windows PC securely.

---

## Features

- **Remote Mouse & Touchpad:** Full cursor movement, left/right/middle clicks, smooth scrolling, and tap gestures.
- **Keyboard & Shortcuts:** Text typing, special function keys (Ctrl, Alt, Win, Del, Esc, Tab), and standard hotkeys (`Ctrl+C`, `Ctrl+V`, `Alt+Tab`, `Win+D`, `Win+L`).
- **Live Screen Streaming:** Real-time desktop streaming with pinch-to-zoom, panning, touch coordinate mapping, and quality adjustments.
- **Clipboard Synchronization:** One-tap text synchronization between Android and Windows clipboard buffers.
- **File Transfer:** Direct local network file upload and download between phone and PC.
- **System Controls:** Safe session management (Shutdown, Restart, Sleep, Lock, Sign Out).
- **Quick Controls:** Fast adjustments for PC volume, mute status, media playback, and display settings.
- **PC Status Dashboard:** Live hardware telemetry for CPU usage, RAM utilization, Disk space, System uptime, IP address, and Power state.
- **App Launcher:** Single-tap launch for pre-approved Windows applications (Chrome, Edge, VS Code, File Explorer, Task Manager, Notepad, Spotify, Settings).
- **Automatic PC Discovery:** Instant server detection via mDNS and local-subnet unicast fallback.
- **Secure Device Pairing:** Temporary 6-digit PIN authentication with encrypted token persistence.
- **Auto-Reconnect:** Automatic network state recovery when switching Wi-Fi access points or recovering from temporary disconnects.
- **Windows Tray & Startup:** System tray integration, background execution, and automatic Windows startup registration.
- **Windows Installer:** Complete Inno Setup installer package for fast deployment.

---

## Download

### Latest Stable Release — V1.0.1

Get official verified binaries from the [Nexa Remote V1.0.1 GitHub Release](https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1):

- 📱 **Android Client APK:** [`Nexa Remote 1.0.1.apk`](https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.1.0.1.apk)
- 💻 **Windows Setup Installer:** [`Nexa Remote Setup.exe`](https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.Setup.exe)
- ⚙️ **Windows Server Standalone EXE:** [`RemoteServer.exe`](https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/RemoteServer.exe)
- 🔐 **Checksum Manifest:** [`SHA256SUMS.txt`](https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/SHA256SUMS.txt)

---

## How It Works

Nexa Remote uses a multi-socket architecture operating across three dedicated local TCP channels:

```text
Android Device (Remote Client)
      │
      │ Local Wi-Fi / LAN
      ▼
Nexa Remote Server (Windows)
      │
      ├── TCP 5000 ── Commands / Authentication / Telemetry
      ├── TCP 5001 ── Low-latency Screen Streaming
      └── TCP 5002 ── Dedicated Binary File Transfer
      │
      ▼
Windows PC Control
```

### Network Ports

| Port | Service | Purpose |
| :--- | :--- | :--- |
| **TCP 5000** | Command Channel | Discovery, pairing PIN, authentication, touch/mouse/keyboard events, PC status |
| **TCP 5001** | Screen Streaming | Live frame buffer video/image streaming |
| **TCP 5002** | File Transfer | Secure local file uploading and downloading |

---

## Automatic PC Discovery

Nexa Remote eliminates manual setup through a dual-layer discovery system:

1. **Primary Discovery (mDNS / DNS-SD):** Listens for servers broadcasting under `_remote._tcp.local.` via `NsdManager`.
2. **Local-Subnet Unicast Fallback:** On Wi-Fi routers where multicast traffic is blocked or filtered (AP Isolation / IGMP Snooping), Nexa Remote automatically probes candidate IPs on the active RFC 1918 private IPv4 subnet (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) on port 5000 using bounded parallel coroutines.
3. **Protocol Validation:** Candidates must pass a Nexa Remote server identification handshake before appearing in the available server list.
4. **Manual IP Fallback:** Users can also enter their PC's local IP address directly.

---

## Security

- **Strictly Local Communication:** All traffic is restricted to your local network (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`). No data is sent over the internet or external cloud infrastructure.
- **Android Keystore Protection:** Device authorization tokens are stored on Android using `AndroidKeyStore` with AES/GCM/NoPadding encryption.
- **Temporary 6-Digit PIN Pairing:** Unrecognized devices must be authorized by entering a 6-digit PIN displayed on the PC server console.
- **Command & App Allowlist:** Input commands and executable launches are validated against strict internal schemas to prevent arbitrary command execution.
- **Production-Signed APK:** Android release builds are signed with official production keys (`remote-release-key`).

---

## Requirements

### Windows PC
- Windows 10 or Windows 11 (64-bit)
- Connected to the same Wi-Fi / LAN network as the phone

### Android Device
- Android 7.0 (API Level 24) or higher
- Connected to the same Wi-Fi / LAN network as the PC

---

## Installation & Setup

### Windows PC
1. Download [`Nexa Remote Setup.exe`](https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.Setup.exe) from the [V1.0.1 GitHub Release](https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1).
2. Run `Nexa Remote Setup.exe` to install Nexa Remote Server.
3. Allow Windows Defender Firewall rules for TCP ports 5000, 5001, and 5002 when prompted.
4. Nexa Remote Server will launch and reside in your Windows System Tray.

### Android Device
1. Download [`Nexa Remote 1.0.1.apk`](https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.1.0.1.apk) from the [V1.0.1 GitHub Release](https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1).
2. Install the APK on your Android device.
3. Open **Nexa Remote**. Your PC will appear automatically under **Available PCs**.

---

## Pairing Procedure

1. Open **Nexa Remote** on Android and select your PC under **Available PCs**.
2. On first connection, a temporary 6-digit PIN will display on the Windows server console.
3. Enter the 6-digit PIN into the prompt on your Android device.
4. Once paired, an encrypted authorization token is saved in your phone's Android Keystore, allowing seamless automatic reconnects in the future.

---

## Troubleshooting

- **PC Not Found Automatically?**
  - Ensure both phone and PC are connected to the same Wi-Fi network (and not guest networks).
  - Verify inbound rules for TCP ports 5000, 5001, and 5002 exist in Windows Defender Firewall.
  - Tap **Enter IP Manually** in the app and enter your PC's IPv4 address (e.g. `192.168.1.50`).
- **Connection Error / Authorization Revoked?**
  - Open **Settings** in the Nexa Remote app, tap **Unpair**, and initiate pairing again to obtain a new authorization token.
- **Wi-Fi Network Changed?**
  - Nexa Remote automatically detects IP changes and reconnects when you rejoin your home Wi-Fi network.

---

## Project Structure

```text
Nexa Remote/
├── app/                        # Android Client Application (Jetpack Compose)
│   ├── build.gradle.kts        # Android build configuration
│   └── src/main/java/com/example/remote/
│       ├── MainActivity.kt     # Jetpack Compose UI & Remote Control Engine
│       ├── discovery/          # NsdDiscoveryManager (mDNS & Subnet Probe)
│       ├── security/           # KeystoreEncryptedStorage
│       └── ui/                 # AppLauncher & PC Status Dashboard
├── dist/                       # Windows Server Standalone Distribution
│   └── RemoteServer/           # Executable & PyInstaller Libraries
├── Nexa Remote V1.0.1 Release/ # Distribution Release Packaging
│   ├── Android/                # Signed Production APK
│   ├── Windows/                # Windows Setup Installer & Executable
│   └── SHA256SUMS.txt          # Release Cryptographic Checksums
├── README.md                   # Repository Documentation
├── LICENSE                     # MIT License
└── RELEASE_NOTES.md            # Release History
```

---

## Release Information

- **Current Version:** V1.0.1
- **Version Code:** `2`
- **Application ID:** `com.example.remote`
- **GitHub Tag:** [`v1.0.1`](https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1)

---

## Responsible Use

Nexa Remote is designed for authorized remote control of your own Windows PC or computers where you have explicit permission. It is intended for local network convenience, accessibility, and productivity.

---

## Community & Policy

- **Contributing:** Please review our [Contributing Guidelines](CONTRIBUTING.md).
- **Security Policy:** Read our [Security Policy](SECURITY.md) for responsible disclosure procedures.
- **License:** Distributed under the [MIT License](LICENSE).

---

## Nexa Remote

> Your PC, remotely connected.

Built for fast, secure, local control of your own Windows PC.
