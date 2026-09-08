# HorizonOS Settings architecture

The settings UI remains inside HorizonOS. `LauncherSettingsScreen` owns only the category rail, shared focus/gamepad navigation, back handling, and routing to internal screens.

Persistent user choices use the existing `LauncherSettingsRepository` DataStore. No settings screen creates its own `SharedPreferences` or independent store.

## Android capability policy

Controllers expose capabilities before calling Android APIs. Ordinary-app limitations are shown in HorizonOS and are not replaced with fake system state.

- Window brightness is available without global settings access. Global brightness/automatic mode needs `WRITE_SETTINGS`.
- Bluetooth bonded devices can be read after `BLUETOOTH_CONNECT`; changing the adapter requires a system/privileged implementation on modern Android.
- Wi-Fi state and scan results are read defensively. Enabling/disabling Wi-Fi requires the capability exposed by `WifiSettingsController` and is not simulated when unavailable.
- Airplane mode is not changed by the ordinary app. The HorizonOS policy toggles persist, while `AirplaneModeController` reports the system operation unavailable until a privileged implementation is installed.
- Notification permission is requested in-app on Android 13+. Other applications' notification channels are not modified.
- Sleep/power suspend is not simulated. `SleepController` reports unavailable for an ordinary app.

The capability layer is prepared for a future system app/device-owner deployment. Device-owner status is tracked separately from the system-app flag; it is not automatically treated as root or privileged access.

## Privileged and rooted builds

The UI never calls shell commands directly. `SystemCapabilitiesDetector` checks
whether HorizonOS is a system/updated-system app, device owner, or can execute
`su -c id` as uid 0. `PrivilegedSystemAccess` is an isolated bridge used by the
controllers for operations that are unavailable to a normal APK.

The standard APK keeps showing the real Android state or `UNAVAILABLE`; values
stored in DataStore are launcher policy only and are not used to pretend that a
radio, notification setting, or power state changed. A production system build
must still be allowlisted for signature/privileged permissions by the ROM.

Implemented privileged/root paths include airplane mode, Wi-Fi/Bluetooth radio
commands, global brightness, screen timeout, date/time, time zone, and sleep.
Shell command availability is checked through the returned exit code, so a
missing command does not crash the launcher or change its displayed state.

## Settings overlays

Every modal Settings surface uses `SettingsOverlay.kt`. `HorizonOverlay` is
implemented as a transparent, full-screen Compose `Dialog`, rather than a
child of the scrollable Settings content. The dialog window is explicitly
expanded to match the display; its scrim and centered panel are therefore
above the menu, content, and footer and cannot be clipped by a parent Column.

The reusable component owns the modal back handling, transparent window setup,
adaptive panel width, vertical scrolling for long content, focus group, and
open/close transition. Outside taps are intentionally ignored: Settings
overlays close through Cancel or the B/Back action. Android 12+ background
blur is requested when supported; the translucent scrim remains the safe
fallback on devices that do not provide window blur.
