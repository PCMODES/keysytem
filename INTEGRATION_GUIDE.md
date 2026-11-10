# PC HUB CENTER — Android License Activation Module
## Developer & Integration Guide

### 1. Overview
**PC HUB CENTER** is an enterprise-grade, lightweight, and reusable Android licensing module designed to protect premium features in Android applications. It features:
- Real-time Cloud Firestore license verification
- Hardware-bound device identity enforcement
- 72-hour offline grace period secured with RSA-2048 digital signatures
- Comprehensive license states: `ACTIVE`, `BLOCKED`, `BANNED`, `EXPIRED`, `INVALID`, `DEVICE_MISMATCH`
- Non-bypassable modal locking dialogs
- Dark cyan/navy cyber-security UI theme

---

### 2. Module Architecture

```
com.pchub.license/
├── PcHubLicenseConfig.kt       # Product ID, RSA Key, and policy configuration
├── PcHubLicenseManager.kt      # Core engine: Firestore validation, token caching, device binding
├── model/
│   └── LicenseModel.kt         # Data classes, enums (LicenseStatus, LicenseType), and UiState
├── security/
│   ├── DeviceIdentityManager.kt# Hardware-salted installation identity and signature extractor
│   └── CryptoEngine.kt         # RSA-2048 verification, offline token encryption
└── ui/
    ├── LicensePopups.kt        # Modals: Invalid, Blocked, Banned, Expired, Device Mismatch
    └── PcHubActivationScreen.kt# Cyber-themed UI with key input and status badges
```

---

### 3. Step-by-Step Integration

#### Step 3.1: Add Firebase Dependencies
Ensure your application's `build.gradle.kts` includes Firebase Firestore and Coroutines:

```kotlin
dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.material3)
}
```

#### Step 3.2: Configure AndroidManifest.xml
Ensure network permissions are declared in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

#### Step 3.3: Initialize in MainActivity
In your root Activity or Application class:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Get Firestore instance
        val dbId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(dbId)

        // 2. Set unique Product ID for this application
        val config = PcHubLicenseConfig(
            productId = "PCHUB-TOOL-001",
            offlineGracePeriodHours = 72L,
            enforceDeviceBinding = true
        )

        // 3. Initialize Manager
        val licenseManager = PcHubLicenseManager.getInstance(this, config, firestore)

        setContent {
            val uiState by licenseManager.uiState.collectAsState()

            when (uiState) {
                is LicenseUiState.Activated -> {
                    // UNLOCKED: Render your application's protected screens
                    MyProtectedAppScreen()
                }
                else -> {
                    // LOCKED: Show PC HUB CENTER Activation Screen
                    PcHubActivationScreen(licenseManager = licenseManager)
                }
            }
        }
    }
}
```

---

### 4. License Types Supported

| License Type | Duration | Description |
|---|---|---|
| `7_DAYS` | 7 Days | Short trial or weekly pass |
| `30_DAYS` | 30 Days | Monthly subscription |
| `90_DAYS` | 90 Days | Quarterly license |
| `180_DAYS` | 180 Days | Half-year pass |
| `365_DAYS` | 365 Days | Annual license |
| `LIFETIME` | Permanent | Never expires (`expiresAt = null`) |
| `CUSTOM` | Configurable | Admin-specified expiration timestamp |

---

### 5. 72-Hour Offline Grace Period
When the user goes offline (e.g., airplane mode or no signal):
1. The module reads the hardware-bound token from secure local storage.
2. It verifies the RSA-2048 digital signature against the public key embedded in `PcHubLicenseConfig`.
3. If the token is within the 72-hour window and bound to the same device, the app remains **UNLOCKED**.
4. Once the device reconnects to the internet, the module silently re-verifies against Firestore to catch any remote administrator bans or blocks.
