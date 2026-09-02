# SpatialLink Material Fidelity Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Raise the accepted SpatialLink Overview composition to luxury spatial instrumentation quality through material depth, a dimensional trust seal, restrained cyan energy, technical typography, and product-facing copy without changing page structure or P0/P1 behavior.

**Architecture:** Keep the existing `ViewModel -> StateFlow -> Compose` flow and the accepted `Overview`, `Field`, and `Identity` destinations. Refine the existing Compose Canvas primitives and page copy only; capability, permission, identity, and platform adapters remain untouched.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Canvas/vector drawing, Material typography as infrastructure, Android instrumented UI tests, Gradle, ADB physical-device verification.

**Spec:** `docs/superpowers/specs/2026-08-28-spatiallink-flagship-ui-design.md` plus the current material-depth requirements in the user-approved visual direction.

## Global Constraints

- Preserve the accepted oversized clipped Spatial Field and three-destination page hierarchy.
- Do not modify capability detection, permission policies, Android Keystore identity, SpatialDeviceId derivation, or P0/P1 domain behavior.
- Do not add P2+ behavior, peer discovery, transfer, NFC pairing, ranging sessions, networking, analytics, or new dependencies.
- Keep technical labels in precision sans-serif; reserve serif typography for the hero title, artifact-level titles, and the seal monogram.
- Use efficient Canvas/vector passes and controlled low-frequency animation; do not add shader, blur, particle-engine, or 3D dependencies.
- Keep Overview product-facing; implementation/debug wording belongs only on Field/System.
- Preserve narrow-phone bounds, font scaling resilience, semantics, and existing testability.

---

### Task 1: Establish the copy and visual-contract regression

**Files:**
- Modify: `app/src/androidTest/java/com/r2h/spatiallink/DiagnosticsInstrumentedTest.kt`

**Interfaces:**
- Consumes: the existing Overview route and `SpatialCommand` semantics.
- Produces: a device-executable assertion that the gateway uses product language and no longer exposes implementation wording.

- [x] **Step 1: Write the failing test**

  Add `overview_gateway_uses_product_language_not_implementation_language` and assert `Spatial systems` is displayed while `Capability surface · inspection only` does not exist.

- [x] **Step 2: Run the test to verify it fails**

  Run the focused physical test through the installed instrumentation APK. The current build fails because `Spatial systems` is absent.

---

### Task 2: Refine the instrument materials and trust seal

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkDesignTokens.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPrimitives.kt`

**Interfaces:**
- Consumes: `StatusMarkModel`, `SpatialLinkTone`, and the existing `SpatialField`, `TrustSeal`, `FieldNode`, and `SpatialCommand` signatures.
- Produces: visually deeper reusable primitives with unchanged public behavior and semantics.

- [x] **Step 1: Add bounded material tokens**

  Add dark bronze, edge highlight, recessed carbon, cyan core, cyan halo, and technical-grid tokens to the existing `SpatialLinkColors` object without changing semantic status mappings.

- [x] **Step 2: Rework the Canvas field in layered passes**

  Update `drawFieldInstrument` to draw recessed backing, unequal structural/secondary/calibration/inner rings, offset shadow and highlight passes, fine axes/ticks, and only a partial cyan energy arc with foundation, glow, core, and energized nodes.

- [x] **Step 3: Replace ordinary field nodes with spatial nodes**

  Render each active/anchor node as halo, dark body, rim, luminous core, and restrained highlight while preserving status-derived color semantics.

- [x] **Step 4: Rebuild `TrustSeal` as a dimensional seal**

  Keep `TrustSeal` as the existing reusable component, but add a recessed inner plate, machined brass rim, cyan inner-energy arc, calibration marks, cardinal geometry, original SL monogram treatment, and small shield motif. Keep private identity material out of the UI.

- [x] **Step 5: Run the affected Kotlin compile**

  Run `./gradlew.bat :feature:diagnostics:compileDebugKotlin` and fix only compilation errors caused by this pass.

---

### Task 3: Integrate anchors, gateway, background, typography, and rail

**Files:**
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPrimitives.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkPages.kt`
- Modify: `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/SpatialLinkDesignTokens.kt`

**Interfaces:**
- Consumes: the unchanged page models and destination callbacks.
- Produces: the same Overview/Field/Identity routes with product-facing presentation only.

- [x] **Step 1: Refine subsystem anchor instruments**

  Replace the plain leading node in `FieldAnchor` with a small shield/link instrument and keep the existing connector line-to-field relationship and content descriptions.

- [x] **Step 2: Refine `SpatialAtmosphere`**

  Add a near-black gradient, subtle radial falloff, and barely visible technical axis/grid marks with no raster texture or heavy effects.

- [x] **Step 3: Refine `SpatialCommand`**

  Keep its click contract and bounds, replace the supporting text with `Spatial systems`, and add layered chamfer borders, recessed surface lighting, a cyan live-line, a spatial glyph, and precise arrow geometry.

- [x] **Step 4: Correct typography roles**

  Verify wordmark, statuses, technical labels, anchors, metadata, CTA support, and navigation use `FontFamily.SansSerif`; leave serif only on conceptual/artifact display text and the seal monogram.

- [x] **Step 5: Refine the custom rail**

  Keep only Overview, Field, and Identity; retain the fine alignment line, calibration marks, and subtle selected-state energy without adding a Material navigation container or pill indicator.

---

### Task 4: Verify device visuals and regression behavior

**Files:**
- Modify: `design-qa.md`
- Evidence: `C:/Users/R2H/AppData/Local/Temp/spatiallink-material-fidelity-overview.png`
- Evidence: `C:/Users/R2H/AppData/Local/Temp/spatiallink-material-fidelity-field.png`
- Evidence: `C:/Users/R2H/AppData/Local/Temp/spatiallink-material-fidelity-identity.png`
- Evidence: `C:/Users/R2H/AppData/Local/Temp/spatiallink-material-fidelity-system.png`

**Interfaces:**
- Consumes: installed debug APK, authorized Android 14/API 34 physical device, approved reference image.
- Produces: fresh screenshots, UI hierarchies, runtime logcat audit, and a passing root design QA report.

- [x] **Step 1: Build and install only SpatialLink artifacts**

  Run `./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`, install only the app and its test APK, and confirm the app package is present.

- [x] **Step 2: Capture and inspect Overview, Field/System, and Identity**

  Launch the app, capture the three routes plus the close-up field crop, dump UI hierarchies, and inspect for depth, hierarchy, copy, bounds, clipping, and accessible labels.

- [x] **Step 3: Run focused and full connected tests**

  Run the focused copy regression, then `./gradlew.bat :app:connectedDebugAndroidTest --console=plain`; count executed, passed, failed, and skipped tests from the output/XML.

- [x] **Step 4: Run fresh final gates**

  Run `./gradlew.bat clean --console=plain`, `./gradlew.bat test --console=plain`, `./gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain`, `./gradlew.bat lint --console=plain`, `./gradlew.bat :app:connectedDebugAndroidTest --console=plain`, and `./gradlew.bat :core:identity:connectedDebugAndroidTest --console=plain` where the task exists.

- [x] **Step 5: Update `design-qa.md` only after comparison**

  Compare the approved reference and fresh implementation captures at the same physical viewport, record the required typography/layout/color/asset/copy surfaces, and end with exactly `final result: passed` only if no actionable P0/P1/P2 finding remains.

---

## Completion Checklist

- [x] Accepted page structure is unchanged.
- [x] Material depth, seal construction, energy arc, nodes, anchors, CTA, typography, background, and rail meet the approved fidelity target.
- [x] Overview contains no implementation/debug wording.
- [x] No P0/P1 behavior or architecture changed.
- [x] Physical screenshots and UI hierarchies are captured and inspected.
- [x] JVM, build, lint, connected app tests, and identity connected tests pass or an exact tooling limitation is reported.
- [x] Design QA ends with `final result: passed`.
