package com.r2h.spatiallink.ble

import com.r2h.spatiallink.discovery.BluetoothState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class AndroidBluetoothStateReaderTest {
    @Test
    fun null_adapter_is_unsupported() {
        assertEquals(
            BluetoothState.UNSUPPORTED,
            reader(adapterAvailable = false).read(),
        )
    }

    @Test
    fun disabled_adapter_is_disabled() {
        assertEquals(
            BluetoothState.DISABLED,
            reader(enabled = false).read(),
        )
    }

    @Test
    fun missing_adapter_is_unsupported_without_requesting_le_operations() {
        assertEquals(
            BluetoothState.UNSUPPORTED,
            reader(adapterAvailable = false).read(),
        )
    }

    @Test
    fun state_reader_source_does_not_acquire_scanner_or_advertiser_objects() {
        val source = Files.readString(
            Path.of("src/main/kotlin/com/r2h/spatiallink/ble/AndroidBluetoothStateReader.kt"),
        )

        assertFalse(source.contains("bluetoothLeScanner"))
        assertFalse(source.contains("bluetoothLeAdvertiser"))
    }

    @Test
    fun enabled_adapter_with_both_le_operations_is_enabled() {
        assertEquals(BluetoothState.ENABLED, reader().read())
    }

    private fun reader(
        adapterAvailable: Boolean = true,
        enabled: Boolean = true,
    ): AndroidBluetoothStateReader =
        AndroidBluetoothStateReader.forTest(
            object : AndroidBluetoothStatePlatform {
                override fun probe(): AndroidBluetoothStateSnapshot =
                    AndroidBluetoothStateSnapshot(
                        adapterAvailable = adapterAvailable,
                        enabled = enabled,
                    )
            },
        )
}
