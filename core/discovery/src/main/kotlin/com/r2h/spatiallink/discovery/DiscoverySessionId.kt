package com.r2h.spatiallink.discovery

import java.security.SecureRandom

class DiscoverySessionId private constructor(private val bytes: ByteArray) {
    fun toByteArray(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiscoverySessionId) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        fun fromBytes(bytes: ByteArray): DiscoverySessionId? {
            if (bytes.size != DiscoveryProtocol.SESSION_ID_SIZE) {
                return null
            }
            if (bytes.all { it == 0.toByte() }) {
                return null
            }
            return DiscoverySessionId(bytes.copyOf())
        }
    }
}

sealed interface SessionIdGenerationResult {
    data class Success(val value: DiscoverySessionId) : SessionIdGenerationResult
    data object Failed : SessionIdGenerationResult
}

fun interface RandomByteSource {
    fun fill(target: ByteArray)
}

interface DiscoverySessionIdSource {
    fun next(): SessionIdGenerationResult
}

class SecureRandomDiscoverySessionIdSource(
    random: RandomByteSource? = null,
    private val maxAttempts: Int = DiscoveryProtocol.MAX_GENERATION_ATTEMPTS,
) : DiscoverySessionIdSource {
    private val secureRandom = SecureRandom()
    private val randomSource = random ?: RandomByteSource { target ->
        secureRandom.nextBytes(target)
    }

    init {
        require(maxAttempts > 0)
    }

    override fun next(): SessionIdGenerationResult {
        repeat(maxAttempts) {
            val bytes = ByteArray(DiscoveryProtocol.SESSION_ID_SIZE)
            randomSource.fill(bytes)
            DiscoverySessionId.fromBytes(bytes)?.let { sessionId ->
                return SessionIdGenerationResult.Success(sessionId)
            }
        }

        return SessionIdGenerationResult.Failed
    }
}
