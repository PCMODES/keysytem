package com.pchub.license.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LicenseStatus(val title: String) {
    UNACTIVATED("Unactivated"),
    ACTIVE("Active"),
    BLOCKED("License Blocked"),
    BANNED("License Banned"),
    EXPIRED("License Expired"),
    INVALID("Invalid License"),
    DEVICE_MISMATCH("Device Mismatch")
}

enum class LicenseType(val displayName: String, val defaultDays: Long) {
    SEVEN_DAYS("7 Days", 7L),
    THIRTY_DAYS("30 Days", 30L),
    NINETY_DAYS("90 Days", 90L),
    ONE_EIGHTY_DAYS("180 Days", 180L),
    THREE_SIXTY_FIVE_DAYS("365 Days", 365L),
    LIFETIME("Lifetime", -1L),
    CUSTOM("Custom Expiry", 0L);

    companion object {
        fun fromString(value: String): LicenseType {
            return when (value.uppercase()) {
                "7_DAYS", "SEVEN_DAYS" -> SEVEN_DAYS
                "30_DAYS", "THIRTY_DAYS" -> THIRTY_DAYS
                "90_DAYS", "NINETY_DAYS" -> NINETY_DAYS
                "180_DAYS", "ONE_EIGHTY_DAYS" -> ONE_EIGHTY_DAYS
                "365_DAYS", "THREE_SIXTY_FIVE_DAYS" -> THREE_SIXTY_FIVE_DAYS
                "LIFETIME" -> LIFETIME
                else -> CUSTOM
            }
        }
    }
}

@IgnoreExtraProperties
data class LicenseDocument(
    @DocumentId val id: String = "",
    val licenseKey: String = "",
    val productId: String = "",
    val status: String = "UNACTIVATED",
    val type: String = "30_DAYS",
    val durationDays: Long = 30L,
    val boundDeviceId: String = "",
    val deviceModel: String = "",
    val allowedPackageName: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val activationDate: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val lastVerificationDate: Timestamp? = null
)

data class OfflineLicenseToken(
    val licenseKey: String,
    val productId: String,
    val deviceId: String,
    val validUntilMillis: Long,
    val signatureBase64: String
)

sealed class LicenseUiState {
    object Idle : LicenseUiState()
    object Checking : LicenseUiState()
    data class Activated(val license: LicenseDocument, val isOfflineGrace: Boolean = false) : LicenseUiState()
    data class ShowPopup(val status: LicenseStatus, val message: String) : LicenseUiState()
}
