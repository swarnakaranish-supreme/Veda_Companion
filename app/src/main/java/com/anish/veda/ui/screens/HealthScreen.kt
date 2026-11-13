package com.anish.veda.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.data.model.HealthMeasurement
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaDailyBarChart
import com.anish.veda.ui.components.VedaMetricCard
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaSecondaryButton
import com.anish.veda.ui.components.VedaSensorQualityMeter
import com.anish.veda.ui.components.VedaSplineTrendChart
import com.anish.veda.ui.components.VedaTopAppBar
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StatusWarning
import com.anish.veda.ui.theme.StrokeFocused
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing
import com.anish.veda.viewmodel.HealthViewModel

@Composable
fun HealthScreen(
    navController: NavController,
    viewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current

    val connectionState by viewModel.connectionState.collectAsState()
    val isMeasuring by viewModel.isMeasuring.collectAsState()
    val liveHeartRate by viewModel.liveHeartRate.collectAsState()
    val liveSpO2 by viewModel.liveSpO2.collectAsState()
    val sensorQuality by viewModel.sensorQuality.collectAsState()
    val recentHistory by viewModel.recentMeasurements.collectAsState()
    val dailySummaries by viewModel.dailySummaries.collectAsState()

    val isConnected = connectionState.isConnected

    Scaffold(
        topBar = {
            VedaTopAppBar(
                title = "Biometric Telemetry",
                statusText = when {
                    !isConnected -> "OFFLINE"
                    isMeasuring -> "SAMPLING ACTIVE"
                    else -> "SENSOR READY"
                },
                statusColor = when {
                    !isConnected -> StatusError
                    isMeasuring -> StatusSuccess
                    else -> AccentCyan
                },
                pulsing = isMeasuring
            )
        },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Health.route,
                onNavigate = { route ->
                    if (route != Screen.Health.route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        containerColor = CanvasDeepBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md)
        ) {
            // MEDICAL ACCURACY DISCLAIMER
            VedaCard(
                containerColor = SurfaceElevated.copy(alpha = 0.5f),
                borderColor = StrokeSubtle
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Medical Disclaimer",
                        modifier = Modifier.size(16.dp),
                        tint = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(VedaSpacing.sm))
                    Text(
                        text = "NON-MEDICAL WELLNESS REFERENCE ONLY",
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "VEDA measurements are intended strictly for fitness and wellness tracking and are not certified for clinical diagnosis, treatment, or medical monitoring.",
                    style = Typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // LIVE SENSOR METRICS DUAL ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    VedaMetricCard(
                        label = "Heart Rate",
                        value = liveHeartRate?.toString(),
                        unit = "BPM",
                        icon = Icons.Default.Favorite,
                        accentColor = StatusError,
                        statusNote = if (liveHeartRate != null) "Pulsing" else null
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    VedaMetricCard(
                        label = "Blood O2 (SpO2)",
                        value = liveSpO2?.toString(),
                        unit = "%",
                        icon = Icons.Default.WaterDrop,
                        accentColor = AccentCyan,
                        statusNote = if (liveSpO2 != null) "Optimal" else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SENSOR QUALITY (MAX30102)
            VedaSensorQualityMeter(
                qualityScore = sensorQuality
            )

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // MEASUREMENT CONTROL ACTION BAR
            VedaCard {
                VedaCardHeader(
                    label = "MAX30102 Optical Acquisition",
                    statusText = if (isMeasuring) "STREAMING 50Hz" else "IDLE",
                    statusColor = if (isMeasuring) StatusSuccess else TextTertiary
                )

                if (isMeasuring) {
                    VedaSecondaryButton(
                        text = "Stop Live Sampling",
                        onClick = { viewModel.stopMeasurement() },
                        icon = Icons.Default.Stop
                    )
                } else {
                    VedaPrimaryButton(
                        text = "Start Live Sampling",
                        onClick = { viewModel.startMeasurement() },
                        icon = Icons.Default.PlayArrow,
                        enabled = isConnected
                    )
                }

                if (!isConnected) {
                    Spacer(modifier = Modifier.height(VedaSpacing.xs))
                    Text(
                        text = "Connect VEDA Smartwatch to begin PPG sensor sampling",
                        style = Typography.bodySmall,
                        color = StatusWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // RECENT TREND: SMOOTH BÉZIER SPLINE CHART
            VedaCard {
                VedaCardHeader(
                    label = "24-Hour Heart Rate Dynamics",
                    statusText = "${recentHistory.size} SAMPLES",
                    statusColor = AccentCyan
                )
                VedaSplineTrendChart(
                    measurements = recentHistory,
                    lineColor = StatusError
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // 7-DAY SUMMARY: DAILY AVERAGES BAR CHART
            VedaCard {
                VedaCardHeader(
                    label = "7-Day Daily Average Baseline",
                    statusText = "WEEKLY TREND",
                    statusColor = AccentCyan
                )
                VedaDailyBarChart(
                    summaries = dailySummaries
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // MEASUREMENT LOGS & CSV EXPORT
            VedaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TELEMETRY LOGS (${recentHistory.size})",
                        style = Typography.labelSmall,
                        color = TextTertiary
                    )
                    if (recentHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear History",
                                modifier = Modifier.size(18.dp),
                                tint = TextTertiary
                            )
                        }
                    }
                }

                if (recentHistory.isEmpty()) {
                    Text(
                        text = "No recorded measurements in local database. Start live sampling to log heart rate and SpO2 telemetry.",
                        style = Typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = VedaSpacing.md)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = VedaSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentHistory.take(8).forEach { item ->
                            HistoryItemRow(item)
                        }
                    }

                    Spacer(modifier = Modifier.height(VedaSpacing.md))

                    VedaSecondaryButton(
                        text = "Export Telemetry (CSV)",
                        onClick = { viewModel.exportAndShareCsv(context) },
                        icon = Icons.Default.FileDownload
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.xl))
        }
    }
}

@Composable
private fun HistoryItemRow(item: HealthMeasurement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VedaRadii.sm))
            .background(SurfaceElevated)
            .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = item.formattedTime,
                style = Typography.labelSmall.copy(fontSize = 10.sp),
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.heartRate} BPM  •  ${item.spo2}% SpO2",
                style = Typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(VedaRadii.pill))
                .background(
                    when (item.status) {
                        "OPTIMAL" -> StatusSuccess.copy(alpha = 0.12f)
                        "GOOD" -> AccentCyan.copy(alpha = 0.12f)
                        else -> StatusWarning.copy(alpha = 0.12f)
                    }
                )
                .border(
                    1.dp,
                    when (item.status) {
                        "OPTIMAL" -> StatusSuccess.copy(alpha = 0.3f)
                        "GOOD" -> AccentCyan.copy(alpha = 0.3f)
                        else -> StatusWarning.copy(alpha = 0.3f)
                    },
                    RoundedCornerShape(VedaRadii.pill)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${item.quality}%",
                style = Typography.labelSmall.copy(fontSize = 9.sp),
                color = when (item.status) {
                    "OPTIMAL" -> StatusSuccess
                    "GOOD" -> AccentCyan
                    else -> StatusWarning
                }
            )
        }
    }
}
