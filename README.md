# SpatialLink P1 Cryptographic Device Identity

SpatialLink preserves its P0 offline-first foundation for local capability and
modeled permission diagnostics, and adds the bounded P1 cryptographic device
identity foundation. Its application ID is `com.r2h.spatiallink`.

## Toolchain

- JDK 17
- Gradle wrapper 9.5.0
- Android Gradle Plugin 9.3.1
- Kotlin 2.4.10
- compile/target SDK 37, minimum SDK 29
- Compose UI 1.12.0 and Material 3 1.4.0

Use the wrapper with JDK 17 from the project root:

```powershell
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat projects --console=plain }
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat test --console=plain }
& { $env:JAVA_HOME = 'D:\R2H-Dev\Java\jdk-17'; .\gradlew.bat :app:assembleDebug --console=plain }
```

## Modules

- `:core:model` is pure Kotlin. It owns immutable domain models, typed issues, permission policy, capability aggregation, legacy ranging fallback, and foundation status.
- `:core:common` owns the injected dispatcher port.
- `:core:capabilities` owns Android feature/service adapters, API-aware permission inspection, partial-failure handling, and guarded platform ranging capability inspection.
- `:core:identity` owns the typed Android Keystore boundary, P-256 identity repository, self-test, signing/verification, and security-level mapping.
- `:feature:diagnostics` owns the immutable UI state, ViewModel, reducer, and stateless Compose screen.
- `:app` is the explicit composition root and launchable Android application.
- `:testing` owns JVM fakes and deterministic fixtures.
- `build-logic` owns convention plugins.

P0 performs no network operation, peer discovery, transfer, pairing, database
access, cloud/analytics work, storage scan, or ranging session. P1 adds only a
local Android Keystore identity: an EC P-256 signing key under
`spatiallink.identity.signing.v1`, a SHA-256 public-key identifier, a bounded
sign/verify self-test, and safe diagnostic metadata. Private-key bytes,
signatures, attestation, and device identifiers are not exposed. The declared
`INTERNET` permission is reserved for future local sockets and is not used by
the current implementation. Runtime permissions are inspected but never
requested automatically at startup.

The UWB base capability uses the platform package feature. API 36+ detailed technology inspection uses the typed `android.ranging.RangingManager` callback adapter. P0 intentionally has no `androidx.core.uwb` dependency.

## P2 — Privacy-Preserving BLE Peer Discovery

P2 is a bounded anonymous BLE discovery layer. Its module graph is:

    :app
    ├── :feature:nearby
    │   ├── :core:designsystem
    │   ├── :core:discovery
    │   └── :connectivity:ble
    ├── :feature:diagnostics
    │   └── :core:designsystem
    ├── :core:discovery
    └── :connectivity:ble ──> :core:discovery

core:discovery remains pure Kotlin with no Android dependency. The
connectivity:ble module is the only Android BLE boundary. feature:nearby and
feature:diagnostics have no feature-to-feature dependency; their shared
presentation infrastructure is only the neutral core:designsystem. P2 has no
dependency on core:identity and never broadcasts SpatialDeviceId, a public
key, or any P1 identity material.

The v1 discovery frame is exactly nine bytes: byte 0 is 0x01, and bytes 1..8
are one 64-bit big-endian DiscoverySessionId. SecureRandom generation rejects
all-zero and retries within a defensive bound. Exhausting that bound is the
typed SESSION_ID_GENERATION_FAILED result. One token exists only for one
30-second foreground session and is held in RAM.

The fixed service-data UUID is
7F83C58D-4C4B-4E97-9B7D-9C06D3A47B91. The non-connectable legacy advertisement
contains only the UUID service-data AD structure: 1 byte AD length, 1 byte AD
type, 16 byte UUID, and the 9 byte payload, for 27 bytes of SpatialLink-owned
structure. The final legacy advertisement must remain <= 31 bytes. P2 does
not assert an exact 30-byte physical packet total and does not fall back to
extended advertising. There is no device name, TX power field, manufacturer
data, duplicate service UUID, capability bitmap, identity material, scan
response, or connectable mode.

Permissions are operation-specific and least-privilege. Scanning requires
BLUETOOTH_SCAN and advertising requires BLUETOOTH_ADVERTISE. BLUETOOTH_CONNECT
is requested only at a verified connect-gated action such as
ACTION_REQUEST_ENABLE when that running API path requires it. Discovery while
Bluetooth is already enabled must not request BLUETOOTH_CONNECT. P2 never
requests runtime permissions automatically at startup.

| Operation | Required permission | BLUETOOTH_CONNECT |
| --- | --- | --- |
| Discovery scan while Bluetooth is already enabled | BLUETOOTH_SCAN | Not required |
| Non-connectable legacy advertisement | BLUETOOTH_ADVERTISE | Not required |
| Bluetooth enable or another verified connect-gated action | Action-specific | Only if the running API requires it |

The production scanner reads only service data from ScanRecord, RSSI, and a
monotonic receipt timestamp. It does not read ScanResult.device, Bluetooth
address, name, alias, bond state, or ScanResult.toString(). Malformed,
wrong-version, zero, self-token, and non-matching frames are discarded
without identity or authenticity claims; accepted peers are always
UNVERIFIED.

Discovery is foreground-only. Each owner-scoped controller owns its session
token, peer cache, scanner, advertiser, timer, and cleanup coordinator. A
second owner cannot replace a STARTING or ACTIVE session. Close and Back stop
discovery before navigation; ON_STOP stops it without automatic restart.
Active ticks re-check scan permission, advertise permission, and Bluetooth
enabled state. Startup is scanner-then-advertiser with rollback if the second
start fails. Cleanup is serialized and exactly once per handle, clears the
six-second peer TTL cache and token, and publishes non-active state only after
cleanup. The cache is RAM-only and bounded to 64 peers. RSSI is not converted
into distance or direction.

Two authorized physical devices are required for mutual discovery
qualification: both must observe each other bidirectionally, remain
UNVERIFIED, expose no identity/address/name/model data, and cleanly stop and
restart with fresh session tokens. That two-device qualification completed
on 2026-08-29: API 34 serial b294f127 and API 30 selector 192.168.0.103:35415
(not the mDNS alias) each observed one anonymous UNVERIFIED presence; both
returned to IDLE with zero peers after explicit END FIELD. Device B connected
XML was non-zero. Status: P2_BLE_DISCOVERY_COMPLETE. Every later trust,
transport, pairing, transfer, and spatial-session phase remains outside this
implementation.
