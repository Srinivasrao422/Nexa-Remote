# Nexa Remote V1.0.1 Release Notes

**Release Date:** October 2026  
**Version:** `1.0.1` (`versionCode = 2`)  
**Application ID:** `com.example.remote`

---

## What's New in V1.0.1

### 1. Automatic Discovery Reliability Fix
- **mDNS Primary Discovery:** Preserved native `NsdManager` DNS-SD (`_remote._tcp.local.`) mDNS discovery as the primary instant discovery path.
- **Local-Subnet Unicast Probe Fallback:** Implemented an automated unicast fallback probe for Wi-Fi routers where mDNS or multicast traffic is suppressed (e.g. AP Isolation, IGMP Snooping).
- **Private Subnet Probing:** Automatically detects active IPv4 subnets across RFC 1918 private ranges (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) on port 5000 using bounded parallel coroutines.
- **Server Verification Handshake:** Validates candidate hosts via Nexa Remote protocol responses prior to adding them to the UI server list.

### 2. Branding & UI Polish
- **Product Identity:** Updated user-facing branding to **Nexa Remote**.
- **Material 3 UI:** Polished Home screen header, connection badge indicators, feature card grid, and Settings screen.
- **Icon Resources:** Updated adaptive vector launcher icons.

### 3. Core Features Preserved
- Touchpad & Mouse Control
- Keyboard & Shortcuts
- Low-latency Live Screen Streaming (Port 5001)
- Clipboard Sync
- File Manager & Transfer (Port 5002)
- System Power Controls
- Quick Audio & Settings
- PC Status Metrics
- App Launcher Allowlist
- Voice Commands

### 4. Security & Compatibility
- **Keystore Encryption:** AES-GCM encrypted token storage via `AndroidKeyStore`.
- **6-Digit PIN Pairing:** Kept existing authentication and 6-digit PIN pairing handshakes intact.
- **Service Ports:** Maintained TCP ports 5000 (Command), 5001 (Stream), and 5002 (Files).

---

## Release Artifacts

- **Android Signed Release APK:** `Nexa Remote V1.0.1 Release/Android/Nexa Remote 1.0.1.apk`
- **Windows Inno Setup Installer:** `Nexa Remote V1.0.1 Release/Windows/Nexa Remote Setup.exe`
- **Windows Server Executable:** `Nexa Remote V1.0.1 Release/Windows/RemoteServer.exe`
- **Checksum Manifest:** `Nexa Remote V1.0.1 Release/SHA256SUMS.txt`
