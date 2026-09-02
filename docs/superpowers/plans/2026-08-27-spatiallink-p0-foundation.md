# SpatialLink P0 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved offline-first SpatialLink P0 Android foundation that inspects local capabilities and permissions, exposes typed immutable state, renders diagnostics, and verifies API 29–37 compatibility.

**Architecture:** Keep pure domain ports, models, policy, aggregation, and status derivation in `:core:model`; keep Android service access in injected probes under `:core:capabilities`; keep UI state and Compose in `:feature:diagnostics`; and construct concrete dependencies only in `:app`. Use a dedicated API-guarded `android.ranging.RangingManager` adapter with a cancellable callback bridge and no reflection.

**Tech Stack:** Gradle Kotlin DSL, Gradle 9.5.0, AGP 9.3.1, Kotlin 2.4.10, JDK 17, compile/target SDK 37, min SDK 29, Compose UI 1.12.0, Material 3 1.4.0, Coroutines, StateFlow, AndroidX, JUnit 4, and Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-08-27-spatiallink-p0-foundation-design.md`

## Global Constraints

- Use `com.r2h.spatiallink` for namespace and application ID.
- Use `minSdk = 29`, `compileSdk = 37`, `targetSdk = 37`, JDK 17, AGP 9.3.1, Gradle 9.5.0, Kotlin 2.4.10, Compose UI 1.12.0, and Material 3 1.4.0.
- Keep `:core:model` free of `android.*`, `androidx.compose.*`, Activity, Context, and ViewModel dependencies.
- Use constructor injection and structured concurrency; do not use `GlobalScope`, raw `Thread`, unowned scopes, ad-hoc executors, or production `runBlocking`.
- Do not add `androidx.core.uwb`, reflection, HTTP/networking libraries, Firebase, analytics, telemetry, cloud services, databases, DI frameworks, ML libraries, broad storage permissions, device identifiers, cryptographic identity, discovery, transfer, pairing, or actual ranging sessions.
- Do not request runtime permissions during application startup.
- Treat missing hardware as `UNAVAILABLE`, not as an error; retain successful subsystem results when another subsystem fails.
- Use stable, explicit dependency versions from `gradle/libs.versions.toml`; do not use dynamic, alpha, beta, rc, snapshot, or latest-release versions.
- Do not modify the neighboring `E:\\Projects\\R2H-Android\\SpatialLink` directory.
- The target is not a Git repository, so do not initialize Git or create commits unless the user later requests repository history.

## File map

### Build and repository files

- Create: `settings.gradle.kts` — root name, module includes, plugin management, and repositories.
- Create: `build.gradle.kts` — intentionally small root build script.
- Create: `gradle.properties` — reproducible Gradle/Kotlin settings without machine paths.
- Create: `gradle/libs.versions.toml` — every external version and dependency alias.
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, and `gradlew.bat` — Gradle 9.5.0 wrapper.
- Create: `build-logic/settings.gradle.kts` and `build-logic/build.gradle.kts` — convention-plugin build.
- Create: `build-logic/src/main/kotlin/spatiallink.android.application.gradle.kts` — application convention.
- Create: `build-logic/src/main/kotlin/spatiallink.android.library.gradle.kts` — Android-library convention.
- Create: `build-logic/src/main/kotlin/spatiallink.android.compose.gradle.kts` — Kotlin Compose compiler convention.
- Create: `build-logic/src/main/kotlin/spatiallink.kotlin.library.gradle.kts` — pure Kotlin/JVM convention.
- Create: `.gitignore` and `.editorconfig` — repository hygiene and formatting.

### Pure domain files

- Create under `core/model/src/main/kotlin/com/r2h/spatiallink/model/`: `CapabilityState.kt`, `CapabilityTypes.kt`, `CapabilityIssue.kt`, `DeviceCapabilities.kt`, `CapabilityProbeResult.kt`, `CapabilitySnapshotAssembler.kt`, `FoundationStatus.kt`, `PermissionModels.kt`, `PermissionPolicy.kt`, and `Ports.kt`.
- Create tests under `core/model/src/test/kotlin/com/r2h/spatiallink/model/`: `CapabilitySnapshotAssemblerTest.kt`, `FoundationStatusTest.kt`, and `PermissionPolicyTest.kt`.

### Shared and Android capability files

- Create under `core/common/src/main/kotlin/com/r2h/spatiallink/common/`: `DispatcherProvider.kt`.
- Create under `core/capabilities/src/main/kotlin/com/r2h/spatiallink/capabilities/`: `CapabilityProbe.kt`, `AndroidDeviceCapabilityDetector.kt`, `AndroidCapabilityFactory.kt`, `AndroidFeatureReader.kt`, `AndroidBluetoothCapabilityProbe.kt`, `AndroidWifiCapabilityProbe.kt`, `AndroidNfcUwbCapabilityProbe.kt`, `AndroidPlatformRangingCapabilityProbe.kt`, `RangingCallbackBridge.kt`, `Api36RangingCapabilitySource.kt`, `Api37RangingTechnologyIds.kt`, `PermissionPlatformAccess.kt`, and `AndroidPermissionStateReader.kt`.
- Create tests under `core/capabilities/src/test/kotlin/com/r2h/spatiallink/capabilities/`: `AndroidDeviceCapabilityDetectorTest.kt`, `CapabilityProbeMappingTest.kt`, `RangingCallbackBridgeTest.kt`, and `AndroidPermissionStateReaderTest.kt`.

### Diagnostics and app files

- Create under `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/`: `DiagnosticsUiState.kt`, `DiagnosticsUiStateReducer.kt`, `DiagnosticsViewModel.kt`, `DiagnosticsViewModelFactory.kt`, and `DiagnosticsScreen.kt`.
- Create tests under `feature/diagnostics/src/test/kotlin/com/r2h/spatiallink/diagnostics/`: `DiagnosticsUiStateReducerTest.kt` and `DiagnosticsViewModelTest.kt`.
- Create under `testing/src/main/kotlin/com/r2h/spatiallink/testing/`: `CapabilityFixtures.kt`, `FakeDeviceCapabilityDetector.kt`, `FakePermissionStateReader.kt`, and `TestDispatcherProvider.kt`; test the fixture provider under `testing/src/test/kotlin/com/r2h/spatiallink/testing/`.
- Create under `app/src/main/java/com/r2h/spatiallink/`: `SpatialLinkApplication.kt`, `MainActivity.kt`, and `ui/theme/SpatialLinkTheme.kt`.
- Create under `app/src/main/res/values/`: `strings.xml` and `themes.xml`; create `app/src/main/res/drawable/ic_spatiallink.xml`; create `app/src/main/AndroidManifest.xml`.
- Create `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml` to exclude the empty P0 data root from legacy and modern backup/transfer paths.
- Create under `app/src/androidTest/java/com/r2h/spatiallink/`: `DiagnosticsInstrumentedTest.kt`.
- Create: `docs/architecture/FOUNDATION.md` and `README.md`.

## Task 1: Bootstrap the Gradle multi-module project

**Files:** Build and repository files listed above.

**Interfaces:** The root build produces the modules `:app`, `:core:common`, `:core:model`, `:core:capabilities`, `:feature:diagnostics`, and `:testing`. The included `build-logic` build produces the four convention plugin IDs used by those modules.

- [ ] **Step 1: Create the root settings and module directories.**

Add `pluginManagement { includeBuild("build-logic") }`, Google/Maven Central/Gradle Plugin Portal repositories, a root version catalog from `gradle/libs.versions.toml`, and these includes:

```kotlin
rootProject.name = "SpatialLink"
include(":app")
include(":core:common")
include(":core:model")
include(":core:capabilities")
include(":feature:diagnostics")
include(":testing")
```

- [ ] **Step 2: Define the explicit version catalog.**

Record AGP 9.3.1, Kotlin 2.4.10, Compose UI 1.12.0, Material 3 1.4.0, Coroutines 1.10.2, AndroidX Core KTX 1.19.0, Activity Compose 1.13.0, Lifecycle 2.11.0, AndroidX Annotation 1.9.1, AndroidX Test JUnit 1.3.0, Espresso 3.7.0, and JUnit 4.13.2 as explicit stable aliases. Add aliases for Compose UI, foundation, runtime, material3, tooling preview, UI test JUnit4, UI test manifest, lifecycle viewmodel, lifecycle runtime compose, and coroutines test.

The catalog must not contain `androidx.core.uwb` or any forbidden dependency.

- [ ] **Step 3: Add the convention build.**

Import the root catalog in `build-logic/settings.gradle.kts` and make `build-logic/build.gradle.kts` apply `kotlin-dsl` with plugin classpath dependencies sourced from the catalog. Configure the convention plugins as follows:

```kotlin
// spatiallink.kotlin.library.gradle.kts
plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
```

The Android application and library conventions configure compile SDK 37, min SDK 29, Java/Kotlin 17, and the Android test runner. The application convention sets target SDK 37 and an explicit non-optimized release configuration using the AGP 9.3.1 DSL. The Compose convention applies `org.jetbrains.kotlin.plugin.compose`; Android modules enable `buildFeatures.compose` in their module script.

- [ ] **Step 4: Add module build scripts and the Gradle wrapper.**

Create module build scripts with only the dependencies needed by each module. Set each Android namespace explicitly, including `com.r2h.spatiallink` for the app. Bootstrap the wrapper from the available wrapper binary, set `distributionUrl` to the official Gradle 9.5.0 binary distribution, and verify the wrapper launches on JDK 17.

- [ ] **Step 5: Add repository hygiene files.**

Ignore `local.properties`, `.gradle`, all module `build` directories, IDE state, APK/AAB outputs, signing material, and machine-specific files. Set Kotlin official code style, UTF-8, LF line endings, and four-space indentation in `.editorconfig`.

- [ ] **Step 6: Run the bootstrap configuration check.**

Run:

```powershell
& { $env:JAVA_HOME = 'D:\\R2H-Dev\\Java\\jdk-17'; .\\gradlew.bat projects --console=plain }
```

Expected: Gradle 9.5.0 starts successfully and lists all six requested project modules without compiling source.

## Task 2: Add the shared dispatcher port

**Files:** `core/common/build.gradle.kts`, `core/common/src/main/kotlin/com/r2h/spatiallink/common/DispatcherProvider.kt`, `core/common/src/test/kotlin/com/r2h/spatiallink/common/DispatcherProviderTest.kt`, and `testing/src/main/kotlin/com/r2h/spatiallink/testing/TestDispatcherProvider.kt`.

**Interfaces:** Produce:

```kotlin
interface DispatcherProvider {
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Default
}
```

- [ ] **Step 1: Add the coroutine dependency and write the smallest compile check.**

Configure `:core:common` as a pure Kotlin library with `kotlinx-coroutines-core`, then add a focused test using a private fixed provider so the common module does not depend on the later `:testing` module. The fixture module will test its own `TestDispatcherProvider` after it is composed.

- [ ] **Step 2: Implement production and test providers.**

Keep the production provider free of scope ownership; it only supplies `Dispatchers.Default`. Keep the test provider constructor-injected and deterministic.

- [ ] **Step 3: Run the focused test.**

Run `:core:common:test` and require a zero exit code before proceeding.

## Task 3: Build pure domain models and policies with TDD

**Files:** All `core/model` production and test files listed in the file map.

**Interfaces:** Define these pure ports and models:

```kotlin
interface DeviceCapabilityDetector {
    suspend fun detect(): DeviceCapabilities
}

interface PermissionStateReader {
    fun snapshot(): PermissionSnapshot
}

data class DeviceCapabilities(
    val androidVersion: String,
    val sdkInt: Int,
    val bluetoothLe: CapabilityState,
    val bleAdvertising: CapabilityState,
    val wifiDirect: CapabilityState,
    val wifiAware: CapabilityState,
    val wifiRtt: CapabilityState,
    val nfc: CapabilityState,
    val nfcHce: CapabilityState,
    val uwb: CapabilityState,
    val platformRanging: CapabilityState,
    val ranging: RangingCapabilities,
    val issues: List<CapabilityIssue>
)
```

- [ ] **Step 1: Write failing capability-state tests.**

Test that aggregation of `[UNAVAILABLE, UNAVAILABLE]` is `UNAVAILABLE`, `[UNKNOWN, UNAVAILABLE]` is `UNKNOWN`, and any list containing `AVAILABLE` is `AVAILABLE`. Test a snapshot assembled from all unavailable hardware states has no issue and derives `READY`.

```kotlin
@Test
fun hardware_absence_is_unavailable_without_partial_status() {
    val capabilities = CapabilitySnapshotAssembler.assemble(
        sdkInt = 29,
        androidVersion = "10",
        results = listOf(CapabilityProbeResult.allUnavailable())
    )

    assertEquals(CapabilityState.UNAVAILABLE, capabilities.bluetoothLe)
    assertEquals(FoundationStatus.READY, deriveFoundationStatus(capabilities))
}
```

- [ ] **Step 2: Implement capability, issue, ranging, and snapshot models.**

Create `CapabilityState`, `SpatialCapability`, `RangingTechnology`, `CapabilitySubsystem`, `CapabilityIssueCategory`, `CapabilityIssueCode`, `CapabilityIssue`, `RangingCapabilities`, `DeviceCapabilities`, and `CapabilityProbeResult`. Keep every collection exposed as a read-only immutable Kotlin collection and copy incoming lists/maps at construction boundaries.

- [ ] **Step 3: Implement snapshot aggregation and legacy ranging fallback.**

Implement `CapabilitySnapshotAssembler.assemble(sdkInt: Int, androidVersion: String, results: List<CapabilityProbeResult>)`. Fill missing spatial keys with `UNKNOWN`; for API <36 derive only meaningful legacy ranging states: UWB from the UWB spatial state when API >=31, BLE RSSI from Bluetooth LE, Wi-Fi NAN RTT from Wi-Fi Aware plus Wi-Fi RTT, and mark BLE Channel Sounding/Wi-Fi Proximity Detection unavailable. For API >=36 require the ranging probe to supply all five technology states, filling absent technology entries as unavailable.

- [ ] **Step 4: Write failing permission-policy tests for API 29/31/33/36/37.**

Test the exact transitions: Bluetooth keys are not required at 29 and required at 31; Nearby Wi-Fi is not required at 32 and required at 33; RANGING is not required at 35 and required at 36; local network is not required at OS 36 and required at OS 37 only when target SDK is at least 37.

- [ ] **Step 5: Implement permission models and pure policy.**

Define `PermissionKey`, `PermissionState` with `GRANTED`, `DENIED`, `NOT_REQUIRED_ON_THIS_OS`, `NOT_DECLARED`, and `UNKNOWN`, and the six-field `PermissionSnapshot`. Implement `PermissionPolicy.isRequired(key, sdkInt, targetSdk)` without Android constants or framework imports.

- [ ] **Step 6: Write and implement foundation-status tests.**

Verify `READY` for known available/unavailable states with no issues, `PARTIAL` for unknown states or recoverable issues, and `ERROR` only when no useful snapshot exists through `deriveFoundationStatus(capabilities: DeviceCapabilities?, fatalError: Boolean)`.

- [ ] **Step 7: Run pure model tests.**

Run `:core:model:test`. Inspect the compiled dependency report to confirm `:core:model` has no Android or Compose dependency.

## Task 4: Add injected Android capability probes and partial-failure aggregation

**Files:** `core/capabilities/build.gradle.kts` and the capability files listed in the file map except the ranging bridge and permission files.

**Interfaces:** Produce:

```kotlin
interface CapabilityProbe {
    val subsystem: CapabilitySubsystem
    val spatialCapabilities: Set<SpatialCapability>
    val rangingTechnologies: Set<RangingTechnology>
    suspend fun inspect(): CapabilityProbeResult
}

class AndroidDeviceCapabilityDetector(
    private val sdkInt: Int,
    private val androidVersion: String,
    private val probes: List<CapabilityProbe>,
    private val dispatchers: DispatcherProvider
) : DeviceCapabilityDetector
```

- [ ] **Step 1: Write a failing detector test with fake probes.**

Use one fake probe returning Bluetooth `AVAILABLE` and another probe throwing a non-cancellation exception. Assert the assembled snapshot retains Bluetooth, marks the failed probe’s declared capabilities `UNKNOWN`, and records the expected typed issue. Add a separate test asserting cancellation propagates.

- [ ] **Step 2: Implement the probe contract and safe failure conversion.**

Run one child `async` operation per probe inside `withContext(dispatchers.default) { supervisorScope { awaitAll(probeJobs) } }`, await every child, and flatten the results before snapshot assembly. Convert `SecurityException` to `SECURITY_FAILURE`, service absence to `SERVICE_UNAVAILABLE`, and other framework failures to `UNEXPECTED_FAILURE`. Re-throw `CancellationException`. A failed probe must return unknown values for its declared keys and one `CapabilityIssue` rather than aborting sibling probes.

- [ ] **Step 3: Implement the feature-reader seam.**

Define `SystemFeatureReader` and `AndroidSystemFeatureReader`, where the production implementation delegates only to `PackageManager.hasSystemFeature(String)`. Use literal platform feature names in the Android adapter to avoid loading newer `PackageManager` fields on old APIs.

- [ ] **Step 4: Implement Bluetooth inspection and mapping.**

Inject an adapter inspection result containing adapter presence, advertising support, and an optional typed issue. The production inspector obtains `BluetoothManager` and its adapter from the application context, calls `isMultipleAdvertisementSupported()`, and never reads an address or enabled-state identity. Map absent BLE feature to unavailable; present feature plus absent service to unknown; present adapter plus false advertising support to unavailable.

- [ ] **Step 5: Implement Wi-Fi inspection and mapping.**

Inject a result for Wi-Fi Direct, Wi-Fi Aware, and Wi-Fi RTT. The production inspector obtains `WifiManager`, `WifiAwareManager`, and `WifiRttManager`, calls `isP2pSupported()`/`isAvailable()`, and maps each feature/service independently. A missing or failing one must not affect the other two.

- [ ] **Step 6: Implement NFC and UWB inspection and mapping.**

Use feature names for NFC, HCE, and UWB. Inspect `NfcAdapter.getDefaultAdapter(context)` only when the NFC feature is present; map a missing adapter with a typed service issue while allowing HCE to use its independent feature result. Report UWB unavailable before API 31 and otherwise use the optional UWB feature without adding `androidx.core.uwb`.

- [ ] **Step 7: Implement the Android capability factory.**

Create all probes with `applicationContext`, pass `Build.VERSION.SDK_INT` and `Build.VERSION.RELEASE` as platform metadata, inject one `DefaultDispatcherProvider`, and select the API 36 ranging source only after the API guard. The factory must not start discovery, open sockets, request permissions, or create a ranging session.

- [ ] **Step 8: Run focused capability tests and inspect dependencies.**

Run `:core:capabilities:test`. Confirm the test output covers partial failure and unknown propagation, then run a dependency report and verify no UWB support library, network client, or forbidden dependency is present.

## Task 5: Implement the typed API 36/37 ranging callback bridge

**Files:** `RangingCallbackBridge.kt`, `Api36RangingCapabilitySource.kt`, `Api37RangingTechnologyIds.kt`, `AndroidPlatformRangingCapabilityProbe.kt`, and `RangingCallbackBridgeTest.kt`.

**Interfaces:** Define the testable registration seam:

```kotlin
interface RangingCallbackRegistration {
    fun register(callback: (Map<Int, Int>) -> Unit)
    fun unregister()
}

class CancellableRangingCapabilityReader(
    private val registration: RangingCallbackRegistration
) {
    suspend fun read(): Map<Int, Int>
}
```

- [ ] **Step 1: Write the failing callback-bridge tests.**

Test a callback result resumes `read()` and calls `unregister()` exactly once. Emit a second callback and cancel after completion; the count must remain one. Test cancellation before a callback calls unregister exactly once and leaves the coroutine cancelled. Test registration failure also invokes unregister at most once and returns the failure to the API-boundary mapper.

```kotlin
@Test
fun cancellation_unregisters_once_without_callback() = runTest {
    val registration = FakeRangingCallbackRegistration()
    val reader = CancellableRangingCapabilityReader(registration)
    val job = launch { reader.read() }

    runCurrent()
    job.cancelAndJoin()

    assertEquals(1, registration.unregisterCount)
}
```

- [ ] **Step 2: Implement the cancellation-safe bridge.**

Use `suspendCancellableCoroutine`, install the cancellation handler before registration, guard completion/unregister with an `AtomicBoolean`, unregister before resuming on the first callback, and rethrow cancellation. Keep the callback, registration, and continuation local to one read operation.

- [ ] **Step 3: Implement the API 36 typed manager registration.**

In an `@RequiresApi(36)` source, obtain `RangingManager` from the application context, use the context-owned `mainExecutor`, register one `RangingCapabilitiesCallback`, copy `getTechnologyAvailability()` into a plain map, and clear the stored callback after unregistration. Catch registration/service failures at the source boundary and convert them to a typed probe result. Do not call `createRangingSession()`.

- [ ] **Step 4: Isolate the API 37-only technology constant.**

Keep the `RangingManager.WIFI_PD` reference in `@RequiresApi(37)` `Api37RangingTechnologyIds`. The API 36 source must not reference that field. Select the API 37 ID only after the OS-level guard; on API 36, Wi-Fi Proximity Detection is unavailable.

- [ ] **Step 5: Implement typed technology/status mapping.**

Map API 36 IDs for UWB, BLE Channel Sounding, Wi-Fi NAN RTT, and BLE RSSI; map the API 37 Wi-Fi PD ID when present. Translate `RangingCapabilities.ENABLED` to available, `NOT_SUPPORTED` and disabled statuses to unavailable, and unrecognized statuses to unknown with a typed issue. Set platform ranging available only when at least one technology is enabled; set it unavailable for a successful empty/disabled map and unknown for service/callback failure.

- [ ] **Step 6: Implement safe pre-36 behavior.**

The unsupported source returns `PLATFORM_RANGING = UNAVAILABLE` and does not mention any `android.ranging.*` type. The detector’s pure fallback supplies the legacy range states described in the specification.

- [ ] **Step 7: Run callback and capability tests.**

Run `:core:capabilities:testDebugUnitTest --tests '*RangingCallbackBridgeTest'` followed by `:core:capabilities:test`. Inspect bytecode/source references to verify API 36/37 classes occur only in the dedicated guarded files and that no reflection string lookup exists.

## Task 6: Implement centralized API-aware permission inspection

**Files:** `PermissionPlatformAccess.kt`, `AndroidPermissionStateReader.kt`, `AndroidPermissionStateReaderTest.kt`, `app/src/main/AndroidManifest.xml`, and `app/build.gradle.kts` if manifest metadata is needed.

**Interfaces:** Produce:

```kotlin
enum class PermissionLookupState {
    GRANTED,
    DENIED,
    NOT_DECLARED,
    UNKNOWN
}

interface PermissionPlatformAccess {
    fun lookup(permissionName: String): PermissionLookupState
}

class AndroidPermissionStateReader(
    private val sdkInt: Int,
    private val targetSdk: Int,
    private val access: PermissionPlatformAccess
) : PermissionStateReader
```

- [ ] **Step 1: Write failing reader tests.**

Use a fake access implementation to verify required-and-granted, required-and-denied, required-but-not-declared, not-required-on-older-OS, and unknown lookup states. Cover every boundary at API 29, 31, 33, 36, and 37.

- [ ] **Step 2: Implement the pure-to-platform mapping.**

Map `PermissionKey` to literal permission names: `android.permission.BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `NEARBY_WIFI_DEVICES`, `ACCESS_LOCAL_NETWORK`, and `RANGING`. Apply `PermissionPolicy` before platform lookup so an unsupported OS reports `NOT_REQUIRED_ON_THIS_OS` even if the manifest contains a newer permission.

- [ ] **Step 3: Implement the Android access adapter.**

Read the app’s declared permission set using the API-29-compatible `PackageManager.GET_PERMISSIONS` overload, then call `Context.checkSelfPermission()` only for declared permissions. Convert package-manager or permission-check failures to `UNKNOWN`; never request a permission and never expose an exception to the domain model.

- [ ] **Step 4: Add the exact manifest declarations.**

Declare legacy Bluetooth permissions with `maxSdkVersion="30"`; modern Bluetooth scan with `usesPermissionFlags="neverForLocation"`, advertise, and connect; `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, and `CHANGE_NETWORK_STATE`; `NEARBY_WIFI_DEVICES` with `neverForLocation`; `ACCESS_FINE_LOCATION` with `maxSdkVersion="32"`; `ACCESS_LOCAL_NETWORK`; `NFC`; `RANGING`; and `INTERNET`. Do not declare storage permissions, required hardware features, or UWB support libraries.

- [ ] **Step 5: Run permission tests and manifest inspection.**

Run `:core:capabilities:testDebugUnitTest --tests '*AndroidPermissionStateReaderTest'`. After an Android manifest merge, inspect the merged manifest to confirm all requested permissions and version flags match the approved strategy and no startup request code exists.

## Task 7: Build diagnostics state, ViewModel, and Compose UI

**Files:** All `feature/diagnostics` production/test files listed in the file map.

**Interfaces:** Define:

```kotlin
data class DiagnosticsUiState(
    val isLoading: Boolean,
    val capabilities: DeviceCapabilities?,
    val permissions: PermissionSnapshot?,
    val error: DiagnosticsError?,
    val foundationStatus: FoundationStatus
)

sealed interface DiagnosticsError {
    data object CapabilityInspectionFailed : DiagnosticsError
    data object PermissionInspectionFailed : DiagnosticsError
}
```

- [ ] **Step 1: Write failing reducer tests.**

Test loading state, successful known snapshot to `READY`, successful partial snapshot to `PARTIAL`, capability failure to `ERROR`, and permission-only failure to a useful capability snapshot with `PARTIAL`. Assert that no raw exception is stored in UI state.

- [ ] **Step 2: Implement the immutable reducer.**

Add `DiagnosticsUiStateReducer.loading()`, `success(capabilities, permissions)`, and `failure(capabilities, permissions, error)`. Keep status derivation in one place and preserve successful fields when a sibling inspection fails.

- [ ] **Step 3: Write the ViewModel test.**

Use `FakeDeviceCapabilityDetector`, `FakePermissionStateReader`, and `TestDispatcherProvider` with `runTest` and `advanceUntilIdle()`. Verify `StateFlow` transitions from loading to result, sibling completion on detector failure, and cancellation behavior.

- [ ] **Step 4: Implement the ViewModel and factory.**

Launch one structured `viewModelScope` job on the injected dispatcher, use `supervisorScope` with two child inspections, rethrow `CancellationException`, map other failures to the two typed errors, and expose only `StateFlow<DiagnosticsUiState>` via `asStateFlow()`. The factory must reject unsupported ViewModel classes rather than silently constructing another type.

- [ ] **Step 5: Implement the stateless Material 3 screen.**

Render title, Android version/API, all nine spatial capabilities, all five ranging technologies, all six permissions, issue count, loading state, and foundation status from `DiagnosticsUiState`. Use a scrollable `LazyColumn`, readable typography, text labels for every state, semantic headings/content descriptions, and no hardware or permission API calls from Composables.

- [ ] **Step 6: Run feature tests.**

Run `:feature:diagnostics:test` and inspect that the tests exercise the public reducer/ViewModel seams rather than private implementation details.

## Task 8: Compose the application and add launchable resources

**Files:** `app/build.gradle.kts`, `SpatialLinkApplication.kt`, `MainActivity.kt`, theme/resources, and `app/src/main/AndroidManifest.xml`.

**Interfaces:** The application constructs concrete capability and permission adapters, while `MainActivity` only obtains the application container and renders `DiagnosticsRoute`.

- [ ] **Step 1: Add the application module dependencies.**

Depend on `:feature:diagnostics`, `:core:capabilities`, and `:core:common`; add Activity Compose, Core KTX, and the Compose UI-test/debug artifacts required by instrumentation. Do not add a network, UWB, DI, or analytics dependency.

- [ ] **Step 2: Implement the explicit composition root.**

Create `SpatialLinkApplication` with an application-context `AppContainer` containing `DefaultDispatcherProvider`, the Android capability detector factory, and `AndroidPermissionStateReader` configured with target SDK 37. No detector or permission call runs in `Application.onCreate()`.

- [ ] **Step 3: Implement `MainActivity`.**

Use `ComponentActivity`, `by viewModels { DiagnosticsViewModelFactory(application.container.capabilityDetector, application.container.permissionStateReader, application.container.dispatcherProvider) }`, `setContent`, and the app theme. Do not access Bluetooth, Wi-Fi, NFC, UWB, RangingManager, or permissions in the Activity.

- [ ] **Step 4: Add light/dark theme and manifest metadata.**

Use a small Compose Material 3 color scheme with system dark-mode selection, a platform window theme, a non-required simple vector launcher icon, `android:exported="true"`, and the application class. Keep portrait and landscape supported through normal configuration handling; do not add `configChanges` to suppress recreation.

- [ ] **Step 5: Run the debug launch build.**

Run `:app:assembleDebug` and inspect the APK manifest/package name before adding instrumentation tests.

## Task 9: Add shared test fixtures, instrumentation coverage, and documentation

**Files:** `testing` module files, `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt`, `README.md`, `docs/architecture/FOUNDATION.md`, and any required test build scripts.

- [ ] **Step 1: Implement reusable test fixtures and fakes.**

Provide deterministic builders for a fully unavailable snapshot, a fully known snapshot, and a partial snapshot; provide fakes for the two pure ports and a dispatcher provider. Keep this module test-only in consumers and keep it free of Android framework dependencies.

- [ ] **Step 2: Add Compose/instrumentation tests.**

Use `createAndroidComposeRule<MainActivity>()` to assert launch, title, capability rows, ranging rows, permission rows, and status text. Add isolated screen tests for loading and unsupported states. Recreate the Activity and assert the diagnostics title remains available. Do not assert that NFC, HCE, UWB, Wi-Fi Aware, or Wi-Fi RTT exists on an emulator; assert row presence and state text instead.

- [ ] **Step 3: Write the README.**

Document the exact JDK 17/Gradle wrapper commands, module responsibilities, offline invariant, permission behavior, and the fact that P0 does not request permissions or implement future transports.

- [ ] **Step 4: Write `FOUNDATION.md` from the implemented code.**

Document purpose, module graph, dependency direction, pure-domain boundary, capability/error model, permission architecture, API 29–37 strategy, API 36/37 ranging isolation and callback lifecycle, test strategy, P0 limitations, and the explicit P1–P13 boundary. Include the Mermaid graph from the approved specification and record that UWB uses platform feature inspection plus unified ranging capability data without `androidx.core.uwb`.

- [ ] **Step 5: Run static scope checks.**

Search source and manifests, excluding generated/build directories, for `androidx.core.uwb`, Retrofit, OkHttp, Ktor client, Firebase, analytics, telemetry, `MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, IMEI, serial, MAC/address identity use, `GlobalScope`, raw `Thread`, `runBlocking`, reflection APIs, discovery/session/transfer code, and raw exception rendering. Resolve every P0-relevant match before final verification.

## Task 10: Perform the final evidence-gated verification

**Files:** No source changes are planned for this task unless a verification failure identifies a defect in the preceding tasks.

- [ ] **Step 1: Confirm the toolchain and device state.**

Run:

```powershell
& 'D:\\R2H-Dev\\Java\\jdk-17\\bin\\java.exe' -version
Get-Command adb
adb devices -l
emulator -list-avds
```

Record the exact outputs. A missing compatible device blocks instrumentation evidence but does not block JVM/build/lint evidence.

- [ ] **Step 2: Run the fresh clean gate.**

Run sequentially with JDK 17 and `--console=plain`:

```powershell
& { $env:JAVA_HOME = 'D:\\R2H-Dev\\Java\\jdk-17'; .\\gradlew.bat clean --console=plain }
& { $env:JAVA_HOME = 'D:\\R2H-Dev\\Java\\jdk-17'; .\\gradlew.bat assembleDebug --console=plain }
& { $env:JAVA_HOME = 'D:\\R2H-Dev\\Java\\jdk-17'; .\\gradlew.bat assembleRelease --console=plain }
& { $env:JAVA_HOME = 'D:\\R2H-Dev\\Java\\jdk-17'; .\\gradlew.bat test --console=plain }
& { $env:JAVA_HOME = 'D:\\R2H-Dev\\Java\\jdk-17'; .\\gradlew.bat lint --console=plain }
```

Inspect each exit code and full failure output. Do not classify a failed gate as passed because another gate is green.

- [ ] **Step 3: Count and inspect JVM test results.**

Read all `build/test-results/test*/TEST-*.xml` files, sum `tests`, `failures`, `errors`, and `skipped`, and record the count and failure details. Require zero failures/errors.

- [ ] **Step 4: Verify the release/debug artifacts and merged manifest.**

Locate the freshly produced APKs, verify the application ID and variant, inspect the merged manifest for permissions/flags, and inspect dependency reports for forbidden libraries and `androidx.core.uwb`.

- [ ] **Step 5: Run instrumentation only when a compatible device is connected.**

If `adb devices` reports a usable device, run:

```powershell
& { $env:JAVA_HOME = 'D:\\R2H-Dev\\Java\\jdk-17'; .\\gradlew.bat :app:connectedDebugAndroidTest --console=plain }
```

Inspect the generated instrumentation XML for the intended test class and record pass/fail counts. If no compatible device is available, record instrumentation as blocked with the exact ADB/emulator output; do not claim it passed.

- [ ] **Step 6: Run the final source/static review.**

Inspect the final module graph, all manifests, all public model types, API-guarded files, callback unregister path, Compose screen imports, dependency list, and documentation. Search for unfinished-work markers and prohibited P1+ features. Review the complete working-tree file list without initializing Git.

- [ ] **Step 7: Complete the evidence matrix.**

Map each master-prompt acceptance criterion to its changed seam and fresh command evidence. Use `Confirmed`, `Partial`, `Unverified`, or `Failed` accurately. End the final report with `P0_FOUNDATION_COMPLETE` only if all mandatory build/test/lint and supported instrumentation criteria are evidenced; otherwise end with `P0_FOUNDATION_BLOCKED` and identify the exact blocker.
