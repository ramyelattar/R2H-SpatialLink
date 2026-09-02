package com.r2h.spatiallink.identity

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.r2h.spatiallink.model.IdentityFailureCode
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.KeyStoreException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.ProviderException
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory

class AndroidKeyStorePlatformAdapter(
    private val securityLevelInspector: KeySecurityLevelInspector = KeySecurityLevelInspector(),
) : IdentityKeyStorePort {
    override suspend fun read(alias: String): PlatformIdentityEntry? = try {
        val keyStore = openKeyStore()
        readEntry(keyStore, alias)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: IdentityPlatformException) {
        throw failure
    } catch (_: Exception) {
        throw IdentityPlatformException(IdentityFailureCode.KEYSTORE_UNAVAILABLE)
    }

    override suspend fun generate(alias: String): PlatformIdentityEntry = try {
        val keyStore = openKeyStore()
        check(!keyStore.containsAlias(alias)) {
            "The identity alias already exists."
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE_PROVIDER,
        )
        generator.initialize(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN,
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(P256_CURVE))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        generator.generateKeyPair()

        readEntry(keyStore, alias)
            ?: throw IdentityPlatformException(IdentityFailureCode.PUBLIC_KEY_UNAVAILABLE)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: IdentityPlatformException) {
        throw failure
    } catch (_: ProviderException) {
        throw IdentityPlatformException(IdentityFailureCode.KEYSTORE_UNAVAILABLE)
    } catch (_: GeneralSecurityException) {
        throw IdentityPlatformException(IdentityFailureCode.GENERATION_FAILED)
    } catch (_: Exception) {
        throw IdentityPlatformException(IdentityFailureCode.GENERATION_FAILED)
    }

    override suspend fun sign(alias: String, payload: ByteArray): ByteArray = try {
        val keyStore = openKeyStore()
        val entry = privateEntry(keyStore, alias)
            ?: throw IdentityPlatformException(IdentityFailureCode.NOT_FOUND)
        // The private key remains AndroidKeyStore-backed. Android's JCA provider
        // selects the compatible signing implementation for that key; the
        // AndroidKeyStore provider does not expose this algorithm by name on
        // every supported release (including API 34).
        Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initSign(entry.privateKey)
            update(payload)
        }.sign()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: IdentityPlatformException) {
        throw failure
    } catch (_: ProviderException) {
        throw IdentityPlatformException(IdentityFailureCode.SIGNING_FAILED)
    } catch (_: GeneralSecurityException) {
        throw IdentityPlatformException(IdentityFailureCode.SIGNING_FAILED)
    } catch (_: Exception) {
        throw IdentityPlatformException(IdentityFailureCode.SIGNING_FAILED)
    }

    override suspend fun verify(
        encodedPublicKey: ByteArray,
        payload: ByteArray,
        signature: ByteArray,
    ): Boolean = try {
        val publicKey = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            .generatePublic(X509EncodedKeySpec(encodedPublicKey.copyOf()))
        Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initVerify(publicKey)
            update(payload)
        }.verify(signature.copyOf())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: GeneralSecurityException) {
        false
    } catch (_: Exception) {
        false
    }

    private fun openKeyStore(): KeyStore = try {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).also { it.load(null) }
    } catch (_: KeyStoreException) {
        throw IdentityPlatformException(IdentityFailureCode.KEYSTORE_UNAVAILABLE)
    } catch (_: IOException) {
        throw IdentityPlatformException(IdentityFailureCode.KEYSTORE_UNAVAILABLE)
    } catch (_: NoSuchAlgorithmException) {
        throw IdentityPlatformException(IdentityFailureCode.KEYSTORE_UNAVAILABLE)
    } catch (_: CertificateException) {
        throw IdentityPlatformException(IdentityFailureCode.KEYSTORE_UNAVAILABLE)
    }

    private fun readEntry(
        keyStore: KeyStore,
        alias: String,
    ): PlatformIdentityEntry? {
        if (!keyStore.containsAlias(alias)) {
            return null
        }
        val entry = privateEntry(keyStore, alias)
            ?: throw IdentityPlatformException(IdentityFailureCode.INVALID_KEY_ENTRY)
        val publicKey = entry.certificate?.publicKey
            ?: throw IdentityPlatformException(IdentityFailureCode.PUBLIC_KEY_UNAVAILABLE)
        val encodedPublicKey = publicKey.encoded
            ?: throw IdentityPlatformException(IdentityFailureCode.PUBLIC_KEY_UNAVAILABLE)
        if (encodedPublicKey.isEmpty()) {
            throw IdentityPlatformException(IdentityFailureCode.PUBLIC_KEY_UNAVAILABLE)
        }
        if (publicKey.algorithm != KeyProperties.KEY_ALGORITHM_EC) {
            throw IdentityPlatformException(IdentityFailureCode.INVALID_KEY_ENTRY)
        }

        return PlatformIdentityEntry(
            algorithm = publicKey.algorithm,
            isP256 = isP256(publicKey),
            encodedPublicKey = encodedPublicKey.copyOf(),
            securityLevel = securityLevelInspector.inspect(entry.privateKey),
        )
    }

    private fun privateEntry(keyStore: KeyStore, alias: String): KeyStore.PrivateKeyEntry? {
        if (!keyStore.containsAlias(alias)) {
            return null
        }
        return try {
            keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        } catch (_: GeneralSecurityException) {
            throw IdentityPlatformException(IdentityFailureCode.INVALID_KEY_ENTRY)
        }
    }

    private fun isP256(publicKey: PublicKey): Boolean {
        val ecPublicKey = publicKey as? ECPublicKey ?: return false
        val actual = ecPublicKey.params
        val expected = AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec(P256_CURVE))
        }.getParameterSpec(ECParameterSpec::class.java)
        return actual.matches(expected)
    }

    private fun ECParameterSpec.matches(other: ECParameterSpec): Boolean {
        val thisField = curve.field as? ECFieldFp
        val otherField = other.curve.field as? ECFieldFp
        return thisField != null &&
            otherField != null &&
            thisField.p == otherField.p &&
            curve.a == other.curve.a &&
            curve.b == other.curve.b &&
            generator.affineX == other.generator.affineX &&
            generator.affineY == other.generator.affineY &&
            order == other.order &&
            cofactor == other.cofactor
    }

    companion object {
        const val ANDROID_KEYSTORE_PROVIDER: String = "AndroidKeyStore"
        const val SIGNATURE_ALGORITHM: String = "SHA256withECDSA"
        const val P256_CURVE: String = "secp256r1"
    }
}
