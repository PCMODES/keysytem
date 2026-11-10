package com.pchub.license

import com.pchub.license.security.CryptoEngine

data class PcHubLicenseConfig(
    val productId: String,
    val rsaPublicKey: String = CryptoEngine.DEFAULT_RSA_PUBLIC_KEY,
    val offlineGracePeriodHours: Long = 72L,
    val enforceDeviceBinding: Boolean = true,
    val enforcePackageNameCheck: Boolean = false,
    val expectedPackageName: String? = null,
    val expectedSigningSha256: String? = null
)
