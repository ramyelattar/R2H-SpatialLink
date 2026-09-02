# SpatialLink P1 Cryptographic Device Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one stable Android-Keystore-backed P-256 device identity to the completed SpatialLink P0 foundation, prove it with ECDSA signatures, expose safe diagnostics, and qualify it on the authorized physical device.

**Architecture:** Keep all identity values, defensive byte wrappers, typed errors, signature outcomes, and the repository port in pure `:core:model`. Implement the Android Keystore provider, API 31 security inspection, API 29–30 fallback, signing, verification, self-test, and mutex-protected repository in the new `:core:identity` Android library. Inject that port into the existing diagnostics ViewModel; Compose renders only an immutable identity state.

**Tech Stack:** Gradle 9.5.0, AGP 9.3.1, Kotlin 2.4.10, JDK 17, compile/target SDK 37, minimum SDK 29, Android Keystore, JCA `SHA256withECDSA`, Kotlin coroutines `Mutex`/`StateFlow`, Compose UI 1.12.0, Material 3 1.4.0, JUnit 4, AndroidX test, and the existing P0 modules.

**Spec:** `docs/superpowers/specs/2026-08-27-spatiallink-p1-device-identity-design.md`

## Global Constraints

- Use `com.r2h.spatiallink` for namespace and application ID.
- Preserve `minSdk = 29`, `compileSdk = 37`, `targetSdk = 37`, JDK 17, AGP 9.3.1, Gradle 9.5.0, Kotlin 2.4.10, Compose UI 1.12.0, and Material 3 1.4.0.
- Keep `:core:model` strictly pure Kotlin with no Android, AndroidX, external crypto-provider, Compose, Activity, Context, Keystore, or ViewModel dependency; its JDK standard-library SHA-256 call is allowed for deterministic value derivation.
- Add exactly one production module: `:core:identity`; do not add `:core:security`, `:core:handshake`, or another identity module.
- Use EC/NIST P-256 `secp256r1`, `SHA256withECDSA`, provider `AndroidKeyStore`, alias `spatiallink.identity.signing.v1`, purpose SIGN, no user authentication requirement, no StrongBox request/requirement, and no attestation.
- Use typed API guards for API 31 security-level inspection and keep all Android Keystore access in injected adapters under `:core:identity`.
- Use `Mutex` or an equivalent structured mechanism for check-create concurrency; do not use `GlobalScope`, raw threads, ad-hoc executors, `runBlocking`, or reflection.
- Treat a valid software-backed Keystore key as acceptable; identity validity is independent from key-security level.
- Do not log, serialize, store, transmit, or expose private-key bytes, full signatures, attestation data, device identifiers, MAC/address values, or personal content.
- Do not add `androidx.core.uwb`, Bouncy Castle, Tink, libsodium, Firebase, Retrofit, OkHttp, Ktor, Hilt, Dagger, Koin, Room, SQLDelight, protobuf, ONNX, ML, cloud, WAN, analytics, or telemetry dependencies.
- Do not request runtime permissions at startup and do not add peer discovery, trust storage/decisions, key agreement, transport encryption, NFC pairing, ranging sessions, transfer, or P2+ functionality.
- Test aliases use `spatiallink.test.identity.<unique>`; instrumentation cleanup may delete only the alias created by that test and never the production alias.
- The checkout is not a Git repository; do not initialize Git or create commits. Use the local plan checklist and fresh command output as evidence.

---

## File map

### Pure model and shared dispatcher files

- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/IdentityModels.kt` — identity enums, typed failure/result values, inspection, and verification outcome.
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/SpatialDeviceId.kt` — SHA-256-derived immutable identifier and canonical/short formatting.
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/DeviceIdentity.kt` — immutable public key and identity aggregate.
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/IdentitySignature.kt` — immutable signature value.
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/IdentityPorts.kt` — pure repository contract.
- Create: `core/model/src/test/kotlin/com/r2h/spatiallink/model/SpatialDeviceIdTest.kt` — digest, formatting, immutability, equality, and hash tests.
- Create: `core/model/src/test/kotlin/com/r2h/spatiallink/model/IdentityModelTest.kt` — identity invariant, signature immutability, and typed result tests.
- Modify: `core/common/src/main/kotlin/com/r2h/spatiallink/common/DispatcherProvider.kt` — add the injected `io` dispatcher.
- Modify: `testing/src/main/kotlin/com/r2h/spatiallink/testing/TestDispatcherProvider.kt` — expose the injected I/O dispatcher with a test-friendly default.
- Modify: `core/common/src/test/kotlin/com/r2h/spatiallink/common/DispatcherProviderTest.kt` and `testing/src/test/kotlin/com/r2h/spatiallink/testing/TestDispatcherProviderTest.kt` — cover the new dispatcher contract.

### Android identity module

- Modify: `settings.gradle.kts` — include only `:core:identity` for P1.
- Create: `core/identity/build.gradle.kts` — Android-library convention, model/common dependencies, coroutines, and AndroidX test dependencies.
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/IdentityKeyStorePort.kt` — platform-entry value and injected Keystore-operation port.
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/AndroidKeyStorePlatformAdapter.kt` — typed Android Keystore generation, load, sign, and public-key verification.
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/KeySecurityLevelInspector.kt` — guarded API selection and safe unknown mapping.
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/Api31KeySecurityLevelReader.kt` — `KeyInfo.getSecurityLevel()` adapter guarded at API 31.
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/LegacyKeySecurityLevelReader.kt` — API 29–30 `isInsideSecureHardware` adapter.
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityRepository.kt` — alias-scoped mutex, typed errors, self-test, sign, verify, and no-rotation behavior.
- Create: `core/identity/src/test/kotlin/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityRepositoryTest.kt` — real repository orchestration with an injected boundary fake.
- Create: `core/identity/src/test/kotlin/com/r2h/spatiallink/identity/IdentityTestFakes.kt` — deterministic fake platform port and dispatcher helpers used only by JVM tests.
- Create: `core/identity/src/androidTest/java/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityInstrumentedTest.kt` — real test-alias Keystore generation, persistence, signing, non-exportability, security level, concurrency, and cleanup.

### Diagnostics and application integration

- Modify: `feature/diagnostics/build.gradle.kts` — add no identity implementation dependency; retain the model port boundary and existing test dependencies.
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsUiState.kt` — add identity status/metadata state and typed identity inspection error.
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsUiStateReducer.kt` — reduce identity repository results without changing P0 foundation semantics.
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsViewModel.kt` — inspect identity as a structured sibling and preserve successful P0 results.
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsViewModelFactory.kt` — inject `DeviceIdentityRepository`.
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsScreen.kt` — render the bounded Device identity section with short ID only.
- Modify: `feature/diagnostics/src/test/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsUiStateReducerTest.kt` and `DiagnosticsViewModelTest.kt` — cover identity ready, missing, recovery, failure, and sibling preservation.
- Create: `testing/src/main/kotlin/com/r2h/spatiallink/testing/FakeDeviceIdentityRepository.kt` — provide the pure identity fake for diagnostics tests.
- Modify: `testing/src/main/kotlin/com/r2h/spatiallink/testing/CapabilityFixtures.kt` — add deterministic identity fixtures.
- Modify: `app/build.gradle.kts` — depend on `:core:identity` at the composition root.
- Modify: `app/src/main/java/com/r2h/spatiallink/SpatialLinkApplication.kt` — construct the production repository with the locked alias.
- Modify: `app/src/main/java/com/r2h/spatiallink/MainActivity.kt` — pass identity repository into the ViewModel factory.
- Modify: `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt` — assert identity section and preserve P0 UI/recreation coverage.

### Documentation and evidence

- Modify: `README.md` — document P1 identity boundaries, alias, signing contract, and test commands.
- Modify: `docs/architecture/FOUNDATION.md` — extend the module graph, identity boundary, lifecycle, security, and P1/P2 boundary.

---

### Task 1: Add pure identity domain values and repository contracts

**Files:**
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/IdentityModels.kt`
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/SpatialDeviceId.kt`
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/DeviceIdentity.kt`
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/IdentitySignature.kt`
- Create: `core/model/src/main/kotlin/com/r2h/spatiallink/model/IdentityPorts.kt`
- Create: `core/model/src/test/kotlin/com/r2h/spatiallink/model/SpatialDeviceIdTest.kt`
- Create: `core/model/src/test/kotlin/com/r2h/spatiallink/model/IdentityModelTest.kt`

**Interfaces:**

Produce these pure Kotlin interfaces and values:

```kotlin
enum class IdentityAlgorithm { ECDSA_P256_SHA256 }
enum class KeySecurityLevel { STRONGBOX, TRUSTED_ENVIRONMENT, SOFTWARE, UNKNOWN_SECURE, UNKNOWN }
enum class IdentitySelfTestStatus { NOT_RUN, PASSED, FAILED }
enum class SignatureVerification { VERIFIED, REJECTED }

enum class IdentityFailureCode {
    NOT_FOUND,
    GENERATION_FAILED,
    KEYSTORE_UNAVAILABLE,
    INVALID_KEY_ENTRY,
    PUBLIC_KEY_UNAVAILABLE,
    SIGNING_FAILED,
    RECOVERY_REQUIRED,
    PLATFORM_FAILURE,
}

data class IdentityFailure(val code: IdentityFailureCode, val detail: String? = null)

sealed interface IdentityResult<out T> {
    data class Success<T>(val value: T) : IdentityResult<T>
    data class Failure(val error: IdentityFailure) : IdentityResult<Nothing>
}

sealed interface IdentityRepositoryResult {
    data class Available(val inspection: IdentityInspection) : IdentityRepositoryResult
    data object NotCreated : IdentityRepositoryResult
    data class Failed(val error: IdentityFailure) : IdentityRepositoryResult
}
```

`SpatialDeviceId.fromEncodedPublicKey()` must hash the exact supplied public-key
encoding with SHA-256, store a defensive copy, expose `toByteArray()`,
`canonicalHex()`, and `shortDisplay()`, and implement content equality/hash.
`IdentityPublicKey` and `IdentitySignature` must make the same defensive-copy
guarantee. `DeviceIdentity` must reject an ID that does not equal the digest of
its public-key encoding.

- [ ] **Step 1: Write the failing immutable-domain tests.**

Use a literal public-key encoding fixture, mutate the source after construction,
mutate the returned arrays, and assert the ID remains unchanged. Assert the
hand-derived SHA-256 digest, exact uppercase canonical hex, `XXXX-XXXX-XXXX`
short display, equality/hash behavior, mismatched identity rejection, and
signature byte immutability. The tests must not call production digest helpers
to compute their expected value.

```kotlin
@Test
fun canonical_id_is_sha256_of_the_encoded_public_key() {
    val encoded = byteArrayOf(0x30, 0x03, 0x01, 0x01, 0x00)

    val id = SpatialDeviceId.fromEncodedPublicKey(encoded)

    assertEquals(
        "139A28812E7519F744D2D3BF3C6009155566F1C52DF8605F822D7714F1477AFA",
        id.canonicalHex(),
    )
}
```

- [ ] **Step 2: Run the focused tests and confirm the intended RED failure.**

Run:

```powershell
.\gradlew.bat :core:model:test --tests '*SpatialDeviceIdTest' --console=plain
```

Expected result: compilation fails because the new identity types do not yet
exist. If the test unexpectedly passes, replace the fixture/assertion so it
exercises the new behavior.

- [ ] **Step 3: Implement the minimal pure Kotlin values.**

Use `MessageDigest.getInstance("SHA-256")`, copy every `ByteArray` at the
boundary, format canonical hex with two uppercase characters per byte, and
format the short ID from the first six digest bytes in three four-character
groups. Keep all Android and external crypto-provider integration out of these
files; the standard-library digest is the only Java API used by the model
value.

- [ ] **Step 4: Run the model tests and the full model module.**

Run:

```powershell
.\gradlew.bat :core:model:test --console=plain
```

Expected result: the new and existing model tests pass with zero failures,
errors, or skipped tests.

---

### Task 2: Extend injected dispatchers and scaffold `:core:identity`

**Files:**
- Modify: `settings.gradle.kts`
- Create: `core/identity/build.gradle.kts`
- Modify: `core/common/src/main/kotlin/com/r2h/spatiallink/common/DispatcherProvider.kt`
- Modify: `testing/src/main/kotlin/com/r2h/spatiallink/testing/TestDispatcherProvider.kt`
- Modify: `core/common/src/test/kotlin/com/r2h/spatiallink/common/DispatcherProviderTest.kt`
- Modify: `testing/src/test/kotlin/com/r2h/spatiallink/testing/TestDispatcherProviderTest.kt`

**Interfaces:**

`DispatcherProvider` produces `default` and `io` `CoroutineDispatcher` values;
`DefaultDispatcherProvider.io` is `Dispatchers.IO`; the test provider defaults
`io` to the supplied test dispatcher. `:core:identity` applies
`spatiallink.android.library`, depends on `:core:model`, `:core:common`, and
`kotlinx-coroutines-core`, and uses existing AndroidX test aliases only for
instrumentation.

- [ ] **Step 1: Add the dispatcher contract test for I/O.**

Extend the existing dispatcher tests with `assertSame` for the supplied I/O
dispatcher and a test-provider default case. Run:

```powershell
.\gradlew.bat :core:common:test :testing:test --console=plain
```

Expected result: RED compilation because `DispatcherProvider.io` is absent.

- [ ] **Step 2: Add the minimal `io` property and module include.**

Set `override val io: CoroutineDispatcher = Dispatchers.IO` in the production
provider, add the property to the test provider, include `:core:identity`, and
create its build file without adding any forbidden library.

- [ ] **Step 3: Run the focused shared tests and module configuration.**

Run:

```powershell
.\gradlew.bat :core:common:test :testing:test :core:identity:tasks --console=plain
```

Expected result: PASS with the identity Android library and its test tasks
visible, and no dependency-resolution error.

---

### Task 3: Implement repository orchestration through a fake platform port

**Files:**
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/IdentityKeyStorePort.kt`
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityRepository.kt`
- Create: `core/identity/src/test/kotlin/com/r2h/spatiallink/identity/IdentityTestFakes.kt`
- Create: `core/identity/src/test/kotlin/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityRepositoryTest.kt`

**Interfaces:**

Define the Android-free boundary used by the repository:

```kotlin
data class PlatformIdentityEntry(
    val algorithm: String,
    val isP256: Boolean,
    val encodedPublicKey: ByteArray,
    val securityLevel: KeySecurityLevel,
)

interface IdentityKeyStorePort {
    suspend fun read(alias: String): PlatformIdentityEntry?
    suspend fun generate(alias: String): PlatformIdentityEntry
    suspend fun sign(alias: String, payload: ByteArray): ByteArray
    suspend fun verify(encodedPublicKey: ByteArray, payload: ByteArray, signature: ByteArray): Boolean
}

class AndroidKeyStoreIdentityRepository(
    private val alias: String = "spatiallink.identity.signing.v1",
    private val keyStore: IdentityKeyStorePort,
    private val dispatchers: DispatcherProvider,
    private val challengeSource: () -> ByteArray,
) : DeviceIdentityRepository
```

The production constructor will provide defaults for the concrete adapter,
dispatcher, and `SecureRandom` challenge source after Task 4. The repository
must validate algorithm `EC`, `isP256`, nonempty public encoding, and the ID
invariant; use one `Mutex`; rethrow cancellation; map all other failures to
typed `IdentityFailure` values; and never call a delete or rotate operation.

- [ ] **Step 1: Write failing repository tests against the wished-for API.**

Add tests for:

1. `get()` returns `NotCreated` and does not call `generate`;
2. `getOrCreate()` generates once, returns `Available`, and self-tests;
3. an existing entry is loaded with zero generation calls;
4. an invalid existing entry returns `INVALID_KEY_ENTRY` and leaves the fake
   generation/deletion counters unchanged;
5. generation exceptions return `GENERATION_FAILED`;
6. self-test verification failure returns `RECOVERY_REQUIRED` without retry;
7. twenty concurrent callers cause one generation;
8. cancellation is not converted into a typed product failure.

Use a synchronized fake port that counts calls and returns a fixed public-key
encoding. Assert returned domain values, not fake call presence alone.

- [ ] **Step 2: Run the repository tests to observe the expected RED failure.**

Run:

```powershell
.\gradlew.bat :core:identity:test --tests '*AndroidKeyStoreIdentityRepositoryTest' --console=plain
```

Expected result: compilation fails because the platform port and repository
implementation are not present.

- [ ] **Step 3: Implement the minimum repository orchestration.**

Use `withContext(dispatchers.io) { mutex.withLock { ... } }` for each operation.
Implement `loadEntry()` with typed validation, `get()` without generation,
`getOrCreate()` with one missing-entry generation, and an internal self-test
that signs a fresh challenge and verifies it without persistence. Construct
`DeviceIdentity` only from copied public bytes; return a safe typed error for
every non-cancellation exception.

- [ ] **Step 4: Run the repository tests and inspect the diff.**

Run:

```powershell
.\gradlew.bat :core:identity:test --tests '*AndroidKeyStoreIdentityRepositoryTest' --console=plain
```

Expected result: all repository orchestration tests pass. Review the diff for
private-key fields, delete/rotate calls, raw throwable propagation, and any
unbounded dispatcher use before continuing.

---

### Task 4: Add typed Android Keystore and API-guarded security adapters

**Files:**
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/AndroidKeyStorePlatformAdapter.kt`
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/KeySecurityLevelInspector.kt`
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/Api31KeySecurityLevelReader.kt`
- Create: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/LegacyKeySecurityLevelReader.kt`
- Modify: `core/identity/src/main/kotlin/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityRepository.kt`
- Modify: `core/identity/src/test/kotlin/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityRepositoryTest.kt`

**Interfaces:**

The concrete adapter owns all Android Keystore and JCA private-key operations.
It must use:

```kotlin
KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
    .setDigests(KeyProperties.DIGEST_SHA256)
    .setUserAuthenticationRequired(false)
```

Key generation and storage use `AndroidKeyStore`. Signing uses the
platform-default `Signature.getInstance("SHA256withECDSA")` with the
AndroidKeyStore-backed private key because API 34 does not expose that
algorithm by name under the `AndroidKeyStore` provider; verification uses the
public key encoding and the same signature algorithm.
No `setIsStrongBoxBacked`, attestation challenge, imported key, private
`encoded` access, or `createRangingSession`-like unrelated behavior is added.

`Api31KeySecurityLevelReader` is annotated `@RequiresApi(31)` and directly
maps `KeyInfo.getSecurityLevel()`. `LegacyKeySecurityLevelReader` maps
`KeyInfo.isInsideSecureHardware` to `UNKNOWN_SECURE` or `SOFTWARE`. The
selector checks `Build.VERSION.SDK_INT` before invoking the API 31 class and
maps inspection exceptions to `UNKNOWN`.

- [ ] **Step 1: Add boundary-focused failing tests for the concrete contract.**

Extend the repository tests with a fake-entry test proving wrong algorithm,
wrong curve flag, and empty public encoding are rejected without rotation.
Add a focused source-level test fixture for security mapping only if the
installed SDK exposes the API 31 constants; the real `KeyInfo` behavior is
covered by connected instrumentation in Task 7.

- [ ] **Step 2: Run the new focused tests and confirm RED.**

Run:

```powershell
.\gradlew.bat :core:identity:test --tests '*AndroidKeyStoreIdentityRepositoryTest' --console=plain
```

Expected result: the new boundary assertions fail because the concrete
adapter/validation paths are not complete.

- [ ] **Step 3: Implement the typed Keystore adapter.**

Load `AndroidKeyStore` per operation, return `null` only for an absent alias,
require a `KeyStore.PrivateKeyEntry` and certificate, derive the public key
encoding from `certificate.publicKey.encoded`, validate EC/P-256 structure,
and map provider/security exceptions to internal typed failures consumed by
the repository. Keep `PrivateKey` variables local to `sign` and security
inspection. Use an independent EC parameter comparison to confirm
`secp256r1`; do not rely on manufacturer or model data.

- [ ] **Step 4: Implement API-guarded security inspection.**

Use a separate `@RequiresApi(31)` reader for `securityLevel` constants and a
legacy reader for `isInsideSecureHardware`. Invoke the modern reader only on
API 31+, preserve valid identities when inspection maps to `UNKNOWN`, and do
not make StrongBox a readiness requirement.

- [ ] **Step 5: Run JVM tests, compile all variants, and inspect references.**

Run:

```powershell
.\gradlew.bat :core:identity:test :core:identity:assembleDebug --console=plain
```

Then search non-generated source:

```powershell
rg -n --glob '!**/build/**' --glob '!**/.gradle/**' 'Class\.forName|java\.lang\.reflect|Method\.invoke|setIsStrongBoxBacked|setAttestationChallenge|privateKey\.encoded|deleteEntry|spatiallink\.identity\.signing\.v1' core\identity
```

The only allowed matches are the fixed alias declaration, test-only
non-exportability observation in Task 7, and test-only cleanup of dynamic
test aliases. Production code must contain no private-byte extraction or
production-alias deletion.

---

### Task 5: Add diagnostics identity state and ViewModel orchestration

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsUiState.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsUiStateReducer.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsViewModel.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsViewModelFactory.kt`
- Modify: `feature/diagnostics/src/test/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsUiStateReducerTest.kt`
- Modify: `feature/diagnostics/src/test/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsViewModelTest.kt`
- Create/modify: `testing/src/main/kotlin/com/r2h/spatiallink/testing/FakeDeviceIdentityRepository.kt`
- Modify: `testing/src/main/kotlin/com/r2h/spatiallink/testing/CapabilityFixtures.kt`

**Interfaces:**

Add:

```kotlin
enum class IdentityStatus { LOADING, READY, NOT_CREATED, UNAVAILABLE, RECOVERY_REQUIRED, ERROR }

data class IdentityUiState(
    val status: IdentityStatus,
    val shortId: String?,
    val algorithm: String?,
    val securityLevel: String?,
    val selfTest: IdentitySelfTestStatus,
    val failureCode: IdentityFailureCode?,
)
```

`DiagnosticsUiState` gains a non-null `identity` initialized to loading.
`DiagnosticsUiStateReducer` maps `IdentityRepositoryResult.Available` to READY,
`NotCreated` to NOT_CREATED, `RECOVERY_REQUIRED` to RECOVERY_REQUIRED, and
other typed failures to UNAVAILABLE. An unexpected identity inspection
exception is `DiagnosticsError.IdentityInspectionFailed`. P0 capability and
permission results remain preserved when identity inspection fails.

- [ ] **Step 1: Write failing reducer tests for identity states.**

Add tests for loading, available identity with short ID/algorithm/security/
self-test values, `NotCreated`, recovery-required, unavailable typed failure,
and identity inspection failure while capabilities and permissions remain
present. Assert that no `Throwable` is stored in UI state.

- [ ] **Step 2: Run the feature tests and observe RED.**

Run:

```powershell
.\gradlew.bat :feature:diagnostics:test --console=plain
```

Expected result: compilation or assertion failure because the identity state
and reducer path do not yet exist.

- [ ] **Step 3: Implement the state/reducer mapping.**

Keep identity status mapping in the reducer, use `DeviceIdentity.id.shortDisplay()`
only for UI state, and retain `failureCode` without raw exception data. Keep
foundation-status derivation based on the existing P0 capability snapshot;
identity validity and key protection remain separate.

- [ ] **Step 4: Write and run the failing ViewModel sibling test.**

Use the real `DiagnosticsViewModel` with fake capability, permission, and
identity ports. Assert that identity inspection runs, the state reaches a
typed result, and capability/permission success survives an identity failure.
Run the focused test before implementation and confirm it fails for the
missing constructor/inspection path.

- [ ] **Step 5: Implement structured identity inspection and factory injection.**

Add one `async { inspectIdentity() }` sibling under the existing
`supervisorScope`, rethrow `CancellationException`, reduce the three results
without discarding successful fields, and pass the repository through the
factory. Do not call the repository from a Composable.

- [ ] **Step 6: Run the full diagnostics test module.**

Run:

```powershell
.\gradlew.bat :feature:diagnostics:test --console=plain
```

Expected result: all existing P0 tests and new P1 state/ViewModel tests pass.

---

### Task 6: Render identity diagnostics and wire the production composition root

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/DiagnosticsScreen.kt`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/r2h/spatiallink/SpatialLinkApplication.kt`
- Modify: `app/src/main/java/com/r2h/spatiallink/MainActivity.kt`
- Modify: `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt`

**Interfaces:**

Render a new `Device identity` heading followed by bounded status rows:
`Identity status`, `Spatial ID`, `Algorithm`, `Key protection`, and `Self-test`.
Show the short ID only; use readable text for null/failed values. Reuse the
existing weighted `StatusRow` with a fixed spacing and right-aligned value so
long `NOT_REQUIRED_ON_THIS_OS` permission values and identity statuses cannot
overlap. The application container constructs
`AndroidKeyStoreIdentityRepository` with the production alias and injects it
into the factory; `Application.onCreate()` still performs no inspection.

- [ ] **Step 1: Add the failing Compose/instrumentation assertions.**

Extend the existing connected test to assert that the `Device identity`
heading and `Identity status` row are present after launch. Add an isolated
screen test state with a long failure/status value if the existing test
structure supports it; otherwise use the existing UI hierarchy bounds check
for the identity row alongside the P0 permission row.

- [ ] **Step 2: Run the focused app instrumentation test and observe RED.**

Run on the already authorized device only:

```powershell
adb devices -l
.\gradlew.bat :app:connectedDebugAndroidTest --tests '*DiagnosticsInstrumentedTest' --console=plain
```

Expected result before implementation: the test cannot find the new identity
heading. Do not create an emulator or alter device settings.

- [ ] **Step 3: Implement the screen and composition wiring.**

Add only the identity section, add `implementation(project(":core:identity"))`
to `:app`, construct the repository in `AppContainer`, and pass it through
`DiagnosticsViewModelFactory`. Do not add permissions, storage, networking,
or startup requests.

- [ ] **Step 4: Run the app connected tests and inspect the merged package.**

Run:

```powershell
.\gradlew.bat :app:assembleDebug :app:connectedDebugAndroidTest --console=plain
```

Inspect the test XML for the intended `DiagnosticsInstrumentedTest` class,
record executed/pass/fail/skip counts, and confirm the merged manifest has no
new permission declaration.

---

### Task 7: Add real physical-device Keystore instrumentation and persistence evidence

**Files:**
- Create: `core/identity/src/androidTest/java/com/r2h/spatiallink/identity/AndroidKeyStoreIdentityInstrumentedTest.kt`
- Modify: `core/identity/build.gradle.kts`
- Modify: `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt` — preserve launch/recreation assertions and add identity-section evidence.

**Interfaces:**

Use one unique test alias per test method, for example
`spatiallink.test.identity.keystore.<testName>.<nanoTime>`, and delete only that
alias in `finally`. Exercise the real `AndroidKeyStoreIdentityRepository` and
`AndroidKeyStorePlatformAdapter` on the connected physical device. For
non-exportability, load the test alias with `KeyStore.getEntry` and assert
`privateEntry.privateKey.encoded == null` without printing the result.

- [ ] **Step 1: Write the connected tests before adding their production test wiring.**

Add tests for:

1. `get()` reports `NotCreated` for a fresh test alias;
2. `getOrCreate()` returns `Available`, then a new repository instance loads
   the same full canonical ID;
3. `sign()` followed by `verify()` returns `VERIFIED`, and modified payload
   returns `REJECTED`;
4. private-key encoding is unavailable through the normal API;
5. security level is one of the typed domain values and does not gate READY;
6. twenty concurrent callers result in one stable identity;
7. only the generated test alias is deleted in cleanup.

Do not log private keys, signatures, challenge bytes, or unrelated device
identifiers. Print the full canonical ID only through a bounded, explicitly
named instrumentation evidence line for the final persistence comparison; it
is public-key-derived identity evidence, not private material.

- [ ] **Step 2: Run the connected identity tests and capture the expected RED result.**

Run:

```powershell
adb devices -l
.\gradlew.bat :core:identity:connectedDebugAndroidTest --console=plain
```

Expected result before the adapter/test wiring is complete: the task fails to
compile or the test class is unavailable. Confirm the failure is caused by the
missing P1 implementation, not by an unauthorized/offline device.

- [ ] **Step 3: Wire the real adapter and test dependencies, then run connected tests.**

Use the existing AndroidX test runner and JUnit aliases. Install only the
SpatialLink app/test packages through Gradle. Run the same connected command,
inspect the generated XML for the intended test class, and record exact counts.

- [ ] **Step 4: Perform non-destructive app persistence qualification.**

Install the current debug APK, launch the app with the safe launcher command,
capture the identity section and full canonical ID through the instrumentation
seam, then run `adb shell am force-stop com.r2h.spatiallink` and relaunch. Do
not clear data, uninstall, reboot, change Wi-Fi/Bluetooth settings, or inspect
personal content. Confirm the canonical ID is identical and no startup
permission controller is visible.

- [ ] **Step 5: Perform bounded logcat and UI evidence capture.**

Clear only logcat, launch once, capture a bounded logcat file and UI hierarchy,
and search for:

```text
FATAL EXCEPTION
AndroidRuntime
KeyStoreException
ProviderException
InvalidKeyException
UnrecoverableKeyException
VerifyError
NoClassDefFoundError
ClassNotFoundException
SecurityException
IllegalStateException
```

Classify harmless platform informational logs separately. Confirm no private
key/signature/challenge material appears and no runtime permission dialog is
shown.

---

### Task 8: Update architecture documentation and run scope/privacy scans

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture/FOUNDATION.md`

**Interfaces:**

Document the new module graph, locked cryptographic algorithm/alias, pure
model boundary, Keystore security mapping, concurrency/self-test/recovery
semantics, no-backup behavior, diagnostics rows, physical qualification, and
the explicit P2 boundary. Do not rewrite P0 design decisions or claim P1
qualification before the commands in Task 9 have passed.

- [ ] **Step 1: Write the documentation changes from the implemented APIs.**

Keep the existing P0 statements about capability absence, permission
inspection, typed API 36/37 ranging, no `androidx.core.uwb`, and no startup
request. Add P1 identity as the only new phase behavior and state that P2 BLE
Peer Discovery is not included.

- [ ] **Step 2: Run static architecture/privacy scans.**

Run these from the project root, excluding generated/build trees:

```powershell
rg -n --glob '!**/build/**' --glob '!**/.gradle/**' 'android\.|androidx\.|Context|Activity|ViewModel|Compose|KeyStore|RangingManager' core\model
rg -n --glob '!**/build/**' --glob '!**/.gradle/**' 'androidx\.core\.uwb|BouncyCastle|bouncycastle|Tink|libsodium|Firebase|Retrofit|OkHttp|Ktor|Hilt|Dagger|Koin|Room|SQLDelight|protobuf|ONNX|ML|GlobalScope|runBlocking|Class\.forName|Method\.invoke' .
rg -n --glob '!**/build/**' --glob '!**/.gradle/**' 'ANDROID_ID|Settings\.Secure|Build\.SERIAL|BLUETOOTH_ADDRESS|deviceAddress|MAC|IMEI|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|MANAGE_EXTERNAL_STORAGE|discoverPeers|createRangingSession|NfcAdapter|deleteEntry' app core feature testing
```

Review every match. `core:model` must have no output for the Android/AndroidX
scan; test-only Keystore cleanup and non-exportability assertions must be
isolated and limited to dynamic test aliases; no production alias deletion or
P1+ behavior may remain.

- [ ] **Step 3: Inspect dependencies and generated manifests.**

Run:

```powershell
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath --console=plain
.\gradlew.bat :app:processDebugMainManifest :core:identity:processDebugManifest --console=plain
```

Confirm no forbidden dependency, no `androidx.core.uwb`, no new dangerous
permission, and the identity module has no personal-data or network access.

---

### Task 9: Run the fresh final P1 verification suite and complete the evidence report

**Files:** No source changes are planned unless a fresh gate identifies a real
defect; any defect fix must follow a new failing regression test, minimum fix,
focused rerun, connected rerun, and complete final rerun.

- [ ] **Step 1: Confirm the locked toolchain and exactly one physical device.**

Run sequentially:

```powershell
& 'D:\R2H-Dev\Java\jdk-17\bin\java.exe' -version
Get-Command adb
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.cpu.abi
```

Proceed only if exactly one intended physical device is in state `device` and
no emulator/AVD is created. Record Android version, API, ABI, and a model label
only as test metadata; never use serial as identity data.

- [ ] **Step 2: Run the mandatory fresh JVM/build/lint gates in order.**

Run each command only after the preceding one exits:

```powershell
.\gradlew.bat clean --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain
.\gradlew.bat lint --console=plain
```

Require exit code 0 for every command. Read the generated JVM XML and sum
`tests`, `failures`, `errors`, and `skipped`; report exact counts without
inventing them.

- [ ] **Step 3: Install, launch, and inspect the physical debug build.**

Run:

```powershell
.\gradlew.bat :app:installDebug --console=plain
adb shell pm list packages | findstr com.r2h.spatiallink
adb shell am force-stop com.r2h.spatiallink
adb shell monkey -p com.r2h.spatiallink -c android.intent.category.LAUNCHER 1
```

Capture a UI hierarchy and verify the identity section, short ID, algorithm,
key protection, self-test, and foundation status. Confirm that no permission
controller dialog appears for Bluetooth, Nearby Wi-Fi, Ranging, or Local
Network. Use the connected identity instrumentation seam for the full
canonical-ID persistence comparison.

- [ ] **Step 4: Run the complete connected suite and verify intended XML.**

Run:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

If identity-module connected tests are not included by the root task, also run:

```powershell
.\gradlew.bat :core:identity:connectedDebugAndroidTest --console=plain
```

Inspect each result XML for the expected test classes and record executed,
passed, failed, and skipped. A green stale filter or a task that executed zero
tests is not sufficient.

- [ ] **Step 5: Re-run the complete mandatory final commands after device tests.**

Run exactly:

```powershell
.\gradlew.bat clean --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain
.\gradlew.bat lint --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Every applicable command must pass on the final source tree. Inspect the
artifact/package and connected result outputs immediately after each long
command.

- [ ] **Step 6: Produce the required P1 evidence report.**

Include sections for Executive Summary, Environment, Module Changes, Identity
Design, Keystore Verification, SpatialDeviceId Verification, Signing
Verification, Concurrency, Error/Recovery, JVM Tests, Connected Tests, Build
Gates, Physical Qualification, P0 Regression, Privacy/Security Scan, Files
Created/Modified, Problems/Fixes, Limitations, Git Status, and P2 Readiness.
Distinguish direct evidence from source inspection and state that API 36/37
physical execution is not required when compile/test isolation is verified.
End with exactly `P1_IDENTITY_COMPLETE` only if every mandatory criterion has
fresh evidence; otherwise end with `P1_IDENTITY_BLOCKED` and the exact
unresolved defect or external blocker.
