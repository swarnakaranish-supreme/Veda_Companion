package com.anish.veda.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.anish.veda.data.model.AiActionType
import com.anish.veda.data.model.AiChatMessage
import com.anish.veda.data.model.MessageSender
import com.anish.veda.data.repository.AiSettingsRepository
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaSecondaryButton
import com.anish.veda.ui.components.VedaStatusChip
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.CanvasMidnightNavy
import com.anish.veda.ui.theme.StatusGreen
import com.anish.veda.ui.theme.StrokeFocused
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing
import com.anish.veda.ui.theme.WarningAmber
import com.anish.veda.utils.TimeUtils
import com.anish.veda.viewmodel.AiAssistantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    navController: NavController,
    viewModel: AiAssistantViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val voiceRmsDb by viewModel.voiceRmsDb.collectAsState()
    val speechPartialText by viewModel.speechPartialText.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    val apiKey by viewModel.apiKey.collectAsState()
    val dataSharingConsent by viewModel.dataSharingConsent.collectAsState()
    val modelName by viewModel.modelName.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Audio recording permission launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceInput()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for voice input.")
            }
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isProcessing) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Snackbar notifications
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.AiAssistant.route,
                onNavigate = { route ->
                    if (route != Screen.AiAssistant.route) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CanvasDeepBlack)
        ) {
            // 1. Top Bar with Assistant Status & Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasDeepBlack)
                    .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AccentViolet
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VEDA AI",
                                style = Typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VedaRadii.xs))
                                    .background(AccentViolet.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "INTELLIGENCE",
                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                    color = AccentViolet
                                )
                            }
                        }
                        Text(
                            text = if (apiKey.isNotBlank() && dataSharingConsent) "GEMINI CLOUD LINKED" else "LOCAL NEURAL ENGINE",
                            style = Typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "AI Settings",
                            tint = AccentViolet,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Quick Command Suggestions Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasMidnightNavy)
                    .padding(horizontal = VedaSpacing.md, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickCommandChip(
                    label = "DAILY BRIEFING",
                    icon = Icons.Default.WbSunny,
                    onClick = { viewModel.executeDailyBriefing() }
                )
                QuickCommandChip(
                    label = "SUMMARIZE ALERTS",
                    icon = Icons.Default.Notifications,
                    onClick = { viewModel.executeNotificationSummary() }
                )
                QuickCommandChip(
                    label = "CHECK HEART RATE",
                    icon = Icons.Default.Favorite,
                    onClick = { viewModel.executeHealthInsights() }
                )
                QuickCommandChip(
                    label = "SET 6AM ALARM",
                    icon = Icons.Default.Alarm,
                    onClick = { viewModel.sendMessage("Set an alarm for 6 AM") }
                )
                QuickCommandChip(
                    label = "START 10M TIMER",
                    icon = Icons.Default.Timer,
                    onClick = { viewModel.sendMessage("Start a 10-minute timer") }
                )
                QuickCommandChip(
                    label = "FIND MY WATCH",
                    icon = Icons.Default.FindReplace,
                    onClick = { viewModel.sendMessage("Find my watch") }
                )
            }

            // 3. Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        onSmartReplyClick = { reply -> viewModel.sendMessage(reply) }
                    )
                }

                // Processing animation bubble
                if (isProcessing) {
                    item {
                        ProcessingIndicatorBubble()
                    }
                }
            }

            // 4. Live Voice Waveform Bar (Visible while listening)
            AnimatedVisibility(visible = isListening) {
                VoiceWaveformBar(
                    rmsDb = voiceRmsDb,
                    partialText = speechPartialText,
                    onCancel = { viewModel.stopVoiceInput() }
                )
            }

            // 5. Input Dock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasMidnightNavy)
                    .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Input Button
                IconButton(
                    onClick = {
                        if (isListening) {
                            viewModel.stopVoiceInput()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                viewModel.startVoiceInput()
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) AlertRed else AccentViolet.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) Color.White else AccentViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening to voice..." else "Speak or type natural command...",
                            style = Typography.bodySmall,
                            color = TextMuted
                        )
                    },
                    maxLines = 3,
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentViolet,
                        unfocusedBorderColor = StrokeSubtle,
                        focusedContainerColor = CanvasDeepBlack,
                        unfocusedContainerColor = CanvasDeepBlack,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                IconButton(
                    onClick = { viewModel.sendMessage(inputText) },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) AccentViolet else SurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) CanvasDeepBlack else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // AI Settings Bottom Sheet
    if (showSettingsSheet) {
        AiSettingsBottomSheet(
            apiKey = apiKey,
            dataSharingConsent = dataSharingConsent,
            selectedModel = modelName,
            onSaveKey = { viewModel.setApiKey(it) },
            onToggleConsent = { viewModel.setDataSharingConsent(it) },
            onSelectModel = { viewModel.setModelName(it) },
            onClearHistory = {
                viewModel.clearHistory()
                showSettingsSheet = false
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

// ============================================================================
// CHAT MESSAGE BUBBLE
// ============================================================================
@Composable
private fun ChatMessageBubble(
    message: AiChatMessage,
    onSmartReplyClick: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(if (isUser) SurfaceElevated else CanvasMidnightNavy)
                    .border(
                        1.dp,
                        if (isUser) AccentCyan.copy(alpha = 0.4f) else AccentViolet.copy(alpha = 0.3f),
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 300.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        style = Typography.bodyMedium,
                        color = TextPrimary
                    )

                    // Action badge if action executed
                    if (message.actionType != AiActionType.NONE) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(VedaRadii.xs))
                                .background(StatusGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTION DISPATCHED",
                                style = Typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = StatusGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TimeUtils.formatSyncTime(message.timestamp),
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextTertiary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Smart reply suggestion pills
        if (!isUser && message.smartReplies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.smartReplies.forEach { reply ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VedaRadii.pill))
                            .background(SurfaceElevated)
                            .border(1.dp, AccentViolet.copy(alpha = 0.5f), RoundedCornerShape(VedaRadii.pill))
                            .clickable { onSmartReplyClick(reply) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = reply,
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = AccentViolet
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// PROCESSING INDICATOR
// ============================================================================
@Composable
private fun ProcessingIndicatorBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(AccentViolet.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AccentViolet,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "VEDA Neural Core synthesizing...",
            style = Typography.labelSmall,
            color = AccentViolet
        )
    }
}

// ============================================================================
// VOICE WAVEFORM BAR
// ============================================================================
@Composable
private fun VoiceWaveformBar(
    rmsDb: Float,
    partialText: String,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CanvasMidnightNavy)
            .border(1.dp, AlertRed.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Waveform bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(
                        (10 + rmsDb * 20).dp,
                        (14 + rmsDb * 30).dp,
                        (20 + rmsDb * 35).dp,
                        (12 + rmsDb * 25).dp,
                        (8 + rmsDb * 15).dp
                    )
                    barHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AlertRed)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (partialText.isNotBlank()) "\"$partialText\"" else "Listening for command...",
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1
                )
            }

            VedaSecondaryButton(
                text = "STOP",
                onClick = onCancel,
                modifier = Modifier.width(72.dp),
                height = 36.dp
            )
        }
    }
}

// ============================================================================
// QUICK COMMAND CHIP
// ============================================================================
@Composable
private fun QuickCommandChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(VedaRadii.pill))
            .background(SurfaceElevated)
            .border(1.dp, StrokeSubtle, RoundedCornerShape(VedaRadii.pill))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentViolet,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = TextSecondary
            )
        }
    }
}

// ============================================================================
// AI SETTINGS BOTTOM SHEET
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettingsBottomSheet(
    apiKey: String,
    dataSharingConsent: Boolean,
    selectedModel: String,
    onSaveKey: (String) -> Unit,
    onToggleConsent: (Boolean) -> Unit,
    onSelectModel: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempKey by remember { mutableStateOf(apiKey) }
    var keyVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = CanvasMidnightNavy,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StrokeSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VEDA AI Configuration",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = AlertRed
                    )
                }
            }

            // Google Gemini API Key Input
            VedaCard {
                Text(
                    text = "Google Gemini API Key",
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stored privately in app preferences. Never hardcoded or shared.",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tempKey,
                    onValueChange = { tempKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste AI Studio API key...", style = Typography.bodySmall, color = TextMuted) },
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentViolet,
                        unfocusedBorderColor = StrokeSubtle,
                        focusedContainerColor = CanvasDeepBlack,
                        unfocusedContainerColor = CanvasDeepBlack,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                VedaSecondaryButton(
                    text = "SAVE API KEY",
                    icon = Icons.Default.Key,
                    onClick = { onSaveKey(tempKey) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Model Selection
            VedaCard {
                Text(
                    text = "Gemini Model Selection",
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiSettingsRepository.SUPPORTED_MODELS.forEach { model ->
                        val isSelected = selectedModel == model
                        val bg = if (isSelected) AccentViolet.copy(alpha = 0.2f) else CanvasDeepBlack
                        val border = if (isSelected) AccentViolet else StrokeSubtle

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(VedaRadii.sm))
                                .clickable { onSelectModel(model) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = model.replace("gemini-", "").replace("-flash", "\nFlash").replace("-pro", "\nPro"),
                                style = Typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp
                                ),
                                color = if (isSelected) AccentViolet else TextSecondary
                            )
                        }
                    }
                }
            }

            // Data Sharing Consent
            VedaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "External Cloud AI Consent",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Allows sending queries to Google Gemini cloud. When OFF, all processing runs 100% on-device.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = dataSharingConsent,
                        onCheckedChange = onToggleConsent,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentViolet,
                            checkedTrackColor = AccentViolet.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
