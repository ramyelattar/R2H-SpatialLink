package com.r2h.spatiallink.identity

import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.r2h.spatiallink.model.KeySecurityLevel
import java.security.KeyFactory
import java.security.PrivateKey

@RequiresApi(31)
class Api31KeySecurityLevelReader {
    fun read(privateKey: PrivateKey): KeySecurityLevel {
        val keyInfo = keyInfoFor(privateKey)
        return when (keyInfo.securityLevel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> KeySecurityLevel.STRONGBOX
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ->
                KeySecurityLevel.TRUSTED_ENVIRONMENT
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> KeySecurityLevel.SOFTWARE
            KeyProperties.SECURITY_LEVEL_UNKNOWN -> KeySecurityLevel.UNKNOWN
            else -> KeySecurityLevel.UNKNOWN
        }
    }

    private fun keyInfoFor(privateKey: PrivateKey): KeyInfo = KeyFactory
        .getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        .getKeySpec(privateKey, KeyInfo::class.java)
}
