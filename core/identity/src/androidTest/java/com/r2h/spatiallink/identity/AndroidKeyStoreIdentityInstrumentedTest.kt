package com.r2h.spatiallink.identity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.r2h.spatiallink.common.DefaultDispatcherProvider
import com.r2h.spatiallink.model.IdentityRepositoryResult
import com.r2h.spatiallink.model.IdentityResult
import com.r2h.spatiallink.model.SignatureVerification
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreIdentityInstrumentedTest {
    @Test
    fun direct_platform_adapter_signs_and_verifies_a_test_alias() = runTest {
        withTestRepository("adapter") { alias, _ ->
            val adapter = AndroidKeyStorePlatformAdapter()
            val entry = adapter.generate(alias)
            val payload = "adapter-test".encodeToByteArray()
            val signature = adapter.sign(alias, payload)

            assertTrue("platform entry: $entry", entry.isP256)
            assertTrue("signature length: ${signature.size}", signature.isNotEmpty())
            assertTrue(adapter.verify(entry.encodedPublicKey, payload, signature))
        }
    }

    @Test
    fun keystore_identity_loads_stably_and_private_key_is_not_exportable() = runTest {
        withTestRepository("stable") { alias, repository ->
            val firstResult = repository.getOrCreate()
            assertTrue("identity creation result: $firstResult", firstResult is IdentityRepositoryResult.Available)
            val first = firstResult as IdentityRepositoryResult.Available
            val secondRepository = repositoryFor(alias)
            val secondResult = secondRepository.getOrCreate()
            assertTrue("identity reload result: $secondResult", secondResult is IdentityRepositoryResult.Available)
            val second = secondResult as IdentityRepositoryResult.Available

            assertEquals(
                first.inspection.identity.id.canonicalHex(),
                second.inspection.identity.id.canonicalHex(),
            )
            println(
                "SPATIALLINK_CANONICAL_ID=${first.inspection.identity.id.canonicalHex()}",
            )
            println(
                "SPATIALLINK_SECURITY_LEVEL=${first.inspection.identity.securityLevel.name}",
            )

            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
            assertNull(entry.privateKey.encoded)
            assertNotNull(entry.certificate.publicKey.encoded)
        }
    }

    @Test
    fun keystore_signature_verifies_original_and_rejects_modified_payload() = runTest {
        withTestRepository("sign") { _, repository ->
            val creation = repository.getOrCreate()
            assertTrue("identity creation result: $creation", creation is IdentityRepositoryResult.Available)
            val payload = "spatiallink-p1-test".encodeToByteArray()
            val signatureResult = repository.sign(payload)
            assertTrue("signature result: $signatureResult", signatureResult is IdentityResult.Success)
            val signature = (signatureResult as IdentityResult.Success).value

            val verified = repository.verify(payload, signature)
            val rejected = repository.verify("spatiallink-p1-tesT".encodeToByteArray(), signature)

            assertEquals(
                SignatureVerification.VERIFIED,
                (verified as IdentityResult.Success).value,
            )
            assertEquals(
                SignatureVerification.REJECTED,
                (rejected as IdentityResult.Success).value,
            )
        }
    }

    @Test
    fun concurrent_keystore_callers_observe_one_stable_identity() = runTest {
        withTestRepository("concurrent") { _, repository ->
            val results = coroutineScope {
                List(12) { async { repository.getOrCreate() } }.awaitAll()
            }
            assertTrue("identity results: $results", results.all { it is IdentityRepositoryResult.Available })
            val identities = results.map {
                (it as IdentityRepositoryResult.Available).inspection.identity.id
            }.toSet()

            assertEquals(1, identities.size)
        }
    }

    private suspend fun withTestRepository(
        name: String,
        block: suspend (String, AndroidKeyStoreIdentityRepository) -> Unit,
    ) {
        val alias = "spatiallink.test.identity.$name.${System.nanoTime()}"
        deleteOwnAlias(alias)
        try {
            block(alias, repositoryFor(alias))
        } finally {
            deleteOwnAlias(alias)
        }
    }

    private fun repositoryFor(alias: String): AndroidKeyStoreIdentityRepository =
        AndroidKeyStoreIdentityRepository(
            alias = alias,
            keyStore = AndroidKeyStorePlatformAdapter(),
            dispatchers = DefaultDispatcherProvider(),
        )

    private fun deleteOwnAlias(alias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }
}
