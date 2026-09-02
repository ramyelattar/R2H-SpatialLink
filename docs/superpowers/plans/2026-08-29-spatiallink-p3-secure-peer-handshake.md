# SpatialLink P3 Secure Peer Handshake Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved P3 secure peer handshake as a pure, testable protocol plus a typed Android BLE/GATT boundary, while preserving the frozen P0 foundation, P1 Keystore identity, P2 anonymous discovery, and the frozen SpatialLink visual system.

**Architecture:** Add pure Kotlin core:handshake for canonical protocol/domain/state-machine behavior and Android connectivity:handshake for typed crypto-provider, endpoint-broker, and GATT control-channel adapters. App composition supplies a narrow P1 identity/signing port. P2 remains unchanged and never depends on P3 or core:identity.

**Tech Stack:** Existing locked Android toolchain (AGP 9.3.1, Gradle 9.5.0, Kotlin 2.4.10, compile/target SDK 37, minSdk 29), Kotlin/JVM tests, Android instrumentation, Jetpack Compose infrastructure already present, Android JCA, pinned org.conscrypt:conscrypt-android:2.6.3, BLE GATT, and existing Gradle verification tools.

**Spec:** docs/superpowers/specs/2026-08-29-spatiallink-p3-secure-peer-handshake.md

## Global Constraints

- [ ] This plan implements P3 only. Do not start P4 transfer, P6 NFC/physical trust, ranging, Wi-Fi, cloud, WAN, analytics, or identity redesign.
- [ ] Preserve P0/P1/P2 behavior and source boundaries. Do not alter the P2 9-byte payload, fixed P2 UUID, 27-byte owned AD structure, legacy <=31-byte gate, non-connectable advertising, device-blind scanner, six-second TTL, 30-second session, or UNVERIFIED state.
- [ ] Never broadcast or log SpatialDeviceId, P1 public key, certificate, signature, Bluetooth address/name/alias/bond state, model, manufacturer, P2 token, raw X25519 material, or session keys.
- [ ] core:handshake remains pure Kotlin and may depend only on pure core:model types. It must not depend on Android, JCA, Keystore, Compose, core:identity, feature modules, connectivity modules, or navigation.
- [ ] connectivity:handshake owns all Android Bluetooth/GATT objects and provider/API guards. It must not depend on feature:nearby, feature:diagnostics, or core:identity.
- [ ] Do not use reflection or hidden APIs. Do not silently downgrade X25519 to P-256. A provider/API failure is typed CRYPTO_UNAVAILABLE and blocks the affected compatibility target.
- [ ] Request BLUETOOTH_CONNECT only at an explicit P3 GATT connection boundary when verified necessary. Normal P2 discovery keeps its existing API-specific least-privilege mapping, including legacy API 29–30 permissions/location behavior.
- [ ] Every task requires fresh tests and an independent spec-compliance/code-quality review before the next task starts. A green implementer report alone is not a gate.
- [ ] Before Task 1, a separate protocol-security review must return explicit PASS/FAIL results for ordering, transcript, disclosure, signatures, KDF, Finished, nonce uniqueness, role reflection, replay, unknown-key-share, identity misbinding, GATT binding, and P2-token binding.
- [ ] Test-first: add a failing focused test or executable boundary assertion before production behavior for each new behavior. Never delete, ignore, or weaken a failing test to obtain a green result.
- [ ] Every asynchronous owner/session resource has one serialized cleanup coordinator. Cleanup happens before terminal-state publication and exactly once from the public lifecycle path.
- [ ] No production P3 source is created until the plan is approved. The plan below is the implementation sequence, not execution evidence.

---

## Pre-implementation Gate 0: Dedicated protocol-security review

This is a review gate, not a production implementation task. It must pass
before Task 1 creates either new module or source directory.

**Review artifact:**

- The reviewer records the result in the active P3 SDD review ledger; this
  correction round does not create a third repository design document.

**Review checklist:**

- [ ] Verify the responder receives INIT_HELLO before sending RESP_HELLO or RESP_AUTH.
- [ ] Verify H0, H_R, H1, identity-claim hashes, exact field order, lengths, domains, roles, suite, nonces, ephemeral keys, and both P2 tokens are canonical and bound.
- [ ] Verify RESP_AUTH contains the responder identity and responder-claim signature, and INIT_AUTH is not sent before that signature verifies.
- [ ] Verify initiator and responder signature coverage, full-transcript confirmation, and P1 sign-only access.
- [ ] Verify HKDF labels/salts, directional authentication/session/Finished keys, and no raw X25519 output as an application key.
- [ ] Verify implicit AEAD counters and nonce uniqueness for RESP_AUTH r2i/1, INIT_AUTH i2r/1, RESP_AUTH_CONFIRM r2i/2; hello/Finished/CLOSE do not advance counters.
- [ ] Verify role reflection, replay, duplicate, stale-counter, unknown-key-share, and identity-misbinding rejection plus the valid-own-identity substitution limitation.
- [ ] Verify the mandatory P3 connectable legacy advertisement, selected P2-lease correlation, fixed GATT UUIDs, one server/client connection, and exact chunk header/reassembly rules.
- [ ] Verify API30/API34 client/server permission semantics and no startup CONNECT request.
- [ ] Return explicit `PASS` or `FAIL` for every checklist item. Any FAIL blocks Task 1 and requires a bounded spec/plan correction round.

**Gate result required:** `PASS` before Task 1.

**Correction-round result:** `PASS` — an independent scoped review of the
corrected specification and plan completed with every checklist item passing
and no Critical or Important findings. This records review readiness only and
does not authorize production implementation in this run.

---

## Task 1: Establish modules, dependency boundary, and cryptographic provider gate

**Files to add or modify:**

- settings.gradle.kts — add :core:handshake and :connectivity:handshake.
- gradle/libs.versions.toml — add the pinned Conscrypt version/dependency.
- core/handshake/build.gradle.kts.
- connectivity/handshake/build.gradle.kts.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakePorts.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/AndroidHandshakeCryptoProvider.kt.
- connectivity/handshake/src/test/kotlin/com/r2h/spatiallink/connectivity/handshake/AndroidHandshakeCryptoProviderTest.kt.
- connectivity/handshake/src/androidTest/java/com/r2h/spatiallink/connectivity/handshake/AndroidHandshakeCryptoInstrumentedTest.kt.
- tools/verify-p3-boundaries.ps1.
- New module README or package documentation only if required by repository conventions.

**Interfaces produced:**

    interface HandshakeCryptoPort {
        fun generateEphemeral(): CryptoResult<EphemeralKeyPair>
        fun deriveX25519(privateKey: EphemeralKeyPair, peerPublicKey: ByteArray): CryptoResult<ByteArray>
        fun sha256(input: ByteArray): ByteArray
        fun hmacSha256(key: ByteArray, input: ByteArray): ByteArray
        fun aeadEncrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, plaintext: ByteArray): CryptoResult<ByteArray>
        fun aeadDecrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, ciphertextAndTag: ByteArray): CryptoResult<ByteArray>
        fun verifyP256(publicKeySpki: ByteArray, input: ByteArray, signature: ByteArray): CryptoResult<Boolean>
    }

    interface HandshakeChannel {
        val incomingFrames: kotlinx.coroutines.flow.Flow<ByteArray>
        suspend fun send(frame: ByteArray): ChannelResult
        suspend fun close()
    }

`CryptoResult`, `EphemeralKeyPair`, and `ChannelResult` are pure typed results
defined in core:handshake. The Android provider is an implementation detail.

**TDD steps:**

- [ ] Add boundary tests that fail if core:handshake imports Android/JCA/Keystore/Compose or if connectivity:handshake imports feature modules/core:identity.
- [ ] Add the typed crypto port for X25519 generation/agreement, SHA-256, HMAC, HKDF support seam, AES-GCM, and P-256 verification without exposing provider classes to core:handshake.
- [ ] Add the typed provider capability matrix for platform XDH API 33+ and pinned Conscrypt API 29–32. Test absent/failed provider as CRYPTO_UNAVAILABLE.
- [ ] Add API-guard tests for minSdk-safe class loading. Do not use reflection.
- [ ] Implement only the module scaffolding and provider adapter required by these tests; do not implement handshake flow.
- [ ] Run :core:handshake:test, the provider/boundary tests, and verify-p3-boundaries.ps1.

**Review gate:**

- [ ] Fresh reviewer confirms the graph has no feature-to-feature or discovery-to-identity edge, core:handshake is pure Kotlin, Conscrypt is pinned, and no P3 protocol/session source has leaked into the Android boundary.
- [ ] Reviewer confirms API 30/API 34 provider matrix is a hard gate and no downgrade/reflection path exists.

## Task 2: Implement canonical P3 frame codec and message models

**Files to add:**

- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeProtocol.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeMessages.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeFrameCodec.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeLimits.kt.
- core/handshake/src/test/kotlin/com/r2h/spatiallink/handshake/HandshakeFrameCodecTest.kt.
- core/handshake/src/test/kotlin/com/r2h/spatiallink/handshake/HandshakeMessageFormatTest.kt.

**TDD steps:**

- [ ] Write exact byte-vector tests for the 7-byte header, big-endian lengths, 83-byte hello bodies, encrypted body length rules, Finished bodies, and CLOSE.
- [ ] Include the responder-claim signature in RESP_AUTH, H_R echo in INIT_AUTH, initiator-claim hash in RESP_AUTH_CONFIRM, and exact signature-length formulas.
- [ ] Write rejection tests for wrong magic/version/type/flags/suite/role, truncation, trailing bytes, oversized frames, malformed length arithmetic, noncanonical DER, and unknown close reason.
- [ ] Implement immutable protocol constants, typed message models, canonical encoder, and decoder with no JSON/text fallback.
- [ ] Ensure all public models contain only protocol-safe bytes and typed values; no Android or BluetoothDevice type can appear.
- [ ] Run :core:handshake:test with fresh output.

**Review gate:**

- [ ] Reviewer independently checks every offset, fixed/variable length, maximum, role, and big-endian rule against the P3 spec.
- [ ] Reviewer confirms UUID validation is not incorrectly placed in the pure payload codec and no P2 advertisement source changed.

## Task 3: Implement identity claims, canonical transcript, signatures, HKDF, and AEAD schedule

**Files to add:**

- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeTranscript.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/IdentityClaim.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HkdfSha256.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeKeySchedule.kt.
- core/handshake/src/test/kotlin/com/r2h/spatiallink/handshake/HandshakeTranscriptTest.kt.
- core/handshake/src/test/kotlin/com/r2h/spatiallink/handshake/HkdfAndNonceTest.kt.
- core/handshake/src/test/kotlin/com/r2h/spatiallink/handshake/IdentityClaimValidationTest.kt.
- connectivity/handshake provider tests for X25519/AES-GCM/HMAC/P-256 verification vectors.

**TDD steps:**

- [ ] Add fixed H0/H_R/H1 transcript vectors with role, field-order, length, and both-token-binding assertions.
- [ ] Add tests that reject claimed ID mismatch, invalid/noncanonical SPKI, noncanonical DER signature, wrong signature domain, and role reflection.
- [ ] Add a disclosure-order test proving the responder-claim signature verifies before the initiator identity/signature is requested or accepted.
- [ ] Add HKDF extract/expand vectors, directional separation, fixed labels, final-salt derivation, and zero-shared-secret rejection tests.
- [ ] Add nonce/counter tests for per-direction uniqueness, no reuse, overflow, AAD binding, and the exact encrypted-message sequence.
- [ ] Implement the pure transcript/claim/KDF logic through typed crypto ports. Keep JCA/provider calls out of core:handshake.
- [ ] Run focused pure tests and provider matrix tests.

**Review gate:**

- [ ] Reviewer checks exact domain strings, H0/H1 construction, role binding, signature input, claim hash, HKDF labels, and nonce formula.
- [ ] Reviewer confirms no raw shared secret/key material escapes the crypto/session boundary and no provider fallback exists.

## Task 4: Implement the pure handshake state machine and owner-scoped lifecycle

**Files to add:**

- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeState.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeSession.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeController.kt.
- core/handshake/src/main/kotlin/com/r2h/spatiallink/handshake/HandshakeFailure.kt.
- core/handshake/src/test/kotlin/com/r2h/spatiallink/handshake/HandshakeStateMachineTest.kt.
- core/handshake/src/test/kotlin/com/r2h/spatiallink/handshake/HandshakeLifecycleTest.kt.

**TDD steps:**

- [ ] Add tests for both initiator/responder valid transitions and every invalid/out-of-order/duplicate/replay transition, including responder receive-first INIT_HELLO behavior.
- [ ] Add tests for mutual signature verification, Finished verification, authenticated terminal state, and no early authentication publication.
- [ ] Add tests proving an attacker with its own valid P1 key may authenticate as that different identity, while a forged claim for an honest identity is rejected; no test may call the former physical trust.
- [ ] Add owner re-entry race tests proving a second start cannot replace the first session, job, generation, lease, peer context, or cleanup coordinator.
- [ ] Add cleanup-order observation tests proving channel/timer/lease/ephemeral state cleanup occurs before terminal publication and happens once.
- [ ] Add timeout, cancellation, owner disposal, foreground-loss, transport-close, malformed-budget, and resource-limit tests.
- [ ] Implement serialized owner-scoped session coordination, immutable generation tagging, typed state/failure results, and zeroing/clearing seam.
- [ ] Run :core:handshake:test with fresh output.

**Review gate:**

- [ ] Reviewer inspects actual synchronization/serialization, not a boolean guard.
- [ ] Reviewer confirms no terminal callback can mutate a closed session, no second session steals resources, and cancellation is not misreported as authentication failure.

## Task 5: Implement the frozen P3 GATT rendezvous and transport boundary

**Files to add:**

- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/P3ControlServiceSpec.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/P3RendezvousAdvertisementSpec.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/P3GattTransportChunkCodec.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/BleHandshakeEndpointBroker.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/BleHandshakeControlServer.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/BleHandshakeControlClient.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/BleHandshakeFrameChannel.kt.
- connectivity/handshake/src/main/kotlin/com/r2h/spatiallink/connectivity/handshake/AndroidBluetoothApiAdapters.kt.
- connectivity/handshake/src/test/kotlin/com/r2h/spatiallink/connectivity/handshake/P3GattTransportChunkCodecTest.kt.
- connectivity/handshake/src/test/kotlin/com/r2h/spatiallink/connectivity/handshake/P3RendezvousAdvertisementTest.kt.
- connectivity/handshake/src/test/kotlin/com/r2h/spatiallink/connectivity/handshake/BleHandshakeEndpointBrokerTest.kt.
- connectivity/handshake/src/androidTest/java/com/r2h/spatiallink/connectivity/handshake/P3GattPermissionInstrumentedTest.kt.

**Interfaces produced for later tasks:**

    data class P2EndpointSelection(
        val ownerGeneration: Long,
        val selectedTokenBytes: ByteArray
    )

    sealed interface EndpointResult<out T> {
        data class Success<T>(val value: T) : EndpointResult<T>
        data class Failure(val reason: HandshakeFailure) : EndpointResult<Nothing>
    }

    interface P3EndpointBroker {
        suspend fun acquireInitiatorLease(selection: P2EndpointSelection): EndpointResult<HandshakeEndpointLease>
        suspend fun startResponderOffer(): EndpointResult<ResponderOffer>
    }

    interface HandshakeEndpointLease {
        suspend fun openChannel(): EndpointResult<HandshakeChannel>
        suspend fun invalidate()
    }

    interface ResponderOffer {
        suspend fun accept(pendingConnection: PendingConnection): EndpointResult<HandshakeChannel>
        suspend fun close()
    }

`P2EndpointSelection`, `HandshakeEndpointLease`, `HandshakeChannel`, and
`PendingConnection` are neutral/opaque types. No signature contains
BluetoothDevice, ScanResult, address, name, or identity metadata.

**TDD steps:**

- [ ] Add tests for the exact separate P3 advertisement: connectable legacy mode, LOW_LATENCY/LOW power settings, one complete 128-bit service UUID, no service data/scan response/name/TX/manufacturer data, 18-byte owned AD structure, 3-byte connectable flags contribution, <=31-byte bound, 30-second owner lifetime, one-offer limit, and no extended fallback.
- [ ] Add tests for selected-P2-lease correlation: match the fixed P2 UUID/version/token inside the Android boundary, retain the platform object only in the one-use lease, require P3 service discovery on that same endpoint, reject a different offer, expire at 10 seconds, and clear on owner close.
- [ ] Add the exact 10-byte chunk-header vectors: version, FIRST/LAST flags, frame ID, zero-based index, count, logical frame length, big-endian encoding, and default-MTU 23 capacity.
- [ ] Add chunk tests for one frame in flight per direction, 512-chunk/4096-byte bounds, contiguous order, identical-current-duplicate ignore, conflicting duplicate rejection, completed-frame duplicate rejection, missing/out-of-order rejection, interleaving rejection, 2-second idle/5-second total timeout, remote CLOSE, and no retransmission.
- [ ] Add GATT tests proving responder=server, initiator=client, one connection carries both directions, PROPERTY_WRITE | PROPERTY_NOTIFY only, CCCD 0x2902 enabled with 0x0100 before INIT_HELLO, WRITE_TYPE_DEFAULT only, notification-only server delivery, no read/indication/write-without-response, MTU 247 request with default 23 fallback, and exact-once close.
- [ ] Add callback-generation tests proving stale GATT callbacks cannot revive a closed lease/session and advertiser/server/client/channel/timers are cleaned exactly once.
- [ ] Add API30 client/server tests using legacy BLUETOOTH/BLUETOOTH_ADMIN expectations and no CONNECT runtime request; preserve the existing P2 API30 scan-location branch. Add API34 client/server tests requiring CONNECT only after explicit BEGIN SECURE LINK or MAKE AVAILABLE and before connectGatt/open-operate BluetoothGattServer.
- [ ] Add tests proving normal P2 discovery permission requests do not gain CONNECT, nearby Wi-Fi, local-network, ranging, or startup behavior.
- [ ] Implement the typed Android GATT client/server, mandatory P3 advertisement, callback mapping, bounded chunk channel, and owner-scoped lease. Do not modify the P2 scanner/advertiser.
- [ ] Run the connectivity JVM tests and the API30/API34 connected boundary tests with non-zero XML counts.

**Review gate:**

- [ ] Reviewer confirms the exact rendezvous and transport contract is implemented rather than deferred: one mandatory connectable P3 advertisement, one selected opaque platform lease, one GATT client/server connection, exact characteristic/CCCD/write/notification/MTU/chunk rules, and no second cryptographic protocol.
- [ ] Reviewer confirms actual source never reads address/name/alias/bond/toString outside an internal platform reference needed to connect, no P2 scanner change is used, no autoConnect/background service/bonding/transfer exists, and no reflection is used.

## Task 6: Add the minimal explicit P3 initiation flow and P1 app composition

This task adds only the functional interaction required to execute the frozen
P3 flow. It does not redesign the frozen visual language or add a new route.

**Files to add or modify:**

- feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbySecureLinkPort.kt.
- feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbySecureLinkUiState.kt.
- feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyScreen.kt — add controlled selection, BEGIN SECURE LINK, MAKE AVAILABLE, ACCEPT, cancel, and typed terminal presentation inside the existing Field surface.
- feature/nearby/src/main/kotlin/com/r2h/spatiallink/nearby/NearbyViewModel.kt — route explicit intents through the injected port and preserve P2 ownership/lifecycle.
- feature/nearby/src/test/kotlin/com/r2h/spatiallink/nearby/NearbySecureLinkFlowTest.kt.
- app/src/main/java/com/r2h/spatiallink/p3/HandshakeIdentityAdapter.kt.
- app/src/main/java/com/r2h/spatiallink/p3/HandshakeComposition.kt — provide owner-scoped P3 coordinator/port and P1 signing adapter.
- app/build.gradle.kts — add only required neutral P3 module dependencies.
- app/src/test/kotlin/com/r2h/spatiallink/HandshakeCompositionTest.kt.
- app/src/androidTest/java/com/r2h/spatiallink/P3InitiationInstrumentedTest.kt.
- Existing app container only where it supplies a factory, never a permanently reusable owner-scoped session.

**Interfaces produced/consumed:**

    interface NearbySecureLinkPort {
        suspend fun begin(selected: AnonymousPeerSelection): SecureLinkResult
        suspend fun makeAvailable(): SecureLinkResult
        suspend fun acceptPending(): SecureLinkResult
        suspend fun cancel()
    }

`AnonymousPeerSelection` is a transient selection handle bound to the current
P2 session. Its token is never rendered. `SecureLinkResult` contains only
typed state/failure and a privacy-safe authenticated result; it contains no
BluetoothDevice, address, name, raw token, key bytes, or signature.

**TDD steps:**

- [ ] Add a UI/state test proving multiple anonymous entries can be displayed but exactly one transient entry can be selected and handed to P3; no token/RSSI-as-identity/Bluetooth identity is rendered.
- [ ] Add intent tests proving BEGIN SECURE LINK is the only initiator trigger, MAKE AVAILABLE enters responder offer/listen, a pending incoming connection remains inert until ACCEPT, and P2 presence alone/startup never starts P3.
- [ ] Add role-flow tests proving the first run can assign Device A as responder and Device B as initiator, both explicit actions are required, and the existing Field surface—not a hidden/new route—owns the controls.
- [ ] Add simultaneous-action tests proving two BEGIN SECURE LINK actions end with NO_RESPONDER_OFFER and two MAKE AVAILABLE actions expire independently without role inversion or a second connection.
- [ ] Add expiry/cancel/back/foreground tests proving selected-peer expiry maps to P2_PEER_EXPIRED, cancel/close stops the owner before navigation, foreground loss closes the session, and return to foreground does not restart.
- [ ] Add composition tests proving P3 receives P1 public identity/sign operation through a narrow adapter, never a private key or raw certificate UI path; the adapter delegates to the existing core:identity repository.
- [ ] Add fresh-owner tests proving a closed Nearby owner receives a new coordinator/factory and cannot reuse its previous lease, channel, callback, timer, or session state.
- [ ] Implement the minimum ViewModel/Field wiring and app composition. Keep the frozen design-system primitives, P2 models, P2 screen architecture, and product copy outside the explicit controls unchanged.
- [ ] Run feature:nearby JVM tests and app JVM/connected tests with fresh non-zero output.

**Review gate:**

- [ ] Reviewer confirms the flow is executable: selectable anonymous entry, BEGIN SECURE LINK, MAKE AVAILABLE, ACCEPT, explicit role assignment, one connection, expiry, simultaneous-action behavior, and no hidden route/UI dependency.
- [ ] Reviewer confirms P1 remains the only identity implementation, core:identity is not a dependency of core:handshake or connectivity:ble, feature:nearby does not depend on diagnostics/connectivity, and no P3 identity is shown before the protocol state allows it.
- [ ] Reviewer confirms owner-scoped lifecycle, stop-before-navigation, foreground loss, no automatic restart, and no visual-foundation redesign.

## Task 7: Static privacy/architecture gates and documentation

**Files to add or modify:**

- tools/verify-p3-boundaries.ps1.
- docs/architecture/FOUNDATION.md — add one concise P3 boundary entry without changing P0/P1/P2 decisions.
- .superpowers/sdd/2026-08-29-spatiallink-p3-secure-peer-handshake/task-7-report.md.
- Existing P2 documentation is read-only during P3; no P2 specification is modified.

**TDD steps:**

- [ ] Add negative fixture checks proving the P3 scanner fails for missing required module structures, feature-to-feature dependency, core:identity dependency from protocol/BLE, reflection, forbidden Bluetooth identity access, P2 payload/UUID mutation, logging of raw secrets, persistence, GATT transfer, ranging, or P4/P6 identifiers.
- [ ] Add negative fixtures for a missing/incorrect mandatory P3 advertisement, wrong owned byte budget, extended-advertising fallback, missing CCCD/chunk fields, non-default write type, missing responder INIT_HELLO guard, premature INIT_AUTH disclosure, shared Finished key, counter rollback/retransmission, or API30/API34 permission collapse.
- [ ] Add positive checks proving required typed API guards, provider boundary, fixed control UUIDs, fixed advertisement fields, 18-byte owned/<=31-byte legacy bound, 10-byte chunk header, frame-size limits, owner-scoped cleanup seam, exact directional Finished labels, and no P2 source modifications.
- [ ] Ensure the scanner does not silently skip missing required files or fabricate historical evidence.
- [ ] Run verify-p2-boundaries.ps1 and verify-p3-boundaries.ps1 with fresh output.

**Review gate:**

- [ ] Reviewer attempts both violating and missing-structure fixtures and verifies non-zero failure.
- [ ] Reviewer confirms the static gate complements, rather than weakens, the existing P2 privacy scanner.

## Task 8: Host and connected regression qualification

**Files to modify only for evidence:**

- .superpowers/sdd/2026-08-29-spatiallink-p3-secure-peer-handshake/progress.md.
- .superpowers/sdd/2026-08-29-spatiallink-p3-secure-peer-handshake/task-8-report.md.
- No production source unless a test exposes a real defect; any defect starts a fresh fix/review round.

**Verification sequence:**

- [ ] Run .\gradlew.bat clean --console=plain.
- [ ] Run .\gradlew.bat test --console=plain.
- [ ] Run .\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain.
- [ ] Run .\gradlew.bat lint --console=plain.
- [ ] Run .\tools\verify-p2-boundaries.ps1.
- [ ] Run .\tools\verify-p3-boundaries.ps1.
- [ ] Run :app:connectedDebugAndroidTest with an explicit physical device and inspect non-zero TEST XML counts.
- [ ] Run :feature:nearby:connectedDebugAndroidTest with an explicit physical device and inspect non-zero TEST XML counts.
- [ ] Run :core:identity:connectedDebugAndroidTest with an explicit physical device and inspect non-zero TEST XML counts.
- [ ] Preserve P2 physical anonymous discovery evidence and verify no P3 source changed P2 runtime behavior.

**Review gate:**

- [ ] Fresh reviewer reconciles command output and XML against the exact final source tree.
- [ ] No green task with zero executed tests is accepted.

## Task 9: Two-device P3 physical qualification and final report

**Required physical evidence:**

- [ ] Confirm exactly two authorized intended devices with selector-qualified ADB commands; never use ambiguous bare adb commands and never create an emulator.
- [ ] Install the same final APK on both and record matching local/installed hashes without uninstalling unrelated packages.
- [ ] Confirm P2 startup is passive and P2 permissions remain least-privilege.
- [ ] Start P2 explicitly, observe anonymous presence on both, and verify no identity/name/address/distance/direction exposure before P3.
- [ ] First run roles explicitly: Device A is responder; its user taps MAKE AVAILABLE, then ACCEPT when the anonymous request arrives. Device B is initiator; its user selects one anonymous P2 entry and taps BEGIN SECURE LINK. Request BLUETOOTH_CONNECT only at the corresponding explicit API31+ GATT server/client boundary; record legacy API30 expectations separately.
- [ ] Verify exactly one GATT server on Device A, one GATT client on Device B, one connection, notifications/CCCD, write-with-response, MTU/default-MTU fallback, and canonical chunk reassembly.
- [ ] Verify the responder receives INIT_HELLO before sending RESP_HELLO/RESP_AUTH; both endpoints complete the exact handshake and reach CRYPTOGRAPHICALLY_AUTHENTICATED only after responder-claim proof, both signatures, and both directional Finished values.
- [ ] Verify no P1 identity is visible in P2 advertising, no raw key/token appears in UI/logcat, and no TRUSTED claim is produced.
- [ ] Verify malformed frame, replay/counter, timeout, cancellation, close-before-navigation, foreground loss, Bluetooth disablement, and permission-loss paths fail safely and clean up exactly once.
- [ ] Verify fresh second sessions use fresh X25519/session material through approved test-only opaque evidence.
- [ ] Verify no GATT transfer, bonding, ranging, Wi-Fi, NFC, persistence, or background service exists.
- [ ] Capture bounded per-device logcat and inspect for fatal/security/class-loading errors, identity/token leakage, GATT transfer, and forbidden P4/P6 activity.
- [ ] Record device/API/ABI, suite counts, APK hash, physical handshake direction, trust-state result, limitations, and exact files changed.
- [ ] Optionally run one second sequential role-reversal: Device B becomes responder through MAKE AVAILABLE/ACCEPT and Device A becomes initiator through anonymous selection/BEGIN SECURE LINK. This is a separate run after the first channel is closed, not a second simultaneous GATT connection.

**Final review gate:**

- [ ] Independent reviewer confirms two-device mutual cryptographic authentication, not merely one-device or anonymous P2 presence.
- [ ] Reviewer confirms physical evidence belongs to the final APK and no source changed afterward. If a production fix occurs, repeat Tasks 8–9 affected gates and the complete host sequence.
- [ ] Do not use a P3 completion marker until all required evidence is non-zero, selector-qualified, privacy-safe, and reconciled. This plan does not authorize a P4 start.

## Files that must not change for P3

- P2 protocol constants or payload codec.
- P2 scanner privacy boundary or any source that reads ScanResult.device for
  P2.
- P2 advertiser mode, 27-byte owned structure, <=31-byte legacy budget, or
  session/TTL limits.
- core:identity key alias, key construction, SpatialDeviceId derivation, or
  Keystore policy.
- P0 capability/permission/domain models.
- Frozen Overview visual composition and design-system responsibility boundary,
  unless a later explicit functional UI requirement is approved.
- Any P4 transfer, P6 NFC/trust, ranging, Wi-Fi, cloud, WAN, analytics, or
  device-identity implementation.

## Rollback and cleanup policy

- [ ] If a task fails review, keep the evidence, record the exact finding, and
  fix only that task before advancing.
- [ ] If a provider/API matrix fails, return typed CRYPTO_UNAVAILABLE and stop
  the affected execution; do not downgrade or bypass the test.
- [ ] If a connected or physical test fails, diagnose the root cause, add or
  preserve a regression test, make the minimum scoped fix, rerun the affected
  gate, then rerun the complete final sequence after the last production fix.
- [ ] Close GATT channels, callbacks, timers, leases, coroutines, peer/session
  memory, and ephemeral secrets exactly once on all terminal paths.
- [ ] Never use destructive ADB commands, factory reset, broad uninstall,
  personal-data access, or global device setting changes.

## Plan completion condition

The implementation plan is complete only when every task has its independent
review gate, host/connected evidence is fresh after the final source change,
two authorized physical devices complete the explicit mutual P3 handshake, and
the final report distinguishes anonymous P2 discovery, cryptographic P3
authentication, and deferred P6 physical trust. The result must not claim P3
complete in this specification phase.
