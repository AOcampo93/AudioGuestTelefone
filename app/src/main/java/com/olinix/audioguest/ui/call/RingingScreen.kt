package com.olinix.audioguest.ui.call

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.Ringer
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.ui.components.AccentCircle
import com.olinix.audioguest.ui.components.InfoChip
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import kotlin.math.PI
import kotlin.math.sin

/**
 * Call mode · "Te están llamando". The bell rings through the tablet's speaker while this screen
 * is up. A tap anywhere, "Contestar" or the handset button answers.
 */
@Composable
fun RingingScreen(event: EventInfo, onAnswer: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val ringer = Ringer(context)
        ringer.start()
        onDispose { ringer.stop() }
    }
    val accent = MaterialTheme.colorScheme.primary
    // Seconds into the bell's 3-second cycle, read only while drawing so the screen is not rebuilt every frame.
    val cycle = rememberInfiniteTransition(label = "ringing")
        .animateFloat(0f, 3f, infiniteRepeatable(tween(3_000, easing = LinearEasing)), label = "cycle")

    Box(
        modifier
            .fillMaxSize()
            .screenBackground()
            .clickable(interactionSource = null, indication = null, onClick = onAnswer),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            InfoChip(text = stringResource(R.string.call_incoming), icon = R.drawable.ic_phone)
            Box(
                modifier = Modifier
                    .size(460.dp)
                    .drawBehind {
                        // Two rings spread out from the phone, one after the other.
                        for (offset in listOf(0f, 0.5f)) {
                            val spread = (cycle.value / 1.5f + offset) % 1f
                            drawCircle(
                                color = accent.copy(alpha = (1 - spread) * 0.5f),
                                radius = size.minDimension / 2 * (0.55f + 0.45f * spread),
                                style = Stroke(3.dp.toPx()),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AccentCircle(
                    icon = R.drawable.ic_phone_filled,
                    size = 240.dp,
                    iconSize = 110.dp,
                    modifier = Modifier.graphicsLayer { rotationZ = shake(cycle.value) },
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = event.headline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 32.sp,
                    color = accent,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.call_title),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 96.sp,
                    lineHeight = 100.sp,
                    color = Ivory,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.call_body),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 30.sp,
                    lineHeight = 40.sp,
                    color = IvorySoft,
                    textAlign = TextAlign.Center,
                )
            }
            PillButton(
                text = stringResource(R.string.call_answer),
                onClick = onAnswer,
                icon = R.drawable.ic_phone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The phone trembles while the bell rings (0–0.4 s and 0.6–1 s of each cycle) and rests in between. */
private fun shake(second: Float): Float {
    val ringing = second < 0.4f || second in 0.6f..1f
    return if (ringing) 9f * sin(second * 2 * PI.toFloat() * 12) else 0f
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun RingingPreview() {
    AudioGuestTheme { RingingScreen(EventInfo.Sample, onAnswer = {}) }
}
