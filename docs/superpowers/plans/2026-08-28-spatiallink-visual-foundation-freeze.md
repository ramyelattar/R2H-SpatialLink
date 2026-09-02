# SpatialLink Visual Foundation Freeze Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the final fidelity gaps in the accepted SpatialLink composition by strengthening the Trust Seal, darkening and differentiating brass materials, tightening field placement, clarifying subsystem connections, refining cyan energy, enforcing technical typography, and separating the gateway from the instrumentation rail.

**Architecture:** Preserve the existing `SpatialLinkShell` destination structure and `DiagnosticsUiState` flow. Change only presentation tokens, Compose Canvas primitives, presentation spacing/copy, and regression tests; capability, permission, identity, and platform adapter code remains untouched.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Canvas/vector drawing, Android instrumented UI tests, Gradle, ADB physical-device verification.

**Spec:** `docs/superpowers/specs/2026-08-28-spatiallink-flagship-ui-design.md` plus the approved final visual foundation requirements in the user request.

## Global Constraints

- Keep the accepted oversized clipped Spatial Field and the existing Overview, Field/System, and Identity page hierarchy.
- Do not modify P0/P1 domain behavior, capability detection, permission policies, Android Keystore identity, or SpatialDeviceId derivation.
- Do not add P2+ behavior, networking, discovery, transfer, NFC pairing, ranging sessions, analytics, or dependencies.
- Keep serif typography only for `Spatial field` and artifact-level identity moments; all technical labels, statuses, metadata, navigation, and CTA metadata use `FontFamily.SansSerif`.
- Use efficient Canvas passes and bounded animation; do not add shader, blur, particle-engine, or 3D rendering.
- Maintain real-phone bounds, semantics, accessibility minimum targets, and scroll behavior.
- Use only the authorized physical device for runtime verification; do not create an emulator or modify personal device data/settings beyond the already-approved package install flow.

---

### Task 1: Add failing visual-contract regression assertions

**Files:**
- Modify: `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt`

**Interfaces:**
- Consumes: existing Overview semantics for `Spatial field stage with anchored system signals`, `Spatial field trust seal`, `OPEN FIELD`, and `OVERVIEW`.
- Produces: device-executable checks for Trust Seal scale and the minimum quiet transition from the field stage to the gateway.

- [x] **Step 1: Write the failing tests**

  Add tests that measure the existing semantics and assert:

  ```kotlin
  @Test
  fun overview_trust_seal_has_artifact_scale() {
      val root = composeRule.onRoot().getUnclippedBoundsInRoot()
      val seal = composeRule
          .onNodeWithContentDescription("Spatial field trust seal")
          .getUnclippedBoundsInRoot()

      assertTrue(
          "the Overview trust seal must read as a primary artifact",
          seal.width >= root.width * 0.48f,
      )
  }

  @Test
  fun overview_gateway_has_a_quiet_transition_to_the_rail() {
      val gateway = composeRule
          .onNodeWithContentDescription("OPEN FIELD")
          .getUnclippedBoundsInRoot()
      val rail = composeRule
          .onNodeWithContentDescription("OVERVIEW")
          .getUnclippedBoundsInRoot()

      assertTrue(
          "the gateway must have a quiet transition before the instrumentation rail",
          rail.top - gateway.bottom >= 28.dp,
      )
  }
  ```

- [ ] **Step 2: Run the focused tests and verify the old implementation fails**

  Run the connected test filter for the two new methods:

  ```powershell
  .\gradlew.bat :app:connectedDebugAndroidTest --tests com.r2h.spatiallink.DiagnosticsInstrumentedTest.overview_trust_seal_has_artifact_scale --console=plain
  .\gradlew.bat :app:connectedDebugAndroidTest --tests com.r2h.spatiallink.DiagnosticsInstrumentedTest.overview_field_has_a_quiet_transition_before_the_gateway --console=plain
  ```

  Expected result: the current 154dp seal and compressed gateway-to-rail transition fail the new assertions, proving the tests detect the intended remaining gaps.

### Task 2: Rebuild the Trust Seal and brass material hierarchy

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkDesignTokens.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPrimitives.kt`

**Interfaces:**
- Consumes: existing `StatusMarkModel`, `SpatialLinkTone`, `TrustSeal`, `SpatialField`, and Canvas helpers.
- Produces: unchanged composable signatures with a larger, dimensional, original SpatialLink artifact and darker differentiated structural materials.

- [x] **Step 1: Add antique-brass material tokens**

  Add or tune named colors for dark antique brass, muted bronze oxide, edge brass, and peak specular highlight. Keep `BrassSpecular` reserved for short highlight passes rather than the full structural ring body. Do not change semantic status colors.

- [x] **Step 2: Increase the field seal scale without changing page structure**

  Increase only the `TrustSeal` layer inside `SpatialField` to at least 176dp and keep the Identity page seal within its existing row bounds. Do not change navigation destinations or data flow.

- [x] **Step 3: Implement the layered Trust Seal**

  In `TrustSeal`, draw in this order: offset black recess, dark bronze outer plate, oxide base rim, narrow champagne edge, restrained specular peak, cyan inner-energy arc, inner calibration ticks, cardinal geometry, and the original SL lock/orbit monogram with a shield accent. Use separate shadow and highlight passes so the center reads as machined rather than a flat stroked circle.

- [x] **Step 4: Reweight field ring classes**

  Update `drawFieldInstrument` so structural rings use dark antique brass plus a narrow champagne edge, secondary rings use muted bronze at lower opacity, calibration rings use faint technical lines and ticks, and inner mechanical rings use offset recess/highlight layers. Keep the partial arc as the only energized field section.

- [x] **Step 5: Improve cyan energy without global neon**

  Retain the existing partial arc geometry and add a dark cyan foundation, localized soft bloom, narrow bright core, deterministic intensity variation, and exactly 2–3 visually dominant energized nodes. Keep animation low-frequency and bounded.

### Task 3: Clarify field anchors, spacing, and strict typography roles

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPages.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPrimitives.kt`

**Interfaces:**
- Consumes: the existing Overview models and anchor semantics.
- Produces: the same content descriptions and routes with explicit instrument → connector → node relationships, moderated hero/field spacing, and a calmer field → gateway → rail sequence.

- [x] **Step 1: Move the field moderately upward**

  Reduce only the Overview hero-to-stage spacer and move the existing field layer upward by a bounded offset. Keep enough top breathing room for the wordmark, title, and status block.

- [x] **Step 2: Give subsystem labels a quiet pocket**

  Keep `FieldAnchor` as the visible subsystem instrument and alter only its local placement/connector treatment so each instrument has a readable quiet pocket, a fine connector line, and a distinct anchor node before the ring geometry. Preserve content descriptions and status values.

- [x] **Step 3: Increase gateway-to-rail separation**

  Preserve `SpatialCommand` height and cut-corner geometry. Increase only the vertical transition after the gateway and before the artifact/rail so the CTA remains substantial without becoming a standard button or being shrunk to fit.

- [x] **Step 4: Enforce the sans-serif technical layer**

  Audit the modified presentation roles and retain `FontFamily.Serif` only on `Spatial field` and artifact-level identity titles/monogram. Ensure `SPATIALLINK`, `READY`, subsystem labels/statuses, metadata, CTA support, and navigation remain explicit `FontFamily.SansSerif` with deliberate tracking.

### Task 4: Run focused green tests, physical visual QA, and final regression gates

**Files:**
- Modify: `design-qa.md`
- Evidence: fresh physical-device Overview, field close-up, Identity, and Field/System screenshots plus UI hierarchies under `C:/Users/R2H/AppData/Local/Temp/`.

**Interfaces:**
- Consumes: current debug APK, authorized Android 14/API 34 physical device, approved reference image, and existing P0/P1 test suite.
- Produces: fresh comparison evidence, bounded logcat audit, and a final QA report with an explicit visual/regression verdict.

- [x] **Step 1: Compile and run the focused green tests**

  Run the affected Kotlin compile and the two new connected tests. Expected result: both pass after the minimum presentation changes, with no production/domain test changes.

- [x] **Step 2: Install and capture a fresh physical build**

  Confirm exactly one `adb devices -l` entry is online, install only `com.r2h.spatiallink` and its legitimate test packages, launch the app without requesting permissions, and capture Overview, field close-up, Identity, and Field/System screenshots and UI hierarchies.

- [x] **Step 3: Compare the same viewport against the approved reference**

  Inspect the paired reference/Overview input and separately inspect the close-up. Verify Trust Seal authority, dark brass depth, cyan arc/nodes, connector clarity, hero/field spacing, gateway/rail separation, sans-serif technical roles, no clipping, and no overlap. Reject the pass if any of those criteria remains materially false.

- [x] **Step 4: Audit runtime behavior and privacy boundary**

  Clear only logcat, launch the installed build, scan the bounded output for SpatialLink-associated fatal/class-loading/security/illegal-state failures, and confirm no ranging session, discovery, transfer, personal-data access, or startup permission request was introduced.

- [x] **Step 5: Run the complete fresh regression suite**

  Run exactly:

  ```powershell
  .\gradlew.bat clean --console=plain
  .\gradlew.bat test --console=plain
  .\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain
  .\gradlew.bat lint --console=plain
  .\gradlew.bat :app:connectedDebugAndroidTest --console=plain
  .\gradlew.bat :core:identity:connectedDebugAndroidTest --console=plain
  ```

  Record actual exit codes and test counts. Do not weaken or ignore failures.

- [x] **Step 6: Update design QA only after evidence review**

  Record the changed files, visual comparison, device metadata, test/build/lint results, architecture/privacy scans, and any environmental MIUI prompt/App Lock observations. End with exactly `final result: passed` only when all requested gates pass.

## Completion Checklist

- [x] Accepted page structure remains unchanged.
- [x] Trust Seal is larger, dimensional, and recognizable as an original SpatialLink artifact.
- [x] Brass reads as dark antique instrumentation with differentiated ring weights.
- [x] Cyan energy is localized, varied, and intentionally node-driven without neon overload.
- [x] Field placement, subsystem pockets, gateway spacing, and typography roles pass physical review.
- [x] Fresh physical screenshots and UI hierarchies are captured and inspected against the reference.
- [x] JVM, debug/release, lint, app connected, and identity connected gates pass.
- [x] P0/P1 behavior and privacy architecture remain unchanged; no P2 behavior is introduced.
- [x] `design-qa.md` ends with `final result: passed`.
