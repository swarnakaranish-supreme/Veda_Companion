package com.anish.veda.data.model

enum class HapticCategory(val label: String) {
    CLICK("Clicks & Taps"),
    BUZZ("Buzzes & Fuzz"),
    ALERT("Alert Pulses"),
    PULSE("Heartbeats & Pulses"),
    RAMP("Ramps & Swells")
}

data class HapticEffect(
    val id: Int,
    val name: String,
    val description: String,
    val category: HapticCategory,
    val intensity: String
)

data class HapticConfig(
    val callProfile: Int = 0,
    val msgProfile: Int = 1,
    val alarmProfile: Int = 1,
    val emergencyProfile: Int = 0,
    val navFeedback: Boolean = true,
    val navEffectId: Int = 24,
    val motorType: String = "ERM Coin Motor"
) {
    companion object {
        val CALL_PROFILES = listOf(
            HapticProfileOption(0, "Pulse Ring", "Rhythmic 750ms pulses with 1.8s cadence", 15),
            HapticProfileOption(1, "Double Ring", "Distinct double pulse followed by a high buzz", 10),
            HapticProfileOption(2, "Heartbeat", "Dual-stage biological thumping alert", 52),
            HapticProfileOption(3, "Urgent Burst", "Rapid alternating high-intensity pulses", 14)
        )

        val MSG_PROFILES = listOf(
            HapticProfileOption(0, "Subtle Click", "Single crisp tactile bump for stealth", 1),
            HapticProfileOption(1, "Double Tap", "Modern dual confirmation tap", 10),
            HapticProfileOption(2, "Sharp Ping", "Crisp high-frequency mechanical click", 4),
            HapticProfileOption(3, "Soft Fuzz", "Velvety organic vibration", 13),
            HapticProfileOption(4, "Long Buzz", "Prominent 100% full-duty alert buzz", 14)
        )

        val ALARM_PROFILES = listOf(
            HapticProfileOption(0, "Escalating", "Gentle ramp up to full double-pulse alarm", 70),
            HapticProfileOption(1, "Heavy Pulse", "Authoritative repeated wake pulses", 14),
            HapticProfileOption(2, "Triple Buzz", "High urgency three-stage tremor", 12),
            HapticProfileOption(3, "Strobe Alert", "Rapid strobing haptic pattern", 1)
        )

        val EMERGENCY_PROFILES = listOf(
            HapticProfileOption(0, "SOS Morse", "Standard international Morse ··· ——— ···", 1),
            HapticProfileOption(1, "Hyper Tremor", "Unrelenting 1000ms high-alert bursts", 16),
            HapticProfileOption(2, "Max Continuous", "Sustained maximum drive ERM haptics", 14)
        )

        val NAV_EFFECTS = listOf(
            HapticEffect(24, "Sharp Tick 1", "Clean rotary encoder mechanical click", HapticCategory.CLICK, "100%"),
            HapticEffect(1, "Strong Click", "Solid tactile detent bump", HapticCategory.CLICK, "100%"),
            HapticEffect(7, "Soft Bump", "Damped organic haptic detent", HapticCategory.CLICK, "100%"),
            HapticEffect(2, "Strong Click 60%", "Medium-power mechanical detent", HapticCategory.CLICK, "60%"),
            HapticEffect(3, "Strong Click 30%", "Whisper-quiet low-power detent", HapticCategory.CLICK, "30%")
        )
    }
}

data class HapticProfileOption(
    val id: Int,
    val title: String,
    val description: String,
    val previewEffectId: Int
)

data class HapticProfilePreset(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val config: HapticConfig
)

object HapticCatalog {
    val PRESETS = listOf(
        HapticProfilePreset(
            id = "cyber_stealth",
            name = "Cyber Stealth",
            tag = "DISCREET",
            description = "Subtle clicks and low-frequency pulses designed for meetings and quiet environments.",
            config = HapticConfig(
                callProfile = 0,
                msgProfile = 0,
                alarmProfile = 0,
                emergencyProfile = 0,
                navFeedback = true,
                navEffectId = 24
            )
        ),
        HapticProfilePreset(
            id = "tactical_feedback",
            name = "Tactical Feedback",
            tag = "BALANCED",
            description = "Crisp mechanical detents with punchy dual-tap messaging notifications.",
            config = HapticConfig(
                callProfile = 1,
                msgProfile = 1,
                alarmProfile = 1,
                emergencyProfile = 1,
                navFeedback = true,
                navEffectId = 1
            )
        ),
        HapticProfilePreset(
            id = "gentle_pulse",
            name = "Gentle Pulse",
            tag = "ORGANIC",
            description = "Soft organic fuzz and heartbeat waves mimicking natural wrist sensations.",
            config = HapticConfig(
                callProfile = 2,
                msgProfile = 3,
                alarmProfile = 0,
                emergencyProfile = 0,
                navFeedback = true,
                navEffectId = 7
            )
        ),
        HapticProfilePreset(
            id = "maximum_alert",
            name = "Maximum Alert",
            tag = "HIGH POWER",
            description = "High-energy full-duty vibration for outdoor sports and noisy situations.",
            config = HapticConfig(
                callProfile = 3,
                msgProfile = 4,
                alarmProfile = 3,
                emergencyProfile = 2,
                navFeedback = true,
                navEffectId = 1
            )
        )
    )

    // DRV2605L TS2200 ROM Effect Library (Curated for ERM Coin Motors)
    val EFFECTS = listOf(
        HapticEffect(1, "Strong Click 100%", "Sharp high-power tactile click", HapticCategory.CLICK, "100%"),
        HapticEffect(2, "Strong Click 60%", "Balanced tactile click", HapticCategory.CLICK, "60%"),
        HapticEffect(3, "Strong Click 30%", "Subtle quiet tactile bump", HapticCategory.CLICK, "30%"),
        HapticEffect(4, "Sharp Click 100%", "High frequency mechanical snap", HapticCategory.CLICK, "100%"),
        HapticEffect(5, "Sharp Click 60%", "Medium mechanical snap", HapticCategory.CLICK, "60%"),
        HapticEffect(7, "Soft Bump 100%", "Smooth non-jarring tactile thump", HapticCategory.CLICK, "100%"),
        HapticEffect(8, "Soft Bump 60%", "Gentle wrist nudge", HapticCategory.CLICK, "60%"),
        HapticEffect(9, "Soft Bump 30%", "Whisper wrist tap", HapticCategory.CLICK, "30%"),
        HapticEffect(10, "Double Click 100%", "Distinct double confirmation pulse", HapticCategory.CLICK, "100%"),
        HapticEffect(11, "Double Click 60%", "Gentle double confirmation", HapticCategory.CLICK, "60%"),
        HapticEffect(12, "Triple Click 100%", "Three rapid mechanical clicks", HapticCategory.CLICK, "100%"),
        HapticEffect(13, "Soft Fuzz 60%", "Velvety textured vibration", HapticCategory.BUZZ, "60%"),
        HapticEffect(14, "Strong Buzz 100%", "Heavy high-urgency vibration", HapticCategory.BUZZ, "100%"),
        HapticEffect(15, "750ms Alert 100%", "Extended call alert burst", HapticCategory.ALERT, "100%"),
        HapticEffect(16, "1000ms Alert 100%", "Maximum continuous emergency strobe", HapticCategory.ALERT, "100%"),
        HapticEffect(24, "Sharp Tick 1", "Ultra-crisp encoder navigation detent", HapticCategory.CLICK, "100%"),
        HapticEffect(25, "Sharp Tick 2", "Refined rotary encoder detent", HapticCategory.CLICK, "80%"),
        HapticEffect(27, "Short Double Click 1", "Brief dual pulse for notifications", HapticCategory.CLICK, "100%"),
        HapticEffect(47, "Buzz 1 100%", "Sustained alert frequency buzz", HapticCategory.BUZZ, "100%"),
        HapticEffect(52, "Pulsing Strong 1", "Rhythmic biological heartbeat pulse", HapticCategory.PULSE, "100%"),
        HapticEffect(70, "Ramp Up Long 1", "Smooth swell from silent to peak", HapticCategory.RAMP, "Variable"),
        HapticEffect(82, "Ramp Down Long 1", "Decaying gentle vibration tail", HapticCategory.RAMP, "Variable")
    )
}
