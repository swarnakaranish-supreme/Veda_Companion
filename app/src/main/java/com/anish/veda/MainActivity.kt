package com.anish.veda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.anish.veda.ui.navigation.VedaNavGraph
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaTheme
import com.anish.veda.ui.theme.getAccentColor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = (application as VedaApplication).preferencesRepository

        setContent {
            val settings by preferencesRepository.settingsFlow.collectAsState()
            val currentAccent = getAccentColor(settings.accentColorIndex)

            VedaTheme(
                accentColor = currentAccent,
                themePreset = settings.themePreset
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VedaBlack
                ) {
                    VedaNavGraph()
                }
            }
        }
    }
}
