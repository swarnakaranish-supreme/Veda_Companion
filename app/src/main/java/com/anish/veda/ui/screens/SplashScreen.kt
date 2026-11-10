package com.anish.veda.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CyberBlue
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        delay(1800) // Splash animation duration
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Futuristic VEDA Emblem
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width / 2 - 10.dp.toPx()) * pulseScale

                    // Subtle background grid circle
                    drawCircle(
                        color = BorderNavy,
                        radius = radius,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Outer Hex/Dial ring
                    drawCircle(
                        color = NeonCyan.copy(alpha = glowAlpha * 0.4f),
                        radius = radius - 8.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Futuristic "V" Chevron
                    val path = Path().apply {
                        moveTo(center.x - 30.dp.toPx(), center.y - 24.dp.toPx())
                        lineTo(center.x, center.y + 28.dp.toPx())
                        lineTo(center.x + 30.dp.toPx(), center.y - 24.dp.toPx())
                    }

                    drawPath(
                        path = path,
                        color = NeonCyan,
                        style = Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Center glowing core dot
                    drawCircle(
                        color = CyberBlue,
                        radius = 4.dp.toPx(),
                        center = Offset(center.x, center.y)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "VEDA",
                color = TextWhite,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "COMPANION OS",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "DIY ESP32-C3 SMARTWATCH",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
        }
    }
}
