# SpatialLink Flagship UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Rebuild the rejected Overview composition into an original, responsive SpatialLink luxury-tech field environment that surfaces existing P0/P1 data through Overview, Field/System, and Identity experiences.

**Architecture:** Keep `DiagnosticsViewModel` and all platform/domain ports unchanged. Replace the current flat hero with a Compose-owned spatial instrument stage: an oversized, off-center Canvas field, anchored status callouts, a precision command control, and a sparse three-point instrumentation rail. Field/System owns the expandable technical clusters; Overview owns confidence and identity summary only.

**Tech Stack:** Kotlin 2.4.10, Jetpack Compose UI 1.12.0, Material 3 1.4.0 as infrastructure, Android API 29-37 compatibility, JUnit4, Compose instrumentation tests, Gradle wrapper 9.5.0, JDK 17.

**Spec:** `docs/superpowers/specs/2026-08-28-spatiallink-flagship-ui-design.md`

## Global Constraints

- Preserve `DiagnosticsViewModel -> StateFlow -> Compose`.
- Keep `core:model` pure Kotlin with no Android or AndroidX dependency.
- Use only existing P0 capability/permission and P1 identity data.
- Do not request runtime permissions from UI startup or any navigation action.
- Do not implement peer discovery, transfer, NFC pairing, networking, or ranging sessions.
- Do not add `androidx.core.uwb`, reflection, analytics, telemetry, cloud, or broad storage access.
- Do not use manufacturer/model checks or device identifiers as product identity.
- Keep missing hardware as `UNAVAILABLE` and indeterminate inspection as `UNKNOWN`.
- Use the approved palette: deep obsidian, warm ivory, champagne brass, ice cyan, and muted mint.
- Do not use stock `Scaffold`, `TopAppBar`, `Card`, or `NavigationBar` as the visible product structure.
- The Overview field must be larger than its clipping stage, intentionally off-center, and visibly layered with unequal rings, material brass tones, and a partial cyan energy arc.
- The Overview must not expose capability, permission, or platform-detail tiles; those belong to Field/System.
- The primary field command must have substantial vertical presence and a custom precision-cut frame; it must not read as a standard outlined button.
- Navigation must be a sparse instrumentation rail with three justified destinations and a centered identity seal, not an equal-weight Material bar.
- All long technical values must stay in bounded tiles or labels with no overlap.
- Verify with focused tests first, then JVM tests, lint, debug/release builds, and the authorized physical device.
- This checkout is not a Git repository; do not initialize one or claim commit evidence. Review the final file set and command output directly.

---

### Task 1: Establish tested presentation projections

**Files:**
- Create: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPresentation.kt`
- Test: `feature/diagnostics/src/test/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPresentationTest.kt`

**Interfaces:**
- Consumes: `DiagnosticsUiState`, `FoundationStatus`, `CapabilityState`, `PermissionState`, `IdentityUiState`, and existing model enums.
- Produces: `SpatialLinkTone`, `StatusMarkModel`, `OverviewModel`, `SystemClusterModel`, `SystemTileModel`, `DiagnosticsUiState.toOverviewModel()`, `DiagnosticsUiState.toSystemClusters()`.

- [x] **Step 1: Write the failing unit tests**

Add tests that assert behavior rather than implementation details:

```kotlin
@Test
fun ready_state_projects_into_trusted_overview() {
    val state = DiagnosticsUiStateReducer.success(
        capabilities = CapabilityFixtures.fullyKnown(),
        permissions = CapabilityFixtures.allPermissions(),
        identity = CapabilityFixtures.availableIdentity(),
    )

    val model = state.toOverviewModel()

    assertEquals("READY", model.foundation.label)
    assertEquals(SpatialLinkTone.HEALTHY, model.foundation.tone)
    assertEquals("TRUSTED", model.identity.label)
    assertEquals("LOCAL LINK", model.localLink.label)
    assertEquals("ECDSA P-256 / SHA-256", model.identityAlgorithm)
}

@Test
fun unavailable_hardware_remains_unavailable_not_error() {
    val state = DiagnosticsUiStateReducer.success(
        capabilities = CapabilityFixtures.fullyUnavailable(),
        permissions = CapabilityFixtures.allPermissions(),
    )

    val tiles = state.toSystemClusters().flatMap { it.tiles }

    assertEquals("UNAVAILABLE", tiles.first { it.title == "UWB" }.status.label)
    assertEquals(SpatialLinkTone.QUIET, tiles.first { it.title == "UWB" }.status.tone)
}

@Test
fun not_required_permission_is_human_readable_and_not_error() {
    val state = DiagnosticsUiStateReducer.success(
        capabilities = CapabilityFixtures.fullyKnown(sdkInt = 34),
        permissions = CapabilityFixtures.allPermissions(PermissionState.NOT_REQUIRED_ON_THIS_OS),
    )

    val permissionTiles = state.toSystemClusters()
        .first { it.title == "Runtime permissions" }
        .tiles

    assertEquals("NOT REQUIRED", permissionTiles.first { it.title == "Local network" }.status.label)
    assertEquals(SpatialLinkTone.QUIET, permissionTiles.first { it.title == "Local network" }.status.tone)
}
```

- [x] **Step 2: Run the tests and verify the expected red state**

Run:

```powershell
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :feature:diagnostics:test --tests 'com.r2h.spatiallink.diagnostics.SpatialLinkPresentationTest' --console=plain }
```

Expected: compilation/test failure because the new projection types and
functions do not exist yet. If the command fails for an unrelated Gradle or
toolchain reason, resolve that environment issue and rerun until the missing
presentation behavior is the reported failure.

- [x] **Step 3: Implement the minimal projection layer**

Create immutable presentation models and pure mapping functions. Use these
rules:

```kotlin
enum class SpatialLinkTone { ACTIVE, HEALTHY, QUIET, CAUTION, ERROR }

data class StatusMarkModel(
    val label: String,
    val tone: SpatialLinkTone,
)

data class OverviewModel(
    val foundation: StatusMarkModel,
    val identity: StatusMarkModel,
    val localLink: StatusMarkModel,
    val platformLabel: String,
    val identityId: String,
    val identityAlgorithm: String,
    val identityProtection: String,
    val identityVerification: String,
    val fieldState: StatusMarkModel,
    val isLoading: Boolean,
    val errorMessage: String?,
)

data class SystemTileModel(
    val title: String,
    val status: StatusMarkModel,
)

data class SystemClusterModel(
    val title: String,
    val subtitle: String,
    val tiles: List<SystemTileModel>,
)
```

Map `FoundationStatus.READY/PARTIAL/ERROR` to `READY/PARTIAL/ERROR`; map
`CapabilityState.AVAILABLE/UNAVAILABLE/UNKNOWN` to
`AVAILABLE/UNAVAILABLE/UNKNOWN`; map `PermissionState` to readable labels
(`GRANTED`, `DENIED`, `NOT REQUIRED`, `NOT DECLARED`, `UNKNOWN`); and map
identity readiness/self-test without exposing raw cryptographic values.
Aggregate local link only from existing Bluetooth and Wi-Fi capability states:
any available state is `READY`, all unavailable is `UNAVAILABLE`, otherwise
`UNKNOWN`. No platform calls belong in this file.

- [x] **Step 4: Run the focused tests and inspect the result**

Run the same `:feature:diagnostics:test --tests ...` command. Expected: all
presentation tests pass with no failures. Do not proceed if the tests pass
without the implementation or if any assertion is weakened.

---

### Task 2: Add the SpatialLink token system and app theme bridge

**Files:**
- Create: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkDesignTokens.kt`
- Modify: `app/src/main/java/com/r2h/spatiallink/ui/theme/SpatialLinkTheme.kt`
- Modify: `app/src/main/res/values/themes.xml`

**Interfaces:**
- Consumes: no domain state; only Compose theme inputs.
- Produces: `SpatialLinkColors`, `SpatialLinkTypography`, and a dark/light
  `MaterialTheme` bridge used by the feature surface.

- [x] **Step 1: Define the token values and typography roles**

Create immutable color roles matching the spec:

```kotlin
val Obsidian = Color(0xFF0B0D0D)
val Carbon = Color(0xFF121616)
val CarbonEdge = Color(0xFF1D2423)
val WarmIvory = Color(0xFFE5DED2)
val MutedIvory = Color(0xFFA8A196)
val ChampagneBrass = Color(0xFFC8B89E)
val BrassDim = Color(0xFF7F725D)
val IceCyan = Color(0xFFA8D7EA)
val CyanDim = Color(0xFF5E9DB2)
val MutedMint = Color(0xFF91A99E)
val SoftRose = Color(0xFFD5A7A3)
```

Use `FontFamily.Serif` for display roles and `FontFamily.SansSerif` for
technical roles. Keep body typography at a 14sp-16sp baseline and define
letter spacing only for labels/navigation.

- [x] **Step 2: Bridge the tokens into Material without exposing stock Material UI**

Build explicit dark and accessible light `ColorScheme`s in
`SpatialLinkTheme.kt`. Keep `MaterialTheme` as a typography/color provider,
but ensure every visible screen uses the SpatialLink token roles. Change the
XML parent/window background so startup does not flash a light utility screen.

- [x] **Step 3: Run the feature and app compilation checks**

Run:

```powershell
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :feature:diagnostics:compileDebugKotlin :app:compileDebugKotlin --console=plain }
```

Expected: PASS. This task contains no domain behavior, so no model tests are
changed here.

---

### Task 3: Rebuild the reusable spatial primitives

**Files:**
- Create: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPrimitives.kt`
- Modify: `feature/diagnostics/build.gradle.kts` only if a direct Compose
  animation artifact is required by the compiler.

**Interfaces:**
- Consumes: immutable presentation models, token roles, and click callbacks.
- Produces: `SpatialAtmosphere`, `SpatialSurface`, `SpatialCommand`,
  `SpatialField`, `SpatialRing`, `FieldNode`, `FieldAnchor`, `TrustSeal`,
  `SystemTile`, `SystemCluster`, `StatusMark`, `InstrumentLabel`,
  `TechnicalValue`, and `SpatialNavigation`.

- [x] **Step 1: Add a failing semantics contract test before page code**

Extend `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt`
with an assertion for the new `Spatial field` heading, `OPEN FIELD` action, and
the `SYSTEM`/`IDENTITY` navigation labels. Run the connected test filter
against the current build and record the expected failure because the current
diagnostics page does not contain those nodes.

- [x] **Step 2: Implement bounded surfaces and status language**

`SpatialSurface` must use matte token fills and thin structural borders.
`SpatialCommand` must draw a layered precision-cut frame, a spatial entry glyph,
and a large command label with a 72dp minimum height. `StatusMark` must render
an explicit uppercase label and a non-color marker. `SystemTile` must keep title
and status in separate vertical regions so long values never overlap or
collide.

- [x] **Step 3: Implement the functional Canvas field instrument**

`SpatialField` takes `StatusMarkModel`, `Modifier`, and an optional
`contentDescription`. Draw an oversized instrument with a clipped off-center
composition: faint background rings, unequal structural rings, radial axes,
calibration ticks, brass markers, depth-separated inner seal layers, a partial
cyan energy arc, and state-driven nodes. The field's emphasis derives from the
supplied state; it must not call an adapter, start a session, discover peers,
or claim a connection. Use a bounded slow sweep only while the surface is
composed, and stop all animation when disposed.

- [x] **Step 4: Implement the identity seal and technical cluster primitives**

`TrustSeal` renders the safe identity state without placing a long ID inside
the seal. `FieldAnchor` attaches a small status instrument and technical label
to a field point using a fine connector line. `SystemCluster` is locally
expandable and uses a single grouped surface containing bounded tiles, not
nested cards or a left/right table. `SpatialNavigation` exposes only Overview,
Field/System, and Identity, with the Identity seal at the rail center and 48dp
minimum targets.

- [x] **Step 5: Compile the feature and run the focused connected test**

Run the feature compilation command from Task 2, then the connected test
filter. Expected: compilation passes; the UI test remains red until the shell
uses the primitives in Task 4.

---

### Task 4: Replace the diagnostics list with the rebuilt premium shell and pages

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsScreen.kt`
- Create: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkShell.kt`
- Create: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPages.kt`

**Interfaces:**
- Consumes: `DiagnosticsUiState`, `DiagnosticsViewModel`, presentation models,
  and primitives from Tasks 1-3.
- Produces: `DiagnosticsRoute` with unchanged ViewModel collection and a
  three-destination, local presentation shell.

- [x] **Step 1: Keep the route/state boundary unchanged**

`DiagnosticsRoute` continues to call
`collectAsStateWithLifecycle()` and passes the immutable state into
`DiagnosticsScreen`. Do not add repository access, platform APIs, or coroutine
work to the screen.

- [x] **Step 2: Implement the Overview surface**

Use a scrollable composition inside an obsidian atmosphere. Place the
editorial heading and overall status in a safe top-left column, then give the
`SpatialField` an oversized width derived from `BoxWithConstraints`, offset its
center toward the right edge, and clip it intentionally in a dedicated stage.
Anchor secure link/local network callouts to field geometry with connector
lines. Add only the major `OPEN FIELD` command and the trusted-device artifact;
do not put technical tiles or an Inspect System dump on Overview. Keep the
composition usable at narrow portrait widths and large font scales.

- [x] **Step 3: Implement the Field surface without future behavior**

Show the larger field instrument, foundation status, known capability summary,
an honest `No active spatial session` state, and the expandable technical
clusters for environment, capabilities, ranging technologies, and permissions.
The primary action returns to Overview; no discovery, ranging, peer list, or
session creation is allowed.

- [x] **Step 4: Implement the Identity surface**

Show `TrustSeal`, safe short ID, formatted algorithm, formatted Keystore
protection level, and signing verification. Map identity loading, not-created,
recovery, unavailable, and error states to explicit status marks. Never render
private keys, raw certificates, signatures, or failure details beyond the
existing safe failure code when the System surface needs it.

- [x] **Step 5: Implement the System surface**

Render environment/foundation, spatial capabilities, ranging technologies, and
runtime permissions as expandable `SystemCluster`s on the Field/System
surface. Keep the existing complete set of enum values visible when data is
available; preserve successful sibling results when one inspection fails. Show
issue count and a calm partial/error explanation without converting absent
hardware into a fatal state.

- [x] **Step 6: Run the focused connected test and inspect the red/green transition**

Run:

```powershell
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.r2h.spatiallink.DiagnosticsInstrumentedTest --console=plain }
```

Expected after the shell is wired: the updated launch, identity, system,
recreation, and long-value bounds tests pass. If a test fails, fix the layout
or semantics; do not delete, ignore, or weaken it.

---

### Task 5: Harden responsive behavior and accessibility

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPrimitives.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPages.kt`
- Modify: `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt`

- [x] **Step 1: Add assertions for the long permission value bounds**

Navigate to System, expand Runtime permissions, locate `Local network` and
`NOT REQUIRED`, and assert the status bounds remain below the tile's right
edge and do not intersect the label bounds. Use real Compose bounds from the
connected app; do not replace this with a string-only assertion.

- [x] **Step 2: Add stable semantics and minimum touch targets**

Mark page headings as headings, give the field instrument a state-specific
content description, expose navigation destination labels, and ensure every
clickable frame has at least a 48dp layout size. Ensure the `READY`, `PARTIAL`,
`UNAVAILABLE`, `UNKNOWN`, `NOT REQUIRED`, and `ERROR` labels remain visible in
the relevant state.

- [x] **Step 3: Check configuration/font-scale-safe layout in source and tests**

Use `BoxWithConstraints`, `widthIn`, `heightIn`, `maxLines`, and controlled
wrapping where needed. Keep the instrument decorative geometry clipped within
its own bounded Box, never over text. Run the connected recreation test and
the long-value test again.

---

### Task 6: Run the complete verification suite and physical-device review

**Files:**
- Modify: `docs/superpowers/specs/2026-08-28-spatiallink-flagship-ui-design.md`
  only if verification reveals an actual design-contract correction.
- Create: `design-qa.md` with the source/reference comparison and final result.

- [x] **Step 1: Run all JVM tests**

```powershell
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat test --console=plain }
```

Expected: PASS with zero failed, errored, or skipped tests.

- [x] **Step 2: Run debug/release compilation and lint**

```powershell
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain }
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat lint --console=plain }
```

Expected: both commands exit 0 with no new lint errors.

- [x] **Step 3: Run connected instrumentation on the authorized physical device**

Confirm `adb devices -l` has exactly one intended online physical device and
does not create an emulator. Install the current debug APK if required, then
run:

```powershell
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest --console=plain }
```

Record executed/passed/failed/skipped counts from the generated test XML and
the Gradle result. Capture a fresh app screenshot and UI hierarchy on the
physical phone. Confirm the app launches without startup permission dialogs,
the Overview/Identity/System surfaces are readable, long permission values do
not overlap, and diagnostics reaches `READY` or an allowed `PARTIAL` state.

- [x] **Step 4: Audit runtime and architecture/privacy boundaries**

Clear only logcat, launch the app, and inspect a bounded logcat capture for
SpatialLink-caused `FATAL EXCEPTION`, `VerifyError`, `NoClassDefFoundError`,
`ClassNotFoundException`, `SecurityException`, and `IllegalStateException`.
Search source/dependency manifests for `androidx.core.uwb`, reflection,
HTTP/WAN clients, analytics, telemetry, cloud, raw device identifiers,
MAC/address reads, broad storage permissions, peer discovery, transfer,
pairing, and ranging sessions. Confirm `core:model` remains free of Android
imports and no startup permission request path was added.

- [x] **Step 5: Complete design QA**

Place the supplied reference image and the fresh physical-device screenshot in
the same comparison input, review the matching viewport/state, and save
`design-qa.md` with `final result: passed` only after correcting all P0/P1/P2
visual defects. Include any remaining P3 polish as follow-up notes rather than
claiming it was fixed.

- [x] **Step 6: Report evidence**

Report changed files, observable page behavior, exact commands and exit
statuses, connected test counts, physical-device metadata limited to Android
version/API/ABI/model label, runtime findings, and any deferred API 36/37
qualification. Do not claim completion from prior P0/P1 runs; every claim in
the final report must be backed by fresh output from this redesign.

