<div align="center">

<img src="assets/logo.svg" alt="AppShield Logo" width="180">

# AppShield

### Android Firewall & Network Protection

Protect your privacy by blocking internet access for selected Android applications.

[![Latest Release](https://img.shields.io/github/v/release/Esenkn/AppShield?label=Latest%20Release)](https://github.com/Esenkn/AppShield/releases/latest)
[![License](https://img.shields.io/github/license/Esenkn/AppShield)](LICENSE)

[Download AppShield](https://github.com/Esenkn/AppShield/releases/latest)

</div>

---

## Features

- View installed applications.
- Search installed applications.
- Select individual applications.
- Select all displayed applications.
- Block selected applications' network traffic.
- Start and stop VPN protection.
- View live firewall statistics.
- Monitor discarded packets and bytes.
- Track firewall uptime.
- Reset firewall statistics.

## Requirements

- Android 6.0 or later.
- VPN permission granted by the user.

## How it works

AppShield uses Android's VPN service to route selected applications' network traffic through a local VPN interface.

Selected applications' packets are discarded rather than forwarded, helping prevent those applications from accessing network services while protection is active.

## Installation

1. Open the [latest GitHub release](https://github.com/Esenkn/AppShield/releases/latest).
2. Download `app-release.apk`.
3. Install the APK on your Android device.
4. Open AppShield and grant VPN permission.
5. Select the applications you want to block and start protection.

## License

MIT License. See the [LICENSE](LICENSE) file.

---

<div align="center">

**AppShield — Your privacy, your control.**

Made by [Esenkn](https://github.com/Esenkn)

</div>
