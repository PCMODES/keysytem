package com.pchub.license.security

import android.content.Context
import android.util.Base64
import com.pchub.license.model.OfflineLicenseToken
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object CryptoEngine {

    private const val PREFS_NAME = "pchub_license_crypto_prefs"
    private const val KEY_TOKEN_PAYLOAD = "cached_token_payload"
    private const val KEY_TOKEN_VALID_UNTIL = "cached_token_valid_until"
    private const val KEY_TOKEN_SIG = "cached_token_sig"
    private const val KEY_LAST_DEVICE_ID = "cached_device_id"
    private const val KEY_PRODUCT_ID = "cached_product_id"

    // Default embedded RSA-2048 public key for PC HUB CENTER
    // Host applications can override this via PcHubLicenseConfig
    const val DEFAULT_RSA_PUBLIC_KEY = """MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3Bv0V6R0Vj8y2MfZt7pP
0zMwqYm3pX9N7kL1v2O4c6B8f0u1wV8f4o2N5y7b8A9c0d1e2f3g4h5i6j7k8l9m
0n1o2p3q4r5s6t7u8v9w0x1y2z3A4B5C6D7E8F9G0H1I2J3K4L5M6N7O8P9Q0R1S
2T3U4V5W6X7Y8Z9a0b1c2d3e4f5g6h7i8j9k0l1m2n3o4p5q6r7s8t9u0v1w2x3y
4z5A6B7C8D9E0F1G2H3I4J5K6L7M8N9O0P1Q2R3S4T5U6V7W8X9Y0Z1a2b3c4d5e
6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7A8B9C0D1E2F3G4H5I6J7K
8wIDAQAB"""

    fun verifyRsaSignature(
        data: String,
        signatureBase64: String,
        publicKeyBase64: String
    ): Boolean {
        return try {
            val cleanKey = publicKeyBase64
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
                .trim()

            val keyBytes = Base64.decode(cleanKey, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(data.toByteArray(Charsets.UTF_8))
            val sigBytes = Base64.decode(signatureBase64, Base64.DEFAULT)
            sig.verify(sigBytes)
        } catch (_: Exception) {
            false
        }
    }

    fun saveOfflineToken(
        context: Context,
        token: OfflineLicenseToken
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN_PAYLOAD, token.payload)
            .putLong(KEY_TOKEN_VALID_UNTIL, token.validUntilMillis)
            .putString(KEY_TOKEN_SIG, token.signatureBase64)
            .putString(KEY_LAST_DEVICE_ID, token.deviceId)
            .putString(KEY_PRODUCT_ID, token.productId)
            .apply()
    }

    fun getOfflineToken(context: Context): OfflineLicenseToken? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val payload = prefs.getString(KEY_TOKEN_PAYLOAD, null) ?: return null
        val validUntil = prefs.getLong(KEY_TOKEN_VALID_UNTIL, 0L)
        val sig = prefs.getString(KEY_TOKEN_SIG, null) ?: return null
        val deviceId = prefs.getString(KEY_LAST_DEVICE_ID, "") ?: ""
        val productId = prefs.getString(KEY_PRODUCT_ID, "") ?: ""

        val parts = payload.split("|")
        val key = parts.getOrNull(0) ?: ""

        return OfflineLicenseToken(
            licenseKey = key,
            productId = productId,
            deviceId = deviceId,
            validUntilMillis = validUntil,
            signatureBase64 = sig
        )
    }

    fun clearOfflineToken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    val OfflineLicenseToken.payload: String
        get() = "$licenseKey|$productId|$deviceId|$validUntilMillis"

    // Helper to generate a local mock signature in testing / demo mode if server RSA is unavailable
    fun generateLocalDemoSignature(data: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
