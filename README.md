# Roamer

A no-root Android tool for overriding a SIM's **home country (region) and carrier name**, so developers can simulate different country/carrier environments on their own device for debugging.

**English** · [简体中文](README.zh-CN.md)

`Kotlin` · `Jetpack Compose` · `Material 3` · `Shizuku` · minSdk 31 (Android 12+)

## What it does

Roamer changes the CarrierConfig **override layer** that the system exposes through the telephony APIs, letting you make an app read your SIM as if it belonged to another country or carrier. It borrows shell privileges through [Shizuku](https://shizuku.rikka.app/) instead of requiring root.

Typical use: verify how your own app behaves for users in the US / Japan / Hong Kong / Korea, or with a specific carrier, without swapping physical SIMs.

## Features

- Override a SIM's country ISO and carrier name with no root, through Shizuku.
- Dual-SIM: override or restore all slots in a single operation.
- One-tap mask to US / JP / KR / CN / HK, using each region's leading carrier; the active button stays highlighted.
- One-tap restore, per SIM or all at once; values are re-derived at runtime, so no snapshot is stored.
- Original and current values shown side by side; technical codes (MCC/MNC/ISO/subId) in a monospace style.
- Overridden tiles marked with a tilted, semi-transparent mask stamp.
- Light / dark theme, switchable independently of the system; Material 3, WCAG AA contrast.
- English and Chinese, following the system language, with per-app override on Android 13+.
- Branded splash screen; colorblind-friendly log (icon plus text).

## What it can and cannot change

Roamer only edits the CarrierConfig override layer. It does **not** touch the SIM or the RIL real values.

| Value | Overridable | API |
| --- | --- | --- |
| Country ISO | ✅ yes | `getSimCountryIso` |
| Carrier name | ✅ yes | `getSimOperatorName` |
| MCC / MNC digits | ❌ no | `getSimOperator` (SELinux protected) |
| In-network real values | ❌ no | `getNetworkOperator` / `getNetworkCountryIso` |

Because of this, Roamer marks MNC as read-only and makes **no promise** to change your real phone number, send/receive SMS, or fool every app. Whether a given app is fooled depends on which API it reads.

## Requirements

- An Android device (tested on a Samsung device running Android 16), Android 12+ (minSdk 31).
- [Shizuku](https://shizuku.rikka.app/) installed and running (start it via wireless debugging or ADB).
- On first launch, grant `READ_PHONE_STATE` (used only to enumerate active SIMs; the app declares no `CALL_PHONE` and cannot place calls).
- Per-app language switching in system settings requires Android 13+.

## How it works

**The privilege path.** Directly calling `overrideConfig` through Shizuku fails on some OEMs: Samsung specifically rejects `getCallingUid() == SHELL`, and every Shizuku call runs under the shell identity. Roamer instead uses Shizuku to invoke `IActivityManager.startInstrumentation` with `INSTR_FLAG_NO_RESTART` (so the target package is not force-stopped and the UI does not crash). The instrumentation runs inside the app process and calls `startDelegateShellPermissionIdentity(myUid, null)`, delegating shell's permissions to the app's own uid. The privileged `overrideConfig` then runs as the app uid (passing the reject-shell guard) while holding `MODIFY_PHONE_STATE` (passing the permission check).

**No-baseline restore.** Roamer saves no pre-override snapshot. On restore it re-derives the truth at runtime:

- The real country ISO is derived from the immutable MCC (`getSimOperator`, which no override can taint). Restore is a two-step write: first write back the real ISO (a non-empty value forces the subscription database to update), then clear the override layer with `overrideConfig(null)`. A blind clear alone does not revert the ISO on this device.
- The carrier name needs no write-back: clearing the layer triggers a pull-on-read that reverts it to the production value automatically.
- "Is it overridden" is decided live: the MCC-derived real ISO differs from the currently effective `getSimCountryIso`.

All reflection goes through [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass); the project builds with no hidden-API compile stubs.

## Build

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :app:assembleDebug        # build the debug APK
./gradlew :app:testDebugUnitTest    # run JVM unit tests
```

## Project layout

```
com.eigenlux.roamer/
├── core/   # the only place touching hidden APIs / privileges
│   ├── ShizukuManager · SimInfo · MccUtil (MCC→ISO truth source)
│   ├── CarrierConfigController            # enumerate + trigger + poll to confirm; N/M batch result
│   ├── InstrumentationTrigger            # Shizuku → startInstrumentation + NO_RESTART
│   ├── PrivilegedOverrideInstrumentation # shell delegation + no-baseline two-step restore
│   └── PrivilegedSubscriptionReader      # binder direct-read of ISub (read-only ICCID display)
├── data/   # pure static presets (CountryPresets 27 regions / CarrierPresets by ISO)
└── ui/     # Compose (Material 3 theme, single-screen MainActivity)
```

## Internationalization

UI strings live in `res/values` (English, the default) and `res/values-zh` (Chinese). Any non-Chinese system locale falls back to English. `res/xml/locales_config.xml` enables the per-app language picker on Android 13+. A JVM test (`StringsParityTest`) enforces that both string tables stay in sync.

## Safety

Roamer is a debugging tool for your own device. It leaves the device clean: because restore re-derives every value at runtime, you can always return a SIM to its true country and carrier after testing.

## Known limitations

- Verified on one Samsung device running Android 16. Other vendors and Android versions are best-effort: the privilege path targets Samsung's reject-shell behavior.
- If a SIM loses service (removed or airplane mode) mid-operation, a restore may report success while an override still lingers.

## License

Released under the [MIT License](LICENSE).

## Acknowledgements

Roamer was inspired by two projects that pioneered the no-root, Shizuku-based approach to carrier/region spoofing:

- [**Ackites/Nrfr**](https://github.com/Ackites/Nrfr)
- [**lmh-codes/Nrfr**](https://github.com/lmh-codes/Nrfr)

Thanks to both for the groundwork.
