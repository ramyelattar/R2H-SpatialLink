package com.r2h.spatiallink.identity

import com.r2h.spatiallink.model.IdentityFailureCode
import com.r2h.spatiallink.model.IdentityRepositoryResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidKeyStoreIdentityRepositoryTest {
    @Test
    fun get_reports_missing_without_generating() = runTest {
        val port = FakeIdentityKeyStorePort()
        val repository = repository(port, StandardTestDispatcher(testScheduler))

        val result = repository.get()

        assertEquals(IdentityRepositoryResult.NotCreated, result)
        assertEquals(0, port.generateCalls)
    }

    @Test
    fun get_or_create_generates_once_and_self_tests_the_identity() = runTest {
        val port = FakeIdentityKeyStorePort()
        val repository = repository(port, StandardTestDispatcher(testScheduler))

        val first = repository.getOrCreate()
        val second = repository.getOrCreate()

        assertTrue(first is IdentityRepositoryResult.Available)
        assertEquals(first, second)
        assertEquals(1, port.generateCalls)
        assertEquals(2, port.signCalls)
        assertEquals(2, port.verifyCalls)
    }

    @Test
    fun existing_entry_is_loaded_without_regeneration() = runTest {
        val port = FakeIdentityKeyStorePort().apply {
            storedEntry = PlatformIdentityEntry(
                algorithm = "EC",
                isP256 = true,
                encodedPublicKey = byteArrayOf(8, 7, 6, 5),
                securityLevel = com.r2h.spatiallink.model.KeySecurityLevel.UNKNOWN,
            )
        }
        val repository = repository(port, StandardTestDispatcher(testScheduler))

        val result = repository.getOrCreate()

        assertTrue(result is IdentityRepositoryResult.Available)
        assertEquals(0, port.generateCalls)
    }

    @Test
    fun invalid_existing_entry_is_reported_without_rotation() = runTest {
        val port = FakeIdentityKeyStorePort().apply {
            storedEntry = PlatformIdentityEntry(
                algorithm = "RSA",
                isP256 = false,
                encodedPublicKey = byteArrayOf(1, 2, 3),
                securityLevel = com.r2h.spatiallink.model.KeySecurityLevel.UNKNOWN,
            )
        }
        val repository = repository(port, StandardTestDispatcher(testScheduler))

        val result = repository.getOrCreate()

        assertEquals(
            IdentityFailureCode.INVALID_KEY_ENTRY,
            (result as IdentityRepositoryResult.Failed).error.code,
        )
        assertEquals(0, port.generateCalls)
    }

    @Test
    fun generation_failure_is_typed_and_does_not_retry() = runTest {
        val port = FakeIdentityKeyStorePort().apply { failGeneration = true }
        val repository = repository(port, StandardTestDispatcher(testScheduler))

        val result = repository.getOrCreate()

        assertEquals(
            IdentityFailureCode.GENERATION_FAILED,
            (result as IdentityRepositoryResult.Failed).error.code,
        )
        assertEquals(1, port.generateCalls)
    }

    @Test
    fun failed_self_test_requires_recovery_without_replacement() = runTest {
        val port = FakeIdentityKeyStorePort().apply { verificationResult = false }
        val repository = repository(port, StandardTestDispatcher(testScheduler))

        val result = repository.getOrCreate()

        assertEquals(
            IdentityFailureCode.RECOVERY_REQUIRED,
            (result as IdentityRepositoryResult.Failed).error.code,
        )
        assertEquals(1, port.generateCalls)
    }

    @Test
    fun concurrent_get_or_create_callers_share_one_generated_identity() = runTest {
        val port = FakeIdentityKeyStorePort()
        val repository = repository(port, StandardTestDispatcher(testScheduler))

        val results = coroutineScope {
            List(20) { async { repository.getOrCreate() } }.map { it.await() }
        }

        assertTrue(results.all { it is IdentityRepositoryResult.Available })
        assertEquals(1, port.generateCalls)
        assertEquals(1, results.distinct().size)
    }

    @Test
    fun cancellation_is_rethrown_while_platform_read_is_suspended() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val port = FakeIdentityKeyStorePort().apply {
            readGate = CompletableDeferred<Unit>()
        }
        val repository = repository(port, dispatcher)
        val job = launch(dispatcher) { repository.get() }

        runCurrent()
        job.cancelAndJoin()
        advanceUntilIdle()

        assertTrue(job.isCancelled)
    }

    private fun repository(
        port: FakeIdentityKeyStorePort,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ): AndroidKeyStoreIdentityRepository = AndroidKeyStoreIdentityRepository(
        alias = "spatiallink.test.identity.repository",
        keyStore = port,
        dispatchers = IdentityTestDispatcherProvider(dispatcher),
        challengeSource = { byteArrayOf(9, 8, 7, 6) },
    )
}
