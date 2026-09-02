package com.r2h.spatiallink.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerCacheTest {
    @Test
    fun repeated_observations_update_a_single_entry() {
        val clock = TestMonotonicClock(nowElapsedMs = 1_000L)
        val cache = PeerCache(localSessionId = sessionId(9), clock = clock)
        val peer = sessionId(1)

        cache.observe(
            DiscoveryObservation(
                sessionId = peer,
                rssiDbm = -70,
                receivedAtElapsedMs = 1_000L,
            ),
        )
        cache.observe(
            DiscoveryObservation(
                sessionId = peer,
                rssiDbm = -55,
                receivedAtElapsedMs = 1_500L,
            ),
        )

        assertEquals(
            listOf(
                NearbyPeer(
                    sessionId = peer,
                    rssiDbm = -55,
                    lastSeenElapsedMs = 1_500L,
                ),
            ),
            cache.snapshot(),
        )
    }

    @Test
    fun local_session_token_is_ignored() {
        val localSessionId = sessionId(9)
        val cache = PeerCache(localSessionId = localSessionId, clock = TestMonotonicClock())

        cache.observe(
            DiscoveryObservation(
                sessionId = localSessionId,
                rssiDbm = -40,
                receivedAtElapsedMs = 100L,
            ),
        )

        assertTrue(cache.snapshot().isEmpty())
    }

    @Test
    fun entry_at_exactly_six_seconds_is_expired_by_the_half_open_ttl_rule() {
        val clock = TestMonotonicClock(nowElapsedMs = 6_000L)
        val cache = PeerCache(localSessionId = sessionId(9), clock = clock)

        cache.observe(
            DiscoveryObservation(
                sessionId = sessionId(1),
                rssiDbm = -65,
                receivedAtElapsedMs = 0L,
            ),
        )

        assertTrue(cache.snapshot().isEmpty())
    }

    @Test
    fun stale_entries_are_removed_before_capacity_enforcement() {
        val clock = TestMonotonicClock(nowElapsedMs = 6_000L)
        val cache = PeerCache(localSessionId = sessionId(99), clock = clock)

        cache.observe(
            DiscoveryObservation(
                sessionId = sessionId(1),
                rssiDbm = -90,
                receivedAtElapsedMs = 0L,
            ),
        )

        clock.setNow(6_001L)
        repeat(DiscoveryProtocol.MAX_PEERS) { index ->
            val tokenValue = index + 2
            cache.observe(
                DiscoveryObservation(
                    sessionId = sessionId(tokenValue),
                    rssiDbm = -50 - index,
                    receivedAtElapsedMs = 6_001L + index,
                ),
            )
        }

        val peers = cache.snapshot()

        assertEquals(DiscoveryProtocol.MAX_PEERS, peers.size)
        assertFalse(peers.any { it.sessionId == sessionId(1) })
    }

    @Test
    fun cache_never_exceeds_sixty_four_entries() {
        val clock = TestMonotonicClock(nowElapsedMs = 1_000L)
        val cache = PeerCache(localSessionId = sessionId(99), clock = clock)

        repeat(DiscoveryProtocol.MAX_PEERS + 5) { index ->
            cache.observe(
                DiscoveryObservation(
                    sessionId = sessionId(index + 1),
                    rssiDbm = -40,
                    receivedAtElapsedMs = 1_000L + index,
                ),
            )
        }

        assertEquals(DiscoveryProtocol.MAX_PEERS, cache.snapshot().size)
    }

    @Test
    fun equal_timestamps_use_lexicographic_token_bytes_as_the_deterministic_tie_breaker() {
        val clock = TestMonotonicClock(nowElapsedMs = 10L)
        val cache = PeerCache(localSessionId = sessionId(99), clock = clock)
        val smaller = sessionId(1)
        val larger = DiscoverySessionId.fromBytes(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 2))!!

        cache.observe(
            DiscoveryObservation(
                sessionId = larger,
                rssiDbm = -40,
                receivedAtElapsedMs = 10L,
            ),
        )
        cache.observe(
            DiscoveryObservation(
                sessionId = smaller,
                rssiDbm = -41,
                receivedAtElapsedMs = 10L,
            ),
        )

        assertEquals(listOf(smaller, larger), cache.snapshot().map(NearbyPeer::sessionId))
    }

    @Test
    fun nearby_peer_exposes_only_anonymous_presence_fields() {
        val fieldNames = NearbyPeer::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()

        assertEquals(
            setOf("sessionId", "rssiDbm", "lastSeenElapsedMs", "authenticity"),
            fieldNames,
        )
        assertFalse(fieldNames.contains("distanceMeters"))
        assertFalse(fieldNames.contains("azimuthDegrees"))
        assertFalse(fieldNames.contains("bearingDegrees"))
        assertFalse(fieldNames.contains("orientation"))
        assertFalse(fieldNames.contains("direction"))
    }

    private fun sessionId(lastByte: Int): DiscoverySessionId =
        DiscoverySessionId.fromBytes(byteArrayOf(0, 0, 0, 0, 0, 0, 0, lastByte.toByte()))!!
}
