package com.r2h.spatiallink

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.r2h.spatiallink.common.DefaultDispatcherProvider
import com.r2h.spatiallink.identity.AndroidKeyStoreIdentityRepository
import com.r2h.spatiallink.identity.AndroidKeyStorePlatformAdapter
import com.r2h.spatiallink.model.IdentityRepositoryResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionIdentityInstrumentedTest {
    @Test
    fun production_alias_reports_full_canonical_id_without_replacement() = runTest {
        val repository = AndroidKeyStoreIdentityRepository(
            alias = AndroidKeyStoreIdentityRepository.PRODUCTION_ALIAS,
            keyStore = AndroidKeyStorePlatformAdapter(),
            dispatchers = DefaultDispatcherProvider(),
        )
        val result = repository.getOrCreate()

        assertTrue("production identity result: $result", result is IdentityRepositoryResult.Available)
        val available = result as IdentityRepositoryResult.Available
        Log.i(
            "SpatialLinkP1Test",
            "SPATIALLINK_PRODUCTION_CANONICAL_ID=" +
                available.inspection.identity.id.canonicalHex(),
        )
    }
}
