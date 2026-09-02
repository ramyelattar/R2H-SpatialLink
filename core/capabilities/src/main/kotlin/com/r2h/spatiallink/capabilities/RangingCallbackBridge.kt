package com.r2h.spatiallink.capabilities

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

interface RangingCallbackRegistration {
    fun register(callback: (Map<Int, Int>) -> Unit)

    fun unregister()
}

class CancellableRangingCapabilityReader(
    private val registration: RangingCallbackRegistration,
) {
    suspend fun read(): Map<Int, Int> = suspendCancellableCoroutine { continuation ->
        val operationLock = Any()
        val completed = AtomicBoolean(false)
        val unregistered = AtomicBoolean(false)
        var registrationFinished = false
        var unregisterRequested = false

        fun unregisterOnce() {
            if (unregistered.compareAndSet(false, true)) {
                try {
                    registration.unregister()
                } catch (_: Exception) {
                    // Cleanup is best effort; the registration is still guarded against repetition.
                }
            }
        }

        fun requestUnregister() {
            val shouldUnregister = synchronized(operationLock) {
                unregisterRequested = true
                registrationFinished
            }
            if (shouldUnregister) {
                unregisterOnce()
            }
        }

        val callback: (Map<Int, Int>) -> Unit = { values ->
            val firstResult = completed.compareAndSet(false, true)
            requestUnregister()
            if (firstResult && continuation.isActive) {
                continuation.resume(values.toMap())
            }
        }

        continuation.invokeOnCancellation {
            completed.compareAndSet(false, true)
            requestUnregister()
        }

        var registrationFailure: Throwable? = null
        var shouldUnregisterAfterRegistration = false
        synchronized(operationLock) {
            if (!completed.get()) {
                try {
                    registration.register(callback)
                } catch (failure: Throwable) {
                    registrationFailure = failure
                    completed.compareAndSet(false, true)
                    unregisterRequested = true
                } finally {
                    registrationFinished = true
                    shouldUnregisterAfterRegistration = unregisterRequested
                }
            } else {
                registrationFinished = true
                shouldUnregisterAfterRegistration = unregisterRequested
            }
        }

        if (shouldUnregisterAfterRegistration) {
            unregisterOnce()
        }

        registrationFailure?.let { failure ->
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(failure))
            }
        }
    }
}
