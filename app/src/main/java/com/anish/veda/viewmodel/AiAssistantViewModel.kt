package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.ai.GeminiApiResult
import com.anish.veda.data.model.AiActionType
import com.anish.veda.data.model.AiChatMessage
import com.anish.veda.data.model.MessageSender
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val aiSettingsRepo = app.aiSettingsRepository
    private val chatDb = app.aiChatDatabaseHelper
    private val commandDispatcher = app.aiCommandDispatcher
    private val geminiClient = app.geminiApiClient
    private val voiceManager = app.voiceInputManager

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val voiceRmsDb: StateFlow<Float> = voiceManager.rmsDb
    val speechPartialText: StateFlow<String> = voiceManager.partialText

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    val apiKey: StateFlow<String> = aiSettingsRepo.apiKey
    val dataSharingConsent: StateFlow<Boolean> = aiSettingsRepo.dataSharingConsent
    val modelName: StateFlow<String> = aiSettingsRepo.modelName

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadHistory()
        observeVoiceInput()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val list = chatDb.getRecentMessages()
            if (list.isEmpty()) {
                // Initialize with welcome greeting from VEDA AI
                val welcome = AiChatMessage(
                    text = "VEDA Neural Core online. I am your wearable intelligence companion. You can speak or type natural commands, ask for notification summaries, review health telemetry, set alarms, or tap suggestions below.",
                    sender = MessageSender.VEDA,
                    timestamp = System.currentTimeMillis(),
                    smartReplies = listOf("Daily Briefing", "Summarize Alerts", "Check Heart Rate", "Set an Alarm")
                )
                chatDb.insertMessage(welcome)
                _messages.value = listOf(welcome)
            } else {
                _messages.value = list
            }
        }
    }

    private fun observeVoiceInput() {
        viewModelScope.launch {
            voiceManager.recognizedTextFlow.collect { text ->
                if (text.isNotBlank()) {
                    sendMessage(text)
                }
            }
        }
        viewModelScope.launch {
            voiceManager.errorFlow.collect { err ->
                _snackbarMessage.tryEmit(err)
            }
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun startVoiceInput() {
        voiceManager.startListening()
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        _inputText.value = ""

        viewModelScope.launch {
            // 1. Record User Message
            val userMsg = AiChatMessage(
                text = trimmed,
                sender = MessageSender.USER,
                timestamp = System.currentTimeMillis()
            )
            val userMsgId = chatDb.insertMessage(userMsg)
            _messages.value = chatDb.getRecentMessages()

            _isProcessing.value = true

            // Special confirmation handling for SOS emergency beacon
            if (trimmed.equals("Confirm SOS", ignoreCase = true)) {
                app.guardianRepository.startCountdown(fromWatch = false)
                val vedaReply = AiChatMessage(
                    text = "Guardian Emergency SOS beacon ACTIVATED. Countdown initiated with live location embedding.",
                    sender = MessageSender.VEDA,
                    timestamp = System.currentTimeMillis(),
                    actionType = AiActionType.TRIGGER_SOS,
                    actionPayload = "TRIGGER_SOS",
                    actionExecuted = true
                )
                chatDb.insertMessage(vedaReply)
                _messages.value = chatDb.getRecentMessages()
                _isProcessing.value = false
                return@launch
            }

            // 2. Try Offline Command Parsing
            val localResult = commandDispatcher.processCommand(trimmed)

            if (localResult.handled) {
                val vedaReply = AiChatMessage(
                    text = localResult.responseText,
                    sender = MessageSender.VEDA,
                    timestamp = System.currentTimeMillis(),
                    actionType = localResult.actionType,
                    actionPayload = localResult.actionPayload,
                    actionExecuted = true,
                    requiresConfirmation = localResult.requiresConfirmation,
                    smartReplies = localResult.smartReplies
                )
                chatDb.insertMessage(vedaReply)
                _messages.value = chatDb.getRecentMessages()
                _isProcessing.value = false
                return@launch
            }

            // 3. Cloud AI via Gemini API (if configured & consented)
            if (aiSettingsRepo.hasValidApiKey && aiSettingsRepo.dataSharingConsent.value) {
                val recentHistory = _messages.value.takeLast(6).map {
                    (if (it.sender == MessageSender.USER) "user" else "model") to it.text
                }

                when (val result = geminiClient.generateResponse(trimmed, recentHistory)) {
                    is GeminiApiResult.Success -> {
                        val vedaReply = AiChatMessage(
                            text = result.responseText,
                            sender = MessageSender.VEDA,
                            timestamp = System.currentTimeMillis(),
                            smartReplies = result.smartReplies
                        )
                        chatDb.insertMessage(vedaReply)
                    }
                    is GeminiApiResult.Error -> {
                        val errorReply = AiChatMessage(
                            text = "${result.errorMessage}\n\n*Note: Offline device commands (e.g. alarms, timers, heart rate, DND, finding your watch) continue to function without cloud access.*",
                            sender = MessageSender.VEDA,
                            timestamp = System.currentTimeMillis(),
                            smartReplies = listOf("Daily Briefing", "Check Heart Rate", "Start Timer")
                        )
                        chatDb.insertMessage(errorReply)
                    }
                }
            } else {
                // Offline fallback guidance
                val offlineReply = if (!aiSettingsRepo.hasValidApiKey) {
                    "I didn't recognize that as a local device command. To unlock full conversational intelligence, configure your Google Gemini API key in AI Settings.\n\nOffline capabilities available right now:\n• \"Set an alarm for 6 AM\"\n• \"Start a 10-minute timer\"\n• \"Show my latest heart rate\"\n• \"Summarize recent notifications\"\n• \"Enable Do Not Disturb\"\n• \"Find my watch\""
                } else {
                    "External AI data sharing is currently paused. Enable Data Sharing Consent in AI Settings to permit sending queries to the Gemini cloud model."
                }

                val vedaReply = AiChatMessage(
                    text = offlineReply,
                    sender = MessageSender.VEDA,
                    timestamp = System.currentTimeMillis(),
                    smartReplies = listOf("Daily Briefing", "Summarize Alerts", "Check Heart Rate")
                )
                chatDb.insertMessage(vedaReply)
            }

            _messages.value = chatDb.getRecentMessages()
            _isProcessing.value = false
        }
    }

    fun executeDailyBriefing() {
        viewModelScope.launch {
            val userMsg = AiChatMessage(text = "Daily Briefing", sender = MessageSender.USER)
            chatDb.insertMessage(userMsg)
            _messages.value = chatDb.getRecentMessages()

            val briefing = commandDispatcher.generateDailyBriefing()
            val vedaMsg = AiChatMessage(
                text = briefing.responseText,
                sender = MessageSender.VEDA,
                actionType = briefing.actionType,
                actionExecuted = true,
                smartReplies = briefing.smartReplies
            )
            chatDb.insertMessage(vedaMsg)
            _messages.value = chatDb.getRecentMessages()
        }
    }

    fun executeNotificationSummary() {
        viewModelScope.launch {
            val userMsg = AiChatMessage(text = "Summarize recent notifications", sender = MessageSender.USER)
            chatDb.insertMessage(userMsg)
            _messages.value = chatDb.getRecentMessages()

            val summary = commandDispatcher.generateNotificationSummary()
            val vedaMsg = AiChatMessage(
                text = summary.responseText,
                sender = MessageSender.VEDA,
                actionType = summary.actionType,
                actionExecuted = true,
                smartReplies = summary.smartReplies
            )
            chatDb.insertMessage(vedaMsg)
            _messages.value = chatDb.getRecentMessages()
        }
    }

    fun executeHealthInsights() {
        viewModelScope.launch {
            val userMsg = AiChatMessage(text = "Show my health insights", sender = MessageSender.USER)
            chatDb.insertMessage(userMsg)
            _messages.value = chatDb.getRecentMessages()

            val health = commandDispatcher.generateHealthInsights()
            val vedaMsg = AiChatMessage(
                text = health.responseText,
                sender = MessageSender.VEDA,
                actionType = health.actionType,
                actionExecuted = true,
                smartReplies = health.smartReplies
            )
            chatDb.insertMessage(vedaMsg)
            _messages.value = chatDb.getRecentMessages()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatDb.clearHistory()
            _messages.value = emptyList()
            loadHistory()
        }
    }

    fun setApiKey(key: String) {
        aiSettingsRepo.setApiKey(key)
    }

    fun setDataSharingConsent(consent: Boolean) {
        aiSettingsRepo.setDataSharingConsent(consent)
    }

    fun setModelName(model: String) {
        aiSettingsRepo.setModelName(model)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.stopListening()
    }
}
