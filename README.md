# Roamer

Roamer is an Android developer tool for overriding a SIM card's reported home country (ISO) and carrier name without requiring root access. It also allows optionally syncing the overridden region to target applications via per-app locales.

**English** · [简体中文](README.zh-CN.md)

Stack: Kotlin · Jetpack Compose · Material 3 · Shizuku · minSdk 31 (Android 12+)

## Overview

Roamer modifies the CarrierConfig override layer exposed by Android telephony APIs via [Shizuku](https://shizuku.rikka.app/). This lets developers test how apps respond to different SIM country ISOs and carrier names without swapping physical SIM cards.

Optionally, Roamer can mirror the primary SIM's overridden country code to selected apps using system per-app locales (Android 13+). Restoring the SIM automatically resets selected apps back to their original locales.

## Features

- Rootless SIM country ISO and carrier name override via Shizuku.
- Multi-SIM support: apply or restore settings across all active slots.
- Preset configurations for popular target regions (US, JP, KR, CN, HK) and carriers.
- Reversible restore: values are derived dynamically at runtime rather than saved to disk.
- Optional per-app region override for targeted app testing.
- UI details: side-by-side display of original vs. active values, monospace technical codes (MCC/MNC/ISO/subId), and light/dark theme support.

## Scope of Overrides

Roamer only edits the CarrierConfig override layer and does not alter underlying SIM or RIL hardware attributes.

| Target Value | Overridable | Telephony API |
| --- | --- | --- |
| Country ISO | Yes | `getSimCountryIso` |
| Carrier Name | Yes | `getSimOperatorName` |
| MCC / MNC Digits | No | `getSimOperator` (SELinux protected) |
| Network Registration | No | `getNetworkOperator` / `getNetworkCountryIso` |

Because MNC and network values are read-only, Roamer does not modify actual phone numbers or cell tower registration. Whether an app detects the override depends on which Telephony or Location APIs it queries.

## Requirements

- Android 12+ (minSdk 31). Tested on Android 16 (Samsung).
- [Shizuku](https://shizuku.rikka.app/) running via Wireless Debugging or ADB.
- `READ_PHONE_STATE` permission granted on first launch to enumerate active SIM slots.
- Per-app region override features require Android 13+ (System per-app locale API).

## Technical Implementation

### Privilege Escalation

Calling `overrideConfig` directly via Shizuku can fail on OEMs like Samsung because OEM security checks reject shell callers (`getCallingUid() == SHELL`). 

To bypass this restriction without root:
1. Roamer calls `IActivityManager.startInstrumentation` via Shizuku with `INSTR_FLAG_NO_RESTART`.
2. The instrumentation runs inside Roamer's own process and invokes `startDelegateShellPermissionIdentity(myUid, null)`.
3. This delegates shell permissions to Roamer's UID, allowing `overrideConfig` to run with `MODIFY_PHONE_STATE` under Roamer's caller identity rather than SHELL.

### Baseline-Free Restore

Roamer does not persist pre-override snapshots. When restoring:
- The real country ISO is calculated from the immutable MCC (`getSimOperator`). The restore operation writes back the real ISO first to update the subscription database, then clears the override layer (`overrideConfig(null)`).
- The carrier name is automatically restored by clearing the override layer, which causes telephony services to pull the original carrier identifier on the next read.

### Reflection & Hidden APIs

All hidden API accesses use [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass), avoiding compile-time hidden API stubs.

## Building

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :app:assembleDebug        # Build debug APK
./gradlew :app:testDebugUnitTest    # Run JVM unit tests
```

## Project Structure

```
com.eigenlux.roamer/
├── core/   # Telephony APIs, Shizuku bindings, and instrumentation logic
│   ├── ShizukuManager
│   ├── CarrierConfigController
│   ├── InstrumentationTrigger
│   ├── PrivilegedOverrideInstrumentation
│   ├── PrivilegedSubscriptionReader
│   ├── RegionLogic
│   └── LocaleOverrideController
├── data/   # Region/carrier presets and local app state storage
├── ui/     # Compose UI, Material 3 theme, and main activity
└── AppPickerScreen.kt
```

## Internationalization

UI text is defined in `res/values` (English default) and `res/values-zh` (Chinese). A JVM unit test (`StringsParityTest`) checks string key parity across translation files.

## Limitations

- Privilege delegation was designed around Samsung's shell UID restrictions on Android 12-16. Other OEM implementations are supported on a best-effort basis.
- If a SIM loses connection (e.g., airplane mode) during operation, telephony state updates may lag behind UI confirmation.

## License

MIT License. See [LICENSE](LICENSE) for details.

## Acknowledgements

Inspired by earlier Shizuku-based carrier override tools:
- [Ackites/Nrfr](https://github.com/Ackites/Nrfr)
- [lmh-codes/Nrfr](https://github.com/lmh-codes/Nrfr)
