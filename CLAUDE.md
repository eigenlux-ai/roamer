# CLAUDE.md — Roamer

Roamer is a rootless Android tool (Kotlin + Jetpack Compose) for overriding a SIM card's reported country code and carrier display name via Shizuku privilege delegation, and optionally syncing that region to targeted per-app locales.

## Project Metadata

- **Application ID / Namespace**: `com.eigenlux.roamer` (debug builds add the `.debug` suffix so they install next to a release build)
- **SDK Target**: `minSdk 31`, `targetSdk 36`, `compileSdk 36`
- **Language & Stack**: Kotlin 2.2.10, Jetpack Compose, Shizuku 13.1.5, HiddenApiBypass 4.3, Haze 1.7.2 (backdrop blur; 1.7.3+ and 2.x need a newer Kotlin and Compose)
- **Design**: `docs/DESIGN.md` is binding. The UI follows the iOS 27 Liquid Glass language; use the controls in `ui/Controls.kt` instead of Material's Switch, SegmentedButton, Checkbox, FilterChip, and filled/text buttons.

## Core Architectural Invariants

### Privilege Escalation
Direct `overrideConfig` calls fail under Shizuku on Samsung devices because OEM checks reject `SHELL` caller UIDs (`getCallingUid() == SHELL`). 
Roamer handles this by:
1. Triggering `IActivityManager.startInstrumentation` via Shizuku with `INSTR_FLAG_NO_RESTART`.
2. Executing `startDelegateShellPermissionIdentity(myUid, null)` within the running process to delegate shell permissions to Roamer's UID.
3. Invoking `overrideConfig` holding `MODIFY_PHONE_STATE` as the application UID.

`startInstrumentation` resolves the instrumentation in the user it is given and returns false when it is not installed there, so the call passes `SystemServices.userId()`, never a fixed `0`.

### Baseline-Free Restore
Roamer does not persist pre-override snapshots. Values are re-derived at runtime:
- **Country ISO**: Derived from the immutable MCC (`MccUtil.countryFromMcc(getSimOperator)`). Restore uses a two-step write: write back the real ISO first to update the subscription database, then call `overrideConfig(null)` to clear the layer.
- **Carrier Name**: Not reliably reverted by clearing the layer (a SIM can keep reporting the overridden name), so restore writes back the carrier-ID name (`getSimCarrierIdName`) together with the real ISO before clearing.
- **Overridden State**: Evaluated live by comparing the MCC-derived ISO against `getSimCountryIso`.

### Multi-SIM Batching
Multiple SIM slots are processed within a single instrumentation run and single shell delegation context. Triggering individual delegations concurrently can cause early permission drops (`stopDelegateShellPermissionIdentity`), failing secondary SIM restores.

### User-Configured Regions
The countries, their carriers, and the quick-switch row are user data (`data/RegionConfig.kt`, edited under Settings). `CountryPresets` and `CarrierPresets` are only the defaults and the source of curated names and locales. The main screen must read `MainViewModel.regions` / `quickSwitch`, never the presets directly. Settings pages edit a draft (`regionsDraft` in the view model, local state on the carrier and quick-switch pages) and only write through on Save. Regions and carriers follow different rules. **Regions**: every ISO 3166-1 code plus the presets is built in; any of them can be put into or taken out of the user's list (a region taken out is kept with `enabled = false`, so its carriers survive). A custom region (`CustomRegion`) is one the user defined for a code the app does not know; it carries the Custom tag and can be deleted for good from the Add page. **Carriers**: a region's preset carriers can never be removed, only turned off; carriers the user added carry the Custom tag and can be removed. A region with no enabled carrier is overridden by ISO alone. For a region outside the presets the per-app locale comes from ICU's likely subtags.

### Configuration Export and Import
`ConfigCodec` writes the regions, custom regions, and quick-switch selection as versioned JSON (`format: roamer-config`, `version: 1`). Import replaces the configuration, drops invalid entries, and normalizes the rest like stored data. Theme and enrolled apps are per-device state and are not part of it. Bump the version and keep reading the old one when the shape changes.

### In-Process Result Channel
The instrumentation attaches to the running app process, so it reports its outcome (overall error and per-subId failures) through `InstrumentationTrigger.report`. `CarrierConfigController` surfaces that error directly and only polls telephony state for SIMs the run did not report as failed. If no outcome arrives, it falls back to polling alone. A failed or partial entry in the log shows `Diagnostics.environment` beneath the message and offers Copy; the line names the device brand, never the model.

### Force Clear
`CarrierConfigController.clearAll` targets SIMs that restore cannot reach because they are not detected as overridden. The `clear` action runs the same write-back as restore, but then calls `overrideConfig(null)` unconditionally (restore keeps the write-back when it has not settled).

## Project Structure

```
com.eigenlux.roamer/
├── core/   # Telephony bindings, privilege delegation, and system interaction
│   ├── ShizukuManager.kt
│   ├── SystemServices.kt
│   ├── SimInfo.kt
│   ├── MccUtil.kt
│   ├── CarrierConfigController.kt
│   ├── InstrumentationTrigger.kt
│   ├── PrivilegedOverrideInstrumentation.kt
│   ├── PrivilegedSubscriptionReader.kt
│   ├── RegionLogic.kt
│   ├── Diagnostics.kt
│   └── LocaleOverrideController.kt
├── data/   # Presets and persistent app settings
│   ├── CountryPresets.kt
│   ├── CarrierPresets.kt
│   ├── RegionConfig.kt
│   ├── ConfigCodec.kt
│   ├── OpenSourceLicenses.kt   # shown on the About page; keep in step with the dependencies
│   └── AppLocaleStore.kt
├── ui/     # Glass material, shared controls, and theme
│   ├── Glass.kt
│   ├── Controls.kt
│   ├── ScreenStack.kt
│   ├── GroupedList.kt
│   ├── Icons.kt
│   └── theme/ (Color, Theme, Type, Shape, ThemeMode)
├── MainActivity.kt
├── MainViewModel.kt
├── SettingsScreen.kt
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

- In-app text is stored in `res/values/strings.xml` (English, default) and `res/values-zh/strings.xml` (Chinese). Chinese is the source copy; write new strings in Chinese first and translate the English from it. Chinese uses 覆写 for "override" everywhere.
- English runs much longer than Chinese. Check any layout change in both languages; the rules are in `docs/DESIGN.md` under "Layout across languages".
- Any modifications or additions to UI strings must update both files. The `StringsParityTest` JVM unit test verifies key parity between string tables.
- Data structures in `CarrierPresets.kt` contain non-translated brand strings.
- `CarrierPresets.kt` covers every ISO region, ordered by subscriber count. Its sources and the markets whose order is unverified are recorded in `docs/CARRIER_DATA.md`; keep the two in sync and never reorder from memory.
