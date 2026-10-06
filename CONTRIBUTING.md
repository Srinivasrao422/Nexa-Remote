# Contributing to Nexa Remote

Thank you for your interest in contributing to Nexa Remote! We welcome bug fixes, documentation improvements, and feature enhancements.

---

## Code of Conduct

Please maintain a respectful, welcoming, and professional environment in all communications and contributions.

---

## How to Contribute

1. **Fork & Branch:** Create a fork of the repository and create a feature branch off `main`:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. **Develop & Test:**
   - Follow standard Kotlin / Jetpack Compose coding guidelines for the Android app.
   - Test UI changes and networking logic on local Wi-Fi.
   - Ensure Android builds succeed: `./gradlew :app:assembleDebug` and `./gradlew :app:assembleRelease`.
3. **Commit Messages:** Write clear, descriptive commit messages outlining the purpose of your changes.
4. **Submit Pull Request:** Open a Pull Request against the `main` branch with a clear description of the problem solved and changes made.

---

## Security Vulnerabilities

If you discover a potential security vulnerability, please refer to our [Security Policy](SECURITY.md) for responsible disclosure guidelines. Do not post unpatched security issues in public issue trackers.
