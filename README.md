# Nexa Remote

**Secure local-network remote control for Windows PCs using Android.**

Nexa Remote allows users to securely control their own Windows PC from an Android smartphone or tablet over a local Wi-Fi network. Built with Jetpack Compose on Android and a native Python/C background server on Windows, Nexa Remote offers fast response times, low latency screen streaming, and encrypted communication.

---

## Overview

Nexa Remote bridges your Android device and Windows PC directly over your local wireless network. It requires no cloud servers, third-party internet relays, or external subscriptions, ensuring that your control sessions and data remain strictly on your local network.

---

## Features

- **Touchpad & Mouse Control:** Full cursor navigation, left/right clicks, scrolling, and dragging gestures.
- **Keyboard & Shortcuts:** Real-time text input, special keys (Ctrl, Alt, Win, Del, Esc), and shortcut combinations.
- **Screen Streaming:** Low-latency live desktop display with pinch-to-zoom, panning, and touch coordinate mapping.
- **Clipboard Synchronization:** Synchronize text instantly between Android and Windows clipboard buffers.
- **File Manager & Transfers:** Direct local network file transfer between your phone and PC.
- **System Controls:** Power management (Shutdown, Restart, Sleep, Lock, Sign Out).
- **Quick Controls:** Volume adjustment, mute, media controls, and display settings.
- **PC Status Dashboard:** Real-time hardware monitoring for CPU usage, RAM utilization, Disk space, Uptime, Network IP, and Power status.
- **App Launcher:** Single-tap launcher for standard Windows applications (Chrome, Edge, VS Code, File Explorer, Task Manager, Notepad, Spotify, etc.).
- **Voice Commands:** Hands-free voice commands parsed locally to trigger actions on your PC.

---

## Architecture & How It Works

Nexa Remote utilizes a client-server architecture operating across three dedicated TCP sockets on the local network:

1. **Command Socket (Port 5000):** Handles initial discovery, pairing PIN handshakes, authenticated JSON control commands, system status queries, and app launcher triggers.
2. **Screen Streaming Socket (Port 5001):** Streams JPEG/RGB frame buffers from the PC display to the Android device with dynamic quality adjustment.
3. **File Transfer Socket (Port 5002):** Dedicated binary channel for secure local file uploads and downloads.

---

## Automatic PC Discovery & Local-Subnet Fallback

Nexa Remote supports hybrid local network discovery:

- **Primary Discovery (mDNS / DNS-SD):** Uses `android.net.nsd.NsdManager` to discover servers advertising under `_remote._tcp.local.` automatically.
- **Local-Subnet Unicast Fallback:** On Wi-Fi networks where routers or access points block multicast traffic (AP Isolation / IGMP Snooping), Nexa Remote automatically probes candidate IPs within the active RFC 1918 private IPv4 subnet (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) on port 5000 using bounded parallel coroutines.
- **Server Verification:** Probed IPs are validated via a lightweight Nexa Remote protocol check before displaying in the available server list.

---

## Security

- **Local Network Only:** Communication is restricted to local network IP addresses. Nexa Remote does not route data over the internet or external servers.
- **AES-GCM Keystore Security:** Device authorization tokens are stored on Android using `AndroidKeyStore` with AES/GCM/NoPadding encryption.
- **6-Digit PIN Pairing:** Unrecognized devices must be authorized by entering a temporary 6-digit PIN displayed on the PC server console.
- **App Allowlist:** The App Launcher operates on a strictly defined list of binaries to prevent arbitrary executable invocation.

---

## Network Ports

Ensure the following inbound TCP ports are allowed on your Windows Defender Firewall:

| Port | Service | Description |
| :--- | :--- | :--- |
| **TCP 5000** | Command Server | Authentication, pairing, input commands, PC status |
| **TCP 5001** | Screen Streaming | Live desktop streaming |
| **TCP 5002** | File Transfer | Local file uploading and downloading |

---

## Requirements

### Windows Server
- Windows 10 or Windows 11 (64-bit)
- Connected to the same Wi-Fi / LAN network as the phone

### Android App
- Android 7.0 (API Level 24) or higher
- Connected to the same Wi-Fi network as the PC

---

## Installation & Setup

### 1. Windows PC Setup
1. Download `Nexa Remote Setup.exe` from the latest [Release Version 1.0.1](Nexa%20Remote%20V1.0.1%20Release/).
2. Run the installer to set up `Nexa Remote Server`.
3. Launch `Nexa Remote Server` from the Start Menu or System Tray.

### 2. Android App Setup
1. Download `Nexa Remote 1.0.1.apk` from the latest [Release Version 1.0.1](Nexa%20Remote%20V1.0.1%20Release/).
2. Install the APK on your Android device.
3. Open **Nexa Remote**. Your PC will appear under **Available PCs**.

---

## Pairing Procedure

1. Tap your PC under **Available PCs** in the Nexa Remote app.
2. If connecting for the first time, a 6-digit PIN will appear on the PC server console.
3. Enter the 6-digit PIN into the app prompt.
4. Once paired, your device token is securely saved, and future connections will authorize automatically.

---

## Project Structure

```
Nexa Remote/
├── app/                        # Android Client Application
│   ├── build.gradle.kts        # Android build script
│   └── src/main/java/com/example/remote/
│       ├── MainActivity.kt     # Jetpack Compose UI & Connection Engine
│       ├── discovery/          # NsdDiscoveryManager (mDNS & Subnet Probe)
│       ├── security/           # KeystoreEncryptedStorage
│       └── ui/                 # AppLauncher & PC Status Screens
├── dist/                       # Windows Server Executable Distribution
│   └── RemoteServer/           # Standalone Windows Executable & Libraries
├── Nexa Remote V1.0.1 Release/ # Distribution Release Artifacts
│   ├── Android/                # Verified Signed Release APK
│   ├── Windows/                # Windows Server & Setup Installer
│   ├── Documentation/          # Version Release Notes
│   └── SHA256SUMS.txt          # Cryptographic Checksum Manifest
└── README.md                   # Project Documentation
```

---

## Release Information

- **Current Stable Version:** V1.0.1
- **Application ID:** `com.example.remote`
- **Version Code:** `2`
- **Release Package:** [`Nexa Remote V1.0.1 Release/`](Nexa%20Remote%20V1.0.1%20Release/)

---

## Troubleshooting

- **PC not appearing automatically?**
  - Verify phone and PC are connected to the same Wi-Fi router.
  - Check Windows Defender Firewall rules for TCP ports 5000, 5001, and 5002.
  - Use the **Connect Manually** option in the app and enter your PC's IPv4 address (e.g. `192.168.1.50`).
- **Authorization Revoked?**
  - Tap **Settings** -> **Unpair Device** in the app and trigger pairing again.

---

## License

This project is licensed under the [MIT License](LICENSE).
