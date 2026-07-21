# CLAUDE.md — Roamer

Roamer is a rootless Android tool (Kotlin + Jetpack Compose) for overriding a SIM card's reported country code and carrier display name via Shizuku privilege delegation, and optionally syncing that region to targeted per-app locales.

## Project Metadata

- **Application ID / Namespace**: `com.eigenlux.roamer`
- **SDK Target**: `minSdk 31`, `targetSdk 36`, `compileSdk 36`
- **Language & Stack**: Kotlin 2.2.10, Jetpack Compose, Shizuku 13.1.5, HiddenApiBypass 4.3

## Core Architectural Invariants

### Privilege Escalation
Direct `overrideConfig` calls fail under Shizuku on Samsung devices because OEM checks reject `SHELL` caller UIDs (`getCallingUid() == SHELL`). 
Roamer handles this by:
1. Triggering `IActivityManager.startInstrumentation` via Shizuku with `INSTR_FLAG_NO_RESTART`.
2. Executing `startDelegateShellPermissionIdentity(myUid, null)` within the running process to delegate shell permissions to Roamer's UID.
3. Invoking `overrideConfig` holding `MODIFY_PHONE_STATE` as the application UID.

### Baseline-Free Restore
Roamer does not persist pre-override snapshots. Values are re-derived at runtime:
- **Country ISO**: Derived from the immutable MCC (`MccUtil.countryFromMcc(getSimOperator)`). Restore uses a two-step write: write back the real ISO first to update the subscription database, then call `overrideConfig(null)` to clear the layer.
- **Carrier Name**: Reverts automatically to production values when the override layer is cleared.
- **Overridden State**: Evaluated live by comparing the MCC-derived ISO against `getSimCountryIso`.

### Multi-SIM Batching
Multiple SIM slots are processed within a single instrumentation run and single shell delegation context. Triggering individual delegations concurrently can cause early permission drops (`stopDelegateShellPermissionIdentity`), failing secondary SIM restores.

## Project Structure

```
com.eigenlux.roamer/
├── core/   # Telephony bindings, privilege delegation, and system interaction
│   ├── ShizukuManager.kt
│   ├── SimInfo.kt
│   ├── MccUtil.kt
│   ├── CarrierConfigController.kt
│   ├── InstrumentationTrigger.kt
│   ├── PrivilegedOverrideInstrumentation.kt
│   ├── PrivilegedSubscriptionReader.kt
│   ├── RegionLogic.kt
│   └── LocaleOverrideController.kt
├── data/   # Presets and persistent app settings
│   ├── CountryPresets.kt
│   ├── CarrierPresets.kt
│   └── AppLocaleStore.kt
├── ui/     # Compose UI components and theme definition
│   ├── theme/ (Color, Theme, Type, Shape, ThemeMode)
│   └── MainActivity.kt
└── AppPickerScreen.kt
```

## Build & Test Commands

Ensure `JAVA_HOME` is set to JDK 21 before building:

```bash
export JAVA_HOME=~/.local/share/mise/installs/java/temurin-21

# Build debug APK
./gradlew :app:assembleDebug

# Run JVM unit tests
./gradlew :app:testDebugUnitTest
```

## Internationalization Guidelines

- In-app text is stored in `res/values/strings.xml` (English, default) and `res/values-zh/strings.xml` (Chinese).
- Any modifications or additions to UI strings must update both files. The `StringsParityTest` JVM unit test verifies key parity between string tables.
- Data structures in `CarrierPresets.kt` contain non-translated brand strings.
