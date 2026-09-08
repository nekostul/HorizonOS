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
