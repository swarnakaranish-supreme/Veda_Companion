package com.anish.veda.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.calls.TelephonyState
import com.anish.veda.ui.components.CyberHeader
import com.anish.veda.ui.components.FuturisticCard
import com.anish.veda.ui.components.GlowingButton
import com.anish.veda.ui.components.GlowingOutlinedButton
import com.anish.veda.ui.components.TechBadge
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CyberBlue
import com.anish.veda.ui.theme.MatrixGreen
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaCardNavy
import com.anish.veda.ui.theme.WarningAmber
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.viewmodel.CallSettingsViewModel

@Composable
fun CallSettingsScreen(
    navController: NavController,
    viewModel: CallSettingsViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val telemetryState by viewModel.telemetryState.collectAsState()

    val hasPhoneStatePerm = PermissionUtils.hasPhoneStatePermission(context)
    val hasContactsPerm = PermissionUtils.hasContactsPermission(context)
    val isNotifListenerEnabled = PermissionUtils.isNotificationListenerEnabled(context)

    val phoneStateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshTelephonyListener()
        }
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setResolveCallerNames(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack)
    ) {
        CyberHeader(
            title = "Call Telemetry",
            subtitle = "OPPO A59 5G • Android 15 Telephony Engine",
            onBackClick = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Target Device & Capability Notice
            FuturisticCard(borderColor = CyberBlue.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMARTWATCH CALL TELEMETRY ONLY",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "VEDA displays real-time incoming caller ID and missed call alerts on your ESP32-C3 OLED display with vibration. ESP32-C3 hardware does not support Bluetooth call audio or voice call answering.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // READ_PHONE_STATE Permission Card with Educational Explanation
            FuturisticCard(
                glowBorder = !hasPhoneStatePerm,
                borderColor = if (hasPhoneStatePerm) MatrixGreen.copy(alpha = 0.6f) else AlertRed
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasPhoneStatePerm) MatrixGreen else AlertRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Telephony State Permission",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasPhoneStatePerm) "Android 15 CallStateListener Active" else "READ_PHONE_STATE Required",
                                color = if (hasPhoneStatePerm) MatrixGreen else AlertRed,
                                fontSize = 12.sp
                            )
                        }
                    }

                    TechBadge(
                        text = if (hasPhoneStatePerm) "GRANTED" else "REQUIRED",
                        color = if (hasPhoneStatePerm) MatrixGreen else AlertRed
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Why is this required? Android 15 requires READ_PHONE_STATE for TelephonyCallback to detect hardware ringing and call termination in real time. VEDA never records your calls or uploads any call audio.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (!hasPhoneStatePerm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlowingButton(
                        onClick = {
                            phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                        },
                        containerColor = AlertRed,
                        contentColor = TextWhite
                    ) {
                        Text(text = "GRANT PHONE STATE PERMISSION")
                    }
                }
            }

            // Notification Listener Access Card
            FuturisticCard(
                glowBorder = !isNotifListenerEnabled,
                borderColor = if (isNotifListenerEnabled) BorderNavy else WarningAmber
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isNotifListenerEnabled) MatrixGreen else WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Notification Listener Access",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isNotifListenerEnabled) "Resolves Caller ID from Dialer" else "Recommended for Caller Identity",
                                color = if (isNotifListenerEnabled) MatrixGreen else WarningAmber,
                                fontSize = 12.sp
                            )
                        }
                    }

                    TechBadge(
                        text = if (isNotifListenerEnabled) "ACTIVE" else "DISABLED",
                        color = if (isNotifListenerEnabled) MatrixGreen else WarningAmber
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Allows VEDA to capture caller names directly from your OPPO/Google dialer's incoming heads-up notification without needing intrusive call log permissions.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                if (!isNotifListenerEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    GlowingButton(
                        onClick = {
                            context.startActivity(PermissionUtils.createNotificationListenerSettingsIntent())
                        },
                        containerColor = WarningAmber,
                        contentColor = VedaBlack
                    ) {
                        Text(text = "ENABLE NOTIFICATION ACCESS")
                    }
                }
            }

            // Live Telemetry Monitor Card
            FuturisticCard(glowBorder = telemetryState.isIncomingActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LIVE TELEPHONY MONITOR",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val statusBadgeColor = when (telemetryState.state) {
                        TelephonyState.RINGING -> AlertRed
                        TelephonyState.OFFHOOK -> MatrixGreen
                        TelephonyState.IDLE -> NeonCyan
                    }
                    val statusText = when (telemetryState.state) {
                        TelephonyState.RINGING -> "RINGING"
                        TelephonyState.OFFHOOK -> if (telemetryState.isOutgoing) "OUTGOING (OFFHOOK)" else "IN CALL (OFFHOOK)"
                        TelephonyState.IDLE -> "IDLE"
                    }

                    TechBadge(text = statusText, color = statusBadgeColor)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "Last Detected Caller", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = telemetryState.lastCaller,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Total Forwarded", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = "${settings.callsForwardedCount} calls",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Last Event: ${telemetryState.lastEvent}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Incoming Calls Forwarding Toggle
            FuturisticCard(glowBorder = settings.forwardCalls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Incoming Call Alerts",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Sends N|1|Phone|Incoming call|CALLER alert and triggers watch vibration",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.forwardCalls,
                        onCheckedChange = { viewModel.setForwardCalls(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VedaBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VedaCardNavy
                        )
                    )
                }
            }

            // Missed Calls Forwarding Toggle
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallMissed,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Missed Call Alerts",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Sends N|2|Phone|Missed call|CALLER reminder when an incoming call is unanswered",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.forwardMissedCalls,
                        onCheckedChange = { viewModel.setForwardMissedCalls(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VedaBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VedaCardNavy
                        )
                    )
                }
            }

            // Show Caller Name Toggle
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = CyberBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Show Caller Name",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (settings.showCallerName) "Displays caller identity. If unavailable, uses UNKNOWN." else "Privacy mode: All callers shown as UNKNOWN",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.showCallerName,
                        onCheckedChange = { viewModel.setShowCallerName(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VedaBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VedaCardNavy
                        )
                    )
                }
            }

            // Contact Name Resolution Toggle
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = MatrixGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Lookup Contact Names",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Resolves raw numbers into address book contact names",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.resolveCallerNames && hasContactsPerm,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (hasContactsPerm) {
                                    viewModel.setResolveCallerNames(true)
                                } else {
                                    contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            } else {
                                viewModel.setResolveCallerNames(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VedaBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VedaCardNavy
                        )
                    )
                }
            }

            // Test Simulation Card
            FuturisticCard {
                Text(
                    text = "SIMULATE PHONE CALL TELEMETRY",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verify how ESP32-C3 renders category 1 (incoming) and category 2 (missed) notifications",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlowingButton(
                        onClick = { viewModel.sendTestIncomingCall("Marcus Vance (+1 555-0199)") }
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "TEST INCOMING CALL (N|1|...)")
                    }

                    GlowingOutlinedButton(
                        onClick = { viewModel.sendTestMissedCall("Elena Rostova (+1 555-0142)") },
                        borderColor = AlertRed,
                        contentColor = AlertRed
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.CallMissed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "TEST MISSED CALL (N|2|...)")
                    }

                    GlowingOutlinedButton(
                        onClick = { viewModel.sendTestUnknownCall() },
                        borderColor = BorderNavy,
                        contentColor = TextWhite
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "TEST UNKNOWN CALLER (N|1|...|UNKNOWN)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
