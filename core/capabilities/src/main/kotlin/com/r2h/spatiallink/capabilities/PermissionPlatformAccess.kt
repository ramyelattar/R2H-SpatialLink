package com.r2h.spatiallink.capabilities

import android.content.Context
import android.content.pm.PackageManager
import com.r2h.spatiallink.model.PermissionKey

enum class PermissionLookupState {
    GRANTED,
    DENIED,
    NOT_DECLARED,
    UNKNOWN,
}

interface PermissionPlatformAccess {
    fun lookup(permissionName: String): PermissionLookupState
}

class AndroidPermissionPlatformAccess(
    context: Context,
) : PermissionPlatformAccess {
    private val applicationContext: Context = context.applicationContext
    private val declaredPermissions: Set<String>? = loadDeclaredPermissions()

    override fun lookup(permissionName: String): PermissionLookupState {
        val declared = declaredPermissions ?: return PermissionLookupState.UNKNOWN
        if (permissionName !in declared) {
            return PermissionLookupState.NOT_DECLARED
        }

        return try {
            when (applicationContext.checkSelfPermission(permissionName)) {
                PackageManager.PERMISSION_GRANTED -> PermissionLookupState.GRANTED
                PackageManager.PERMISSION_DENIED -> PermissionLookupState.DENIED
                else -> PermissionLookupState.UNKNOWN
            }
        } catch (_: Exception) {
            PermissionLookupState.UNKNOWN
        }
    }

    private fun loadDeclaredPermissions(): Set<String>? = try {
        applicationContext.packageManager
            .getPackageInfo(applicationContext.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toSet()
            ?: emptySet()
    } catch (_: Exception) {
        null
    }
}

internal fun permissionName(key: PermissionKey): String = when (key) {
    PermissionKey.BLUETOOTH_SCAN -> "android.permission.BLUETOOTH_SCAN"
    PermissionKey.BLUETOOTH_ADVERTISE -> "android.permission.BLUETOOTH_ADVERTISE"
    PermissionKey.BLUETOOTH_CONNECT -> "android.permission.BLUETOOTH_CONNECT"
    PermissionKey.NEARBY_WIFI_DEVICES -> "android.permission.NEARBY_WIFI_DEVICES"
    PermissionKey.ACCESS_LOCAL_NETWORK -> "android.permission.ACCESS_LOCAL_NETWORK"
    PermissionKey.RANGING -> "android.permission.RANGING"
}
