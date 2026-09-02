package com.r2h.spatiallink.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertSame
import org.junit.Test

class DispatcherProviderTest {
    @Test
    fun fixed_provider_returns_the_supplied_dispatcher() {
        val supplied = StandardTestDispatcher()

        assertSame(supplied, FixedDispatcherProvider(supplied).default)
    }

    @Test
    fun fixed_provider_returns_the_supplied_io_dispatcher() {
        val supplied = StandardTestDispatcher()

        assertSame(supplied, FixedDispatcherProvider(supplied).io)
    }

    private class FixedDispatcherProvider(
        override val default: CoroutineDispatcher,
        override val io: CoroutineDispatcher = default,
    ) : DispatcherProvider
}
