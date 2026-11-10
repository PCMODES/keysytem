package com.pchub.license

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.pchub.license.model.LicenseDocument
import com.pchub.license.model.LicenseStatus
import com.pchub.license.model.LicenseUiState
import com.pchub.license.model.OfflineLicenseToken
import com.pchub.license.security.CryptoEngine
import com.pchub.license.security.CryptoEngine.payload
import com.pchub.license.security.DeviceIdentityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class PcHubLicenseManager private constructor(
    private val context: Context,
    val config: PcHubLicenseConfig,
    private val firestore: FirebaseFirestore
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow<LicenseUiState>(
        if (hasSavedLicense()) LicenseUiState.Checking else LicenseUiState.Idle
    )
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    val currentDeviceId: String = DeviceIdentityManager.getDeviceId(context)
    val currentDeviceModel: String = DeviceIdentityManager.getDeviceModel()

    private var activeRealtimeListener: ListenerRegistration? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            verifyCurrentLicense()
        }
    }

    fun hasSavedLicense(): Boolean {
        return !prefs.getString(KEY_SAVED_LICENSE_KEY, null).isNullOrBlank()
    }

    fun getSavedLicenseKey(): String? {
        return prefs.getString(KEY_SAVED_LICENSE_KEY, null)?.trim()?.uppercase()
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Attach real-time snapshot listener to catch admin blocks, bans, and unblocks instantly
    private fun attachRealtimeListener(licenseKey: String) {
        activeRealtimeListener?.remove()
        val docRef = firestore.collection("licenses").document(licenseKey)

        activeRealtimeListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Realtime listener error: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                clearSavedLicense()
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.INVALID,
                    "This license has been removed by the administrator."
                )
                return@addSnapshotListener
            }

            val license = snapshot.toObject(LicenseDocument::class.java)
            if (license == null || license.productId != config.productId) {
                clearSavedLicense()
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.INVALID,
                    "This license belongs to a different application."
                )
                return@addSnapshotListener
            }

            // Realtime instant block / ban / expire handling
            when (license.status.uppercase()) {
                "BLOCKED" -> {
                    _uiState.value = LicenseUiState.ShowPopup(
                        LicenseStatus.BLOCKED,
                        "This license has been blocked by the administrator."
                    )
                    return@addSnapshotListener
                }
                "BANNED" -> {
                    _uiState.value = LicenseUiState.ShowPopup(
                        LicenseStatus.BANNED,
                        "This license has been permanently banned."
                    )
                    return@addSnapshotListener
                }
                "EXPIRED" -> {
                    _uiState.value = LicenseUiState.ShowPopup(
                        LicenseStatus.EXPIRED,
                        "Your license has expired."
                    )
                    return@addSnapshotListener
                }
            }

            val now = Date()
            val expiresAt = license.expiresAt?.toDate()
            if (expiresAt != null && expiresAt.before(now)) {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.EXPIRED,
                    "Your license has expired."
                )
                return@addSnapshotListener
            }

            if (config.enforceDeviceBinding && license.boundDeviceId.isNotBlank() && license.boundDeviceId != currentDeviceId) {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.DEVICE_MISMATCH,
                    "This license is already bound to another device. It cannot be used on this device."
                )
                return@addSnapshotListener
            }

            // If active and valid, automatically unlock/keep unlocked
            if (license.status.equals("ACTIVE", ignoreCase = true)) {
                _uiState.value = LicenseUiState.Activated(license, isOfflineGrace = false)
            }
        }
    }

    suspend fun verifyCurrentLicense() = withContext(Dispatchers.IO) {
        val savedKey = getSavedLicenseKey()

        if (savedKey.isNullOrBlank()) {
            _uiState.value = LicenseUiState.Idle
            return@withContext
        }

        _uiState.value = LicenseUiState.Checking

        // 1. OFFLINE VERIFICATION (72-Hour Grace Period)
        if (!isOnline()) {
            val token = CryptoEngine.getOfflineToken(context)
            if (token != null && token.licenseKey == savedKey && token.productId == config.productId) {
                val now = System.currentTimeMillis()
                val isNotExpired = now <= token.validUntilMillis
                val isDeviceMatch = token.deviceId == currentDeviceId

                val isSignatureValid = CryptoEngine.verifyRsaSignature(
                    token.payload,
                    token.signatureBase64,
                    config.rsaPublicKey
                ) || token.signatureBase64 == CryptoEngine.generateLocalDemoSignature(token.payload)

                if (isNotExpired && isDeviceMatch && isSignatureValid) {
                    val offlineDoc = LicenseDocument(
                        id = savedKey,
                        licenseKey = savedKey,
                        productId = config.productId,
                        status = "ACTIVE",
                        type = "30_DAYS",
                        boundDeviceId = currentDeviceId,
                        deviceModel = currentDeviceModel,
                        expiresAt = Timestamp(Date(token.validUntilMillis))
                    )
                    _uiState.value = LicenseUiState.Activated(offlineDoc, isOfflineGrace = true)
                    return@withContext
                }
            }

            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.EXPIRED,
                "Offline license grace period has expired. Please connect to the internet to verify."
            )
            return@withContext
        }

        // 2. ONLINE LIVE VERIFICATION WITH FIRESTORE
        try {
            val docRef = firestore.collection("licenses").document(savedKey)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                clearSavedLicense()
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.INVALID,
                    "The entered license key is invalid."
                )
                return@withContext
            }

            val license = snapshot.toObject(LicenseDocument::class.java)

            // Strict App Isolation: Key must match this app's Product ID
            if (license == null || license.productId != config.productId) {
                clearSavedLicense()
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.INVALID,
                    "This license belongs to a different application."
                )
                return@withContext
            }

            evaluateAndHandleLicense(license, docRef = docRef)

            // Attach real-time listener so any admin block/ban reflects immediately
            attachRealtimeListener(savedKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying license online", e)
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.INVALID,
                e.localizedMessage ?: "Connection error while verifying license."
            )
        }
    }

    suspend fun activateKey(inputKey: String) = withContext(Dispatchers.IO) {
        val cleanKey = inputKey.trim().uppercase()
        if (cleanKey.isBlank()) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.INVALID,
                "The entered license key is invalid."
            )
            return@withContext
        }

        _uiState.value = LicenseUiState.Checking

        if (!isOnline()) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.INVALID,
                "Internet connection is required to activate your license key."
            )
            return@withContext
        }

        val docRef = firestore.collection("licenses").document(cleanKey)
        val snapshot: com.google.firebase.firestore.DocumentSnapshot

        try {
            snapshot = docRef.get().await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore query failed: ${e.message}", e)
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.INVALID,
                "Could not connect to license server. Please try again."
            )
            return@withContext
        }

        // Must exist in Cloud Firestore
        if (!snapshot.exists()) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.INVALID,
                "The entered license key is invalid."
            )
            return@withContext
        }

        val license = snapshot.toObject(LicenseDocument::class.java)

        // Strict App Check: 1 Key for 1 App
        if (license == null || license.productId != config.productId) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.INVALID,
                "This license key belongs to a different application."
            )
            return@withContext
        }

        // Check Banned, Blocked, Expired
        when (license.status.uppercase()) {
            "BANNED" -> {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.BANNED,
                    "This license has been permanently banned."
                )
                return@withContext
            }
            "BLOCKED" -> {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.BLOCKED,
                    "This license has been blocked by the administrator."
                )
                return@withContext
            }
            "EXPIRED" -> {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.EXPIRED,
                    "Your license has expired."
                )
                return@withContext
            }
        }

        val now = Date()
        val expiresAt = license.expiresAt?.toDate()
        if (expiresAt != null && expiresAt.before(now)) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.EXPIRED,
                "Your license has expired."
            )
            return@withContext
        }

        // Strict Device Check: 1 Key for 1 Device
        if (config.enforceDeviceBinding && license.boundDeviceId.isNotBlank() && license.boundDeviceId != currentDeviceId) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.DEVICE_MISMATCH,
                "This license is already bound to another device. It cannot be used on this device."
            )
            return@withContext
        }

        // Calculate Expiration
        val calculatedExpiresAt: Timestamp? = if (license.expiresAt != null) {
            license.expiresAt
        } else {
            when (license.type.uppercase()) {
                "LIFETIME" -> null
                else -> {
                    val duration = if (license.durationDays > 0) license.durationDays else 30L
                    Timestamp(Date(now.time + duration * 86_400_000L))
                }
            }
        }

        // Bind device and activate atomically
        val updatePayload = mutableMapOf<String, Any?>(
            "status" to "ACTIVE",
            "boundDeviceId" to currentDeviceId,
            "deviceModel" to currentDeviceModel,
            "activationDate" to (license.activationDate ?: FieldValue.serverTimestamp()),
            "lastVerificationDate" to FieldValue.serverTimestamp()
        )
        if (calculatedExpiresAt != null) {
            updatePayload["expiresAt"] = calculatedExpiresAt
        }

        try {
            docRef.set(updatePayload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update license in Firestore: ${e.message}", e)
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.INVALID,
                "Failed to activate license on server. Please try again."
            )
            return@withContext
        }

        // Save key locally
        prefs.edit().putString(KEY_SAVED_LICENSE_KEY, cleanKey).apply()

        // Cache 72-Hour Offline Token
        val graceMillis = config.offlineGracePeriodHours * 3600_000L
        val tokenValidUntil = if (calculatedExpiresAt != null) {
            minOf(now.time + graceMillis, calculatedExpiresAt.toDate().time)
        } else {
            now.time + graceMillis
        }

        val tokenPayload = "$cleanKey|${config.productId}|$currentDeviceId|$tokenValidUntil"
        val localSig = CryptoEngine.generateLocalDemoSignature(tokenPayload)
        CryptoEngine.saveOfflineToken(
            context,
            OfflineLicenseToken(
                licenseKey = cleanKey,
                productId = config.productId,
                deviceId = currentDeviceId,
                validUntilMillis = tokenValidUntil,
                signatureBase64 = localSig
            )
        )

        // Log Activation Telemetry
        try {
            firestore.collection("activations").add(
                mapOf(
                    "licenseKey" to cleanKey,
                    "productId" to config.productId,
                    "deviceId" to currentDeviceId,
                    "deviceModel" to currentDeviceModel,
                    "action" to "ACTIVATION_SUCCESS",
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {}

        val updatedDoc = license.copy(
            status = "ACTIVE",
            boundDeviceId = currentDeviceId,
            deviceModel = currentDeviceModel,
            expiresAt = calculatedExpiresAt
        )
        _uiState.value = LicenseUiState.Activated(updatedDoc, isOfflineGrace = false)

        // Attach real-time listener immediately
        attachRealtimeListener(cleanKey)
    }

    private suspend fun evaluateAndHandleLicense(
        license: LicenseDocument,
        docRef: com.google.firebase.firestore.DocumentReference
    ) {
        when (license.status.uppercase()) {
            "BANNED" -> {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.BANNED,
                    "This license has been permanently banned."
                )
                return
            }
            "BLOCKED" -> {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.BLOCKED,
                    "This license has been blocked by the administrator."
                )
                return
            }
            "EXPIRED" -> {
                _uiState.value = LicenseUiState.ShowPopup(
                    LicenseStatus.EXPIRED,
                    "Your license has expired."
                )
                return
            }
        }

        val now = Date()
        val expiresAt = license.expiresAt?.toDate()
        if (expiresAt != null && expiresAt.before(now)) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.EXPIRED,
                "Your license has expired."
            )
            return
        }

        // Strict Device Check: 1 Key for 1 Device
        if (config.enforceDeviceBinding && license.boundDeviceId.isNotBlank() && license.boundDeviceId != currentDeviceId) {
            _uiState.value = LicenseUiState.ShowPopup(
                LicenseStatus.DEVICE_MISMATCH,
                "This license is already bound to another device. It cannot be used on this device."
            )
            return
        }

        // Heartbeat update
        try {
            docRef.update("lastVerificationDate", FieldValue.serverTimestamp()).await()
        } catch (_: Exception) {}

        _uiState.value = LicenseUiState.Activated(license, isOfflineGrace = false)
    }

    fun dismissPopup() {
        _uiState.value = LicenseUiState.Idle
    }

    fun clearSavedLicense() {
        activeRealtimeListener?.remove()
        activeRealtimeListener = null
        prefs.edit().remove(KEY_SAVED_LICENSE_KEY).apply()
        CryptoEngine.clearOfflineToken(context)
        _uiState.value = LicenseUiState.Idle
    }

    companion object {
        private const val TAG = "PcHubLicenseManager"
        private const val PREFS_NAME = "pchub_license_manager_prefs"
        private const val KEY_SAVED_LICENSE_KEY = "pchub_saved_license_key"

        @Volatile
        private var INSTANCE: PcHubLicenseManager? = null

        fun getInstance(
            context: Context,
            config: PcHubLicenseConfig,
            firestore: FirebaseFirestore
        ): PcHubLicenseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PcHubLicenseManager(
                    context.applicationContext,
                    config,
                    firestore
                ).also { INSTANCE = it }
            }
        }

        fun get(): PcHubLicenseManager {
            return INSTANCE ?: error("PcHubLicenseManager has not been initialized yet.")
        }
    }
}
