# CLAUDE.md — Roamer

A no-root **SIM home-region / carrier-environment override debugging tool** (Android, Kotlin + Jetpack Compose). It borrows shell privileges through Shizuku and, via instrumentation, overrides the system CarrierConfig so developers can simulate different country/carrier environments on their own device.

- **applicationId / namespace**: `com.eigenlux.roamer`
- **Target device**: a Samsung device / Android 16, Shizuku authorized
- **minSdk 31 · targetSdk 36 · compileSdk 36.1**; Kotlin 2.2.10 · Compose BOM 2026.02.01 · AGP 9.2.1
  (minSdk 31 because the persistent `overrideConfig(int, PersistableBundle, boolean)` overload is API 31+)
- **Dependencies**: Shizuku 13.1.5 (api + provider) · hiddenapibypass 4.3 · core-splashscreen 1.0.1

## Safety contract (must follow)

> The device must stay clean around override tests: **whenever the override feature runs, it must be possible to restore the original values, and testing must leave the device restored.**

This project satisfies that with a **baseline-free, derive-at-runtime** approach (no original snapshot is persisted; see "Core invariants"). After every override test, trigger the restore so the device returns to its initial state.

## Capability boundary (read before changing code)

Override edits only the CarrierConfig "override layer"; it does not touch the SIM / RIL real values:

- ✅ Can change: `getSimCountryIso` (country ISO), `getSimOperatorName` (carrier name)
- ❌ CarrierConfig cannot change: `getSimOperator` (MCC/MNC digits), `getNetworkOperator/CountryIso` (in-network real values), `gsm.sim.operator.*` (SELinux protected)
- ⚠️ MCC/MNC **can** be changed via `ITelephony.setCarrierTestOverride(subId, mccmnc, …)`, but that is a "test override" with heavy side effects (the whole SIM is treated as another carrier, service may drop). **Roamer does not include it in scope**; it only overrides country ISO + carrier name.
- Therefore the UI marks MNC read-only, and Roamer makes **no promise** to change the real phone number / send-receive SMS / fool every app. Whether a given app is fooled depends on which API it reads.

## Architecture

```
com.eigenlux.roamer/
├── core/   # the only place touching hidden APIs / privileges
│   ├── ShizukuManager                    # Shizuku liveness / authorization checks
│   ├── SimInfo                           # single-SIM display state (data class, incl. real* runtime-truth fields)
│   ├── MccUtil                           # MCC→ISO truth source (reflect AOSP MccTable + pure static fallback table), shared by App/privileged
│   ├── CarrierConfigController           # enumerate SIMs + trigger + poll to confirm; live overridden decision; N/M batch result
│   ├── InstrumentationTrigger            # Shizuku → IActivityManager.startInstrumentation + NO_RESTART
│   ├── PrivilegedOverrideInstrumentation # privileged proxy: shell-permission delegation + baseline-free two-step restore; dual entry (App / headless am instrument)
│   └── PrivilegedSubscriptionReader      # binder direct-read of ISub, bypassing App-side ICCID redaction (read-only display)
├── data/   # pure static presets (no hidden APIs, unit-testable)
│   ├── CountryPresets                    # 27 regions: iso + mcc + @StringRes nameRes (country names via resources, follow locale)
│   └── CarrierPresets                    # carrier names linked by country ISO, descending by market share (first = default)
├── ui/theme/  # Compose theme
│   ├── Color / Theme (RoamerTheme + successColor) / Shape (RoamerShapes + Spacing)
│   ├── Type (Typography + RoamerCode monospace code-value style)
│   └── ThemeMode (SYSTEM/LIGHT/DARK + ThemeModeStore, SharedPreferences persistence)
└── MainActivity.kt                       # single-screen Compose: overview (device/Shizuku/dual-SIM mini) → SIM detail cards → appearance → log
```

**Convention**: privileged logic lives only in `core/`; everything else is plain Kotlin, unit-testable / previewable.

**Resources**: `res/values` (English, default) + `res/values-zh` (Chinese) + `res/xml/locales_config.xml` (per-app language) + `res/drawable` (ic_mask stamp / ic_sim_landscape).

**Tests** (JVM): `MccUtilTest` (MCC→ISO fallback table), `PresetsTest` (preset consistency), `StringsParityTest` (the two strings.xml must share the same keys + placeholders).

## Core invariants (must follow)

**Restore saves no original values; everything is re-derived at runtime** (the old baseline-snapshot approach is deprecated and deleted):

- **ISO**: the real value is derived at runtime from the immutable MCC (`MccUtil.countryFromMcc(getSimOperator)`, immune to override pollution) → **two-step restore**: ① write back the real ISO (a non-empty value triggers the subscription DB to update) ② `overrideConfig(null)` clears the layer.
- **Carrier name**: pull-on-read; clearing the layer reverts it to the production value automatically, **no write-back needed** (verified on real devices).
- **Whether overridden**: decided live = MCC-derived real ISO ≠ currently effective `getSimCountryIso` (not a saved flag).
- **Original carrier name (display)**: `getSimCarrierIdName()` (derived from the Carrier-ID database, **immune** to the carrier_name override, device-verified) → while an override is in effect the "Original" field still shows the real carrier name, stored in `SimInfo.realCarrierName`.
- **Restore/refresh timing**: clearing is pull-on-read with propagation delay. The restore criterion must **wait until** the ISO returns to its true value (the privileged side has a separate `pollEffectiveIso` and only clears the layer after confirming), then the upper layer `loadSims` refreshes; otherwise the overview keeps the stale override name.

Why baseline can be dropped: both pillars of a baseline fail here — ISO can be re-derived from the immutable MCC, and the name reverts automatically on clear. Converged in `CarrierConfigController` + `PrivilegedOverrideInstrumentation`.

## Verified facts (real Samsung device / Android 16)

- **The privilege solution**: Samsung specifically rejects `overrideConfig` when `getCallingUid() == SHELL`, so calling it directly through Shizuku's `ShizukuBinderWrapper` **does not pass**. The solution = via Shizuku, invoke `IActivityManager.startInstrumentation` + **`INSTR_FLAG_NO_RESTART`** (no force-stop, UI does not crash) to launch instrumentation, which internally calls **`startDelegateShellPermissionIdentity(myUid, null)`** to delegate shell permissions to the App uid → `getCallingUid()` = App uid (passes the reject-shell guard) + carries `MODIFY_PHONE_STATE` (passes the permission check).
- ⚠️ The `am instrument` command force-stops the target package → self-triggering kills the UI, so it is **headless-only** (adb/Termux); in-app must use the startInstrumentation API above.
- Override / delegation all go through **reflection + HiddenApiBypass**, with no hidden-API compile stubs (self-contained build).
- **🔴 Restore cannot rely on "write empty / clear"**: AOSP `UiccProfile.handleSimCountryIsoOverride` only writes the subscription country code when the override is non-empty; after clearing, the old value lingers. Every Nrfr fork's blind `overrideConfig(null)` fails to revert the ISO on this device. The solution = the **two-step method** (write back the real ISO first, then clear).
- **✅ Carrier name reverts automatically on clear** (verified): name overridden to a test value → plain `overrideConfig(null)` with zero write-back → automatically returns to the production carrier name. So restore needs **no handling** for the name.
- **🔴 Correction**: `SubscriptionInfo.carrierName` / `getSimOperatorName` (App-side public APIs) are **not immune** to the carrier_name override. So the "Original · carrier name" does not rely on them, but on the immune `getSimCarrierIdName()`.
- **Multi-SIM batching**: multiple SIMs must be processed sequentially within **one** instrumentation and **one** shell delegation. Triggering them separately clobbers each other because `stopDelegateShellPermissionIdentity` is process-level (manifests as the secondary SIM's carrier failing to revert during "restore all"). See memory `restore-all-batch-single-instrumentation`.
- **ICCID**: read via the privileged binder (`PrivilegedSubscriptionReader`) **for read-only display only**, gracefully degrading to empty on failure.

## i18n conventions

- All in-app visible copy lives in `res/values/strings.xml` (**English, default**) + `res/values-zh/strings.xml` (Chinese). The default directory being English means **non-Chinese systems fall back to English naturally**; `locales_config.xml` + manifest `android:localeConfig` enable the system per-app language picker (Android 13+).
- **Adding / changing copy must update both xml files** (`StringsParityTest` turns red to enforce this).
- Country names go through `country_*` resources (`CountryPreset.nameRes`; the UI composes `name (iso)` via `countryLabel()`).
- Carrier names are the **override values written to the system** (brand identifiers), so they are **not** i18n'd and stay as data in `CarrierPresets` (cn/tw keep their Chinese brand names).
- Developer logcat / exception copy is English-only and does not go into resources.
- Non-Composable code (e.g. `CarrierConfigController` result copy) uses `ctx.getString(id, args)`; the Compose side uses `stringResource` — both read the same locale.

## Build & run

- **JAVA_HOME must be set** (managed by mise; gradlew does not find it automatically):
  `export JAVA_HOME=~/.local/share/mise/installs/java/temurin-21`
- Build: `./gradlew :app:assembleDebug`; unit tests: `./gradlew :app:testDebugUnitTest`
- adb: `~/Library/Android/sdk/platform-tools/adb`
- **Run prerequisites** (re-do after the package rename): ① re-authorize `com.eigenlux.roamer` in Shizuku; ② grant `READ_PHONE_STATE` on first launch (used to enumerate SIMs; no `CALL_PHONE`).
- Keep the device clean around override tests (see "Safety contract").

## Design conventions

- Brand: "Dusk Harbor · Departure", primary Harbor blue (hue 230) + brass amber accent; `dynamicColor` off by default (to keep brand consistency).
- Technical code values (MCC/MNC/ISO/subId) uniformly use the `RoamerCode` monospace style in `ui/theme/Type.kt`.
- Light / dark dual theme (`ThemeMode` tri-state, manual override of system), contrast meets WCAG AA.
- Android 12+ system splash screen (core-splashscreen, Harbor-blue background + icon foreground; native from API 31).
- See `docs/DESIGN.md`.

## Known TODOs

Originally three defects sharing a root cause: **instrumentation is fire-and-forget, and the App side uses the "ISO value" proxy signal to stand in for "the operation truly finished"**.

- **✅ #1 busy race — FIXED (2026-07-03, before open-source)**: `busy` is now owned solely by `run()` and held (via a `try/finally`) until the whole op finishes; `refreshAll()` no longer touches `busy` (it uses a separate `refreshing` flag for the progress bar and skips while `busy`/`opMutex.isLocked`); privileged ops are serialized by an `opMutex` so two instrumentation runs can never overlap and clobber the process-level shell delegation. See `MainActivity` `run()` / `refreshAll()`.
- **#2 false success with no service** (documented limitation): the restore criterion `realIso.isBlank() || …` treats "cannot obtain the real value" (no SIM / airplane mode) as satisfied → the UI may report "restored" while the device still carries the override. Fix (future): a blank realIso should be failure / pending.
- **#3 layer-clear not verified** (benign): restore success only checks step ① (ISO back to real value), not step ② (`overrideConfig(null)` truly cleared). The privileged side already polls before clearing, so residue is benign; not user-visible.

## Docs index (`docs/`)

- `PRODUCT.md` product strategy · `DESIGN.md` design system
- Planning / feature / dev-landing docs were removed when phase 1 wrapped; historical decisions are captured in "Verified facts" above and in memory.
