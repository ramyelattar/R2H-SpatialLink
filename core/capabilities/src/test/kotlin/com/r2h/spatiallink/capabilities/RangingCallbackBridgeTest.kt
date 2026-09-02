package com.r2h.spatiallink.capabilities

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RangingCallbackBridgeTest {
    @Test
    fun callback_resumes_read_and_unregisters_exactly_once() = runTest {
        val registration = FakeRangingCallbackRegistration()
        val reader = CancellableRangingCapabilityReader(registration)
        var result: Map<Int, Int>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            result = reader.read()
        }

        runCurrent()
        registration.emit(mapOf(1 to 2))
        registration.emit(mapOf(3 to 4))
        job.join()

        assertEquals(mapOf(1 to 2), result)
        assertEquals(1, registration.unregisterCount)
        job.cancelAndJoin()
        assertEquals(1, registration.unregisterCount)
    }

    @Test
    fun cancellation_unregisters_once_without_callback() = runTest {
        val registration = FakeRangingCallbackRegistration()
        val reader = CancellableRangingCapabilityReader(registration)
        val job = launch { reader.read() }

        runCurrent()
        job.cancelAndJoin()

        assertEquals(1, registration.unregisterCount)
    }

    @Test
    fun registration_failure_is_returned_to_the_api_boundary() = runTest {
        val failure = IllegalStateException("registration failed")
        val registration = FakeRangingCallbackRegistration(registerFailure = failure)
        val reader = CancellableRangingCapabilityReader(registration)

        try {
            reader.read()
            fail("read should fail when registration fails")
        } catch (actual: IllegalStateException) {
            assertEquals(failure::class, actual::class)
            assertEquals(failure.message, actual.message)
        }

        assertEquals(1, registration.unregisterCount)
    }

    private class FakeRangingCallbackRegistration(
        private val registerFailure: Throwable? = null,
    ) : RangingCallbackRegistration {
        private var callback: ((Map<Int, Int>) -> Unit)? = null
        var unregisterCount: Int = 0
            private set

        override fun register(callback: (Map<Int, Int>) -> Unit) {
            this.callback = callback
            registerFailure?.let { throw it }
        }

        override fun unregister() {
            unregisterCount += 1
            callback = null
        }

        fun emit(values: Map<Int, Int>) {
            callback?.invoke(values)
        }
    }
}
