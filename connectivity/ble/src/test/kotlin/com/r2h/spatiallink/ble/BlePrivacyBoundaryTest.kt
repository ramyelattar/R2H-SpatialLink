package com.r2h.spatiallink.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class BlePrivacyBoundaryTest {
    @Test
    fun production_ble_sources_do_not_read_android_device_identity_or_persist_discovery() {
        val root = Path.of("src/main/kotlin/com/r2h/spatiallink/ble")
        val source = Files.walk(root).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }
                .map { Files.readString(it) }
                .reduce("", String::plus)
        }

        assertPrivacySafe(source)
        assertTrue(source.contains("scanRecord"))
        assertTrue(source.contains("getServiceData"))
    }

    @Test
    fun structural_check_catches_direct_and_renamed_device_access_paths() {
        assertRejected("fun read(result: ScanResult) = result.device")
        assertRejected(
            "fun helper(result: ScanResult) = result.getDevice()" +
                " fun read(result: ScanResult) = helper(result)",
        )
        assertRejected("fun read(result: ScanResult) = result.address")
        assertRejected("fun read(result: ScanResult) = result.toString()")
    }

    private fun assertRejected(source: String) {
        val rejected = runCatching { assertPrivacySafe(source) }.exceptionOrNull()
        assertTrue("privacy source check accepted a forbidden access path", rejected != null)
    }

    private fun assertPrivacySafe(source: String) {
        val code = source
            .replace(Regex("(?s)/\\*.*?\\*/"), " ")
            .replace(Regex("(?m)//.*$"), " ")
            .replace(Regex("(?s)\"(?:\\\\.|[^\"\\\\])*\""), " ")
        val forbidden = Regex(
            """\b(?:device|address|name|alias|bondState|getDevice|getAddress|getName|getAlias|getBondState)\b|\btoString\s*\(|\bBluetoothDevice\b|SpatialDeviceId|AndroidKeyStore|SharedPreferences|RoomDatabase|DataStore""",
        )
        assertFalse(
            "forbidden BLE identity or persistence access in structural source",
            forbidden.containsMatchIn(code),
        )
    }
}
