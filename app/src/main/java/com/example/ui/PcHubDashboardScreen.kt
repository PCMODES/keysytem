package com.example.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pchub.license.PcHubLicenseManager
import com.pchub.license.model.LicenseDocument
import com.pchub.license.security.DeviceIdentityManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PcHubDashboardScreen(
    license: LicenseDocument,
    isOfflineGrace: Boolean,
    licenseManager: PcHubLicenseManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedExpiry = license.expiresAt?.toDate()?.let { dateFormat.format(it) } ?: "Lifetime Access"

    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo().apply { actManager?.getMemoryInfo(this) }
    val freeRamGb = "%.1f".format(memInfo.availMem / (1024.0 * 1024.0 * 1024.0))
    val totalRamGb = "%.1f".format(memInfo.totalMem / (1024.0 * 1024.0 * 1024.0))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080C15),
                        Color(0xFF0D1527),
                        Color(0xFF080C15)
                    )
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF0E172A), CircleShape)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlocked",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "PC HUB CENTER",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Protected Application Console",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Online / Offline Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isOfflineGrace) Color(0xFFFFB300) else Color(0xFF00E676),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = if (isOfflineGrace) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (isOfflineGrace) Color(0xFFFFB300) else Color(0xFF00E676),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOfflineGrace) "72h Offline" else "Verified Live",
                    color = if (isOfflineGrace) Color(0xFFFFB300) else Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // LICENSE STATUS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .testTag("license_details_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active Status",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STATUS: ACTIVE",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "TYPE: ${license.type}",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // License Key Text
                Text(
                    text = "ACTIVE LICENSE KEY",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = license.licenseKey,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "EXPIRES AT",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formattedExpiry,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "BOUND HARDWARE",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = licenseManager.currentDeviceModel,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: PROTECTED UTILITIES
        Text(
            text = "PROTECTED UTILITIES & TELEMETRY",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Utility 1: System Telemetry
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hardware Performance Monitor",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CPU CORES", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${Runtime.getRuntime().availableProcessors()} Cores", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("AVAILABLE RAM", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$freeRamGb GB / $totalRamGb GB", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ARCH", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Utility 2: Security & Integrity Diagnostics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF2979FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Security & Anti-Tamper Integrity",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val certSha = remember { DeviceIdentityManager.getAppSignatureSha256(context) }
                Text("APP SIGNING FINGERPRINT", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = certSha.take(28) + "...",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connection Latency: ${latencyMs?.let { "${it}ms" } ?: "Not tested"}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isPinging = true
                                val start = SystemClock.elapsedRealtime()
                                licenseManager.verifyCurrentLicense()
                                latencyMs = SystemClock.elapsedRealtime() - start
                                isPinging = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("ping_test_button")
                    ) {
                        Text(
                            text = if (isPinging) "Testing..." else "Test Ping",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // LICENSE MANAGEMENT ACTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch { licenseManager.verifyCurrentLicense() }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("refresh_license_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Re-verify", color = Color(0xFF00E5FF), fontSize = 13.sp)
            }

            Button(
                onClick = {
                    licenseManager.clearSavedLicense()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("switch_license_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Switch Key", color = Color(0xFFFF5252), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
