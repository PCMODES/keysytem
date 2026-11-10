package com.pchub.license.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

object DeviceIdentityManager {

    private const val PREFS_NAME = "pchub_device_identity_prefs"
    private const val KEY_DEVICE_UUID = "pchub_cached_device_uuid"

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_DEVICE_UUID, null)
        if (!cached.isNullOrEmpty()) {
            return cached
        }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Exception) {
            ""
        }

        val hardwareFingerprint = "${Build.MANUFACTURER}|${Build.DEVICE}|${Build.BOARD}|${Build.HARDWARE}"
        val rawIdentity = if (androidId.isNotBlank() && androidId != "9774d56d682e549c") {
            "$androidId|$hardwareFingerprint"
        } else {
            UUID.randomUUID().toString() + "|$hardwareFingerprint"
        }

        val hashed = sha256(rawIdentity).take(32)
        prefs.edit().putString(KEY_DEVICE_UUID, hashed).apply()
        return hashed
    }

    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    fun getAppSignatureSha256(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return "UNKNOWN"
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(certBytes)
            hash.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            "UNKNOWN"
        }
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
