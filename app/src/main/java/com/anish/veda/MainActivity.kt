package com.anish.veda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.anish.veda.ui.navigation.VedaNavGraph
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Notice: Do NOT automatically start VedaBleForegroundService here!
        // Android 14/15 requires BLUETOOTH_CONNECT/BLUETOOTH_SCAN runtime permissions
        // before starting a connectedDevice foreground service.
        // The service is started safely only after permissions are granted and user initiates connection.

        setContent {
            VedaTheme {
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
