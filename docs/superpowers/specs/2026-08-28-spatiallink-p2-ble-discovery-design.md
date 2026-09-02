# SpatialLink P2 Privacy-Preserving BLE Peer Discovery

- Date: 2026-08-28
- Application ID: `com.r2h.spatiallink`
- Status: design approved with mandatory corrections incorporated; written-spec review gate
- Scope: P2 anonymous, foreground-only BLE presence discovery
- Product boundary: discovery is presence, not identity, trust, connection, or transfer

## 1. Decision summary

P2 adds a bounded foreground discovery session. During that session SpatialLink:

1. creates one cryptographically random, eight-byte ephemeral session token;
2. advertises one anonymous legacy BLE service-data frame containing that token;
3. scans only for the SpatialLink service-data UUID and v1 frame marker;
4. retains validated anonymous observations in RAM for six seconds;
5. presents neutral presence in the existing frozen SpatialLink Field language; and
6. stops both platform operations when the user stops the session, the 30-second window ends, Bluetooth or permission state becomes unusable, the owner is disposed, or either operation fails.

P2 does not bind an observation to the P1 cryptographic identity. Every discovered peer remains `UNVERIFIED`.

## 2. Scope and hard boundary

### Included

- pure Kotlin protocol and peer-cache rules in `:core:discovery`;
- Android BLE scanner and advertiser adapters in `:connectivity:ble`;
- foreground discovery orchestration and presentation in `:feature:nearby`;
- least-privilege, operation-aware runtime permission handling;
- legacy advertisement byte-budget enforcement;
- anonymous, symmetric presence visualization using the frozen SpatialLink visual language;
- static privacy and dependency checks, pure domain tests, adapter tests, and connected qualification.

### Excluded

The P2 implementation contains no GATT, connection establishment, pairing, handshake, trust promotion, cryptographic exchange, transfer, networking, cloud service, ranging session, Wi-Fi discovery, NFC pairing, background service, WorkManager job, boot receiver, persistent discovery history, database, raw packet log, device identifier, address/name lookup, or P3 behavior.

The frozen Overview, Identity, P0 capability, P0 permission, and P1 Keystore behavior remain behaviorally unchanged. P2 may route the existing `OPEN FIELD` action to the nearby experience, but it does not redesign the frozen visual foundation or move domain logic into Compose.

## 3. Module and responsibility boundary

The module graph is:

```text
:app
  ├── :feature:diagnostics
  ├── :feature:nearby
  └── :core:identity

:feature:diagnostics ──> :core:designsystem
:feature:nearby      ──> :core:designsystem
:feature:nearby      ──> :core:discovery
:feature:nearby      ──> :connectivity:ble
:connectivity:ble    ──> :core:discovery
:connectivity:ble    ──> :core:common

:core:discovery       ──> pure Kotlin/JVM and coroutine infrastructure only
:core:designsystem    ──> Compose presentation infrastructure only
```

There is no `:feature:nearby -> :feature:diagnostics` dependency. The app-level host owns feature selection and supplies each feature with its own state owner. A feature never imports another feature's state, ViewModel, route implementation, or data source.

### 3.1 `:core:discovery`

`:core:discovery` is pure Kotlin/JVM. It owns the protocol value objects, codec, token generation, observation validation, peer cache, session policy, state machine, and platform-neutral ports. It may use the repository's pure Kotlin coroutine infrastructure for `StateFlow` and suspension, but it has no Android framework or AndroidX dependency and no dependency on `:core:identity`.

### 3.2 `:connectivity:ble`

`:connectivity:ble` is the only P2 module that touches Android Bluetooth APIs. It owns typed API access, `BluetoothManager`/`BluetoothAdapter` access, scanner and advertiser callback bridges, Android `ScanFilter`/`ScanSettings`/`AdvertiseData` construction, elapsed-time sourcing, permission-name mapping, and typed platform failure mapping. Android callback types do not cross into the feature or pure domain.

### 3.3 `:feature:nearby`

`:feature:nearby` owns the P2 ViewModel, `StateFlow` exposure, user-intent handling, permission-launch coordination, and the nearby Field/System presentation. It consumes domain state and platform-neutral ports; it does not detect capabilities, inspect permission APIs, read Bluetooth device properties, or perform identity operations.

### 3.4 `:core:designsystem`

`:core:designsystem` is the neutral home for the already-frozen SpatialLink visual infrastructure. Extraction is responsibility-boundary work only. It may contain the existing SpatialLink colors, typography, spacing, surfaces, framing, Field instrument, status marks, technical labels, and stateless instrumentation primitives that are genuinely reusable by diagnostics and nearby.

It contains no diagnostics state, nearby state, ViewModel, Bluetooth logic, permission logic, capability detection, identity logic, route selection, or business navigation. A stateless visual rail primitive is allowed; destination selection remains with the app-level host.

## 4. Protocol contract

### 4.1 Service identity

The only P2 service-data UUID is:

```text
7F83C58D-4C4B-4E97-9B7D-9C06D3A47B91
```

The UUID is used as the 128-bit service-data key. It is not duplicated as a separate service-UUID advertisement field.

### 4.2 Anonymous v1 frame

The service-data payload is exactly nine bytes:

| Byte offset | Size | Meaning | Required value |
|---:|---:|---|---|
| 0 | 1 | protocol version | `0x01` |
| 1..8 | 8 | `DiscoverySessionId` | unsigned 64-bit token, most-significant byte first |

The frame has no length field, capability bitmap, identity material, certificate, signature, device name, transmit power, manufacturer data, or additional metadata.

### 4.3 Session ID rules

`DiscoverySessionId` is an immutable eight-byte value with defensive-copy access and content-based equality/hash behavior. The production source uses `java.security.SecureRandom`; tests inject a deterministic source. Construction rejects an all-zero value. The production generator makes at most eight SecureRandom draws for one session ID: it retries a zero draw and returns the first non-zero value; eight zero draws produce a typed `SESSION_ID_GENERATION_FAILED` result rather than an unbounded loop. The codec also rejects an all-zero payload. The value is created once for each 30-second discovery session, is never rotated during that session, is held only in RAM, and is never logged, persisted, displayed, used as a SpatialDeviceId, or used for trust.

### 4.4 Legacy advertisement budget

P2 uses the legacy 31-byte advertising budget. The SpatialLink-owned service-data structure is deliberately close to the limit:

| AD structure | Total bytes |
|---|---:|
| Service-data structure: length byte + type byte + 128-bit UUID + 9-byte payload | 27 |
| SpatialLink-owned structure | 27 |

The 27-byte owned structure is composed of one AD length byte, one AD type byte, the 16-byte service-data UUID, and the nine-byte P2 payload. The final legacy advertisement must remain at or below 31 bytes. Android documents a separate three-byte flags contribution for connectable advertising; P2 is explicitly non-connectable, so the design does not assert an exact physical packet total for this configuration.

The advertiser is constructed with exactly one service-data entry, `includeDeviceName = false`, `includeTxPowerLevel = false`, no manufacturer data, no service-UUID entry, no scan response, and no other metadata. The implementation must keep the encoded service-data payload at exactly nine bytes, verify the owned structure is 27 bytes, and verify the final legacy data remains within the 31-byte bound without hard-coding a 30-byte packet-size assertion.

`ADVERTISE_FAILED_DATA_TOO_LARGE` is a P2 defect. It is surfaced as a typed advertising failure, causes the session to roll back, and is covered by an explicit regression test. P2 never silently switches to extended advertising, changes the payload, adds a scan response, or drops fields to work around this failure.

The byte-budget test is a protocol gate, not a best-effort assertion: it checks the fixed UUID and all `AdvertiseData` fields at the BLE boundary, the nine-byte payload, the version byte, the eight-byte token, the absence of every optional field, the 27-byte owned structure, the <=31-byte legacy bound, and the rejection of any oversized construction.

## 5. Privacy boundary

### 5.1 Advertising

The advertisement carries only the fixed protocol UUID, version byte, and random session token. It never carries `SpatialDeviceId`, short identity, public key, certificate, signature, Android ID, IMEI, serial, MAC address, Bluetooth address, device name, alias, user phone name, manufacturer/model, account, email, phone number, username, or capability material.

### 5.2 Scanning

The production scanner consumes only:

- the `ScanRecord` service-data bytes for the fixed UUID;
- the RSSI value; and
- a monotonic receipt time supplied by the platform clock.

The scanner never dereferences or stores the scan result's Bluetooth device object. It does not read an address, name, alias, bond state, or string representation. It does not persist raw packets or observations, and it does not log tokens or scan records.

The static privacy tests enforce this boundary over production P2 source. A passing test requires no `BluetoothDevice` access, address/name accessor, bond lookup, device stringification, identity import, or persistence API in the scanning path.

### 5.3 Meaning of an observation

The BLE boundary selects the fixed service-data UUID and passes only its payload to the pure decoder. The decoder accepts only an exact nine-byte payload, version `0x01`, and non-zero token. Invalid payloads are ignored safely; an unrelated UUID is rejected by the BLE filter/boundary rather than by `DiscoveryFrameCodec`. A valid observation proves only that an anonymous v1 frame was received. Its authenticity is `UNVERIFIED`; it is never promoted to a trusted peer.

P2 does not claim replay resistance. It does not convert RSSI to meters and does not infer direction, azimuth, bearing, orientation, or target identity.

## 6. Pure domain model

The domain model is platform-neutral and deterministic under an injected monotonic clock.

### 6.1 Value objects and ports

The implementation will expose equivalent pure Kotlin contracts with no Android types:

```kotlin
class DiscoverySessionId

sealed interface SessionIdGenerationResult {
    data class Success(val value: DiscoverySessionId) : SessionIdGenerationResult
    data object Failed : SessionIdGenerationResult
}

interface DiscoverySessionIdSource {
    fun next(): SessionIdGenerationResult
}

interface DiscoveryFrameCodec {
    fun encode(sessionId: DiscoverySessionId): ByteArray
    fun decode(payload: ByteArray): DiscoverySessionId?
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
```

The public UI state does not expose raw session tokens. The cache may use the token as its in-memory deduplication key and may expose anonymous counts and neutral presence strengths derived from RSSI evidence only.

### 6.2 Peer cache policy

- TTL: six seconds from the latest monotonic observation.
- Capacity: at most 64 peers.
- Repeated observations update the existing token entry; they do not create duplicates.
- The local session token is rejected before insertion.
- Expired entries are evicted before capacity enforcement.
- If capacity is exceeded, eviction is deterministic: oldest `lastSeenElapsedMs` first, then lexicographically smallest eight-byte token as the tie-breaker.
- Clock values are monotonic elapsed milliseconds. Wall-clock time, calendar time, and timezone are not used.
- Expiry is recalculated whenever observations or the session timer update the state, so a peer cannot remain visible after its six-second TTL.

### 6.3 Discovery state

The controller exposes a typed `StateFlow` with these states:

- `IDLE`: no active session;
- `UNAVAILABLE`: Bluetooth LE hardware or the required legacy operation is absent;
- `AWAITING_PERMISSION`: the active operation lacks a permission and is waiting for user action;
- `BLUETOOTH_DISABLED`: the adapter is off and the user must explicitly choose the system enable flow;
- `STARTING`: permission and adapter prerequisites have passed and platform operations are being registered;
- `ACTIVE`: scanner and advertiser are both running, with remaining session time and anonymous peer state;
- `STOPPING`: cleanup is in progress;
- `COMPLETED`: the bounded session ended normally; and
- `ERROR`: a real platform or protocol defect occurred.

Hardware absence maps to `UNAVAILABLE`, never to `ERROR`. Permission denial and Bluetooth-off state remain typed states. Cancellation and owner disposal clean up and return to the appropriate non-error state rather than manufacturing a product failure.

## 7. Android adapter design

All Android API use is typed and isolated in `:connectivity:ble`. The module compiles against and is tested with the locked API 37 SDK and uses `Build.VERSION.SDK_INT` guards where an API-level symbol or behavior requires them. Reflection is not used.

### 7.1 Bluetooth state and capability probe

The adapter obtains the local adapter through the typed `BluetoothManager` service. It reads only adapter availability and enabled state required to decide whether a session can start. It does not enumerate bonded devices, read addresses, inspect names, or create device identity.

The result is typed as available, disabled, or unsupported. A missing adapter, missing LE scanner, missing legacy advertiser, or unsupported legacy advertising operation returns `UNAVAILABLE` with a stable reason. No startup permission request is hidden in the probe.

### 7.2 Advertiser

The advertiser uses `BluetoothLeAdvertiser.startAdvertising` with:

- `AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY`;
- `setConnectable(false)`;
- the exact nine-byte service-data payload under the fixed 128-bit UUID;
- `includeDeviceName = false`;
- `includeTxPowerLevel = false`;
- no manufacturer data;
- no duplicate service UUID; and
- no scan response.

The application controls the session duration and stops after 30 seconds. It does not use an extended advertising API. `onStartFailure` is mapped to a typed failure, including an explicit `DATA_TOO_LARGE` value for `ADVERTISE_FAILED_DATA_TOO_LARGE`.

The adapter owns one callback instance per start attempt. Stop is idempotent and unregisters the exact callback instance supplied to the corresponding start call exactly once. Callback completion, synchronous exception, cancellation, timeout, permission loss, Bluetooth disablement, and owner disposal all reach the same cleanup path.

### 7.3 Scanner

The scanner uses `BluetoothLeScanner.startScan` with:

- one `ScanFilter` built with the fixed service-data UUID;
- a service-data mask that matches byte 0 as `0x01` while leaving the eight-byte token unconstrained;
- `SCAN_MODE_LOW_LATENCY`;
- report delay zero; and
- no address, device, name, or manufacturer filter.

The callback extracts service data, RSSI, and the injected monotonic receipt time. It validates through the pure codec and emits only a platform-neutral `DiscoveryObservation`. It does not pass the Android scan result, Bluetooth device, or raw record across the adapter boundary.

Scanner callback errors are typed. The exact callback instance is passed to `stopScan` exactly once. The same cleanup path handles normal stop, timeout, cancellation, callback error, permission loss, Bluetooth disablement, and owner disposal.

### 7.4 Startup transaction and rollback

The controller starts the scanner first and the advertiser second. The session becomes `ACTIVE` only after both starts have succeeded. If scanner startup fails, the advertiser is never started. If advertiser startup fails, the already-started scanner is stopped exactly once before the failure is surfaced. Any callback failure after activation stops the other subsystem before exposing `ERROR`.

Cancellation propagates as cancellation after cleanup. It is not converted into `ERROR`. No executor, callback, continuation, context, or platform handle survives the owning session. If an adapter needs an executor for a callback bridge, it uses an injected, owner-scoped executor and releases the reference with the adapter handle; it does not create an unbounded executor per observation or per callback.

## 8. Least-privilege permission policy

Permission requirements are calculated for the concrete operation, not for the Android Bluetooth permission family.

### 8.1 Normative operation table

| Android API | Discovery scan | Discovery advertise | Explicit Bluetooth-enable consent |
|---:|---|---|---|
| 29–30 | runtime `ACCESS_FINE_LOCATION` for BLE results | no modern runtime Bluetooth permission; legacy Bluetooth manifest permissions remain normal platform access | legacy platform behavior; no Android 12 `CONNECT` request exists |
| 31–37 | runtime `BLUETOOTH_SCAN` | runtime `BLUETOOTH_ADVERTISE` | runtime `BLUETOOTH_CONNECT` only when the app invokes the verified `ACTION_REQUEST_ENABLE` consent boundary and it is missing |

For an already-enabled discovery session on API 31–37, the initial request set is exactly the missing permissions from `{BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE}`. `BLUETOOTH_CONNECT` is not included merely because it exists in the Android 12+ permission family.

The current API 37 Android reference documents `BLUETOOTH_SCAN` on the scan operation, `BLUETOOTH_ADVERTISE` on the advertising operation, and `BLUETOOTH_CONNECT` on `BluetoothAdapter.ACTION_REQUEST_ENABLE`. The API 37 SDK also exposes the typed scanner, advertiser, filter, and callback methods used by this design. The implementation must preserve this operation boundary in both its policy tests and its runtime request arrays.

If a later verified platform call used by P2 is found to have an additional connect-gated requirement, the adapter returns a typed `CONNECT_REQUIRED` result at that concrete boundary. The permission coordinator may then request `BLUETOOTH_CONNECT` for that operation only and retry it. This is a guarded exception path, not a default discovery requirement. A `SecurityException` from an unverified or unrelated call is a defect and is not hidden by broad permission requests.

### 8.2 Other nearby permissions

P2 never requests `NEARBY_WIFI_DEVICES`, `ACCESS_LOCAL_NETWORK`, `RANGING`, location beyond the API 29–30 BLE scan requirement, storage, contacts, accounts, or any personal-data permission. The existing manifest declarations and `neverForLocation` intent remain aligned with the product's no-location design.

### 8.3 Permission timing and denial

No permission is requested at application startup, Activity creation, ViewModel initialization, diagnostics inspection, or passive Overview rendering. A request can be launched only after the user initiates `OPEN FIELD` and the controller has identified the missing permission for the requested operation.

The permission bridge reports granted, denied, permanently denied, or not-applicable as typed values. Denial returns `AWAITING_PERMISSION` or a user-readable unavailable state; it does not start a partial scanner or advertiser. Permission loss during an active session stops both subsystems exactly once.

### 8.4 Permission tests

The pure policy tests cover API 29, 30, 31, 33, 34, 36, and 37 for scan, advertise, already-enabled discovery, Bluetooth-enable consent, and not-applicable Wi-Fi/local/ranging operations. They assert:

- API 31+ discovery does not include CONNECT by family membership;
- already-enabled scan-plus-advertise requests only missing SCAN and ADVERTISE;
- the explicit enable boundary includes CONNECT only when that boundary is invoked and the permission is missing;
- API 29–30 scan maps to fine location;
- API 29–30 advertising has no modern runtime request;
- Wi-Fi, local-network, and ranging permissions are never returned by P2; and
- a verified adapter `CONNECT_REQUIRED` result is the only additional discovery-time route to a CONNECT request.

## 9. Session lifecycle

P2 sessions are foreground-only and application-controlled. One session lasts no longer than 30 seconds. There is no background service, always-on scan, scheduled restart, boot behavior, or state persistence.

The lifecycle is:

```text
IDLE
  -> permission gate
  -> BLUETOOTH_DISABLED or UNAVAILABLE when applicable
  -> STARTING
  -> ACTIVE
  -> STOPPING
  -> COMPLETED or IDLE
```

Any scanner/advertiser start failure, callback failure, protocol construction defect, permission loss, Bluetooth disablement, user stop, timeout, cancellation, or owner disposal passes through a single idempotent cleanup coordinator. Both handles are closed before a terminal state is published. A user stop and a 30-second timeout are normal completion; a real platform failure is `ERROR`; unsupported hardware is `UNAVAILABLE`.

The active state exposes only bounded session progress and anonymous peer presence. It does not expose a raw token, Android object, device address, name, or identity material.

## 10. UI and product integration

The app-level host keeps the frozen SpatialLink navigation language and routes the existing `OPEN FIELD` intent into `:feature:nearby`. The Overview remains a confidence surface rather than a diagnostics dump.

The nearby surface uses the existing Spatial Field visual primitive as a functional, neutral presence instrument:

- peer presence is rendered as symmetric anonymous nodes or energy points;
- the local device is not labeled with an identity in the discovery view;
- active/starting/stopping/disabled/unavailable/error states use the existing semantic status marks;
- no raw session ID, RSSI number, address, name, or trust claim is shown;
- no direction, distance, target acquisition, connection, transfer, or ranging affordance is introduced; and
- permission prompts are user-initiated and operation-specific.

Detailed P0 capability/permission diagnostics remain in the diagnostics/System feature. Nearby consumes neither diagnostics state nor diagnostics composables; both features consume the extracted neutral design system.

## 11. Test-first acceptance matrix

Implementation begins with failing or absent tests for the following behaviors and then adds the minimum production code to satisfy them. Assertions remain strict.

### 11.1 Pure `:core:discovery` tests

- random token generation uses eight bytes and rejects all-zero output;
- token equality is content-based and defensive copies prevent mutation;
- v1 encode/decode is exactly nine bytes with version `0x01` and big-endian token bytes;
- wrong length, wrong version, and all-zero token are rejected by the pure payload codec; transport UUID mismatch is rejected by the BLE filter/boundary;
- zero then non-zero SecureRandom output produces a valid session ID;
- eight consecutive zero SecureRandom outputs produce the typed `SESSION_ID_GENERATION_FAILED` result;
- one token is reused for one session and a fresh token is created for the next session;
- self-token observations are ignored;
- repeated observations deduplicate;
- six-second TTL eviction uses only monotonic time;
- capacity is never above 64 and tie-breaking is deterministic;
- RSSI remains evidence only; no distance or direction value exists;
- all lifecycle transitions and cancellation semantics are typed; and
- hardware-unavailable, permission, and partial-start outcomes are not represented as successful active discovery.

### 11.2 Android BLE adapter tests

Using injected platform seams and fake callbacks, tests assert:

- fixed UUID and exact nine-byte service-data payload;
- exact 27-byte SpatialLink-owned service-data structure, a final legacy-data bound of <=31 bytes, and rejection of an oversized payload without asserting an exact physical packet total;
- no name, TX power, manufacturer data, duplicate UUID, scan response, or identity metadata;
- non-connectable, low-latency legacy settings;
- scanner filter matches the fixed service-data UUID and version byte only;
- scanner output contains only service data, RSSI, and monotonic receipt time;
- no Android device object crosses the adapter boundary;
- callback failures map to typed failures;
- stop/unregister happens exactly once for every exit path;
- scanner-start failure never leaves an advertiser running;
- advertiser-start failure rolls back the scanner; and
- cancellation and owner disposal release callbacks and handles.

### 11.3 Static architecture/privacy tests

Source checks enforce:

- `:core:discovery` has no Android or AndroidX dependency;
- `:feature:nearby` has no dependency on `:feature:diagnostics`;
- `:core:designsystem` has no state, ViewModel, Bluetooth, permission, identity, or navigation/business dependency;
- `:core:discovery` and `:connectivity:ble` do not depend on `:core:identity`;
- no `androidx.core.uwb` dependency or UWB/ranging API is introduced;
- no reflection is used for P2 platform access;
- no Bluetooth address/name/alias/bond/device-string access exists in the production scanning path;
- no persistence, database, networking, cloud, analytics, telemetry, or device identity appears in P2; and
- no GATT, handshake, transfer, ranging session, Wi-Fi, or NFC behavior is introduced.

### 11.4 Connected qualification

One authorized physical Android device qualifies installation, launch, permission timing, Bluetooth-off/unsupported degradation, callback cleanup, and runtime stability. A one-device result cannot claim two-device discovery. The status is `P2_CODE_COMPLETE` plus `TWO_DEVICE_QUALIFICATION_PENDING` until two real devices observe one another in both directions. `P2_BLE_DISCOVERY_COMPLETE` requires that two-device evidence. An unresolved defect or external qualification blocker is `P2_BLE_DISCOVERY_BLOCKED`.

## 12. Verification basis

The permission and API boundary in this document was checked against:

- [Android Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions), including API 31+ scan/advertise/connect roles and API 29–30 BLE location behavior;
- [BluetoothLeScanner API reference](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner), including `startScan` and `stopScan` requirements;
- [BluetoothLeAdvertiser API reference](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeAdvertiser), including legacy advertising and `BLUETOOTH_ADVERTISE`;
- [BluetoothAdapter API reference](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter), including `ACTION_REQUEST_ENABLE`, adapter state, and LE object access;
- [ScanFilter.Builder API reference](https://developer.android.com/reference/android/bluetooth/le/ScanFilter.Builder), including service-data masks; and
- the installed API 37 SDK at `D:\R2H-Dev\Android\SDK\platforms\android-37.0\android.jar`.

The implementation must re-run these checks as part of the API-guarded adapter review if the locked compile SDK changes. The compile/target baseline remains API 37, minimum API 29, Kotlin/JVM 17, and the repository's approved Gradle/Android toolchain.

## 13. Self-review record

### Mandatory correction review

- Least privilege: passed. Already-enabled discovery requests only missing scan and advertise permissions. CONNECT is scoped to the verified explicit enable action or a typed, concrete adapter result.
- Feature dependency: passed. Nearby and diagnostics depend on neutral `:core:designsystem`; neither feature depends on the other.
- Legacy budget: passed. The SpatialLink-owned service-data structure is exactly 27 bytes, the final legacy advertisement is bounded at <=31 bytes without an exact physical-packet assertion, and data-too-large is a defect with no extended-advertising fallback.
- UUID ownership: passed. The fixed service-data UUID is validated by the BLE filter/boundary; the pure payload codec validates only length, version, token, and malformed input safety.
- Generation semantics: passed. Construction rejects all-zero values, the SecureRandom source retries up to eight draws, and repeated zero output becomes a typed generation failure.
- Device privacy: passed. The scan boundary accepts service data, RSSI, and monotonic time only; the Bluetooth device object is not read or exposed.

### Contradiction review

- Bluetooth disabled is a typed state and requires user consent; it does not trigger a hidden enable call or automatic permission request.
- API 29–30 scan location handling and API 31+ `neverForLocation` handling are separated rather than conflated.
- Hardware absence is `UNAVAILABLE`, while a genuine callback/platform defect is `ERROR`.
- A 30-second session lifetime and six-second peer TTL describe different clocks and do not conflict.
- The legacy-only transport rule and the 31-byte budget rule agree; the design has no extended-advertising escape hatch.
- The nearby UI uses shared visual infrastructure without importing diagnostics behavior and does not introduce identity or trust semantics.
- P2 observes presence only; no P3 handshake, trust, connection, transfer, ranging, NFC, or networking behavior is implied by the UI or protocol.

### Placeholder and scope review

- No unresolved TODO, TBD, open decision, or approval-dependent design choice remains in this specification.
- The implementation plan is intentionally not included in this file; it is the next workflow artifact after written-spec review.
- No production code, P3 work, or test weakening is authorized by this document.

**Self-review result: PASS. The corrected P2 specification is internally consistent and ready for written-spec review.**
