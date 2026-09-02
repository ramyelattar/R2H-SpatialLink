package com.r2h.spatiallink.connectivity.handshake

import android.os.Build
import com.r2h.spatiallink.handshake.CryptoFailureCode
import com.r2h.spatiallink.handshake.CryptoResult
import com.r2h.spatiallink.handshake.EphemeralKeyPair
import com.r2h.spatiallink.handshake.HandshakeCryptoPort
import org.conscrypt.Conscrypt
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Provider
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AndroidHandshakeCryptoProvider private constructor(
    private val sdkInt: Int,
    private val selector: PlatformCryptoSelector,
) : HandshakeCryptoPort {
    constructor() : this(
        sdkInt = Build.VERSION.SDK_INT,
        selector = DefaultPlatformCryptoSelector,
    )

    override fun generateEphemeral(): CryptoResult<EphemeralKeyPair> =
        withPrimitives { it.generateEphemeral() }

    override fun deriveX25519(
        privateKey: EphemeralKeyPair,
        peerPublicKey: ByteArray,
    ): CryptoResult<ByteArray> = withPrimitives { it.deriveX25519(privateKey, peerPublicKey) }

    override fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance(SHA_256).digest(input.copyOf())

    override fun hmacSha256(
        key: ByteArray,
        input: ByteArray,
    ): ByteArray = Mac.getInstance(HMAC_SHA_256).run {
        init(SecretKeySpec(key.copyOf(), HMAC_SHA_256))
        doFinal(input.copyOf())
    }

    override fun aeadEncrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        plaintext: ByteArray,
    ): CryptoResult<ByteArray> = withPrimitives {
        it.aeadEncrypt(key, nonce, aad, plaintext)
    }

    override fun aeadDecrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        ciphertextAndTag: ByteArray,
    ): CryptoResult<ByteArray> = withPrimitives {
        it.aeadDecrypt(key, nonce, aad, ciphertextAndTag)
    }

    override fun verifyP256(
        publicKeySpki: ByteArray,
        input: ByteArray,
        signature: ByteArray,
    ): CryptoResult<Boolean> = withPrimitives {
        it.verifyP256(publicKeySpki, input, signature)
    }

    internal fun providerPathForTest(): ProviderPath? =
        (selection as? PlatformCryptoSelection.Available)?.path

    internal fun expectedProviderPathForCurrentApi(): ProviderPath =
        if (sdkInt >= PLATFORM_XDH_MIN_API) {
            ProviderPath.PLATFORM_XDH
        } else {
            ProviderPath.CONSCRYPT_XDH
        }

    private val selection: PlatformCryptoSelection by lazy(LazyThreadSafetyMode.PUBLICATION) {
        selector.select(sdkInt)
    }

    private inline fun <T> withPrimitives(
        operation: (PlatformCryptoPrimitives) -> CryptoResult<T>,
    ): CryptoResult<T> = when (val resolved = selection) {
        is PlatformCryptoSelection.Available -> operation(resolved.primitives)
        is PlatformCryptoSelection.Unavailable -> CryptoResult.Failure(
            code = CryptoFailureCode.CRYPTO_UNAVAILABLE,
            detail = resolved.detail,
        )
    }

    companion object {
        internal fun forTest(
            sdkInt: Int,
            selector: PlatformCryptoSelector,
        ): AndroidHandshakeCryptoProvider = AndroidHandshakeCryptoProvider(
            sdkInt = sdkInt,
            selector = selector,
        )

        private const val HMAC_SHA_256 = "HmacSHA256"
        private const val PLATFORM_XDH_MIN_API = 33
        private const val SHA_256 = "SHA-256"
    }
}

internal enum class ProviderPath {
    PLATFORM_XDH,
    CONSCRYPT_XDH,
}

internal sealed interface PlatformCryptoSelection {
    data class Available(
        val path: ProviderPath,
        val primitives: PlatformCryptoPrimitives,
    ) : PlatformCryptoSelection

    data class Unavailable(
        val detail: String,
    ) : PlatformCryptoSelection

    companion object {
        fun available(
            path: ProviderPath,
            primitives: PlatformCryptoPrimitives,
        ): PlatformCryptoSelection = Available(path, primitives)

        fun unavailable(detail: String): PlatformCryptoSelection = Unavailable(detail)
    }
}

internal fun interface PlatformCryptoSelector {
    fun select(sdkInt: Int): PlatformCryptoSelection
}

internal interface PlatformCryptoPrimitives {
    fun generateEphemeral(): CryptoResult<EphemeralKeyPair>

    fun deriveX25519(
        keyPair: EphemeralKeyPair,
        peerPublicKey: ByteArray,
    ): CryptoResult<ByteArray>

    fun sha256(input: ByteArray): ByteArray

    fun hmacSha256(
        key: ByteArray,
        input: ByteArray,
    ): ByteArray

    fun aeadEncrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        plaintext: ByteArray,
    ): CryptoResult<ByteArray>

    fun aeadDecrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        ciphertextAndTag: ByteArray,
    ): CryptoResult<ByteArray>

    fun verifyP256(
        publicKeySpki: ByteArray,
        input: ByteArray,
        signature: ByteArray,
    ): CryptoResult<Boolean>
}

private object DefaultPlatformCryptoSelector : PlatformCryptoSelector {
    override fun select(sdkInt: Int): PlatformCryptoSelection = try {
        if (sdkInt >= PLATFORM_XDH_MIN_API) {
            PlatformCryptoSelection.available(
                path = ProviderPath.PLATFORM_XDH,
                primitives = JcaX25519Primitives(provider = null),
            )
        } else {
            PlatformCryptoSelection.available(
                path = ProviderPath.CONSCRYPT_XDH,
                primitives = JcaX25519Primitives(provider = ConscryptProviderHolder.provider),
            )
        }
    } catch (exception: Throwable) {
        PlatformCryptoSelection.unavailable(
            detail = exception.message ?: exception::class.java.simpleName,
        )
    }

    private const val PLATFORM_XDH_MIN_API = 33
}

private class JcaX25519Primitives(
    private val provider: Provider?,
) : PlatformCryptoPrimitives {
    override fun generateEphemeral(): CryptoResult<EphemeralKeyPair> = runCryptoOperation {
        val generated = keyPairGenerator().generateKeyPair()
        EphemeralKeyPair(
            publicKey = generated.public.encoded.toRawX25519PublicKey(),
            privateKeyPkcs8 = generated.private.encoded.copyOf(),
        )
    }

    override fun deriveX25519(
        keyPair: EphemeralKeyPair,
        peerPublicKey: ByteArray,
    ): CryptoResult<ByteArray> = runCryptoOperation {
        require(peerPublicKey.size == X25519_PUBLIC_KEY_LENGTH) {
            "X25519 public key must be 32 bytes."
        }
        val privateKey = keyFactory().generatePrivate(PKCS8EncodedKeySpec(keyPair.privateKeyPkcs8.copyOf()))
        val publicKey = keyFactory().generatePublic(X509EncodedKeySpec(peerPublicKey.toX25519SubjectPublicKeyInfo()))
        keyAgreement().run {
            init(privateKey)
            doPhase(publicKey, true)
            generateSecret()
        }
    }

    override fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance(SHA_256).digest(input.copyOf())

    override fun hmacSha256(
        key: ByteArray,
        input: ByteArray,
    ): ByteArray = Mac.getInstance(HMAC_SHA_256).run {
        init(SecretKeySpec(key.copyOf(), HMAC_SHA_256))
        doFinal(input.copyOf())
    }

    override fun aeadEncrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        plaintext: ByteArray,
    ): CryptoResult<ByteArray> = runCryptoOperation {
        requireAes256GcmInputs(key, nonce)
        Cipher.getInstance(AES_GCM_NO_PADDING).run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.copyOf(), AES), GCMParameterSpec(GCM_TAG_BITS, nonce.copyOf()))
            updateAAD(aad.copyOf())
            doFinal(plaintext.copyOf())
        }
    }

    override fun aeadDecrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        ciphertextAndTag: ByteArray,
    ): CryptoResult<ByteArray> = runCryptoOperation {
        requireAes256GcmInputs(key, nonce)
        Cipher.getInstance(AES_GCM_NO_PADDING).run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key.copyOf(), AES), GCMParameterSpec(GCM_TAG_BITS, nonce.copyOf()))
            updateAAD(aad.copyOf())
            doFinal(ciphertextAndTag.copyOf())
        }
    }

    override fun verifyP256(
        publicKeySpki: ByteArray,
        input: ByteArray,
        signature: ByteArray,
    ): CryptoResult<Boolean> = runCryptoOperation {
        val publicKey = KeyFactory.getInstance(EC).generatePublic(X509EncodedKeySpec(publicKeySpki.copyOf()))
        require(publicKey is ECPublicKey && publicKey.params.matches(expectedP256Params())) {
            "EC public key must use secp256r1."
        }
        Signature.getInstance(SHA256_WITH_ECDSA).run {
            initVerify(publicKey)
            update(input.copyOf())
            verify(signature.copyOf())
        }
    }

    private fun requireAes256GcmInputs(
        key: ByteArray,
        nonce: ByteArray,
    ) {
        require(key.size == AES_256_KEY_LENGTH) {
            "AES-256-GCM key must be 32 bytes."
        }
        require(nonce.size == GCM_NONCE_LENGTH) {
            "AES-GCM nonce must be 12 bytes."
        }
    }

    private fun expectedP256Params(): ECParameterSpec =
        AlgorithmParameters.getInstance(EC).apply {
            init(ECGenParameterSpec(P256_CURVE))
        }.getParameterSpec(ECParameterSpec::class.java)

    private fun ECParameterSpec.matches(other: ECParameterSpec): Boolean {
        val thisField = curve.field as? ECFieldFp ?: return false
        val otherField = other.curve.field as? ECFieldFp ?: return false
        return thisField.p == otherField.p &&
            curve.a == other.curve.a &&
            curve.b == other.curve.b &&
            generator.affineX == other.generator.affineX &&
            generator.affineY == other.generator.affineY &&
            order == other.order &&
            cofactor == other.cofactor
    }

    private fun keyPairGenerator(): KeyPairGenerator =
        provider?.let { KeyPairGenerator.getInstance(X25519, it) }
            ?: KeyPairGenerator.getInstance(X25519)

    private fun keyFactory(): KeyFactory =
        provider?.let { KeyFactory.getInstance(X25519, it) }
            ?: KeyFactory.getInstance(X25519)

    private fun keyAgreement(): KeyAgreement =
        provider?.let { KeyAgreement.getInstance(X25519, it) }
            ?: KeyAgreement.getInstance(X25519)

    private inline fun <T> runCryptoOperation(
        block: () -> T,
    ): CryptoResult<T> = try {
        CryptoResult.Success(block())
    } catch (exception: IllegalArgumentException) {
        CryptoResult.Failure(
            code = CryptoFailureCode.INVALID_INPUT,
            detail = exception.message,
        )
    } catch (exception: GeneralSecurityException) {
        CryptoResult.Failure(
            code = CryptoFailureCode.CRYPTO_UNAVAILABLE,
            detail = exception.message ?: exception::class.java.simpleName,
        )
    }

    companion object {
        private const val AES = "AES"
        private const val AES_256_KEY_LENGTH = 32
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val EC = "EC"
        private const val GCM_TAG_BITS = 128
        private const val GCM_NONCE_LENGTH = 12
        private const val HMAC_SHA_256 = "HmacSHA256"
        private const val P256_CURVE = "secp256r1"
        private const val SHA_256 = "SHA-256"
        private const val SHA256_WITH_ECDSA = "SHA256withECDSA"
        private const val X25519 = "X25519"
        private const val X25519_PUBLIC_KEY_LENGTH = 32
    }
}

private object ConscryptProviderHolder {
    val provider: Provider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Conscrypt.newProvider()
    }
}

private fun ByteArray.toRawX25519PublicKey(): ByteArray {
    require(size == X25519_SPKI_LENGTH) {
        "X25519 subjectPublicKeyInfo must be 44 bytes."
    }
    require(copyOfRange(0, X25519_SPKI_PREFIX.size).contentEquals(X25519_SPKI_PREFIX)) {
        "Unsupported X25519 subjectPublicKeyInfo prefix."
    }
    return copyOfRange(X25519_SPKI_PREFIX.size, size)
}

private fun ByteArray.toX25519SubjectPublicKeyInfo(): ByteArray {
    require(size == X25519_PUBLIC_KEY_LENGTH) {
        "X25519 public key must be 32 bytes."
    }
    return X25519_SPKI_PREFIX + copyOf()
}

private const val X25519_PUBLIC_KEY_LENGTH = 32
private const val X25519_SPKI_LENGTH = 44
private val X25519_SPKI_PREFIX = byteArrayOf(
    0x30,
    0x2a,
    0x30,
    0x05,
    0x06,
    0x03,
    0x2b,
    0x65,
    0x6e,
    0x03,
    0x21,
    0x00,
)
