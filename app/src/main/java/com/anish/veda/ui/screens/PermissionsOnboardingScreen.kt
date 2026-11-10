package com.anish.veda.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.anish.veda.ui.components.CyberHeader
import com.anish.veda.ui.components.FuturisticCard
import com.anish.veda.ui.components.GlowingButton
import com.anish.veda.ui.components.TechBadge
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CyberBlue
import com.anish.veda.ui.theme.MatrixGreen
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaSurfaceNavy
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.viewmodel.PermissionCheckItem
import com.anish.veda.viewmodel.PermissionsViewModel

@Composable
fun PermissionsOnboardingScreen(
    navController: NavController,
    viewModel: PermissionsViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionsList by viewModel.permissionStates.collectAsState()

    // Activity result launcher for runtime permissions (BT, Calls)
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshStatus()
    }

    // Refresh when user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack)
    ) {
        CyberHeader(
            title = "Hardware Authorization",
            subtitle = "Enable Android services for ESP32-C3 link"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "VEDA Companion requires low-level system permissions to forward Bluetooth packets, monitor telephone state and stream notification headers to your watch.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Permissions Checklist
            permissionsList.forEach { item ->
                PermissionCardItem(
                    item = item,
                    onGrantClick = {
                        when (item.id) {
                            "bluetooth" -> {
                                runtimePermissionLauncher.launch(
                                    PermissionUtils.getRequiredBluetoothPermissions()
                                )
                            }
                            "notification_listener" -> {
                                context.startActivity(
                                    PermissionUtils.createNotificationListenerSettingsIntent()
                                )
                            }
                            "calls" -> {
                                runtimePermissionLauncher.launch(
                                    PermissionUtils.getRequiredCallPermissions()
                                )
                            }
                            "battery" -> {
                                context.startActivity(
                                    PermissionUtils.createBatteryOptimizationIntent(context)
                                )
                            }
                            "post_notif" -> {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    runtimePermissionLauncher.launch(
                                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Proceed Button
            GlowingButton(
                onClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.PermissionsOnboarding.route) { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = "CONTINUE TO DASHBOARD",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCardItem(
    item: PermissionCheckItem,
    onGrantClick: () -> Unit
) {
    val icon = when (item.id) {
        "bluetooth" -> Icons.Default.Bluetooth
        "notification_listener" -> Icons.Default.Notifications
        "calls" -> Icons.Default.Call
        "battery" -> Icons.Default.BatteryChargingFull
        else -> Icons.Default.Shield
    }

    FuturisticCard(
        glowBorder = !item.isGranted && item.isCritical,
        borderColor = if (item.isGranted) MatrixGreen.copy(alpha = 0.5f) else if (item.isCritical) AlertRed.copy(alpha = 0.5f) else BorderNavy
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (item.isGranted) MatrixGreen.copy(alpha = 0.15f) else VedaSurfaceNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isGranted) Icons.Default.Check else icon,
                        contentDescription = null,
                        tint = if (item.isGranted) MatrixGreen else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.isCritical) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TechBadge(text = "CRITICAL", color = if (item.isGranted) MatrixGreen else AlertRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.description,
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (item.isGranted) {
                TechBadge(text = "ENABLED", color = MatrixGreen)
            } else {
                GlowingButton(
                    onClick = onGrantClick,
                    modifier = Modifier.width(84.dp),
                    height = 36.dp,
                    containerColor = if (item.isCritical) AlertRed else CyberBlue,
                    contentColor = TextWhite
                ) {
                    Text(text = "GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
