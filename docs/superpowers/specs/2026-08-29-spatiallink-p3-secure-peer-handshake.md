# SpatialLink P3 Secure Peer Handshake

Status: correction round 1 applied; specification/plan review complete; implementation not authorized  
Date: 2026-08-29  
Implementation status: not started

## 1. Product boundary

P3 converts an explicitly selected anonymous nearby presence from P2 into a
short-lived, mutually authenticated, encrypted peer session. It is a protocol
and connectivity phase, not a redesign of P0, P1, or P2.

P3 uses one concrete interaction in the existing Field surface. The initial
implementation supports exactly one selectable anonymous peer and one
owner-scoped P3 attempt at a time; it does not support a peer queue or
multi-peer handshake fan-out.

The flow is:

1. P2 is active and the existing Field surface renders anonymous presence
   entries. Each entry is selectable through a transient in-memory UI handle;
   the handle never displays or persists the P2 token, RSSI as identity, or a
   Bluetooth identity.
2. The prospective responder explicitly selects MAKE AVAILABLE. This enters a
   P3 offer/listen state, starts the separate connectable P3 GATT rendezvous,
   and waits for one connection. The responder then explicitly accepts the
   incoming secure-link request before the handshake starts.
3. The prospective initiator explicitly selects one anonymous entry and taps
   BEGIN SECURE LINK. The app acquires a one-use, short-lived endpoint lease
   for that selected current P2 presence and opens one GATT connection.
4. The initiator is the GATT client and sends INIT_HELLO. The responder is the
   GATT server and must first receive and validate INIT_HELLO before sending
   RESP_HELLO. Roles are assigned by these explicit product actions and the
   channel endpoint, never by address, name, model, or timing guess.
5. The responder's explicit acceptance and the initiator's explicit request
   are both required. No P3 handshake starts from P2 presence alone, from app
   startup, or from a passive incoming connection.

If both users tap BEGIN SECURE LINK, neither side is a responder offer, so
both one-shot attempts terminate with the typed NO_RESPONDER_OFFER result
after bounded rendezvous cleanup. The application does not silently invert a
role or open a second channel. If both users tap MAKE AVAILABLE, both offers
remain listeners until their offer lifetime expires and no handshake starts.
The user must explicitly choose one responder and one initiator.

If multiple anonymous peers are present, the user may select one visible
anonymous entry, but only that current in-memory selection can be handed to
P3. Other entries remain P2-only. If the selected peer expires before endpoint
acquisition, the attempt ends with P2_PEER_EXPIRED/ENDPOINT_EXPIRED, clears
the selection, and does not retry automatically.

The minimal feature:nearby addition is limited to the existing Field surface:
select anonymous presence, MAKE AVAILABLE, BEGIN SECURE LINK, ACCEPT, and
cancel/close states. No new route or visual-language redesign is introduced;
the frozen SpatialLink visual primitives remain the presentation layer.

P3 proves possession of a SpatialLink P1 signing key and establishes fresh
session keys. It does not prove that a human, phone owner, or physical device
is trusted. Physical trust remains a later P6 concern.

This document freezes the protocol vocabulary, byte formats, boundaries,
security model, compatibility strategy, test obligations, and implementation
order. It does not authorize production implementation in this phase.

## 2. Security goals

P3 must provide all of the following:

1. Mutual cryptographic authentication. Each endpoint proves possession of the
   P1 ECDSA P-256 private signing key corresponding to the identity it presents.
2. Fresh ephemeral key agreement. Every handshake generates a new X25519
   ephemeral key pair and derives session material from the new shared secret.
3. Transcript integrity. Roles, protocol version, suite, nonces, ephemeral
   keys, P2 session context, identities, and signatures are bound to one
   canonical transcript.
4. Replay resistance. A completed handshake cannot be replayed into a fresh
   session because fresh ephemeral keys, fresh nonces, fresh P2 session
   context, and Finished key confirmation are required.
5. Confidentiality and integrity. Identity material after the ephemeral
   exchange and all post-hello session/control payloads are protected by
   authenticated encryption. Hello framing is transcript-bound and Finished
   framing is protected by explicit key-confirmation MACs.
6. Key confirmation. Both endpoints prove possession of the final directional
   key before the session is reported as cryptographically authenticated.
7. Bounded failure. Malformed input, timeouts, cancellation, endpoint loss,
   provider failure, and resource exhaustion fail closed and release
   ephemeral state.
8. No identity regression. P1 remains the sole source of the permanent
   identity. P2 remains anonymous and unchanged.

## 3. Non-goals

P3 must not implement or imply:

- P4 high-speed transport, file transfer, capsules, persistence, or history;
- P6 NFC physical trust, TOFU, durable trust lists, revocation, or user trust
  policy;
- peer discovery, new P2 advertisements, scan-response data, or a stable
  identity advertisement;
- Bluetooth address, device name, model, manufacturer, account, or Android
  identifier as application identity;
- RSSI-to-distance conversion, direction, bearing, azimuth, or physical
  proximity proof;
- Bluetooth bonding, pairing UI, GATT data transfer beyond the bounded P3
  control channel, or background discovery;
- custom elliptic-curve arithmetic, custom AEAD, custom hash, or custom
  signature primitives;
- a silent P-256 key-agreement downgrade when X25519 is unavailable;
- reflection-based API access;
- session-key or peer-identity persistence;
- automatic reconnect, automatic permission requests, or automatic session
  restart;
- P3 UI claims that a cryptographically authenticated peer is TRUSTED.

## 4. Locked P0, P1, and P2 foundations

P0 remains the capability and permission foundation. Runtime permissions remain
operation-specific and are not requested at startup.

P1 remains:

- Android Keystore-backed EC P-256 / secp256r1 signing key;
- production alias spatiallink.identity.signing.v1;
- SHA256withECDSA;
- SIGN only, with no private-key export;
- no user-auth requirement, StrongBox requirement, or remote attestation;
- X.509 SubjectPublicKeyInfo public-key encoding;
- SpatialDeviceId equal to SHA-256 of the encoded public key, exactly 32 bytes;
- no Android ID, IMEI, serial, MAC, account, or random UUID as identity.

P2 remains:

- service-data UUID
  7F83C58D-4C4B-4E97-9B7D-9C06D3A47B91;
- exactly 9 payload bytes: version 0x01 followed by an 8-byte
  big-endian ephemeral DiscoverySessionId;
- a 27-byte SpatialLink-owned AD structure and a final legacy advertisement
  bounded to 31 bytes;
- non-connectable legacy advertising, no scan response, no name, TX power,
  manufacturer data, capability bitmap, or identity material;
- device-blind P2 scanning: the P2 scanner does not read ScanResult.device,
  address, name, alias, bond state, or toString();
- foreground-only operation, one 30-second session, 6-second peer TTL,
  maximum 64 peers, self-token rejection, and UNVERIFIED authenticity only;
- no GATT, ranging, transfer, networking, NFC pairing, or P3 behavior.

P3 does not change any P1/P2 source, wire format, permission policy, or state
meaning. A P2 NearbyPeer carries anonymous token/RSSI/time only. P3 receives a
separate explicit handoff context and never upgrades P2 data into a stable
identity.

## 5. Threat model

The protocol assumes an active network/radio attacker can observe, replay,
delay, reorder, drop, inject, truncate, and mutate BLE control frames. It also
assumes a malicious nearby application may attempt to advertise or connect
using arbitrary data. It does not assume the radio itself hides Bluetooth
metadata from the operating system or other privileged components.

| Threat | Required P3 response | Residual limitation |
| --- | --- | --- |
| Passive frame capture | P2 is anonymous; post-exchange identity and payloads use AEAD | Metadata and timing remain observable |
| Replay of old hello/auth frames | Fresh X25519 keys, fresh nonces, P2 session context, transcript binding, counters, and Finished | A live endpoint can be attacked in real time |
| Identity impersonation | Recompute SpatialDeviceId from the presented SPKI and verify the role-bound P1 signatures | An endpoint with the honest peer's private key can still act as that peer |
| Identity substitution | A modified honest transcript or claim cannot pass the signatures and Finished checks | An active attacker that legitimately owns its own P1 identity may substitute its own endpoint before P6 binds the intended physical peer |
| Downgrade | One fixed suite is accepted; unknown suite/version/flags fail closed | A future version must define explicit negotiation |
| Frame confusion/reflection | Role, type, ordering, direction, transcript domain, and directional keys are bound | Transport must preserve the logical frame bytes |
| Malformed or oversized input | Canonical decoder, exact lengths, bounded queues, malformed-frame budget, close on violation | Radio DoS and jamming are not prevented |
| Key compromise after a session | Ephemeral private/shared material is destroyed after derivation and completion | Live process compromise can expose current secrets |
| P1 key compromise later | Fresh X25519 provides forward secrecy for past session keys if ephemeral material was destroyed | It does not revoke the compromised identity |
| Endpoint mix-up | One-use endpoint lease, P2 token context, role-bound transcript, and single owner session | OS-level device metadata is still outside protocol control |
| Accidental data exposure | No raw keys/tokens in UI, logs, reports, or persistent stores; identity appears only after explicit handshake | Authenticated peer identity is intentionally disclosed to the peer |
| Resource exhaustion | One active handshake per owner, bounded lease/frame/queue/time budgets | An attacker can still consume bounded attempts |
| Unauthorized background activity | Explicit owner, foreground-only lifecycle, close-before-navigation, no auto-restart | A host process may still be killed by the OS |

P3 prevents an attacker from modifying an honest peer's transcript or
claiming possession of that honest peer's P1 private key. It does not prevent
an active attacker that legitimately owns a different SpatialLink P1 identity
from substituting its own endpoint and completing a cryptographically
authenticated session as itself. Until P6 provides a prior physical/trusted
binding, the user cannot know that the authenticated key belongs to the
intended physical person or device.

The protocol does not claim resistance to a compromised Android OS, a
compromised application process, radio jamming, traffic analysis, or a
malicious endpoint that legitimately owns a P1 key.

## 6. Trust model

P3 defines these states and their exact meanings:

| State | Meaning |
| --- | --- |
| DISCOVERED_UNVERIFIED | P2 saw an anonymous 9-byte presence frame. No identity claim is available. |
| HANDSHAKING | An explicit owner action created a bounded P3 attempt. Authentication is incomplete. |
| CRYPTOGRAPHICALLY_AUTHENTICATED | The peer presented a valid P1 public key and SpatialDeviceId, both signatures verified over the canonical transcript, and both Finished values verified. |
| CANCELLED | The owner, lifecycle, transport, or endpoint cancelled the attempt. |
| FAILED | The attempt failed closed for a typed protocol, crypto, transport, timeout, or resource reason. |
| CLOSED | The channel and all session material have been released. |

P3 does not use TRUSTED. It does not silently rewrite P2 UNVERIFIED into
trust. A UI may describe the terminal success as “cryptographically
authenticated” only after the complete Finished exchange. P6 may later add
physical trust as a separate, explicit state transition.

## 7. Identity-disclosure model

P1 identity is never broadcast in P2. A P3 identity claim is first sent only
after both endpoints have exchanged fresh X25519 public keys and derived
directional authentication keys. The claim is encrypted with AES-256-GCM and
authenticated with the handshake transcript.

The first responder identity claim is encrypted and accompanied by a
responder signature over the canonical responder-claim transcript. The
initiator must recompute the responder SpatialDeviceId from the presented SPKI
and verify that signature before it discloses its own identity. The initiator
identity is then disclosed in the corresponding encrypted message and is not
accepted until the responder verifies it and returns its full-transcript
confirmation signature.

The disclosure-order alternatives were evaluated explicitly:

- Simultaneous identity disclosure was rejected because it would require both
  stable identities to be sent before either side had authenticated the
  ephemeral exchange, increasing exposure to unauthenticated endpoints.
- Encrypted identity disclosure after ephemeral agreement was selected because
  P2 observers still see only anonymous presence and the first identity bytes
  are protected by a fresh, transcript-bound key.
- Staged disclosure was selected within that encrypted design: the responder
  claim/signature is sent first, the initiator verifies it and then sends its
  own claim/signature, and the responder returns a full-transcript
  confirmation. Neither side reports authentication until the Finished
  sequence succeeds. This slightly lengthens the flow but prevents an
  unauthenticated active endpoint from learning the initiator identity merely
  by receiving the first handshake message.

The exact first encrypted disclosure point is:

RESP_AUTH identity bytes become available to the initiator only inside the
encrypted P3 channel after RESP_HELLO and X25519 key derivation. The
initiator sends its own identity only after verifying the responder's
RESP_AUTH signature. Either identity becomes cryptographically authenticated
only after the full responder/initiator signatures and both Finished values
have verified. No identity is shown in P2 UI or logged by P3.

P3 exposes only the canonical full SpatialDeviceId and public key to the
handshake owner that explicitly requested the session. It does not expose a
BluetoothDevice, radio address, name, alias, bond state, model, manufacturer,
or raw certificate/signature bytes to product UI. A later UI adapter may choose
to display a short identity representation, but that display is outside this
protocol specification and must not become a stable identity advertisement.

## 8. P2 to P3 handoff

P2 remains device-blind. A new Android-only endpoint broker in
connectivity:handshake performs the explicit, short-lived handoff. The
P2-derived opaque BluetoothDevice lease is necessary to correlate the chosen
presence, but it is not sufficient to connect by itself because P2 advertising
is non-connectable. A separate P3 connectable rendezvous is mandatory.

The initiator handoff is:

1. The app passes the selected anonymous P2 token and current P2 session
   context after BEGIN SECURE LINK. The token is an internal handoff value,
   never a UI or persistence value.
2. The broker performs a filtered, short-lived service-data scan for the fixed
   P2 UUID and exact version byte, matching only that selected token.
3. The broker may hold the matching ScanResult.device/BluetoothDevice
   internally. It must not read or expose address, name, alias, bond state, or
   toString(). The reference is stored only in the owner-scoped opaque lease.
4. The broker waits for the selected device's separate P3 connectable service
   advertisement, then connects only to that leased platform object and
   verifies the P3 service/characteristic UUIDs after service discovery.
5. The broker returns a one-use HandshakeEndpointLease. It has a 10-second
   acquisition expiry, is bound to the owner/session generation and selected
   P2 token, and is invalid after close, timeout, mismatch, or owner disposal.
6. The P3 session consumes the lease once. No P2 NearbyPeer or core discovery
   type gains a BluetoothDevice field.

The responder flow is:

1. The user taps MAKE AVAILABLE in the existing Field surface.
2. The owner opens the P3 GATT server and starts the exact P3 rendezvous
   advertisement below. It accepts at most one pending connection.
3. A connection is only a pending secure-link request. The responder shows a
   generic anonymous request state and requires the user to tap ACCEPT before
   the responder feeds INIT_HELLO to the protocol session.
4. On ACCEPT, the responder owns the GATT server channel and the initiator
   owns the client channel; one connection carries both logical directions.

The broker has no stable identity store and no global mutable session. P2
scanner/advertiser handles remain owned by P2. P3 endpoint acquisition is an
explicit operation and does not auto-start from P2 presence.

P3 control uses separate, explicit GATT service constants:

- control service UUID:
  5E7A1D24-2C3F-4F89-8E11-6B5A02D4C370
- control characteristic UUID:
  5E7A1D25-2C3F-4F89-8E11-6B5A02D4C370

P3 does not alter the P2 advertisement. A P3 responder starts the following
separate rendezvous advertisement only after MAKE AVAILABLE and after the
required GATT-server permission boundary succeeds:

- mode: legacy, connectable, non-scannable response;
- AdvertiseSettings: ADVERTISE_MODE_LOW_LATENCY, ADVERTISE_TX_POWER_LOW,
  connectable true, timeout 0 (the owner enforces the offer lifetime);
- AdvertiseData: exactly one complete 128-bit service UUID field containing
  the P3 control service UUID; no service data, manufacturer data, name, TX
  power, or duplicate UUID;
- serialized SpatialLink-owned AD structure: 1-byte AD length 0x11,
  1-byte AD type 0x07 (complete 128-bit service UUID list), and 16-byte P3
  service UUID, for 18 bytes owned;
- connectable flags may contribute the platform's 3-byte flags structure, so
  the bounded legacy accounting is 18 + 3 = 21 bytes and is <=31 bytes;
- no extended-advertising fallback; ADVERTISE_FAILED_DATA_TOO_LARGE,
  ADVERTISE_FAILED_TOO_MANY_ADVERTISERS, or any other startup failure stops
  the GATT server and publishes typed P3_OFFER_UNAVAILABLE;
- lifetime: at most 30 seconds or until the first accepted GATT connection;
  the owner stops advertising immediately when the one connection is claimed;
- ownership: one responder offer per owner, one global app-process offer
  budget, and exactly-once stop for advertiser/server/callbacks on accept,
  cancel, timeout, failure, foreground loss, or owner disposal;
- payload privacy: no SpatialDeviceId, P1 public key/signature, P2 token,
  Bluetooth/device name, account, model, manufacturer, or user identifier.

The initiator knows that the endpoint belongs to the selected anonymous P2
presence without exporting a MAC/address because the connectivity broker
retains the selected P2-matched platform object internally, observes the P3
service UUID on that same platform endpoint, and binds the selected local and
remote P2 token context into INIT_HELLO/RESP_HELLO. A different P3 offer is
not accepted merely because it advertises the same service UUID.

Multiple simultaneous offers are handled by the same rule: an initiator has
one selected P2 lease and connects only to that lease; a responder accepts one
connection and rejects/closes later connections. No P3 token or identity is
added to the advertisement. If the platform cannot allocate the single
responder advertiser, the offer fails typed and the GATT server is closed; it
does not retry or fall back to extended advertising.

BLUETOOTH_CONNECT is requested only after explicit P3 user action and before
the concrete GATT operation that requires it, whether that operation is
client-side connectGatt or server-side open/operate BluetoothGattServer. It is
never added to normal P2 scan/advertise permission requests and is never
requested at application startup.

### Frozen GATT transport subprotocol

The responder is the GATT server. The initiator is the GATT client. One GATT
connection and one characteristic carry both logical directions:

- service: the P3 control service UUID above;
- characteristic: the P3 control characteristic UUID above;
- characteristic properties: PROPERTY_WRITE | PROPERTY_NOTIFY;
- characteristic permissions: PERMISSION_WRITE; no characteristic read and
  no PROPERTY_WRITE_NO_RESPONSE or PROPERTY_INDICATE;
- client writes: WRITE_TYPE_DEFAULT (write with response) only;
- server-to-client delivery: notifications only, after the client enables the
  mandatory CCCD descriptor UUID 0x2902;
- the client writes the standard enable-notification value 0x0100 to the CCCD
  and waits for a successful descriptor-write callback before the channel is
  ready; INIT_HELLO cannot be sent before this point;
- CCCD permissions: read/write for descriptor configuration, with no bonded or
  encrypted-link requirement; P3 message confidentiality comes from AEAD;
- no second simultaneous GATT connection is used by one handshake.

After service discovery the client requests MTU 247 once. The channel uses
the negotiated MTU when the callback succeeds, otherwise the current/default
MTU 23. ATT application payload capacity is MTU - 3. The channel never
assumes that 247 was granted.

Each GATT value carries a canonical 10-byte transport chunk header followed by
chunk bytes. All integers are unsigned big-endian:

| Offset | Size | Field | Rule |
| --- | ---: | --- | --- |
| 0 | 1 | chunk version | exactly 0x01 |
| 1 | 1 | flags | FIRST=0x01, LAST=0x02, reserved bits zero; one chunk uses 0x03 |
| 2 | 2 | logical frame ID | sender-direction value, starts at 1; zero forbidden |
| 4 | 2 | chunk index | zero-based, contiguous |
| 6 | 2 | chunk count | 1..512 |
| 8 | 2 | logical frame length | 7..4096, including the P3 7-byte frame header |

The chunk payload is the next part of one complete P3 logical frame. The
chunk header is not part of the logical frame, transcript, GCM AAD, or AEAD
counter. With default MTU 23, payload capacity is 10 bytes and the largest
4096-byte frame uses 410 chunks, within the 512-chunk bound.

For each direction, one logical frame is in flight at a time. The first
chunk has index 0 and FIRST; the final chunk has index count - 1 and LAST.
The accumulated payload length must equal logical frame length exactly. A
duplicate current chunk with identical bytes is ignored; a duplicate with
different bytes, a duplicate after frame completion, an out-of-order or
missing chunk, an interleaved frame, a wrong count/length/flag, or a chunk
index outside the declared range closes the channel with a typed transport
failure. There is no transport retransmission.

Reassembly starts a 2-second inter-chunk idle deadline and a 5-second total
logical-frame deadline. It buffers at most 4096 payload bytes and discards
partial state on timeout or close. The frame ID cannot wrap during a
connection; a sender closes before reusing zero. A remote CLOSE discards the
partial frame and enters the single cleanup path. GATT callback, CCCD,
advertiser, server, client, channel, timer, and reassembly cleanup is
exactly-once.

## 9. Bootstrap-channel analysis

### Selected channel: bounded BLE GATT control link with mandatory P3 rendezvous advertisement

GATT is selected because it is available on the locked minSdk 29 baseline,
fits short protocol control messages, is compatible with the already qualified
BLE path, and keeps the P3 transport local and explicit. It is a control
carrier only; it is not P4 transfer. The separate connectable P3 service-UUID
advertisement defined in the handoff section is mandatory for the responder;
the P2-derived opaque platform lease alone is not considered connectable.

The Android boundary owns BluetoothDevice, BluetoothGatt, callbacks, the
single MTU-247 request with the specified default-MTU-23 fallback,
characteristic notifications/writes, and permission checks. The pure protocol module sees only an opaque byte channel and typed
close/failure events. Every callback is tagged with an owner/session
generation; stale callbacks are ignored after close. GATT callbacks are
unregistered or closed exactly once.

### Rejected alternatives

- BLE L2CAP CoC: not selected for the first P3 implementation because
  server/PSM availability, API/vendor behavior, and stream lifecycle are less
  deterministic across the API 29–36 support floor.
- Wi-Fi local sockets: rejected because it adds local-network/bootstrap
  permissions and a second discovery path without helping the first secure
  handshake.
- WAN/cloud relay: rejected by the offline/local product boundary and would
  create P4/networking scope.
- NFC bootstrap: deferred to P6 physical trust, not a P3 control channel.
- A protocol-only abstraction with no carrier: useful as a seam, but not an
  implementation; P3 needs the selected GATT carrier to qualify a real
  handshake.

## 10. Chosen handshake construction

### Construction alternatives considered

The following standard patterns were evaluated against the existing P1
signing-only identity and the minSdk 29 local-channel boundary:

- A bespoke authenticated handshake construction using standard cryptographic
  primitives and SIGMA/TLS-1.3 design principles was selected. It cleanly
  combines P1 ECDSA proof of possession with fresh X25519, transcript binding,
  labeled HKDF, AEAD, and explicit Finished confirmation.
- Noise-family patterns were considered, but the usual static-DH identity
  assumptions do not match P1, which deliberately stores only a Keystore P-256
  signing key. Adapting a Noise pattern would require a signature extension
  and a protocol-specific mapping with no compatibility benefit over the
  explicitly specified construction. No static X25519 identity is introduced.
- A full TLS 1.3 implementation or another general authenticated-channel
  framework was considered, but would add a larger wire/state surface and a
  certificate/key integration model that does not match the P1 port. P3 uses a
  narrowly bounded SIGMA/TLS-1.3-style pattern, not TLS wire compatibility or
  a new general-purpose framework.

The selected construction is therefore the smallest standard-primitives
design that preserves P1 as a signing identity, provides fresh X25519
forward-secret agreement, and can be tested independently of the GATT carrier.

P3 uses the following bespoke construction informed by SIGMA/TLS-1.3 design
principles. It is not equivalent to a standardized SIGMA or TLS protocol:

1. Exchange role-bound hello messages containing fresh nonces, fresh X25519
   public keys, and the two P2 session tokens.
2. Derive directional authentication keys from the X25519 shared secret and
   the hello transcript.
3. Send the responder identity encrypted under the responder-to-initiator
   authentication key.
4. Send the initiator identity, an echo of the responder claim hash, and an
   initiator P1 signature encrypted under the initiator-to-responder key.
5. Send a responder signature and claim-hash echo under the responder-to-
   initiator key.
6. Exchange explicit Finished HMAC values over the final transcript.
7. Release final directional session keys only after both Finished values
   verify.

The construction is not wire-compatible with TLS and must not be described as
TLS. The implementation uses standard X25519, HKDF-SHA256, SHA-256,
AES-256-GCM, HmacSHA256, and P-256 SHA256withECDSA operations behind typed
ports. No custom cryptographic primitive is permitted.

The sole protocol suite is:

- suite ID 0x0001;
- X25519 ephemeral agreement;
- HKDF-SHA256;
- AES-256-GCM with a 128-bit tag;
- P-256 SHA256withECDSA signatures.

There is no in-protocol negotiation among weaker alternatives. Unknown suite,
version, role, type, flag, length, or encoding fails closed.

## 11. Cryptographic primitive rationale

X25519 is mandatory for every P3 handshake. Platform JCA exposes XDH/X25519
on modern Android, but the locked minSdk is 29 and Android's public XDH
algorithm surface is API 33+. The implementation therefore uses a typed
provider boundary: platform XDH on API 33+, and a pinned Conscrypt Android
provider for API 29–32. The provider is registered and used through typed JCA
APIs only; reflection is prohibited.

The implementation plan pins org.conscrypt:conscrypt-android:2.6.3 and adds a
real API 30/API 34 provider capability matrix before the handshake is wired.
The matrix must prove X25519 key generation, import/export, key agreement,
AES-GCM, HMAC, and P-256 signature verification. If that matrix fails, the
typed result is CRYPTO_UNAVAILABLE and the release is blocked for the
affected API range. It must not silently downgrade to P-256 agreement or
invent curve arithmetic.

AES-256-GCM is selected over ChaCha20-Poly1305 for the first implementation
because the Android support floor has a stable GCMParameterSpec surface and
the selected provider matrix can directly verify it. GCM uses a 12-byte nonce
and a 16-byte authentication tag. Every key/nonce pair is unique by
direction, a fixed transcript-derived prefix, and a monotonically increasing
counter. Associated data is the exact common frame header.

HKDF-SHA256 provides the extract/expand separation and labeled directional
keys. SHA-256 is used for canonical transcript and identity-claim hashes.
P1 remains the signing authority; P3 never generates, exports, or duplicates
the P1 private key.

References used for this compatibility decision:

- Android KeyPairGenerator API reference:
  https://developer.android.com/reference/java/security/KeyPairGenerator
- Android KeyAgreement API reference:
  https://developer.android.com/reference/javax/crypto/KeyAgreement
- Android NamedParameterSpec API reference:
  https://developer.android.com/reference/java/security/spec/NamedParameterSpec
- Android BluetoothDevice API reference:
  https://developer.android.com/reference/android/bluetooth/BluetoothDevice
- Android Bluetooth permission guidance:
  https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- Conscrypt capabilities:
  https://github.com/google/conscrypt/blob/master/CAPABILITIES.md
- Conscrypt Android artifact:
  https://central.sonatype.com/artifact/org.conscrypt/conscrypt-android

## 12. Message flow

The initiator and responder are assigned by the explicit P3 owner action and
channel role, not by Bluetooth address or device model.

    Initiator                                      Responder
        |                                             |
        |  INIT_HELLO                                 |
        |-------------------------------------------->|
        |                              RESP_HELLO     |
        |<--------------------------------------------|
        |                                             |
        |  derive Z, H0, PRK0, auth keys              |
        |                                             |
        |       RESP_AUTH (AEAD, responder claim +    |
        |       responder-claim signature)            |
        |<--------------------------------------------|
        |                                             |
        |  validate responder claim/signature          |
        |                                             |
        |  INIT_AUTH (AEAD, signature H1)             |
        |-------------------------------------------->|
        |                                             |
        |  verify responder proof first               |
        |                         RESP_AUTH_CONFIRM   |
        |<--------------------------------------------|
        |                                             |
        |  verify responder signature                 |
        |  INIT_FINISHED                              |
        |-------------------------------------------->|
        |                                             |
        |                         RESP_FINISHED       |
        |<--------------------------------------------|
        |                                             |
        |       both endpoints: AUTHENTICATED         |

The responder starts in AWAITING_INIT_HELLO and does not send RESP_HELLO or
RESP_AUTH before receiving and validating INIT_HELLO. The initiator must not
send INIT_AUTH until RESP_AUTH identity and responder-claim signature have
verified. An endpoint must reject any message that is not valid for its exact
current state, role, direction, and message counter. A CLOSE frame is terminal
and does not establish authentication.

## 13. Exact byte-level framing

All integer fields are unsigned big-endian. There is no JSON, text encoding,
implicit length, optional trailing field, compression, or extension bit.

Every logical frame has a 7-byte header:

| Offset | Size | Field | Value |
| --- | ---: | --- | --- |
| 0 | 2 | magic | 0x53, 0x4C |
| 2 | 1 | protocol version | 0x01 |
| 3 | 1 | message type | defined below |
| 4 | 1 | flags | exactly 0x00 |
| 5 | 2 | body length | unsigned big-endian, 0..4089 |

The complete frame length is 7 plus body length and may not exceed 4096. The
decoder rejects wrong magic/version/flags, unknown types, body truncation,
trailing bytes, lengths above the maximum, and noncanonical field encodings.

Message types:

| Type | Name | Plain/encrypted |
| ---: | --- | --- |
| 0x01 | INIT_HELLO | plain |
| 0x02 | RESP_HELLO | plain |
| 0x03 | RESP_AUTH | AEAD body |
| 0x04 | INIT_AUTH | AEAD body |
| 0x05 | RESP_AUTH_CONFIRM | AEAD body |
| 0x06 | INIT_FINISHED | plain MAC body |
| 0x07 | RESP_FINISHED | plain MAC body |
| 0x7F | CLOSE | plain terminal body |

Role values are 0x01 initiator and 0x02 responder.

The INIT_HELLO and RESP_HELLO body is exactly 83 bytes:

| Offset | Size | Field |
| --- | ---: | --- |
| 0 | 1 | role |
| 1 | 2 | suite ID, exactly 0x0001 |
| 3 | 32 | fresh nonce |
| 35 | 32 | raw X25519 public key |
| 67 | 8 | initiator P2 DiscoverySessionId bytes |
| 75 | 8 | responder P2 DiscoverySessionId bytes |

INIT_HELLO requires role 0x01 and the responder token field is the selected
context token. RESP_HELLO requires role 0x02, echoes the initiator token, and
sets the responder token to its own current P2 token. Token bytes are context
only, never identity.

RESP_AUTH plaintext before AEAD:

| Field | Size |
| --- | ---: |
| role, exactly 0x02 | 1 |
| public-key length | 2, 1..256 |
| responder X.509 SPKI public key | declared length |
| responder SpatialDeviceId | 32 |
| responder-claim signature length | 2, 1..128 |
| responder signature over the responder-claim transcript | declared length |

The transmitted body is ciphertext followed by a 16-byte GCM tag. Its length
is 1 + 2 + public-key length + 32 + 2 + signature length + 16.

The responder-claim signature is required before the initiator discloses its
identity. It is not the final full-transcript confirmation signature.

INIT_AUTH plaintext before AEAD:

| Field | Size |
| --- | ---: |
| role, exactly 0x01 | 1 |
| public-key length | 2, 1..256 |
| initiator X.509 SPKI public key | declared length |
| initiator SpatialDeviceId | 32 |
| responder-claim transcript hash H_R | 32 |
| signature length | 2, 1..128 |
| DER ECDSA signature | declared length |

The transmitted body is ciphertext followed by a 16-byte GCM tag.

RESP_AUTH_CONFIRM plaintext before AEAD:

| Field | Size |
| --- | ---: |
| role, exactly 0x02 | 1 |
| initiator claim hash | 32 |
| signature length | 2, 1..128 |
| DER ECDSA signature | declared length |

The transmitted body is ciphertext followed by a 16-byte GCM tag.

INIT_FINISHED and RESP_FINISHED bodies are exactly 33 bytes:

| Offset | Size | Field |
| --- | ---: | --- |
| 0 | 1 | role |
| 1 | 32 | Finished HMAC |

CLOSE body is exactly 2 bytes: role followed by a close-reason enum. It is
never accepted as proof of authentication.

## 14. Canonical identity claims, transcript, and signatures

The identity-claim domain is the ASCII string:

SPATIALLINK/P3/HANDSHAKE/IDENTITY/v1

For a role, SPKI public key, and 32-byte claimed identity, define:

I(role, publicKey, id) =
SHA256(D_ID || role || u16be(publicKeyLength) || publicKey || id)

The canonical claim hash is C_role = I(role, publicKey, id). RESP_AUTH carries
the responder claim and its responder-claim signature. INIT_AUTH carries H_R as
the responder-proof echo, and RESP_AUTH_CONFIRM carries C_initiator as the
initiator-claim echo. These values are distinct: H_R binds the responder proof
to H0, while C_initiator identifies the canonical initiator claim inside H1.

The public key must be a valid P-256 X.509 SubjectPublicKeyInfo. Parsing,
canonical re-encoding, and byte-for-byte equality are required. The encoded
key length is 1..256. The claimed SpatialDeviceId must equal
SHA256(publicKey). Any mismatch fails closed.

The transcript domain is the ASCII string:

SPATIALLINK/P3/HANDSHAKE/TRANSCRIPT/v1

H0 is:

SHA256(D_TRANSCRIPT || canonical INIT_HELLO frame ||
canonical RESP_HELLO frame)

After validating the responder's canonical P-256 claim, the responder-claim
transcript hash H_R is:

H_R = SHA256(D_TRANSCRIPT || H0 ||
0x02 || u16be(length(pkR)) || pkR || idR)

H1 is:

SHA256(D_TRANSCRIPT || H0 ||
0x01 || u16be(length(pkI)) || pkI || idI ||
0x02 || u16be(length(pkR)) || pkR || idR)

The role bytes and field lengths are part of both hashes. H1 is formed only
from canonical validated identity claims. The actual frame headers, protocol
version, suite, fresh nonces, ephemeral public keys, and P2 token context are
already bound through H0. H_R is the canonical responder-only claim proof
that permits the initiator to disclose its identity; it must not be reused as
the final mutual transcript hash.

The signature domain is:

SPATIALLINK/P3/HANDSHAKE/SIGNATURE/v1

The responder signature inside RESP_AUTH covers:

D_SIGNATURE || 0x02 || H_R

The initiator signature inside INIT_AUTH covers:

D_SIGNATURE || 0x01 || H1

The responder full-transcript confirmation signature inside
RESP_AUTH_CONFIRM covers:

D_SIGNATURE || 0x02 || H1

Signatures use the existing P1 SHA256withECDSA Keystore operation. They are
DER-encoded ECDSA values. The decoder accepts only canonical DER:
SEQUENCE(INTEGER r, INTEGER s), positive minimally encoded integers, no
trailing bytes, and total length 1..128. P3 never exposes the signature to
product UI or logs.

## 15. Ephemeral agreement

Each handshake endpoint generates exactly one fresh X25519 key pair before
its hello. Reuse across sessions is forbidden. Raw public keys are exactly 32
bytes in the protocol. The provider boundary validates the expected key type,
encoding, and length.

The endpoints compute:

Z = X25519(ephemeralPrivateLocal, ephemeralPublicPeer)

An all-zero shared result is rejected as KEY_AGREEMENT_FAILED. Z is never
used directly as an application key, transmitted, logged, persisted, or
returned outside the crypto/session boundary.

An X25519 provider failure is typed CRYPTO_UNAVAILABLE or
KEY_AGREEMENT_FAILED as appropriate. It must not trigger a P-256 fallback.

## 16. HKDF and key schedule

The KDF domain is:

SPATIALLINK/P3/HANDSHAKE/KDF/v1

The initial salt is:

salt0 = SHA256(D_KDF || ASCII("salt") || H0)

PRK0 = HKDF-Extract(salt0, Z)

Authentication keys:

- K_auth_i2r = HKDF-Expand(PRK0, info("auth-i2r", H0), 32)
- K_auth_r2i = HKDF-Expand(PRK0, info("auth-r2i", H0), 32)

The final salt is:

salt1 = SHA256(D_KDF || ASCII("final-salt") || H1)

PRK1 = HKDF-Extract(salt1, PRK0)

Final outputs are 32 bytes:

- K_session_i2r = HKDF-Expand(PRK1, info("session-i2r", H1), 32)
- K_session_r2i = HKDF-Expand(PRK1, info("session-r2i", H1), 32)
- K_finished_i2r = HKDF-Expand(PRK1, info("finished-i2r", H1), 32)
- K_finished_r2i = HKDF-Expand(PRK1, info("finished-r2i", H1), 32)
- K_binding = HKDF-Expand(PRK1, info("session-binding", H1), 32)

For every label, info is exactly:

ASCII("SPATIALLINK/P3/HANDSHAKE/KDF/v1/") ||
ASCII(label) || 0x00 || transcriptHash

The implementation must use an independent HKDF test vector suite and verify
that the two roles derive equal corresponding keys and unequal opposite
direction keys. The two Finished keys are direction-specific; the role byte
in the Finished input remains a separate defense against reflection.

## 17. AEAD nonces and key confirmation

The nonce domain is:

SPATIALLINK/P3/HANDSHAKE/NONCE/v1

For each direction, the 12-byte AES-GCM nonce is:

first4(SHA256(D_NONCE || role || H0)) || u64be(counter)

The counter is implicit and is not transmitted in the 7-byte logical frame
header or the 10-byte GATT chunk header. Each direction starts at counter 1.
Only AEAD-protected logical frames consume a counter. INIT_HELLO,
RESP_HELLO, INIT_FINISHED, RESP_FINISHED, and CLOSE do not consume one.
For the exact handshake sequence, the expected values are:

| Direction | Encrypted message | Counter |
| --- | --- | ---: |
| responder -> initiator | RESP_AUTH | 1 |
| initiator -> responder | INIT_AUTH | 1 |
| responder -> initiator | RESP_AUTH_CONFIRM | 2 |

An encoder reserves and consumes the next counter when AEAD encoding succeeds,
before transport send. If transport send then fails, the session terminates;
the counter is never rolled back and no retransmission is attempted. A
receiver accepts only the expected counter after successful AEAD validation
and message decoding, then advances it. A duplicate GATT delivery of an
already accepted logical frame therefore presents a stale implicit counter and
terminates with REPLAY_OR_COUNTER_INVALID. Timeout, close, or any protocol
error terminates the session; counters are not reused. A new session creates
new keys, nonce prefixes, and counters starting at 1.

A counter reuse, overflow, wrong direction, or unexpected message type fails
closed. The exact common 7-byte frame header is GCM associated data; GATT
chunk headers are transport-only. GCM uses a 16-byte tag.

The Finished domain is:

SPATIALLINK/P3/HANDSHAKE/FINISHED/v1

The initiator Finished HMAC is:

HMAC-SHA256(K_finished_i2r, D_FINISHED || 0x01 || H1)

The responder Finished HMAC is:

HMAC-SHA256(K_finished_r2i, D_FINISHED || 0x02 || H1)

The responder sends RESP_FINISHED only after verifying INIT_FINISHED. The
initiator reports success only after verifying RESP_FINISHED. Final session
keys become available to the next owner only after the full exchange.

## 18. State machine

Only one owner-scoped handshake session may exist at a time. The state model
is:

- IDLE
- CHANNEL_ESTABLISHING
- AWAITING_RESP_HELLO
- AWAITING_INIT_HELLO
- AWAITING_RESPONDER_IDENTITY
- AWAITING_INITIATOR_IDENTITY
- AWAITING_RESPONDER_AUTH_CONFIRM
- AWAITING_INITIATOR_FINISHED
- AWAITING_RESPONDER_FINISHED
- AUTHENTICATED
- FAILED
- CANCELLED
- CLOSED

The initiator path is:

IDLE -> CHANNEL_ESTABLISHING -> AWAITING_RESP_HELLO ->
AWAITING_RESPONDER_IDENTITY -> AWAITING_RESPONDER_AUTH_CONFIRM ->
AWAITING_RESPONDER_FINISHED -> AUTHENTICATED

The responder path is:

IDLE -> CHANNEL_ESTABLISHING -> AWAITING_INIT_HELLO ->
AWAITING_INITIATOR_IDENTITY -> AWAITING_INITIATOR_FINISHED ->
AUTHENTICATED

The responder path is explicit: it first receives and validates INIT_HELLO,
then sends RESP_HELLO, derives Z/H0/authentication material, sends RESP_AUTH,
and only then enters AWAITING_INITIATOR_IDENTITY. It never sends RESP_HELLO
or RESP_AUTH before INIT_HELLO has been accepted. The exact event table is:

| Current state | Accepted event | Required check | Action and next state |
| --- | --- | --- | --- |
| CHANNEL_ESTABLISHING (initiator) | channel opened | lease owner/generation and deadline valid | send INIT_HELLO; enter AWAITING_RESP_HELLO |
| CHANNEL_ESTABLISHING (responder) | channel opened | offer owner/generation and deadline valid | enter AWAITING_INIT_HELLO; send nothing |
| AWAITING_RESP_HELLO | RESP_HELLO | exact frame, role 0x02, suite, echoed initiator token, responder token | derive Z/H0/PRK0; enter AWAITING_RESPONDER_IDENTITY |
| AWAITING_INIT_HELLO | INIT_HELLO | exact frame, role 0x01, suite, selected token context, no prior hello | send RESP_HELLO; derive Z/H0/PRK0; send RESP_AUTH; enter AWAITING_INITIATOR_IDENTITY |
| AWAITING_RESPONDER_IDENTITY | RESP_AUTH | auth AEAD counter 1, role 0x02, canonical claim, ID hash, responder signature H_R | send INIT_AUTH with full H1 signature; enter AWAITING_RESPONDER_AUTH_CONFIRM |
| AWAITING_INITIATOR_IDENTITY | INIT_AUTH | auth AEAD counter 1, role 0x01, canonical claim, H_R echo, ID hash, initiator signature H1 | send RESP_AUTH_CONFIRM with full H1 signature; enter AWAITING_INITIATOR_FINISHED |
| AWAITING_RESPONDER_AUTH_CONFIRM | RESP_AUTH_CONFIRM | auth AEAD counter 2, role 0x02, initiator claim hash, responder signature H1 | send INIT_FINISHED; enter AWAITING_RESPONDER_FINISHED |
| AWAITING_INITIATOR_FINISHED | INIT_FINISHED | exact role 0x01 and K_finished_i2r HMAC over H1 | send RESP_FINISHED; enter AUTHENTICATED only after send succeeds |
| AWAITING_RESPONDER_FINISHED | RESP_FINISHED | exact role 0x02 and K_finished_r2i HMAC over H1 | enter AUTHENTICATED |
| any nonterminal | timeout/cancel/close | owner and generation | cleanup first, then typed terminal state |
| terminal | any callback/event | no state mutation | ignore stale event |

Concurrent start/open calls are serialized by an owner-scoped session
coordinator. A second open/start cannot replace a STARTING or ACTIVE session,
running job, session ID, peer cache, endpoint lease, crypto state, or cleanup
coordinator. Stale callbacks carry the session generation and are ignored.

## 19. Timeout, cancellation, and lifecycle

The budgets are:

- endpoint acquisition: 10 seconds;
- complete handshake: 15 seconds;
- per-message wait: 5 seconds;
- responder offer/listen lifetime: 30 seconds;
- GATT reassembly: 2 seconds idle and 5 seconds total per logical frame;
- malformed-frame budget: 8 frames before close;
- one active handshake per owner;
- no automatic retry or reconnect.

Rendezvous is a separate owner state before the core handshake: IDLE,
OFFER_LISTENING, PENDING_ACCEPT, or CLOSED. MAKE AVAILABLE creates one
OFFER_LISTENING lease and one server/advertiser. A pending GATT connection
enters PENDING_ACCEPT and is not allowed to deliver INIT_HELLO to the pure
protocol session until ACCEPT. BEGIN SECURE LINK creates one initiator lease
and one client attempt. The two owner states converge on one GATT channel or
terminate with a typed result; neither side opens a second connection.

If both owners request initiation, no server exists and both attempts end as
NO_RESPONDER_OFFER after the bounded acquisition timeout. If both owners only
offer, neither sends INIT_HELLO and both offers expire. There is no automatic
role inversion, retry, or simultaneous dual-channel resolution.

The application owner must stop P3 before leaving the Nearby surface. The
sequence is:

request close -> cancel channel and timer -> close GATT exactly once -> clear
protocol/session/identity material -> invalidate lease -> publish CANCELLED or
FAILED -> navigate away.

Foreground loss follows the same sequence. P3 is foreground-only and must not
use a foreground service. Returning to foreground does not restart a prior
session.

Every asynchronous operation is owner-scoped. The controller has one cleanup
coordinator, and cleanup is idempotent but invoked through one serialized
terminal path. Terminal publication occurs after channel closure, callback
removal, timers, leases, queues, and ephemeral secrets have been cleared.

## 20. Typed failure model

The public protocol result must use typed failures rather than exceptions as
product state. At minimum:

- INVALID_INPUT
- INVALID_STATE
- CHANNEL_UNAVAILABLE
- NO_RESPONDER_OFFER
- P2_PEER_EXPIRED
- ENDPOINT_NOT_FOUND
- ENDPOINT_EXPIRED
- P3_OFFER_UNAVAILABLE
- PERMISSION_REQUIRED
- BLUETOOTH_DISABLED
- BLUETOOTH_CONNECT_REQUIRED
- CRYPTO_UNAVAILABLE
- KEY_GENERATION_FAILED
- KEY_AGREEMENT_FAILED
- INVALID_PEER_KEY
- AUTHENTICATION_FAILED
- IDENTITY_MISMATCH
- SIGNATURE_INVALID
- TRANSCRIPT_INVALID
- AEAD_AUTHENTICATION_FAILED
- REPLAY_OR_COUNTER_INVALID
- MALFORMED_FRAME
- FRAME_TOO_LARGE
- RESOURCE_LIMIT
- TIMEOUT
- CANCELLED
- TRANSPORT_CLOSED
- INTERNAL_FAILURE

Invalid peer data never becomes a successful anonymous or authenticated
state. Cancellation is not a security failure. Bluetooth absence or disabled
state is typed and does not crash the app. Provider absence is visible as
CRYPTO_UNAVAILABLE and blocks the affected handshake without a weaker
construction.

## 21. Resource limits

These limits are protocol constants, not UI preferences:

- complete logical frame maximum: 4096 bytes including the 7-byte header;
- maximum frame body: 4089 bytes;
- maximum SPKI length: 256 bytes;
- maximum signature length: 128 bytes;
- maximum canonical transcript input: 2048 bytes;
- maximum one-use endpoint lease lifetime: 10 seconds;
- maximum responder offer/listen lifetime: 30 seconds;
- maximum one complete handshake: 15 seconds;
- maximum per-message wait: 5 seconds;
- maximum chunks per logical frame: 512;
- maximum reassembly buffer: 4096 bytes;
- maximum reassembly idle/total time: 2/5 seconds;
- maximum active handshake sessions per owner: 1;
- maximum responder offers per owner: 1;
- maximum outstanding endpoint leases per app process: 4;
- maximum queued callback events per session: 8;
- malformed-frame budget per session: 8;
- no unbounded buffers, queues, retries, or coroutines.

The fixed message formats are substantially below the frame maximum. Any
length arithmetic must be checked before allocation and before integer
conversion.

## 22. Secret lifecycle and cleanup

P1 signing remains in the existing Keystore adapter. The P3 process receives
only the public identity and a sign operation through a narrow port. It never
receives a P1 private key.

P3 keeps ephemeral X25519 private key, Z, PRK0, PRK1, authentication keys,
session keys, nonce counters, and pending identity claims in one
owner/session-scoped object. It clears references on every terminal path:
success, authentication failure, malformed input, timeout, cancellation,
transport loss, lifecycle loss, and owner disposal. Where the runtime permits,
byte arrays are zeroed before references are dropped. Secret state is never
persisted or included in exceptions, UI state, analytics, or logs.

The endpoint lease, P3 rendezvous advertiser, GATT server/client, CCCD
callbacks, chunk reassembly, timers, and all GATT objects are
invalidated/closed before terminal state publication. An owner cannot reuse a
closed session object or a consumed offer.

## 23. Module architecture

The neutral pure protocol module is core:handshake:

- pure Kotlin only;
- protocol constants, canonical byte codec, message models, transcript and
  HKDF logic, state machine, typed ports, and deterministic tests;
- no Android, Compose, JCA, Keystore, Bluetooth, coroutines tied to Android,
  ViewModel, navigation, or feature dependency.

The Android boundary is connectivity:handshake:

- typed API-guarded X25519/AES-GCM/HMAC/P-256 verification adapter;
- pinned Conscrypt provider registration for API 29–32 compatibility;
- P2-token-filtered endpoint broker;
- typed GATT control server/client and logical-frame chunking;
- Android permission/Bluetooth state checks;
- callback/session-generation and exactly-once cleanup;
- no UI, ViewModel, P2 feature dependency, or P1 private-key access.

The existing core:identity remains the P1 implementation and is not made a
dependency of core:handshake or connectivity:ble. The app supplies the P1
identity/sign adapter through the core:handshake port. This prevents a
discovery-to-identity dependency and keeps the protocol testable.

feature:nearby remains the P2 feature and receives only a consumer-owned
NearbySecureLinkPort for the minimal existing-Field controls: anonymous peer
selection, BEGIN SECURE LINK, MAKE AVAILABLE, ACCEPT, and cancel. It is not
made a dependency of connectivity:handshake. The app implements the port and
owns the P3 coordinator; no feature-to-feature dependency is introduced and
the frozen visual system is not redesigned.

## 24. Dependency graph

The planned graph is:

    :app
      -> :feature:diagnostics
      -> :feature:nearby
      -> :core:identity
      -> :core:handshake
      -> :connectivity:handshake
      -> :connectivity:ble
      -> :core:discovery
      -> :core:model
      -> :core:designsystem

    :connectivity:handshake
      -> :core:handshake

    :core:handshake
      -> :core:model

    :core:discovery
      -> :core:model

    :connectivity:ble
      -> :core:discovery
      -> :core:model

    :feature:nearby
      -> :core:discovery
      -> :core:designsystem

There is no :feature:nearby -> :feature:diagnostics edge, no
core:discovery -> core:identity edge, no core:handshake -> core:identity edge,
and no P3 dependency on P4 or P6.

The existing P2 and P1 module dependency files are not changed except where
the app must add an explicit, tested P3 composition dependency. No P2 protocol
source is moved or rewritten.

## 25. Android API compatibility

The locked project baseline is compile/target SDK 37, minSdk 29, AGP 9.3.1,
Gradle 9.5.0, Kotlin 2.4.10, and the existing Compose/Android toolchain.

Typed adapter rules:

- use platform XDH/X25519 JCA on API 33+ behind a compile-safe guarded
  adapter;
- use the pinned Conscrypt Android provider on API 29–32 through typed JCA
  APIs after the provider matrix passes;
- use typed API guards for the API 37 Bluetooth GATT overload;
- use the legacy typed connectGatt path for API 29–36;
- no reflection, hidden APIs, unsafe casts to future platform types, or
  minSdk-incompatible class loading;
- catch and map provider/platform failures to typed P3 results;
- never silently downgrade X25519 to P-256 agreement.

The API matrix must run on API 30 and API 34 physical/instrumented targets,
plus available host tests. API 29 behavior is covered by minSdk-compatible
bytecode/static checks and a provider test where a real API 29 target exists.
An API 36 device may verify guarded class loading but is not a reason to add
UWB, RangingManager, or any P2/P3-unrelated capability.

The BLE permission policy is operation-aware:

- P2 keeps its existing API-specific least-privilege mapping: API 31+ uses
  BLUETOOTH_SCAN and BLUETOOTH_ADVERTISE, while API 29–30 retain the existing
  legacy BLUETOOTH/BLUETOOTH_ADMIN declarations and the existing foreground
  scan-location requirement where that P2 scan path requires it;
- on API 29–30, the legacy BLUETOOTH/BLUETOOTH_ADMIN permissions cover the
  typed GATT client and server operations and there is no
  BLUETOOTH_CONNECT runtime request;
- on API 31+, both the GATT client connectGatt boundary and the GATT server
  open/operate boundary require BLUETOOTH_CONNECT. The app requests it only
  after the explicit BEGIN SECURE LINK or MAKE AVAILABLE action that enters
  that concrete boundary;
- P3 adds no nearby Wi-Fi, local-network, ranging, or additional location
  permission; it inherits the existing API 29–30 P2 scan policy and adds no
  automatic startup permission request;
- the P3 app action must explain the connection before requesting a runtime
  permission. API30 client/server and API34 client/server tests must exercise
  these separate expectations.

## 26. Privacy and logging

Production P3 logs may contain only event type, typed result, role, bounded
state name, and coarse duration. They must never contain:

- raw DiscoverySessionId or P2 token;
- SpatialDeviceId or short ID;
- public key, certificate, signature, private key, shared secret, PRK, session
  key, nonce, or frame bytes;
- BluetoothDevice, Bluetooth address, name, alias, bond state, model, or
  manufacturer;
- user/account/device identifiers;
- peer location, distance, direction, bearing, or azimuth.

The endpoint broker's internal BluetoothDevice reference is process-local,
owner-scoped, one-use, and cleared on every terminal path. Test-only seams
may compare opaque token/key digests or booleans, but must not print raw
secrets. Static source scans must enforce the narrower P3 privacy boundary
without weakening the existing P2 scanner checks.

## 27. Test matrix

### Pure protocol tests in core:handshake

Test-first coverage must include:

- exact frame header and all message body lengths;
- big-endian integer encoding;
- wrong magic/version/type/role/flags/suite rejection;
- truncation, trailing bytes, oversized body, malformed DER, noncanonical
  lengths, and unknown close reason;
- both role-specific state machines and every valid transition;
- every invalid/out-of-order/replayed transition;
- identity recomputation and mismatch rejection;
- canonical P-256 SPKI and DER ECDSA validation;
- responder-claim H_R and full H0/H1 transcript vectors, including role and
  P2-token binding;
- responder signature verification before initiator identity disclosure;
- full-transcript responder confirmation and initiator/responder signature
  coverage;
- X25519 raw key length and all-zero shared-secret rejection;
- HKDF labels, salts, directional separation, and independent vectors;
- AES-GCM AAD, nonce/counter uniqueness, tamper rejection, and direction
  separation through the crypto port test double;
- complete message-sequence counter vectors: RESP_AUTH=r2i/1,
  INIT_AUTH=i2r/1, RESP_AUTH_CONFIRM=r2i/2; hello/Finished/CLOSE do not
  advance counters;
- signature inputs and Finished HMAC vectors;
- duplicate frame, reflection, stale counter, and replay rejection;
- identity impersonation rejection versus valid-own-identity substitution
  acceptance as a documented pre-P6 limitation;
- timeout, cancellation, owner close, and terminal idempotence;
- resource bounds and no secret exposure through public result objects.

### Android boundary tests in connectivity:handshake

Test-first coverage must include:

- API guard/class-loading matrix;
- provider availability and X25519/AES-GCM/HMAC/P-256 verification matrix on
  API 30 and API 34;
- missing provider maps to CRYPTO_UNAVAILABLE, never a downgrade;
- exact GATT service/characteristic UUIDs and logical-frame chunking;
- exact connectable P3 rendezvous advertisement, 18-byte owned AD structure,
  <=31-byte legacy bound, no extended fallback, one-offer ownership, and
  collision failure;
- explicit CONNECT permission boundary for API30 client/server and API34
  client/server expectations;
- one client/server connection, PROPERTY_WRITE | PROPERTY_NOTIFY, CCCD,
  WRITE_TYPE_DEFAULT, notification-only server delivery, MTU/default-MTU
  policy, and canonical 10-byte chunk header;
- chunk duplicate, out-of-order, missing, interleaving, reassembly timeout,
  remote-close, and exact cleanup behavior;
- endpoint lease token filtering, expiry, one-use consumption, and owner close;
- GATT callback generation filtering;
- callback, scanner, advertiser, timer, and GATT cleanup exactly once;
- connect failure, MTU failure, notification/write failure, and remote close;
- no Bluetooth identity accessor in the production P3 broker API;
- no P2 source or P2 advertisement changes.

### Integration and regression tests

- existing P0/P1/P2 JVM suites remain green;
- existing P1 core:identity connected suite remains non-zero and green;
- existing P2 app/nearby connected suites remain non-zero and green;
- one owner cannot start two handshakes concurrently;
- responder cannot emit RESP_HELLO before receiving INIT_HELLO;
- close-before-navigation, foreground loss, timeout, and prerequisite loss
  clear state before terminal publication;
- P2 device-blind scanner static tests remain green;
- P3 privacy/boundary scanner fails on missing required structures or forbidden
  access, rather than silently skipping.

## 28. Physical qualification plan

Physical execution occurs only after all host, unit, connected, and review
gates pass. It requires two authorized physical Android devices and explicit
selector-bound ADB commands.

The current qualified P2 pair provides an API 34 and API 30 baseline. P3
qualification must:

1. Verify the same final build on both devices and record APK hashes.
2. Confirm no startup permission or identity disclosure.
3. Establish the least-privilege P2 permissions first, then request CONNECT
   only at explicit P3 connection, whether the local role is GATT client or
   GATT server.
4. On the first run assign Device A as the responder: its user taps MAKE
   AVAILABLE and ACCEPT; Device B is the initiator: its user selects one
   anonymous P2 entry and taps BEGIN SECURE LINK. Verify one GATT server,
   one GATT client, and one bidirectional connection.
5. Establish the GATT control channel without address/name UI and verify the
   responder receives INIT_HELLO before emitting RESP_HELLO.
6. Verify both sides reach CRYPTOGRAPHICALLY_AUTHENTICATED for the same
   transcript, then close cleanly.
7. Verify a fresh handshake uses fresh ephemeral keys and fresh session keys.
8. Optionally run a second sequential attempt with Device B as responder and
   Device A as initiator; this is a role-reversal run, not a second
   simultaneous connection.
9. Exercise malformed frame, timeout, cancellation, close-before-navigation,
   foreground loss, Bluetooth disablement, and permission-loss paths.
10. Verify no P2 advertisement changes, no P1 identity in P2, no GATT
    transfer, no ranging, no distance/direction claim, no persistent secret,
    and no secret-bearing logs.
11. Capture bounded logcat, UI hierarchy, test XML, and selector-qualified
    state without recording raw keys, tokens, addresses, or names.

One-device success is code/one-device qualification only. Two-device mutual
cryptographic authentication is required for any future P3 physical-complete
marker. Physical radio metadata privacy cannot be proven beyond the app's
boundary; it must be reported as a limitation.

## 29. P4 and P6 deferred boundaries

P4 may later consume an authenticated opaque channel and session-binding
output, but P3 must not define file/capsule formats, transfer scheduling,
resumption, history, or high-speed transport.

P6 may later consume a P3 authenticated identity as an input to physical trust,
but P3 must not add NFC, trust enrollment, TOFU, approval history, revocation,
or a TRUSTED state.

No P3 code may create a second identity key, alter the P1 alias, advertise
identity, or modify P2's fixed service-data frame.

## 30. Self-review and coverage result

The complete correction-round review is performed before production
implementation is authorized. Every requested correction is addressed:

| Correction | Result | Evidence |
| --- | --- | --- |
| A — initiation and peer selection | ADDRESSED | Section 1 and Section 8 freeze one selectable anonymous peer, BEGIN SECURE LINK, MAKE AVAILABLE, ACCEPT, explicit roles, simultaneous-initiation behavior, expiry, and the minimal existing-Field interaction. |
| B — GATT rendezvous | ADDRESSED | Section 8 freezes a mandatory separate connectable legacy P3 advertisement: one complete 128-bit service UUID, 18 owned bytes, 3-byte platform flags contribution, <=31-byte bound, 30-second lifetime, one-owner offer, no identity/token/name, and no extended fallback. |
| C — GATT transport framing | ADDRESSED | Section 8 freezes one server/client connection, PROPERTY_WRITE plus PROPERTY_NOTIFY, CCCD, write-with-response, MTU 247 request/default 23, 10-byte chunk header, 512 chunks, 4096-byte reassembly, ordering, duplicate, timeout, and close rules. |
| D — BLUETOOTH_CONNECT semantics | ADDRESSED | Sections 8 and 25 require CONNECT only after explicit action for API31+ client connectGatt or server open/operate; API29–30 use legacy BLUETOOTH expectations; API30/API34 client/server tests are required. |
| E — responder state machine | ADDRESSED | Section 18 adds AWAITING_INIT_HELLO, makes the responder receive INIT_HELLO before RESP_HELLO/RESP_AUTH, and gives exact role-specific transitions and checks. |
| F — identity disclosure order | ADDRESSED | Sections 7, 12, 13, and 14 put a responder-claim signature in RESP_AUTH; INIT_AUTH is sent only after that proof; H_R, H1, full confirmation, formats, and tests are updated. |
| G — Finished keys | ADDRESSED | Section 16 derives K_finished_i2r and K_finished_r2i with distinct labels; Section 17 uses the matching directional key and keeps role binding. |
| H — AEAD counters | ADDRESSED | Section 17 defines implicit counters, exact values (RESP_AUTH r2i/1, INIT_AUTH i2r/1, RESP_AUTH_CONFIRM r2i/2), non-consuming messages, send-failure, duplicate, timeout, and new-session behavior. |
| I — MITM claim | ADDRESSED | Section 5 distinguishes identity impersonation from valid-own-identity substitution and records the pre-P6 physical-intent limitation; Section 27 adds both cases to the test matrix. |
| J — protocol-security gate | ADDRESSED | This section and the implementation plan add a separate pre-Task-1 protocol-security review with explicit PASS/FAIL checks for ordering, transcript, disclosure, signatures, KDF, Finished, nonces, reflection, replay, unknown-key-share, misbinding, GATT binding, and P2-token binding. |

### Dedicated protocol-security review

| Review area | Verdict | Evidence checked |
| --- | --- | --- |
| Message ordering and responder receive-first rule | PASS | Initiator and responder paths plus exact event table; responder cannot emit RESP_HELLO before INIT_HELLO. |
| Transcript and version/role binding | PASS | Canonical H0, H_R, H1, domains, frame bytes, roles, lengths, suite, nonces, ephemeral keys, and P2 context. |
| Identity disclosure and signature coverage | PASS | Encrypted RESP_AUTH with responder-claim signature precedes INIT_AUTH; full responder confirmation signs H1. |
| Key schedule and Finished construction | PASS | Directional auth/session/Finished labels, HKDF salts, AAD, exact HMAC inputs, and counter table. |
| AEAD nonce uniqueness | PASS | Directional H0-derived prefixes, counters beginning at 1, no retransmission, and terminal session reset. |
| Role reflection and replay | PASS | Role-bound messages/signatures/MACs, strict state ordering, counters, fresh X25519/nonces, and P2 context. |
| Unknown-key-share and identity misbinding | PASS | SPKI re-encoding, SpatialDeviceId recomputation, H_R/H1 claim binding, token context, and one-use lease. |
| GATT channel binding | PASS | Selected P2-matched opaque platform lease, mandatory P3 service discovery, fixed service/characteristic UUIDs, one connection, and no address export. |
| P2 token binding | PASS | Current initiator/responder tokens are carried only in hello and H0; they are never identity or advertisement data. |

An independent scoped reviewer re-read the actual corrected specification and
plan rather than relying on this table. It returned PASS for every review area,
reported no Critical or Important findings, and confirmed that no P3 production
source or module directory exists.

### Separate verdicts

- Protocol security: PASS
- Architecture: PASS
- Privacy: PASS
- Android compatibility: PASS, contingent on the mandatory typed API30/API34
  provider and GATT matrix during implementation
- Implementation-plan executability: PASS
- Scope: PASS

The cross-phase review confirms that P1 remains the sole permanent identity,
P2 remains anonymous and unchanged, P3 is isolated behind core:handshake and
connectivity:handshake, P4 receives only a future opaque-channel boundary,
and P6 retains physical-trust semantics.

No P3 production source or module directory exists. The plan is corrected but
implementation is not authorized in this round.
