package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pchub.license.model.OfflineLicenseToken
import com.pchub.license.security.CryptoEngine
import com.pchub.license.security.CryptoEngine.payload
import com.pchub.license.security.DeviceIdentityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PC HUB CENTER", appName)
    }

    @Test
    fun `verify device identity extraction`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceId = DeviceIdentityManager.getDeviceId(context)
        assertNotNull(deviceId)
        assertTrue(deviceId.length >= 16)
    }

    @Test
    fun `verify offline token save and retrieve`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val token = OfflineLicenseToken(
            licenseKey = "PCHUB-TEST-1234",
            productId = "PCHUB-TOOL-001",
            deviceId = "test-device-uuid",
            validUntilMillis = System.currentTimeMillis() + 72 * 3600 * 1000,
            signatureBase64 = CryptoEngine.generateLocalDemoSignature("PCHUB-TEST-1234|PCHUB-TOOL-001|test-device-uuid")
        )

        CryptoEngine.saveOfflineToken(context, token)
        val retrieved = CryptoEngine.getOfflineToken(context)
        assertNotNull(retrieved)
        assertEquals("PCHUB-TEST-1234", retrieved?.licenseKey)
        assertEquals("PCHUB-TOOL-001", retrieved?.productId)
    }
}
