package com.r2h.spatiallink.discovery

class PeerCache(
    private val localSessionId: DiscoverySessionId,
    private val clock: MonotonicClock,
) {
    private val entriesBySessionId = linkedMapOf<DiscoverySessionId, NearbyPeer>()

    fun observe(observation: DiscoveryObservation) {
        evictExpired(nowElapsedMs = clock.elapsedRealtimeMs())
        if (observation.sessionId == localSessionId) {
            return
        }

        entriesBySessionId[observation.sessionId] = NearbyPeer(
            sessionId = observation.sessionId,
            rssiDbm = observation.rssiDbm,
            lastSeenElapsedMs = observation.receivedAtElapsedMs,
        )
        enforceCapacity()
    }

    fun snapshot(): List<NearbyPeer> {
        evictExpired(nowElapsedMs = clock.elapsedRealtimeMs())
        return entriesBySessionId.values
            .sortedWith(
                compareByDescending<NearbyPeer> { it.lastSeenElapsedMs }
                    .thenBy { it.sessionId.toSortableKey() },
            )
            .toList()
    }

    fun clear() {
        entriesBySessionId.clear()
    }

    private fun evictExpired(nowElapsedMs: Long) {
        val iterator = entriesBySessionId.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (nowElapsedMs - entry.value.lastSeenElapsedMs >= DiscoveryProtocol.PEER_TTL_MS) {
                iterator.remove()
            }
        }
    }

    private fun enforceCapacity() {
        if (entriesBySessionId.size <= DiscoveryProtocol.MAX_PEERS) {
            return
        }

        entriesBySessionId.entries
            .sortedWith(
                compareBy<Map.Entry<DiscoverySessionId, NearbyPeer>> { it.value.lastSeenElapsedMs }
                    .thenBy { it.key.toSortableKey() },
            )
            .take(entriesBySessionId.size - DiscoveryProtocol.MAX_PEERS)
            .forEach { entry ->
                entriesBySessionId.remove(entry.key)
            }
    }
}

private fun DiscoverySessionId.toSortableKey(): String =
    toByteArray().joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
