package com.r2h.spatiallink.ble

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.r2h.spatiallink.discovery.DiscoveryOperation
import com.r2h.spatiallink.discovery.DiscoveryPermission
import com.r2h.spatiallink.discovery.DiscoveryPermissionPolicy
import com.r2h.spatiallink.discovery.DiscoveryPermissionReader

fun interface AndroidPermissionChecker {
    fun checkSelfPermission(permission: String): Int
}

class AndroidDiscoveryPermissionReader private constructor(
    private val sdkInt: Int,
    private val checker: AndroidPermissionChecker,
) : DiscoveryPermissionReader {
    constructor(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ) : this(
        sdkInt = sdkInt,
        checker = AndroidPermissionChecker { permission ->
            context.applicationContext.checkSelfPermission(permission)
        },
    )

    override fun missing(operation: DiscoveryOperation): Set<DiscoveryPermission> =
        DiscoveryPermissionPolicy.required(sdkInt, operation)
            .filterTo(linkedSetOf()) { permission ->
                checker.checkSelfPermission(permission.manifestName()) !=
                    PackageManager.PERMISSION_GRANTED
            }

    @SuppressLint("InlinedApi")
    private fun DiscoveryPermission.manifestName(): String =
        when (this) {
            DiscoveryPermission.LEGACY_FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
            DiscoveryPermission.BLUETOOTH_SCAN -> Manifest.permission.BLUETOOTH_SCAN
            DiscoveryPermission.BLUETOOTH_ADVERTISE -> Manifest.permission.BLUETOOTH_ADVERTISE
            DiscoveryPermission.BLUETOOTH_CONNECT -> Manifest.permission.BLUETOOTH_CONNECT
        }

    companion object {
        internal fun forTest(
            sdkInt: Int,
            checker: (String) -> Int,
        ): AndroidDiscoveryPermissionReader =
            AndroidDiscoveryPermissionReader(
                sdkInt = sdkInt,
                checker = AndroidPermissionChecker(checker),
            )
    }
}
