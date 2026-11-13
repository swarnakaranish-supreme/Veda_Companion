package com.anish.veda.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AiSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("veda_ai_secure_prefs", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API_KEY, "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _provider = MutableStateFlow(prefs.getString(KEY_PROVIDER, "GEMINI") ?: "GEMINI")
    val provider: StateFlow<String> = _provider.asStateFlow()

    private val _modelName = MutableStateFlow(prefs.getString(KEY_MODEL, "gemini-2.0-flash") ?: "gemini-2.0-flash")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _dataSharingConsent = MutableStateFlow(prefs.getBoolean(KEY_CONSENT, false))
    val dataSharingConsent: StateFlow<Boolean> = _dataSharingConsent.asStateFlow()

    private val _enableVoiceOutput = MutableStateFlow(prefs.getBoolean(KEY_VOICE_OUT, false))
    val enableVoiceOutput: StateFlow<Boolean> = _enableVoiceOutput.asStateFlow()

    val hasValidApiKey: Boolean
        get() = _apiKey.value.trim().length >= 10

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _apiKey.value = trimmed
    }

    fun setProvider(prov: String) {
        prefs.edit().putString(KEY_PROVIDER, prov).apply()
        _provider.value = prov
    }

    fun setModelName(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _modelName.value = model
    }

    fun setDataSharingConsent(consent: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, consent).apply()
        _dataSharingConsent.value = consent
    }

    fun setEnableVoiceOutput(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_OUT, enabled).apply()
        _enableVoiceOutput.value = enabled
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKey.value = ""
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_MODEL = "ai_model_name"
        private const val KEY_CONSENT = "user_data_sharing_consent"
        private const val KEY_VOICE_OUT = "voice_output_enabled"

        val SUPPORTED_MODELS = listOf(
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        )
    }
}
