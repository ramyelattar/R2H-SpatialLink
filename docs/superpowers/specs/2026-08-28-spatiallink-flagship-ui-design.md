# SpatialLink Flagship UI Design Specification

**Date:** 2026-08-28  
**Status:** Approved visual direction; composition rebuild authorized by the user

## Goal

Turn the existing P0/P1 diagnostics surface into an original SpatialLink
product experience. The application will present a calm, luxury-tech spatial
field first, with capability, permission, and cryptographic identity data
revealed through deliberate secondary surfaces.

The supplied concept image is a visual reference for quality, composition,
material, and typography. It is not an artwork source. SpatialLink will use
its own functional field instrument and its existing truthful state data. The
field is an oversized, intentionally clipped environment rather than a small
radar widget placed inside a conventional page.

## Scope

This redesign changes the presentation layer only:

- preserve `DiagnosticsViewModel -> StateFlow -> Compose`;
- preserve the existing capability, permission, and identity repositories;
- create a bespoke Compose shell with Overview, Field/System, and Identity
  surfaces;
- expose only data already available from P0 capability diagnostics, P0
  permission diagnostics, and P1 cryptographic identity inspection;
- make the radial field instrument reusable and state-driven;
- retain honest loading, partial, unavailable, unknown, and error states;
- keep the app responsive on narrow portrait phones, landscape, and larger
  font scales;
- verify the result with updated connected UI tests and existing JVM/build
  gates.

Transfer is not implemented. Peer discovery, nearby-peer presence, target
acquisition, connection establishment, ranging sessions, transfer progress,
and transfer history remain future product concepts and have no production
behavior in this change.

## Product hierarchy

### Overview

The launch surface is a spatial overview, not a diagnostic dump. It contains:

- a quiet SpatialLink wordmark with restrained platform context;
- a large editorial `Spatial field` heading, READY state, and breathing room;
- an oversized off-center `SpatialField` that exceeds the viewport and is
  deliberately clipped at the right edge;
- layered structural rings, radial axes, calibration ticks, brass markers,
  inner depth, a partial cyan energy arc, spatial nodes, and a central trust
  seal;
- two truthful field-anchored annotations: secure link and local network;
- one major precision-framed `Open field` control;
- a restrained trusted-device artifact with safe short Spatial ID below the
  field;
- no capability, permission, or platform-detail dump on the launch surface.

### Field/System

Field is the dedicated secondary technical surface in P0/P1. It shows the
functional instrument, a concise capability-only explanation, and expandable
clusters for environment, spatial capabilities, ranging technologies, and
runtime permissions. It does not scan, discover peers, establish a session,
or imply that nearby devices exist.

### Identity

Identity is a premium trust artifact. It shows only safe metadata:

- short Spatial ID;
- identity readiness;
- `ECDSA P-256 / SHA-256` algorithm;
- Android Keystore protection level;
- signing verification status.

Private-key bytes, certificates, signatures, attestation, device identifiers,
and other sensitive internals remain inaccessible to Compose.

### Transfer

Transfer is intentionally absent from the implemented navigation because no
P0/P1 transfer behavior exists.

## Navigation and interaction

The app uses a sparse custom `SpatialNavigation` instrumentation rail at the
bottom of the content. It contains Overview, Field/System, and Identity. The
Identity destination is the visual center seal, while the two other
destinations sit as precision anchors on the rail. It is a bespoke surface
built with Compose primitives rather than a stock Material `NavigationBar`.

Navigation changes only local presentation state. It does not create a new
ViewModel or alter platform inspection behavior. The Overview primary action
opens Field/System. No control requests a runtime permission or starts a
platform session.

System clusters are locally expandable. Expansion state is UI-only and can be
restored with `rememberSaveable`; it does not persist product data.

## Visual language

The design is dark-first, architectural, and restrained:

| Token | Value | Use |
| --- | --- | --- |
| Deep Obsidian | `#0B0D0D` | primary application environment |
| Carbon | `#121616` | raised surfaces and navigation rail |
| Carbon Edge | `#1D2423` | restrained surface separation |
| Warm Ivory | `#E5DED2` | primary editorial and technical content |
| Muted Ivory | `#A8A196` | secondary copy and metadata |
| Champagne Brass | `#C8B89E` | trust structure, frames, and premium emphasis |
| Brass Dim | `#7F725D` | quiet dividers and inactive structure |
| Ice Cyan | `#A8D7EA` | active spatial field and live signal accents |
| Cyan Dim | `#5E9DB2` | inactive signal geometry |
| Muted Mint | `#91A99E` | ready, trusted, and healthy semantics |
| Soft Rose | `#D5A7A3` | errors and rejected states |

Color is never the only status signal. Every state also has a text label and
an instrument/shape treatment.

The field uses matte carbon layers, a restrained radial atmosphere, nested
shadow/highlight strokes, unequal ring weights, brass material stops, and a
partial cyan energy arc with small nodes. Surfaces use thin structural strokes
and deliberate negative space. No neon wash, noisy particle field, heavy
shader, card-in-card stack, or ornamental artwork is required.

## Typography

The system uses two existing platform font families, avoiding a font download:

- `FontFamily.Serif` only for conceptual/display headings and selected
  identity-artifact text;
- `FontFamily.SansSerif` for READY, field anchors, values, navigation, and all
  technical copy.

Display text is large but constrained to the viewport. Technical labels use
deliberate uppercase letter spacing. Body text stays readable at a 14sp-16sp
baseline. Long values are bounded and allowed to wrap inside their own tile;
they never compete with adjacent labels.

## Reusable Compose primitives

The feature module will own the following focused primitives:

- `SpatialAtmosphere`: low-cost background depth wash and structural hairlines;
- `SpatialSurface`: matte base surface with optional border and tonal layer;
- `SpatialCommand`: major precision-cut entry control for the field surface;
- `SpatialField`: state-driven oversized Canvas instrument with unequal rings,
  partial energy arc, nodes, depth layers, and center seal;
- `SpatialRing`: one efficient instrument ring with semantic accent treatment;
- `FieldNode`: an active point in the field instrument;
- `FieldAnchor`: a connector line, node instrument, and technical label tied to
  a field point;
- `TrustSeal`: identity-oriented seal using safe status data;
- `SystemTile`: bounded capability/permission/identity datum;
- `SystemCluster`: expandable grouped technical surface;
- `StatusMark`: label plus semantic marker for all modeled states;
- `InstrumentLabel`: compact annotation attached to a spatial surface;
- `TechnicalValue`: bounded technical label/value treatment;
- `SpatialNavigation`: sparse three-point instrumentation rail with a centered
  identity seal.

These are presentation components. They receive values or immutable view data;
they do not call Android services, repositories, or permission APIs.

## Existing data mapping

The existing `DiagnosticsUiState` remains the source of truth. A small pure
presentation mapping layer may derive display models without changing domain
types:

| Existing value | Surface | Presentation |
| --- | --- | --- |
| `foundationStatus` | Overview, Field/System | overall `StatusMark` and field instrument state |
| `capabilities.bluetoothLe`, `bleAdvertising` | Overview annotations, System | secure/local signal tile states |
| Wi-Fi capabilities | Overview annotation, System | local link readiness and capability tiles |
| NFC, UWB, platform ranging | Field/System | spatial capability tiles |
| `ranging.technologies` | Field/System | ranging technology cluster |
| `PermissionSnapshot` | Field/System | permission cluster grouped by state |
| `IdentityUiState` | Overview, Identity | trust seal, safe ID, algorithm, protection, self-test |
| `error` and `issues` | Field/System and contextual surfaces | calm partial/error explanation |

`AVAILABLE`, `GRANTED`, `READY`, and passed self-test values use muted mint or
ice cyan according to meaning. `UNAVAILABLE` and `NOT_REQUIRED_ON_THIS_OS`
remain normal truthful states, not error banners. `UNKNOWN`, partial data, and
inspection errors use explicit labels and a distinct neutral/brass/rose mark.

## State and failure behavior

- Loading renders a stable field silhouette with a concise inspection label;
  it does not spin indefinitely or trigger permission requests.
- A complete known snapshot renders `READY`.
- Unknown capability values and issue lists render `PARTIAL` without discarding
  successful sibling results.
- A missing hardware feature is rendered as `UNAVAILABLE`, not as a failure.
- A failed subsystem keeps successful data visible and exposes the relevant
  technical cluster as partial/unavailable.
- Identity recovery and platform failures are shown inside the Identity
  surface without leaking raw cryptographic data.

## Responsive, accessibility, and performance rules

- Use `BoxWithConstraints` and bounded width constraints for hero composition;
  no fixed 390dp-only placement.
- Use a scrollable content column for technical surfaces and compact hero
  composition on small heights.
- Keep labels and values in separate bounded regions. Do not overlay long
  status strings.
- Provide heading semantics, content descriptions for the field instrument,
  readable contrast, and touch targets of at least 48dp.
- Respect system font scale and landscape width; text may wrap within a tile,
  while the outer layout remains stable.
- Use Compose Canvas primitives and a bounded slow sweep only when visible;
  do not run particle simulation, 3D rendering, or continuous high-cost work.
- Use the system light scheme when requested, with an accessible warm-ivory
  adaptation of the same token roles. Dark mode remains the primary art
  direction.

## Architecture and file boundaries

- `app/src/main/java/com/r2h/spatiallink/ui/theme/SpatialLinkTheme.kt` owns the
  Material bridge and typography entry point.
- `feature/diagnostics/src/main/kotlin/com/r2h/spatiallink/diagnostics/`
  owns the shell, destinations, presentation mappings, primitives, and
  stateless page content.
- `DiagnosticsViewModel.kt`, capability adapters, permission readers, identity
  repository, and all `core:model` types remain behaviorally unchanged.
- No new Android dependency is introduced into `core:model`.
- The only UI dependency addition, if required by the implementation, is the
  Compose Material icon artifact used for small semantic navigation/technical
  glyphs; the field instrument itself remains efficient Canvas UI.

## Verification gates

1. Add pure presentation mapping tests before its implementation and observe
   the expected failing test.
2. Run focused feature tests after each mapping/state slice.
3. Update connected Compose tests for Overview, Identity, System, scrollability,
   and activity recreation; preserve assertions against real state labels.
4. Run JVM tests, debug and release builds, lint, and connected instrumentation
   on the authorized physical device without creating an emulator.
5. Inspect a fresh physical-device screenshot and UI hierarchy for narrow-width
   text bounds, especially long permission values.
6. Re-scan the architecture/privacy boundary for Android leakage into
   `core:model`, reflection, networking, identifiers, startup permission
   requests, peer discovery, transfer, NFC pairing, and ranging sessions.

## Explicit non-goals

This specification does not authorize:

- runtime permission requests;
- peer discovery or nearby-device scanning;
- transfer or transfer history;
- networking, cloud, analytics, telemetry, or broad storage access;
- NFC pairing;
- actual UWB, BLE Channel Sounding, Wi-Fi RTT, or any ranging session;
- manufacturer/model capability checks;
- raw device identifiers, MAC/address reads, private keys, certificates, or
  signatures;
- changes to the approved P0/P1 platform-adapter architecture.
