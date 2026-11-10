package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PcHubDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.pchub.license.PcHubLicenseConfig
import com.pchub.license.PcHubLicenseManager
import com.pchub.license.model.LicenseUiState
import com.pchub.license.ui.PcHubActivationScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Firestore with the named database ID
        val databaseId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)

        // 2. Configure PC HUB CENTER License Module with Product ID
        val licenseConfig = PcHubLicenseConfig(
            productId = "PCHUB-TOOL-001",
            offlineGracePeriodHours = 72L,
            enforceDeviceBinding = true
        )

        // 3. Initialize License Manager
        val licenseManager = PcHubLicenseManager.getInstance(
            context = this,
            config = licenseConfig,
            firestore = firestore
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF080C15)
                ) {
                    PcHubAppNavigator(licenseManager = licenseManager)
                }
            }
        }
    }
}

@Composable
fun PcHubAppNavigator(licenseManager: PcHubLicenseManager) {
    val uiState by licenseManager.uiState.collectAsState()

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "license_screen_transition"
    ) { state ->
        when (state) {
            is LicenseUiState.Activated -> {
                // UNLOCKED: Open original application interface
                PcHubDashboardScreen(
                    license = state.license,
                    isOfflineGrace = state.isOfflineGrace,
                    licenseManager = licenseManager
                )
            }
            is LicenseUiState.Checking -> {
                if (licenseManager.hasSavedLicense()) {
                    // Saved key exists -> verify in background with splash, don't ask for key!
                    PcHubVerifyingScreen()
                } else {
                    PcHubActivationScreen(licenseManager = licenseManager)
                }
            }
            else -> {
                // LOCKED or Popup: Show PC HUB CENTER License Activation Screen
                PcHubActivationScreen(licenseManager = licenseManager)
            }
        }
    }
}

@Composable
fun PcHubVerifyingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080C15),
                        Color(0xFF0D1527),
                        Color(0xFF080C15)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF0E172A), CircleShape)
                    .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "PC HUB CENTER Logo",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PC HUB CENTER",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Verifying License Status...",
                color = Color(0xFF00E5FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color(0xFF00E5FF),
                strokeWidth = 2.5.dp
            )
        }
    }
}
