# SpatialLink P2 Privacy-Preserving BLE Peer Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the bounded P2 foreground BLE presence session defined by the approved specification, with an anonymous nine-byte legacy frame, least-privilege permissions, deterministic pure-Kotlin policy, isolated Android adapters, and no feature-to-feature dependency.

**Architecture:** :core:discovery owns the pure protocol, token, cache, lifecycle, permission policy, and platform-neutral ports. :connectivity:ble owns typed Android BLE access and callback cleanup; :feature:nearby owns the ViewModel and user-facing presence surface. The frozen visual primitives move from diagnostics into neutral :core:designsystem, while :app owns composition and feature selection.

**Tech Stack:** Kotlin 2.4.10, JDK 17, Gradle 9.5.0, Android Gradle Plugin 9.3.1, compile/target SDK 37, minimum SDK 29, Kotlin coroutines 1.10.2, Jetpack Compose UI 1.12.0, Material 3 1.4.0, Android BLE legacy scanner/advertiser APIs, and JUnit 4.

**Spec:** docs/superpowers/specs/2026-08-28-spatiallink-p2-ble-discovery-design.md

## Global Constraints

- :core:discovery remains pure Kotlin/JVM with no Android framework, AndroidX, or :core:identity dependency.
- :core:designsystem contains only neutral frozen SpatialLink presentation infrastructure; it contains no state, ViewModel, Bluetooth, permission, capability, identity, route, or business logic.
- :feature:nearby and :feature:diagnostics both depend on :core:designsystem; neither feature depends on the other.
- The fixed service-data UUID is 7F83C58D-4C4B-4E97-9B7D-9C06D3A47B91.
- The v1 service-data payload is exactly nine bytes: byte 0 is 0x01; bytes 1..8 are one big-endian DiscoverySessionId.
- The SpatialLink-owned service-data AD structure is 27 bytes; the final legacy advertisement is bounded at <= 31 bytes without asserting an exact physical packet total.
- ADVERTISE_FAILED_DATA_TOO_LARGE is a hard defect; no extended advertising, scan response, duplicate UUID, or payload reduction is a fallback.
- The production SecureRandom generator retries all-zero output for at most eight draws; repeated zero output becomes typed SESSION_ID_GENERATION_FAILED.
- P2 scanning reads only fixed-UUID service-data payload, RSSI, and monotonic receipt time; production code never accesses the scan result's Bluetooth device object.
- Discovery needs BLUETOOTH_SCAN for scanning and BLUETOOTH_ADVERTISE for advertising on API 31+; BLUETOOTH_CONNECT is requested only for the verified ACTION_REQUEST_ENABLE boundary or another typed, concrete connect-gated operation.
- API 29–30 BLE scanning uses runtime ACCESS_FINE_LOCATION; P2 never requests NEARBY_WIFI_DEVICES, ACCESS_LOCAL_NETWORK, or RANGING.
- One 30-second foreground session uses one RAM-only token; peer TTL is six seconds; cache capacity is 64; self-token observations are ignored; authenticity is UNVERIFIED only.
- There is no GATT, pairing, handshake, trust promotion, transfer, networking, ranging session, NFC work, background execution, persistence, direction, azimuth, or RSSI-to-distance conversion.
- Cleanup is exactly once for every scanner and advertiser exit path, and a partial startup rolls back the already-started side.
- DiscoveryController is a session-owner-scoped object: it owns the session token, peer cache, scanner and advertiser handles, active timer/tick, and cleanup coordinator. AppContainer owns only long-lived factories and stateless Android dependencies; every NearbyViewModel receives a fresh controller that cannot be reused after close.
- Nearby close and Back use one leave path that completes controller cleanup before the route is hidden. Foreground loss uses the same cleanup coordinator, and returning to the foreground never restarts discovery automatically.
- While ACTIVE, one low-frequency tick rechecks the required SCAN and ADVERTISE permissions plus Bluetooth enabled state. Any invalid prerequisite uses the same exactly-once cleanup path; there is no busy polling.
- No startup permission request is allowed. Runtime permission requests are launched only after a user initiates discovery and the concrete missing operation is known.
- Physical qualification branches on one or two authorized intended devices. One device can produce P2_CODE_COMPLETE with TWO_DEVICE_QUALIFICATION_PENDING; only verified mutual observation on two devices can produce P2_BLE_DISCOVERY_COMPLETE.
- The final connected verification includes :core:identity:connectedDebugAndroidTest. :connectivity:ble has no module-level instrumentation suite in this plan; Android BLE integration is proven through the app and nearby connected suites, and no zero-test connected task is required.
- Use the existing approved toolchain and repository conventions. Do not add androidx.core.uwb, reflection, or a new external UI/framework dependency.
- The current checkout has no .git directory; do not initialize Git or add commit steps. Verification output is the change record for this execution.

---

## File map and implementation order

The work is ordered so each pure policy is tested before Android or UI integration depends on it.

| Task | Responsibility | Primary outputs |
|---:|---|---|
| 1 | Module graph and frozen visual extraction | :core:designsystem, :core:discovery, :connectivity:ble, and :feature:nearby scaffolding; diagnostics no longer owns shared visuals |
| 2 | Protocol and session-token domain | exact v1 codec, immutable token, bounded SecureRandom generation |
| 3 | Permission policy, peer cache, and discovery lifecycle | pure API mapping, TTL/capacity rules, typed state machine, rollback contract |
| 4 | Typed Android BLE boundary | legacy advertiser, filtered scanner, platform clock/state, exact cleanup and privacy boundary |
| 5 | Nearby orchestration and app composition | ViewModel/StateFlow, JIT permission effects, explicit Bluetooth-enable flow, feature-isolated root |
| 6 | Nearby presentation and regression tests | neutral anonymous Field surface, Compose semantics, diagnostics integration without feature coupling |
| 7 | Static architecture/privacy gates and documentation | source checks, README/foundation updates, evidence template |
| 8 | Complete verification and physical qualification | JVM/build/lint/connected gates, one-device evidence, two-device completion decision |

## Task 1: Establish module graph and extract frozen visual infrastructure

**Files:**

- Modify: settings.gradle.kts
- Create: core/designsystem/build.gradle.kts
- Create: core/discovery/build.gradle.kts
- Create: connectivity/ble/build.gradle.kts
- Create: feature/nearby/build.gradle.kts
- Modify: app/build.gradle.kts
- Modify: feature/diagnostics/build.gradle.kts
- Modify: app/src/main/java/com/r2h/spatiallink/ui/theme/SpatialLinkTheme.kt
- Move: feature/diagnostics/src/main/res/font/spatiallink_technical_regular.ttf to core/designsystem/src/main/res/font/spatiallink_technical_regular.ttf
- Create: core/designsystem/src/main/kotlin/com/r2h/spatiallink/designsystem/SpatialLinkColors.kt
- Create: core/designsystem/src/main/kotlin/com/r2h/spatiallink/designsystem/SpatialLinkFonts.kt
- Create: core/designsystem/src/main/kotlin/com/r2h/spatiallink/designsystem/SpatialLinkStatus.kt
- Create: core/designsystem/src/main/kotlin/com/r2h/spatiallink/designsystem/SpatialLinkAtmosphere.kt
- Create: core/designsystem/src/main/kotlin/com/r2h/spatiallink/designsystem/SpatialLinkSurfaces.kt
- Create: core/designsystem/src/main/kotlin/com/r2h/spatiallink/designsystem/SpatialLinkField.kt
- Create: core/designsystem/src/main/kotlin/com/r2h/spatiallink/designsystem/SpatialLinkNavigation.kt
- Modify: feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPages.kt
- Modify: feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPresentation.kt
- Modify: feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkShell.kt
- Delete after imports are migrated: feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkDesignTokens.kt, SpatialLinkFonts.kt, and SpatialLinkPrimitives.kt

**Interfaces:**

- Produces package com.r2h.spatiallink.designsystem with the existing public visual API: SpatialLinkColors, SpatialLinkFonts.Technical, SpatialLinkTone, StatusMarkModel, SpatialAtmosphere, SpatialSurface, SpatialFrame, SpatialCommand, StatusMark, TechnicalValue, InstrumentLabel, FieldAnchor, FieldNode, SpatialField, SpatialRing, ActionArrow, TrustSeal, SystemTile, and SystemCluster.
- Produces a stateless SpatialNavigationRail and visual NavigationGlyph values; route selection and destination meaning remain outside the design system.
- Consumes the existing diagnostics visual implementation without changing token values, dimensions, copy, drawing geometry, animation cadence, or page composition.
- Leaves SpatialDestination, OverviewModel, SystemClusterModel, diagnostics state, and diagnostics mapping inside :feature:diagnostics until the app-level routing task consumes them.

- [ ] **Step 1: Add the four module includes and convention build files.**

  Add these entries to settings.gradle.kts:

  ~~~kotlin
  include(":core:designsystem")
  include(":core:discovery")
  include(":connectivity:ble")
  include(":feature:nearby")
  ~~~

  Configure :core:designsystem and :feature:nearby with spatiallink.android.library plus spatiallink.android.compose; configure :core:discovery with spatiallink.kotlin.library; configure :connectivity:ble with spatiallink.android.library. Keep compile SDK 37, minimum SDK 29, Java/Kotlin 17, and the existing namespace prefix.

- [ ] **Step 2: Add only existing dependencies to the new modules.**

  Use the current version catalog. :core:designsystem depends on Compose foundation, runtime, UI, and Material 3. :core:discovery depends on :core:common and kotlinx-coroutines-core; its tests use coroutines-test and JUnit. :connectivity:ble depends on :core:discovery, :core:common, androidx.annotation, and coroutines. :feature:nearby depends on :core:discovery, :connectivity:ble, :core:designsystem, Compose, lifecycle runtime-compose, lifecycle ViewModel, coroutines, JUnit, and the existing Android test libraries. Add :core:designsystem and :feature:nearby to app/build.gradle.kts; add :core:designsystem to diagnostics. Do not add a new library coordinate.

- [ ] **Step 3: Write the module-boundary check before moving source.**

  Create tools/verify-p2-boundaries.ps1. Make it fail unless settings.gradle.kts includes all four modules, feature/nearby/build.gradle.kts contains no diagnostics project dependency, feature/diagnostics/build.gradle.kts contains no nearby dependency, and core/designsystem production source contains none of DiagnosticsUiState, Nearby, Bluetooth, Permission, ViewModel, or IdentityRepository. Run it before and after extraction; the pre-extraction run is expected to report only the missing module graph.

- [ ] **Step 4: Move the frozen tokens and font without changing values.**

  Copy SpatialLinkColors exactly into :core:designsystem under the neutral package, move the bundled technical font, and move SpatialLinkFonts so its generated R reference resolves to the design-system package. Change the app theme import to com.r2h.spatiallink.designsystem.SpatialLinkColors. Keep the existing dark/light Material bridge unchanged.

- [ ] **Step 5: Split the existing primitive file by responsibility.**

  Move the existing drawing and composable bodies without visual redesign:

  - SpatialLinkStatus.kt: tone/status models, StatusMark, TechnicalValue, InstrumentLabel, FieldAnchor, FieldNode, and subsystem instrument drawing.
  - SpatialLinkAtmosphere.kt: SpatialAtmosphere.
  - SpatialLinkSurfaces.kt: SpatialSurface, SpatialFrame, SpatialCommand, ActionArrow, SystemTile, and SystemCluster.
  - SpatialLinkField.kt: SpatialField, SpatialRing, TrustSeal, field drawing helpers, node helpers, and entry glyph.
  - SpatialLinkNavigation.kt: retain the frozen visual rail but replace the diagnostics-owned SpatialDestination parameter with items: List<SpatialNavigationItem>, selectedKey: String, and onSelected: (String) -> Unit. SpatialNavigationItem contains only a key, label, and visual glyph enum.

  Update diagnostics imports and keep its page-specific SpatialDestination mapping outside the design system. Do not add state, navigation decisions, or feature imports to the neutral module.

- [ ] **Step 6: Compile both consumers and run the boundary check.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :core:designsystem:compileDebugKotlin :feature:diagnostics:compileDebugKotlin --console=plain }
  .\tools\verify-p2-boundaries.ps1
  ~~~

  Expected result: both Kotlin compilation tasks pass, the boundary script passes, and no design-system visual output or diagnostics test expectation changes are required.

## Task 2: Implement and test the exact protocol and session-token domain

**Files:**

- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoveryProtocol.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoverySessionId.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoveryFrameCodec.kt
- Create: core/discovery/src/test/kotlin/com/r2h/spatiallink/discovery/DiscoverySessionIdTest.kt
- Create: core/discovery/src/test/kotlin/com/r2h/spatiallink/discovery/DiscoveryFrameCodecTest.kt
- Create: core/discovery/src/test/kotlin/com/r2h/spatiallink/discovery/DiscoveryProtocolTest.kt

**Interfaces:**

~~~kotlin
object DiscoveryProtocol {
    const val SERVICE_UUID: String = "7F83C58D-4C4B-4E97-9B7D-9C06D3A47B91"
    const val V1_VERSION: Byte = 0x01
    const val PAYLOAD_SIZE: Int = 9
    const val SESSION_ID_SIZE: Int = 8
    const val SESSION_DURATION_MS: Long = 30_000L
    const val PEER_TTL_MS: Long = 6_000L
    const val ACTIVE_TICK_MS: Long = 1_000L
    const val MAX_PEERS: Int = 64
    const val MAX_GENERATION_ATTEMPTS: Int = 8
}

class DiscoverySessionId private constructor(private val bytes: ByteArray) {
    fun toByteArray(): ByteArray
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    companion object {
        fun fromBytes(bytes: ByteArray): DiscoverySessionId?
    }
}

sealed interface SessionIdGenerationResult {
    data class Success(val value: DiscoverySessionId) : SessionIdGenerationResult
    data object Failed : SessionIdGenerationResult
}

fun interface RandomByteSource {
    fun fill(target: ByteArray)
}

class SecureRandomDiscoverySessionIdSource(
    random: RandomByteSource? = null,
    private val maxAttempts: Int = DiscoveryProtocol.MAX_GENERATION_ATTEMPTS,
) : DiscoverySessionIdSource {
    private val secureRandom = java.security.SecureRandom()
    private val randomSource = random ?: RandomByteSource { target ->
        secureRandom.nextBytes(target)
    }

    init {
        require(maxAttempts > 0)
    }

    override fun next(): SessionIdGenerationResult {
        repeat(maxAttempts) {
            val bytes = ByteArray(DiscoveryProtocol.SESSION_ID_SIZE)
            randomSource.fill(bytes)
            DiscoverySessionId.fromBytes(bytes)?.let {
                return SessionIdGenerationResult.Success(it)
            }
        }
        return SessionIdGenerationResult.Failed
    }
}

interface DiscoverySessionIdSource {
    fun next(): SessionIdGenerationResult
}

interface DiscoveryFrameCodec {
    fun encode(sessionId: DiscoverySessionId): ByteArray
    fun decode(payload: ByteArray): DiscoverySessionId?
}

object V1DiscoveryFrameCodec : DiscoveryFrameCodec
~~~

The source keeps one SecureRandom instance in production rather than constructing one per session; the RandomByteSource seam exists only to make zero-draw cases deterministic. DiscoveryFrameCodec receives payload bytes only. It has no UUID parameter and performs no transport-metadata validation.

- [ ] **Step 1: Write the token-generation tests first.**

  DiscoverySessionIdTest must assert defensive copies, content equality, eight-byte length, construction rejection for zero bytes, and bounded generation:

  ~~~kotlin
  @Test
  fun zero_then_non_zero_returns_a_valid_id() {
      val source = scriptedRandom(zeroBytes, nonZeroBytes)
      val result = SecureRandomDiscoverySessionIdSource(source).next()
      assertTrue(result is SessionIdGenerationResult.Success)
  }

  @Test
  fun eight_zero_draws_return_typed_generation_failure() {
      val source = scriptedRandom(repeatByteArray(8, 0))
      assertEquals(
          SessionIdGenerationResult.Failed,
          SecureRandomDiscoverySessionIdSource(source).next(),
      )
  }
  ~~~

  The scripted source records draw count; the second test asserts exactly eight draws and no ninth attempt.

- [ ] **Step 2: Run the focused test to verify it fails for missing domain types.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :core:discovery:test --tests '*DiscoverySessionIdTest' --console=plain }
  ~~~

  Expected result: failure because the new source and value object do not yet exist. Do not skip the test or replace it with a non-deterministic random test.

- [ ] **Step 3: Implement the immutable token and bounded source.**

  Make the constructor private, copy input bytes on construction and access, compare with contentEquals, and compute contentHashCode. Reject any length other than eight and any all-zero value. The generator allocates an eight-byte array per bounded draw, calls the injected source, returns the first non-zero value, and returns SessionIdGenerationResult.Failed after MAX_GENERATION_ATTEMPTS zero draws. Validate maxAttempts > 0 at construction so a faulty source cannot create an infinite loop or a zero-attempt success.

- [ ] **Step 4: Write the exact payload codec tests.**

  DiscoveryFrameCodecTest must assert:

  - encode returns exactly nine bytes;
  - byte zero is 0x01;
  - bytes 1..8 equal the token in most-significant-byte-first order;
  - decode accepts one valid frame;
  - wrong length, wrong version, all-zero token, and malformed input return null without throwing; and
  - UUID mismatch is not a codec test and is not represented in the codec signature.

- [ ] **Step 5: Implement the codec and protocol constants, then run the focused tests.**

  Encode by allocating a nine-byte array, writing V1_VERSION at index zero, and copying defensive token bytes into indices 1 through 8. Decode only after checking exact length and version, then call DiscoverySessionId.fromBytes(payload.copyOfRange(1, 9)). Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :core:discovery:test --tests '*DiscoverySessionIdTest' --tests '*DiscoveryFrameCodecTest' --tests '*DiscoveryProtocolTest' --console=plain }
  ~~~

  Expected result: all token and payload tests pass, and no Android task or Android framework dependency appears in the core test.

## Task 3: Add pure permission policy, peer cache, and lifecycle state machine

**Files:**

- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoveryPermissionPolicy.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoveryObservation.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/NearbyPeer.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/PeerCache.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoveryState.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoveryPorts.kt
- Create: core/discovery/src/main/kotlin/com/r2h/spatiallink/discovery/DiscoveryController.kt
- Create: core/discovery/src/test/kotlin/com/r2h/spatiallink/discovery/DiscoveryPermissionPolicyTest.kt
- Create: core/discovery/src/test/kotlin/com/r2h/spatiallink/discovery/PeerCacheTest.kt
- Create: core/discovery/src/test/kotlin/com/r2h/spatiallink/discovery/DiscoveryControllerTest.kt
- Create: core/discovery/src/test/kotlin/com/r2h/spatiallink/discovery/DiscoveryTestFakes.kt

**Interfaces:**

~~~kotlin
enum class DiscoveryOperation { SCAN, ADVERTISE, ENABLE_BLUETOOTH }

enum class DiscoveryPermission {
    LEGACY_FINE_LOCATION,
    BLUETOOTH_SCAN,
    BLUETOOTH_ADVERTISE,
    BLUETOOTH_CONNECT,
}

object DiscoveryPermissionPolicy {
    fun required(sdkInt: Int, operation: DiscoveryOperation): Set<DiscoveryPermission>
    fun requiredForDiscoverySession(sdkInt: Int): Set<DiscoveryPermission>
}

data class DiscoveryObservation(
    val sessionId: DiscoverySessionId,
    val rssiDbm: Int,
    val receivedAtElapsedMs: Long,
)

enum class PeerAuthenticity { UNVERIFIED }

data class NearbyPeer(
    val sessionId: DiscoverySessionId,
    val rssiDbm: Int,
    val lastSeenElapsedMs: Long,
    val authenticity: PeerAuthenticity = PeerAuthenticity.UNVERIFIED,
)

interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

enum class BluetoothState { UNSUPPORTED, DISABLED, ENABLED }

interface BluetoothStateReader {
    fun read(): BluetoothState
}

interface DiscoveryPermissionReader {
    fun missing(operation: DiscoveryOperation): Set<DiscoveryPermission>
}

fun interface DiscoveryControllerFactory {
    fun create(): DiscoveryController
}

interface BleOperationHandle {
    suspend fun stop()
}

interface BleScanner {
    suspend fun start(
        onObservation: (DiscoveryObservation) -> Unit,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle
}

interface BleAdvertiser {
    suspend fun start(
        payload: ByteArray,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle
}
~~~

DiscoveryFailureCode includes SESSION_ID_GENERATION_FAILED, SCAN_START_FAILED, ADVERTISE_START_FAILED, DATA_TOO_LARGE, CALLBACK_FAILED, and UNEXPECTED_PLATFORM_FAILURE. DiscoveryState.Active exposes only remaining time and a bounded anonymous peer list; the UI mapper may derive presence strength from RSSI evidence but never meters or direction.

- [ ] **Step 1: Write the permission-policy matrix tests first.**

  DiscoveryPermissionPolicyTest covers APIs 29, 30, 31, 33, 34, 36, and 37:

  ~~~kotlin
  @Test
  fun api_31_already_enabled_discovery_requests_scan_and_advertise_only() {
      assertEquals(
          setOf(
              DiscoveryPermission.BLUETOOTH_SCAN,
              DiscoveryPermission.BLUETOOTH_ADVERTISE,
          ),
          DiscoveryPermissionPolicy.requiredForDiscoverySession(31),
      )
  }

  @Test
  fun api_31_bluetooth_enable_consent_is_the_connect_boundary() {
      assertEquals(
          setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
          DiscoveryPermissionPolicy.required(31, DiscoveryOperation.ENABLE_BLUETOOTH),
      )
  }

  @Test
  fun api_29_scan_uses_fine_location_and_advertise_has_no_modern_runtime_permission() {
      assertEquals(
          setOf(DiscoveryPermission.LEGACY_FINE_LOCATION),
          DiscoveryPermissionPolicy.required(29, DiscoveryOperation.SCAN),
      )
      assertTrue(
          DiscoveryPermissionPolicy.required(29, DiscoveryOperation.ADVERTISE).isEmpty(),
      )
  }
  ~~~

  Add assertions that no operation returns NEARBY_WIFI_DEVICES, ACCESS_LOCAL_NETWORK, or RANGING, and that API 31+ discovery never includes CONNECT merely from the Bluetooth permission family.

- [ ] **Step 2: Run the permission tests before implementing the policy.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :core:discovery:test --tests '*DiscoveryPermissionPolicyTest' --console=plain }
  ~~~

  Expected result: failure due to missing policy types. The test must not be changed to make a blanket CONNECT set pass.

- [ ] **Step 3: Implement operation-aware permission mapping.**

  Return fine location only for API 29–30 scan, scan permission only for API 31+ scan, advertise permission only for API 31+ advertise, and CONNECT only for API 31+ ENABLE_BLUETOOTH. Implement requiredForDiscoverySession as the union of scan and advertise only. Keep DiscoveryPermission separate from Android manifest strings.

- [ ] **Step 4: Write cache tests for deduplication, TTL, self-token, and capacity.**

  Use a fake monotonic clock and deterministic token factory. Assert repeated token observations update one entry, the local session token is ignored, an observation at exactly six seconds is expired according to the chosen half-open TTL rule, stale items are removed before capacity enforcement, the count never exceeds 64, and equal timestamps use lexicographic token bytes as the deterministic tie-breaker. Assert no type exposes a distance, azimuth, bearing, or orientation field.

- [ ] **Step 5: Implement the bounded RAM-only peer cache.**

  Store entries in a private token-keyed map. On every insert and snapshot, evict entries where now - lastSeenElapsedMs >= DiscoveryProtocol.PEER_TTL_MS, ignore the local session token, update RSSI/time for an existing token, then remove deterministic oldest entries until the size is 64. Return immutable snapshots sorted by newest observation and token tie-breaker; never persist or log entries.

- [ ] **Step 6: Write controller lifecycle and rollback tests.**

  DiscoveryControllerTest covers:

  - missing scan/advertise permissions -> AWAITING_PERMISSION with only those missing values;
  - disabled adapter -> BLUETOOTH_DISABLED without calling either BLE adapter;
  - unsupported adapter -> UNAVAILABLE, not ERROR;
  - successful scanner and advertiser startup -> ACTIVE with one session token;
  - scanner startup failure -> advertiser is never started;
  - advertiser startup failure -> scanner handle stops exactly once;
  - callback failure -> both sides stop exactly once and typed ERROR is published;
  - timeout after 30 seconds -> both sides stop exactly once and COMPLETED is published;
  - cancellation and close -> cleanup occurs, cancellation is not converted to product error; and
  - a generation failure -> typed SESSION_ID_GENERATION_FAILED and no adapter start;
  - ACTIVE -> SCAN permission loss -> both sides stop exactly once, peers and token are cleared, and AWAITING_PERMISSION is published;
  - ACTIVE -> ADVERTISE permission loss -> both sides stop exactly once, peers and token are cleared, and AWAITING_PERMISSION is published; and
  - ACTIVE -> Bluetooth disabled -> both sides stop exactly once, peers and token are cleared, and BLUETOOTH_DISABLED is published.

  Fakes record start/stop counts and payloads without Android classes.

- [ ] **Step 7: Implement the state machine and single cleanup coordinator.**

  The controller checks permissions, then Bluetooth state, then generates and encodes the session token. It starts the scanner first and advertiser second. It publishes ACTIVE only after both handles exist. A single cleanup coordinator owns all exit paths and uses an atomic/idempotent guard so each handle is stopped once; cleanup clears the cache and discards the session token before publishing a non-active state. CancellationException is rethrown after cleanup; platform exceptions map to typed failures. While ACTIVE, a single delay(DiscoveryProtocol.ACTIVE_TICK_MS) loop rechecks the permission reader for SCAN and ADVERTISE and the BluetoothStateReader; any loss routes through that same cleanup coordinator. The loop also advances the 30-second session and six-second cache expiry using injected monotonic time or a test scheduler, never wall time, and never busy-polls.

- [ ] **Step 8: Run all pure discovery tests.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :core:discovery:test --console=plain }
  ~~~

  Expected result: all protocol, token, permission, cache, lifecycle, rollback, and cancellation tests pass with no Android dependency.

## Task 4: Implement the typed Android BLE boundary and legacy budget gate

**Files:**

- Create: connectivity/ble/src/main/kotlin/com/r2h/spatiallink/ble/BleAndroidConstants.kt
- Create: connectivity/ble/src/main/kotlin/com/r2h/spatiallink/ble/AndroidBluetoothStateReader.kt
- Create: connectivity/ble/src/main/kotlin/com/r2h/spatiallink/ble/AndroidDiscoveryPermissionReader.kt
- Create: connectivity/ble/src/main/kotlin/com/r2h/spatiallink/ble/AndroidBleScanner.kt
- Create: connectivity/ble/src/main/kotlin/com/r2h/spatiallink/ble/AndroidBleAdvertiser.kt
- Create: connectivity/ble/src/main/kotlin/com/r2h/spatiallink/ble/LegacyAdvertisementSpec.kt
- Create: connectivity/ble/src/main/kotlin/com/r2h/spatiallink/ble/AndroidBleDiscoveryFactory.kt
- Create: connectivity/ble/src/test/kotlin/com/r2h/spatiallink/ble/LegacyAdvertisementBudgetTest.kt
- Create: connectivity/ble/src/test/kotlin/com/r2h/spatiallink/ble/AndroidDiscoveryPermissionReaderTest.kt
- Create: connectivity/ble/src/test/kotlin/com/r2h/spatiallink/ble/AndroidBleScannerTest.kt
- Create: connectivity/ble/src/test/kotlin/com/r2h/spatiallink/ble/AndroidBleAdvertiserTest.kt
- Create: connectivity/ble/src/test/kotlin/com/r2h/spatiallink/ble/BlePrivacyBoundaryTest.kt

**Interfaces:**

~~~kotlin
data class LegacyAdvertisementSpec(
    val serviceUuid: java.util.UUID,
    val serviceData: ByteArray,
    val includeDeviceName: Boolean,
    val includeTxPowerLevel: Boolean,
    val manufacturerDataEntries: Int,
    val serviceUuidEntries: Int,
    val scanResponsePresent: Boolean,
) {
    val ownedServiceDataBytes: Int
        get() = 1 + 1 + 16 + serviceData.size

    fun fitsLegacyBudget(): Boolean = ownedServiceDataBytes <= 31
}

interface AndroidBleAdapterProvider {
    fun bluetoothAdapter(): android.bluetooth.BluetoothAdapter?
}
~~~

Production adapters receive applicationContext, an injected adapter provider, and an injected MonotonicClock. They use typed BluetoothManager, BluetoothAdapter, BluetoothLeScanner, BluetoothLeAdvertiser, ScanFilter.Builder, ScanSettings.Builder, AdvertiseData.Builder, and AdvertiseSettings.Builder calls. They do not use reflection.

- [ ] **Step 1: Write the legacy budget and exact-field tests first.**

  LegacyAdvertisementBudgetTest asserts:

  ~~~kotlin
  @Test
  fun v1_owned_service_data_is_27_bytes_and_within_the_legacy_bound() {
      val spec = productionSpec(validNineBytePayload)
      assertEquals(27, spec.ownedServiceDataBytes)
      assertTrue(spec.fitsLegacyBudget())
  }
  ~~~

  Also assert the fixed UUID, nine-byte payload, includeDeviceName == false, includeTxPowerLevel == false, zero manufacturer entries, zero service-UUID entries, no scan response, and that an artificially oversized payload fails fitsLegacyBudget(). Do not assert a 30-byte physical packet total. Add a callback mapping test that ADVERTISE_FAILED_DATA_TOO_LARGE becomes DiscoveryFailureCode.DATA_TOO_LARGE and no extended advertiser is selected.

- [ ] **Step 2: Implement the advertisement specification and builder.**

  Convert DiscoveryProtocol.SERVICE_UUID to UUID only inside :connectivity:ble. Require serviceData.size == DiscoveryProtocol.PAYLOAD_SIZE before building. Build exactly one AdvertiseData service-data entry using the nine-byte payload. Set includeDeviceName(false) and includeTxPowerLevel(false); do not call addManufacturerData, addServiceUuid, or supply a scan response. Build legacy settings with ADVERTISE_MODE_LOW_LATENCY and setConnectable(false). Reject a spec whose owned structure exceeds 31 bytes before calling the platform.

- [ ] **Step 3: Write permission-reader and Bluetooth-state tests.**

  Use a fake checkSelfPermission seam. Assert the Android reader translates the pure policy to exactly Manifest.permission.ACCESS_FINE_LOCATION on API 29–30 scan, BLUETOOTH_SCAN on API 31+ scan, BLUETOOTH_ADVERTISE on API 31+ advertise, and BLUETOOTH_CONNECT only for the explicit enable operation. Verify the existing app manifest entries remain sufficient and do not add a foreground-service or new permission declaration. Assert no reader path requests nearby Wi-Fi, local network, ranging, storage, or location on API 31+.

  Assert AndroidBluetoothStateReader uses only typed adapter availability/enabled state. A null adapter or unavailable LE object maps to BluetoothState.UNSUPPORTED; an off adapter maps to DISABLED; an enabled adapter maps to ENABLED. It never enumerates bonded devices or accesses address/name properties.

- [ ] **Step 4: Implement the typed state and permission adapters.**

  Use context.getSystemService(BluetoothManager::class.java)?.adapter. Read isEnabled and obtain scanner/advertiser only in the concrete start boundary. Keep the permission-to-manifest mapping in the Android module. The explicit Bluetooth-enable intent is handled later by the feature effect; the adapter does not call BluetoothAdapter.enable().

- [ ] **Step 5: Write scanner boundary tests.**

  Capture the scan configuration and callback with an injected platform seam. Assert one ScanFilter with the fixed service-data UUID and a one-byte version mask byteArrayOf(0x01)/byteArrayOf(0xFF.toByte()), low-latency settings, zero report delay, and no address/device/name/manufacturer filter. Feed a fake result whose record has valid service data and RSSI, then assert the emitted DiscoveryObservation contains the decoded token, RSSI, and fake elapsed time only.

  Add the source privacy test over AndroidBleScanner.kt and related production BLE sources. It fails if the scanner source contains device/address/name accessors, bond access, toString on a scan result, persistence calls, or identity imports. The test must not permit a helper to hide a device access behind a new name.

- [ ] **Step 6: Implement scanner callback extraction and cleanup.**

  In the callback, call only the scan record service-data accessor for the fixed UUID, read RSSI, obtain SystemClock.elapsedRealtime(), decode through V1DiscoveryFrameCodec, and emit a platform-neutral observation. Do not pass the Android ScanResult beyond the adapter. Retain the exact callback instance and call stopScan once from an idempotent BleOperationHandle.

- [ ] **Step 7: Write advertiser callback and cleanup tests.**

  Assert successful start returns a handle, synchronous platform exceptions become typed failures, onStartFailure(ADVERTISE_FAILED_DATA_TOO_LARGE) becomes DATA_TOO_LARGE, callback failure closes the handle, and repeated stop calls invoke platform stop exactly once. Assert no advertiser starts if the budget gate fails.

- [ ] **Step 8: Implement advertiser callback and cleanup.**

  Keep one callback instance per start and pass it to the matching stopAdvertising call. Route success, failure, cancellation, timeout, permission loss, Bluetooth disablement, owner disposal, and exceptions into the same idempotent stop path. Do not allocate an unbounded executor; if a callback executor is required by the implementation seam, inject one owner-scoped instance and release the handle reference at stop.

- [ ] **Step 9: Run the adapter-focused test suite.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :connectivity:ble:test --console=plain }
  .\tools\verify-p2-boundaries.ps1
  ~~~

  Expected result: exact-field, budget, permission, scanner, advertiser, callback, cleanup, and privacy tests pass; the boundary script reports no forbidden source access.

## Task 5: Integrate the controller with the nearby ViewModel and app composition root

**Files:**

- Create: feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyUiState.kt
- Create: feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyEffect.kt
- Create: feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyViewModel.kt
- Create: feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyViewModelFactory.kt
- Modify: app/src/main/java/com/r2h/spatiallink/SpatialLinkApplication.kt
- Modify: app/src/main/java/com/r2h/spatiallink/MainActivity.kt
- Create: app/src/main/java/com/r2h/spatiallink/SpatialLinkRoot.kt
- Modify: feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsScreen.kt
- Modify: feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkShell.kt
- Create: feature/nearby/src/test/kotlin/com/r2h/spatiallink/nearby/NearbyViewModelTest.kt
- Create: feature/nearby/src/test/kotlin/com/r2h/spatiallink/nearby/NearbyControllerOwnershipTest.kt

**Interfaces:**

~~~kotlin
sealed interface NearbyEffect {
    data class RequestRuntimePermissions(
        val permissions: Set<DiscoveryPermission>,
    ) : NearbyEffect
    data object RequestBluetoothEnable : NearbyEffect
    data object LeaveCompleted : NearbyEffect
}

class NearbyViewModel(
    private val controller: DiscoveryController,
    private val permissionReader: DiscoveryPermissionReader,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    val uiState: StateFlow<NearbyUiState>
    val effects: Flow<NearbyEffect>
    fun openField()
    fun stopField()
    fun requestClose()
    fun onForegroundLost()
    fun onRuntimePermissionsResult()
    fun onBluetoothEnableResult()
}
~~~

NearbyUiState mirrors the typed controller state and maps active peers to anonymous count/strength only. It contains no raw token, Android object, address, name, device identifier, or identity material.

- [ ] **Step 1: Write ViewModel tests for user-intent timing and permission effects.**

  NearbyViewModelTest asserts the initial state is idle and emits no effect; calling openField with missing scan/advertise permissions emits only RequestRuntimePermissions for those permissions; an already-enabled API 31 session never includes CONNECT; a disabled adapter emits CONNECT permission only when the explicit enable boundary requires it, followed by RequestBluetoothEnable; permission denial returns a typed waiting state; a successful fake controller state is exposed through StateFlow; and requestClose emits LeaveCompleted only after controller cleanup completes.

- [ ] **Step 2: Run the focused ViewModel test to verify it fails.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :feature:nearby:test --tests '*NearbyViewModelTest' --console=plain }
  ~~~

  Expected result: failure because the nearby feature and ViewModel do not yet exist.

- [ ] **Step 3: Implement the ViewModel and effect coordinator.**

  Collect controller state in the ViewModel scope using the injected dispatcher. openField is the only entry point that can initiate a permission effect. It first evaluates the union of scan and advertise requirements; after a permission result it retries. If Bluetooth is disabled, it evaluates ENABLE_BLUETOOTH separately and emits CONNECT only for that boundary before emitting the system-enable effect. onBluetoothEnableResult retries only after a successful system result. requestClose() and onForegroundLost() suspend until the controller's single cleanup path completes, then expose the non-active state; requestClose() emits LeaveCompleted after cleanup and onForegroundLost() never emits a navigation effect. Call controller.close from onCleared. Do not restart on lifecycle start.

- [ ] **Step 4: Add the concrete nearby controller to AppContainer.**

  Construct AndroidBluetoothStateReader, AndroidDiscoveryPermissionReader, AndroidBleScanner, and AndroidBleAdvertiser once from application context and the existing dispatcher provider, then expose a DiscoveryControllerFactory that constructs a new owner-scoped DiscoveryController on every create() call. Pass applicationContext, not an Activity context, into adapters. Keep the existing capability, permission-inspection, and identity repositories unchanged. AppContainer must not retain a reusable DiscoveryController.

  NearbyControllerOwnershipTest creates two feature owners through the factory, closes the first owner's controller, then asserts the second controller is a distinct usable instance that can start a session. The first controller is never reused after close; its token, cache, timer, handles, and cleanup coordinator are not shared with the second owner.

- [ ] **Step 5: Move top-level feature selection to the app host without feature coupling.**

  Add SpatialLinkRoot as the composition host. It owns a saveable showNearby flag, renders DiagnosticsRoute with an onOpenNearby callback, and renders NearbyRoute when the callback is invoked. MainActivity creates the nearby ViewModel from the factory so it is owner-scoped. DiagnosticsRoute and SpatialLinkShell pass the callback through; they do not import nearby types. app imports both feature packages, and nearby never imports diagnostics. The root sets showNearby = false only after NearbyRoute reports LeaveCompleted; it never hides the route before scanner/advertiser cleanup has finished.

- [ ] **Step 6: Implement explicit Activity-result launchers in the nearby route boundary.**

  Use rememberLauncherForActivityResult(RequestMultiplePermissions()) for the exact manifest names mapped from DiscoveryPermission, and StartActivityForResult for Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE). Launchers are collected from NearbyEffect; they are not invoked during composition, Activity creation, or ViewModel initialization. Forward results to onRuntimePermissionsResult and onBluetoothEnableResult. Install one lifecycle observer at the route boundary: ON_STOP calls onForegroundLost() and ON_START is observational only. Add active-session lifecycle tests proving ON_STOP stops scanner and advertiser once, clears peers/token, publishes a non-active state, and does not auto-restart on ON_START. Back and the visible close affordance call requestClose() through this same route boundary.

- [ ] **Step 7: Run integration unit tests and compile the app.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :core:discovery:test :connectivity:ble:test :feature:nearby:test :feature:diagnostics:test :app:compileDebugKotlin --console=plain }
  ~~~

  Expected result: all pure and feature tests pass, diagnostics tests remain green, and the app compiles with both features present and no feature-to-feature project dependency.

## Task 6: Build the anonymous nearby surface and Compose regressions

**Files:**

- Create: feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyScreen.kt
- Create: feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyPresentation.kt
- Create: feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyFieldStateMapper.kt
- Modify: app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt
- Create: feature/nearby/src/androidTest/java/com/r2h/spatiallink/nearby/NearbyInstrumentedTest.kt
- Modify: feature/diagnostics/src/test/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPresentationTest.kt

**Interfaces:**

~~~kotlin
@Composable
fun NearbyRoute(
    viewModel: NearbyViewModel,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun NearbyScreen(
    state: NearbyUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestClose: () -> Unit,
    modifier: Modifier = Modifier,
)
~~~

The screen uses only neutral design-system primitives: the existing SpatialAtmosphere, SpatialField, status marks, framing, and technical labels. The active field renders symmetric anonymous presence nodes. It uses product language such as NEARBY FIELD, UNVERIFIED PRESENCE, and END FIELD; it never renders a session ID or Android metadata.

- [ ] **Step 1: Write pure presentation-mapping tests.**

  Assert every DiscoveryState maps to a stable semantic label and tone: IDLE, UNAVAILABLE, AWAITING_PERMISSION, BLUETOOTH_DISABLED, STARTING, ACTIVE, STOPPING, COMPLETED, and ERROR. For active state, assert peer count is bounded and only anonymous presence strength is exposed. Assert no mapped UI model contains a token, address, name, distance, or direction field.

- [ ] **Step 2: Implement the nearby screen using shared primitives.**

  Keep the Field dominant and neutral. Use SpatialField as a visual instrument, add a bounded presence-count/status readout, and use the existing precision-cut command framing for start/stop. The route's BackHandler and close affordance call onRequestClose; the host's onClosed callback is invoked only after the ViewModel emits LeaveCompleted. No navigation action can hide the route while scanner or advertiser handles remain active. All state transitions remain in the ViewModel/controller.

- [ ] **Step 3: Add Compose semantics that make the acceptance gate inspectable.**

  Provide content descriptions for the nearby field, current state, anonymous presence count, start/stop command, and close command. Do not encode raw peer tokens in content descriptions. Ensure inactive/unsupported states remain readable without a permission dialog.

- [ ] **Step 4: Add nearby Compose tests.**

  NearbyInstrumentedTest verifies idle rendering, unavailable rendering, active anonymous presence rendering, stop control, and absence of raw identity/technical device labels. It uses a fake ViewModel/state where platform interaction is not the subject of the test. Keep production behavior real in the app path.

- [ ] **Step 5: Update existing app instrumentation without weakening it.**

  Preserve the existing Overview, Identity, system diagnostics, typography, geometry, recreation, and privacy assertions. Change only the old test that assumed OPEN FIELD opened the capability-only Field page: route that assertion through the FIELD diagnostic destination, and add a separate test that OPEN FIELD reaches the nearby surface or its typed permission/disabled state without auto-requesting at app launch. Do not delete the existing long-permission or recreation tests.

- [ ] **Step 6: Run Compose and diagnostics regressions.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :feature:nearby:test :feature:diagnostics:test :app:compileDebugAndroidTestKotlin --console=plain }
  ~~~

  Expected result: all presentation and diagnostics unit tests pass, Android test sources compile, and the frozen visual surface remains unchanged outside the new nearby route.

## Task 7: Complete static architecture/privacy checks and update project documentation

**Files:**

- Modify: tools/verify-p2-boundaries.ps1
- Modify: README.md
- Modify: docs/architecture/FOUNDATION.md
- Modify: design-qa.md

**Interfaces:**

- The boundary script exits nonzero on any violation and prints one finding per line; exit code zero is required before final verification.
- Documentation names :core:designsystem, :core:discovery, :connectivity:ble, and :feature:nearby, records the least-privilege permission table, and preserves P0/P1 status.
- design-qa.md receives only actual P2 test/device evidence after commands run; no predicted pass counts are written.

- [ ] **Step 1: Expand the static source checks.**

  Check only production source/build roots and fail on:

  ~~~text
  androidx.core.uwb
  java.lang.reflect
  kotlin.reflect
  diagnostics in feature/nearby/build.gradle.kts
  nearby in feature/diagnostics/build.gradle.kts
  :core:identity in core/discovery or connectivity/ble
  BluetoothDevice
  device/address/name accessors
  bonded-device access
  ScanResult.toString
  SharedPreferences
  Room
  Retrofit
  OkHttp
  Ktor
  Firebase
  analytics
  telemetry
  createRangingSession
  BluetoothGatt
  NFC pairing, transfer, and networking implementation markers
  manufacturer/model capability checks
  ~~~

  The script allows the fixed UUID, protocol constants, Android ScanRecord service-data access, and typed adapter classes. It does not scan documentation text as production source.

- [ ] **Step 2: Update README and foundation architecture docs.**

  Add the P2 module graph and explain that :core:discovery is pure Kotlin, :connectivity:ble is the Android boundary, nearby and diagnostics share only :core:designsystem, and P2 has no identity dependency. Record the exact v1 frame, 27-byte owned structure/<=31-byte legacy gate, operation-specific permissions, foreground-only lifecycle, six-second TTL, 64-peer bound, and two-device qualification rule. Keep P0/P1 statements intact and do not claim P2 completion before final evidence.

- [ ] **Step 3: Add an evidence template to design-qa.md.**

  Add a dated P2 — Privacy-Preserving BLE Peer Discovery section with fields for build/test commands, package installation, permission timing, adapter state, active session, peer observations, logcat exceptions, source scans, one-device result, and two-device result. Leave values blank or mark them NOT RUN until actual commands produce evidence; do not invent counts.

- [ ] **Step 4: Run static checks and documentation-only validation.**

  Run:

  ~~~powershell
  .\tools\verify-p2-boundaries.ps1
  rg -n 'feature:nearby|core:designsystem|31 bytes|SESSION_ID_GENERATION_FAILED|BLUETOOTH_CONNECT|BluetoothDevice|two-device' README.md docs/architecture/FOUNDATION.md design-qa.md
  ~~~

  Expected result: the boundary script passes; documentation contains the approved decisions; no P2 completion status is claimed without runtime evidence.

## Task 8: Execute full verification and physical-device qualification

**Files:**

- Modify: design-qa.md with actual command output and evidence paths only
- Create only evidence artifacts under existing build/report/temp locations; do not add generated artifacts to source

**Interfaces:**

- Verification status is based on command output, test XML, bounded logcat, package state, and physical-device observations.
- One device can qualify installation, launch, permission timing, cleanup, and graceful absence; it cannot close mutual two-device discovery. Two-device qualification requires two authorized intended physical devices and explicit serial-scoped commands.
- P2_BLE_DISCOVERY_COMPLETE requires two real physical devices to observe each other in both directions. With all code/host/one-device gates passing but no pair, record exactly P2_CODE_COMPLETE and TWO_DEVICE_QUALIFICATION_PENDING.

- [ ] **Step 1: Confirm the intended physical device without creating an emulator.**

  Run:

  ~~~powershell
  adb devices -l
  adb shell getprop ro.build.version.release
  adb shell getprop ro.build.version.sdk
  adb shell getprop ro.product.cpu.abi
  adb shell pm list features
  ~~~

  Accept exactly one or exactly two intended physical devices in state device; reject unauthorized/offline devices, emulators, and any count greater than two. Identify the intended devices explicitly, use adb -s <serial> for every device-specific command, and record Android version, API, ABI, and model label per device without using serials as application data or identity. Inspect relevant Bluetooth feature declarations per device. Do not create an emulator/AVD. If one device is present, follow the one-device branch; if two are present, follow the two-device branch.

- [ ] **Step 2: Run a fresh host verification before installation.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat clean --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat test --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat lint --console=plain }
  ~~~

  Inspect generated reports immediately. Do not proceed to the device if a host gate fails.

- [ ] **Step 3: Install only the current SpatialLink debug package.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:installDebug --console=plain }
  adb shell pm list packages | findstr com.r2h.spatiallink
  adb shell monkey -p com.r2h.spatiallink -c android.intent.category.LAUNCHER 1
  ~~~

  Record that only com.r2h.spatiallink and its test package were touched. Do not uninstall unrelated packages or access personal content.

  Before the first launch or any just-in-time permission request, establish the SpatialLink-only permission baseline with adb -s <serial> shell dumpsys package com.r2h.spatiallink. Record the current grant state for BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT, and API 29–30 ACCESS_FINE_LOCATION as applicable. Do not infer no-startup-dialog evidence from an already-granted P2 permission set. If a denied/not-granted baseline is required, uninstall/reinstall only com.r2h.spatiallink (and its instrumentation package if required) and record the resulting package state; never use pm grant as the sole user-facing permission-flow evidence and never alter unrelated packages or security settings.

- [ ] **Step 4: Verify first-launch permission timing and runtime behavior.**

  Clear only logcat with adb -s <serial> logcat -c, launch the app on every intended device, and inspect each physical screen. Confirm no app-triggered startup dialog requests scan, advertise, connect, nearby Wi-Fi, ranging, local network, or location on API 31+ (or unrelated permissions on API 29–30). Then tap OPEN FIELD as explicit user intent and record the resulting typed permission/disabled/active state per device. Distinguish Android compatibility messages from app permission requests. For two devices, do not count a permission state as an absence-of-dialog result unless the recorded baseline was denied/not granted.

- [ ] **Step 5: Audit bounded runtime logs.**

  Capture bounded application logcat and search:

  ~~~powershell
  adb logcat -d -v threadtime > "$env:TEMP\spatiallink-p2-logcat.txt"
  Select-String -Path "$env:TEMP\spatiallink-p2-logcat.txt" -Pattern 'FATAL EXCEPTION|AndroidRuntime|VerifyError|NoClassDefFoundError|ClassNotFoundException|SecurityException|IllegalStateException|BluetoothLeScanner|BluetoothLeAdvertiser|BluetoothDevice|BluetoothGatt'
  ~~~

  Verify no SpatialLink-caused fatal, class-loading, security, device-access, GATT, or lifecycle failure. Separate unrelated vendor logs and record exact findings.

- [ ] **Step 6: Run connected instrumentation suites and inspect XML.**

  Run:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :feature:nearby:connectedDebugAndroidTest :core:identity:connectedDebugAndroidTest --console=plain }
  ~~~

  Inspect each generated TEST-*.xml and the Gradle output for app, nearby, and identity. Report executed, passed, failed, skipped, and errors for every suite. The plan intentionally omits a module-level connectivity connected task because that module has only JVM tests; Android BLE integration is exercised through the app and nearby connected tests. A green task with no executed test cases is not sufficient. If a test fails, diagnose the root cause, preserve or add the regression assertion, make the minimum fix, rerun the focused test, then rerun the complete connected suite.

- [ ] **Step 7: Complete physical boundary checks.**

  Exercise stop, active-session Back/Close, Activity/app ON_STOP foreground loss, permission denial, Bluetooth-off/unsupported behavior where safely possible, and a 30-second timeout. Confirm scanner/advertiser cleanup happens once, ON_STOP does not restart on return, no raw token/device information appears in UI or logs, no GATT/connection/ranging/transfer/NFC/network behavior occurs, and no personal-data permission or content access occurs. In the one-device branch, record anonymous presence only and do not infer mutual discovery. In the two-device branch, install the same current build on both devices, start Field explicitly on both, verify A observes B and B observes A, verify both remain UNVERIFIED, stop both and verify peer cleanup, restart both, prove fresh per-session token generation through test-only evidence, and verify that no SpatialDeviceId/public key/name/address/model or directional/distance claim is exposed. Raw tokens remain test-only evidence and never appear in production UI or logs.

  The one-device branch is eligible for P2_CODE_COMPLETE plus TWO_DEVICE_QUALIFICATION_PENDING after all code, host, installation, launch, permission-baseline, lifecycle, connected, runtime, and privacy gates pass. The two-device branch may advance to P2_BLE_DISCOVERY_COMPLETE only after mutual observation and the restart/cleanup/privacy checks above pass on both devices.

- [ ] **Step 8: Perform a fresh final host and connected suite after all fixes.**

  Run the full final sequence with no overlapping Gradle processes:

  ~~~powershell
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat clean --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat test --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat lint --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :feature:nearby:connectedDebugAndroidTest --console=plain }
  & { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :core:identity:connectedDebugAndroidTest --console=plain }
  .\tools\verify-p2-boundaries.ps1
  ~~~

  Every applicable command must pass. Update design-qa.md from actual output, including test XML paths and device result. Do not mark P2 complete because API 36/37 execution is unavailable; those versions are not required for software closure. If only one device was qualified, use P2_CODE_COMPLETE plus TWO_DEVICE_QUALIFICATION_PENDING; use P2_BLE_DISCOVERY_COMPLETE only for the two-device branch with verified mutual discovery.

## Plan self-review

### Spec coverage

- Protocol UUID, exact v1 payload, big-endian token, secure random, all-zero construction rejection, bounded retry, RAM-only lifetime, and no identity broadcast: Tasks 2 and 4.
- 27-byte owned service-data structure, <=31 legacy bound, exact fields, data-too-large defect, and no extended fallback: Task 4.
- UUID/filter ownership outside the pure payload codec: Tasks 2 and 4.
- API 29/30/31/33/34/36/37 operation-aware permission mapping and conditional CONNECT: Tasks 3, 4, 5, and 8.
- No ScanResult.device access and static privacy checks: Tasks 4 and 7.
- Six-second TTL, 64-peer capacity, stale eviction, self-token rejection, monotonic time, UNVERIFIED authenticity, no distance/direction: Task 3 and Task 6.
- Exactly-once cleanup, cancellation, callback failures, partial-start rollback, 30-second foreground session: Tasks 3, 4, and 8.
- Neutral :core:designsystem and no feature-to-feature dependency: Task 1 and Task 7.
- Nearby ViewModel/StateFlow, user-initiated permissions, explicit Bluetooth enable flow, and frozen visual language: Tasks 5 and 6.
- JVM, build, lint, connected, source, runtime, package permission baseline, one-device branch, and two-device branch evidence: Task 8.
- The final connected gate restores :core:identity:connectedDebugAndroidTest and reports its TEST XML counts; the connectivity module has no instrumentation suite because its Android BLE proof is through app/nearby instrumentation.
- P1 identity remains separate and P3 behavior is absent: Global Constraints, Tasks 1, 4, 5, and 7.

### Placeholder and contradiction scan

- Every production type referenced by a later task is defined in an earlier task or in the repository's existing source map.
- The generation source returns SessionIdGenerationResult, so bounded failure is representable without an exception loop.
- The pure codec accepts payload bytes only; UUID validation is assigned to the Android BLE filter boundary.
- The byte-budget plan asserts 27 owned bytes and a <=31 bound, never an exact 30-byte physical packet.
- CONNECT is absent from the normal already-enabled discovery set and appears only for ENABLE_BLUETOOTH or a verified concrete connect-gated result.
- Each NearbyViewModel receives a fresh owner-scoped controller from a factory; cleanup completes before LeaveCompleted navigation, ON_STOP stops without auto-restart, and active prerequisite loss uses the same coordinator.
- Physical qualification accepts one or two authorized devices, records a one-device pending outcome, and reserves P2_BLE_DISCOVERY_COMPLETE for explicit two-device mutual observation.
- The package permission baseline is recorded before launch/JIT qualification, and the final P1 connected suite is mandatory.
- No unresolved implementation marker, open decision, or unbounded later step remains in the plan.

### Type and dependency consistency

- DiscoveryPermissionPolicy returns pure DiscoveryPermission values; AndroidDiscoveryPermissionReader maps them to manifest strings.
- DiscoveryController consumes BleScanner, BleAdvertiser, BluetoothStateReader, DiscoveryPermissionReader, MonotonicClock, and DiscoverySessionIdSource; Android adapters implement those ports.
- NearbyViewModel consumes a fresh DiscoveryController produced by DiscoveryControllerFactory and the pure permission reader; Compose consumes NearbyUiState and NearbyEffect only.
- SpatialLinkRoot imports both features; neither feature imports the other; :core:designsystem imports neither feature.
- Final evidence is written only after the corresponding command or device observation exists.

**Plan self-review result: PASS. The implementation plan incorporates the eight approved execution corrections, preserves the approved specification and three written-spec corrections, resolves the connectivity connected-suite choice as option B, and does not authorize production work before the plan gate.**
