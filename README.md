# Roamer

Roamer is an Android developer tool for overriding a SIM card's reported home country (ISO) and carrier name without requiring root access. It also allows optionally syncing the overridden region to target applications via per-app locales.

**English** · [简体中文](README.zh-CN.md)

Stack: Kotlin · Jetpack Compose · Shizuku · Haze · minSdk 31 (Android 12+)

## Overview

Roamer modifies the CarrierConfig override layer exposed by Android telephony APIs via [Shizuku](https://shizuku.rikka.app/). This lets developers test how apps respond to different SIM country ISOs and carrier names without swapping physical SIM cards.

Optionally, Roamer can mirror the primary SIM's overridden country code to selected apps using system per-app locales (Android 13+). Restoring the SIM automatically resets selected apps back to their original locales.

## Features

- Rootless SIM country ISO and carrier name override via Shizuku.
- Multi-SIM support: apply or restore settings across all active slots. Quick switch moves every SIM to one region and restores a SIM whose real country is that region.
- Preset configurations for popular target regions (US, JP, KR, CN, HK) and carriers.
- Reversible restore: values are derived dynamically at runtime rather than saved to disk.
- Force clear: writes back the real values and clears every SIM's override layer, for SIMs that restore cannot target.
- Failure reasons from the privileged run are reported in the in-app log, per SIM.
- Optional per-app region override for targeted app testing.
- UI details: a Liquid Glass interface with side-by-side original vs. active values, monospace technical codes (MCC/MNC/ISO/subId), and light/dark themes.

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

- Android 12+ (minSdk 31). Tested on Android 16 and 17 (Samsung).
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
- Clearing the override layer does not reliably revert the carrier name (a SIM can keep reporting the overridden name), so restore also writes back the carrier-ID name (`getSimCarrierIdName`) before clearing.

### Force Clear

Restore only targets SIMs detected as overridden, which is decided by comparing the MCC-derived ISO with the effective one. Force clear runs the same write-back on every SIM and then clears the override layer unconditionally, even when the write-back has not settled.

### Result Reporting

The instrumentation runs inside the app process, so it hands its outcome (overall error and per-SIM failures) back in memory. A failed run is reported immediately with its real cause; telephony state is polled only for SIMs the run did not report as failed.

### Reflection & Hidden APIs

All hidden API accesses use [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass), avoiding compile-time hidden API stubs.

## Building

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :app:assembleDebug        # Build debug APK
./gradlew :app:testDebugUnitTest    # Run JVM unit tests
```

Debug builds use the application ID `com.eigenlux.roamer.debug`, so they install next to a release build and need their own Shizuku authorization. CI runs the unit tests and a debug build on every push to `main` and on pull requests.

## Project Structure

```
com.eigenlux.roamer/
├── core/   # Telephony APIs, Shizuku bindings, and instrumentation logic
│   ├── ShizukuManager
│   ├── SystemServices
│   ├── CarrierConfigController
│   ├── InstrumentationTrigger
│   ├── PrivilegedOverrideInstrumentation
│   ├── PrivilegedSubscriptionReader
│   ├── RegionLogic
│   └── LocaleOverrideController
├── data/   # Region/carrier presets and local app state storage
├── ui/     # Glass material, shared controls, and theme
├── MainActivity.kt
├── MainViewModel.kt
└── AppPickerScreen.kt
```

## Internationalization

UI text is defined in `res/values` (English default) and `res/values-zh` (Chinese). A JVM unit test (`StringsParityTest`) checks string key parity across translation files.

## Limitations

- Privilege delegation was designed around Samsung's shell UID restrictions on Android 12-16. Other OEM implementations are supported on a best-effort basis.
- After a restore, the carrier name is the carrier-ID name, which can differ from the name the SIM reported before the override.
- If a SIM loses connection (e.g., airplane mode) during operation, telephony state updates may lag behind UI confirmation.

## License

MIT License. See [LICENSE](LICENSE) for details.

## Acknowledgements

Inspired by earlier Shizuku-based carrier override tools:
- [Ackites/Nrfr](https://github.com/Ackites/Nrfr)
- [lmh-codes/Nrfr](https://github.com/lmh-codes/Nrfr)
