package com.pchub.license.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pchub.license.PcHubLicenseManager
import com.pchub.license.model.LicenseUiState
import kotlinx.coroutines.launch

@Composable
fun PcHubActivationScreen(
    licenseManager: PcHubLicenseManager,
    modifier: Modifier = Modifier
) {
    val uiState by licenseManager.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var licenseKeyInput by remember { mutableStateOf("") }
    val isLoading = uiState is LicenseUiState.Checking

    // Render Modal Popups for Invalid, Blocked, Banned, Expired states
    if (uiState is LicenseUiState.ShowPopup) {
        val popupState = uiState as LicenseUiState.ShowPopup
        PcHubLicensePopup(
            status = popupState.status,
            customMessage = popupState.message,
            onDismiss = { licenseManager.dismissPopup() }
        )
    }

    Box(
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
            .testTag("activation_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Brand Header & Glowing Shield
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                // Glowing Cyber Shield
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), Color.Transparent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFF0E172A), CircleShape)
                            .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "PC HUB CENTER Logo",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Brand Name
                Text(
                    text = "PC HUB CENTER",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "License Activation System",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your license key to unlock this application",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CENTER SECTION: Input Field, ACTIVATE Button & BUY KEY Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(22.dp))
                    .testTag("activation_input_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E172A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "LICENSE KEY",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Outlined License Key Input Box
                    OutlinedTextField(
                        value = licenseKeyInput,
                        onValueChange = { licenseKeyInput = it.uppercase() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("license_key_input"),
                        placeholder = {
                            Text(
                                text = "PCHUB-XXXX-XXXX-XXXX",
                                color = Color(0xFF475569),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Key Icon",
                                tint = Color(0xFF00E5FF)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val item = clipboard?.primaryClip?.getItemAt(0)
                                    val pasted = item?.text?.toString()?.trim()
                                    if (!pasted.isNullOrBlank()) {
                                        licenseKeyInput = pasted.uppercase()
                                    }
                                },
                                modifier = Modifier.testTag("paste_key_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (licenseKeyInput.isNotBlank() && !isLoading) {
                                    scope.launch { licenseManager.activateKey(licenseKeyInput) }
                                }
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF090D16),
                            unfocusedContainerColor = Color(0xFF090D16),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00E5FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. ACTIVATE BUTTON
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (licenseKeyInput.isNotBlank() && !isLoading) {
                                scope.launch { licenseManager.activateKey(licenseKeyInput) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("activate_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2979FF)
                        ),
                        enabled = !isLoading && licenseKeyInput.isNotBlank()
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "VERIFYING...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVATE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. BUY KEY (WHATSAPP) BUTTON
                    Button(
                        onClick = {
                            val phoneNumber = "919118260403"
                            val prefilledMessage = "Hello PC HUB CENTER, I want to purchase a license key for Product: ${licenseManager.config.productId} (Device: ${licenseManager.currentDeviceModel} / ID: ${licenseManager.currentDeviceId})"
                            val url = "https://wa.me/$phoneNumber?text=${Uri.encode(prefilledMessage)}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("buy_key_whatsapp_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366) // WhatsApp Brand Green
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp Chat",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BUY KEY (WHATSAPP)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTTOM SECTION: Clean Minimal Security Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "PC HUB CENTER SECURITY ARCHITECTURE",
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Encrypted Hardware-Bound Protection",
                    color = Color(0xFF334155),
                    fontSize = 11.sp
                )
            }
        }
    }
}
