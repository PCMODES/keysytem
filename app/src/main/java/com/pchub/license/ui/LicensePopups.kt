package com.pchub.license.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pchub.license.model.LicenseStatus

@Composable
fun PcHubLicensePopup(
    status: LicenseStatus,
    customMessage: String? = null,
    onDismiss: () -> Unit
) {
    val (title, message, buttonText, icon, accentColor) = when (status) {
        LicenseStatus.INVALID -> Tuple5(
            "Invalid License",
            customMessage ?: "The entered license key is invalid.",
            "TRY AGAIN",
            Icons.Default.ErrorOutline,
            Color(0xFFFF3366)
        )
        LicenseStatus.BLOCKED -> Tuple5(
            "License Blocked",
            "This license has been blocked by the administrator.",
            "OK",
            Icons.Default.Block,
            Color(0xFFFF9100)
        )
        LicenseStatus.BANNED -> Tuple5(
            "License Banned",
            "This license has been permanently banned.",
            "OK",
            Icons.Default.Lock,
            Color(0xFFFF1744)
        )
        LicenseStatus.EXPIRED -> Tuple5(
            "License Expired",
            customMessage ?: "Your license has expired.",
            "OK",
            Icons.Default.HourglassBottom,
            Color(0xFFFFB300)
        )
        LicenseStatus.DEVICE_MISMATCH -> Tuple5(
            "Device Mismatch",
            customMessage ?: "This license is bound to another device.",
            "OK",
            Icons.Default.Warning,
            Color(0xFFFF5252)
        )
        else -> Tuple5(
            "Notice",
            customMessage ?: "License status verification notice.",
            "OK",
            Icons.Default.Warning,
            Color(0xFF00E5FF)
        )
    }

    Dialog(
        onDismissRequest = { /* Modal locking: dismiss only via button */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("license_popup_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Brand Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PC HUB CENTER",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Icon with Glowing Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Popup Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Popup Message
                Text(
                    text = message,
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("popup_action_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    )
                ) {
                    Text(
                        text = buttonText,
                        color = if (accentColor == Color(0xFF00E5FF)) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
