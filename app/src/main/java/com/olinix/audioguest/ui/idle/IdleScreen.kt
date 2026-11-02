package com.olinix.audioguest.ui.idle

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.CharcoalWarm
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe

/** Screen 1 · Idle / invitation. The whole screen is tappable. */
@Composable
fun IdleScreen(
    event: EventInfo,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to CharcoalWarm,
                        0.62f to Charcoal,
                        center = Offset(size.width / 2, 0f),
                        radius = size.height * 1.2f,
                    )
                )
            }
            .clickable(interactionSource = null, indication = null, onClick = onTap),
    ) {
        // Giant handset silhouette as a watermark.
        Icon(
            painter = painterResource(R.drawable.ic_phone_filled),
            contentDescription = null,
            tint = accent.copy(alpha = 0.05f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 190.dp)
                .size(640.dp)
                .rotate(-16f),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 96.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            EventHeadline(event = event, accent = accent, modifier = Modifier.weight(1.15f))

            Column(
                modifier = Modifier.weight(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(72.dp),
            ) {
                PulsingPhone(accent = accent)
                TapHint(accent = accent)
            }
        }
    }
}

@Composable
private fun EventHeadline(event: EventInfo, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = event.date.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
        Spacer(Modifier.height(36.dp))
        Text(
            text = event.label,
            style = MaterialTheme.typography.headlineMedium,
            color = Taupe,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = event.hosts,
            style = MaterialTheme.typography.displayLarge,
            color = Ivory,
        )
        Box(
            Modifier
                .padding(top = 48.dp, bottom = 40.dp)
                .size(width = 120.dp, height = 2.dp)
                .background(accent)
        )
        Text(
            text = stringResource(R.string.idle_title),
            style = MaterialTheme.typography.displaySmall,
            color = Ivory,
            modifier = Modifier.widthIn(max = 600.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.idle_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Sand,
        )
    }
}

/** Handset circle with three expanding rings that invite guests to tap. */
@Composable
private fun PulsingPhone(accent: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3600, easing = LinearEasing)),
        label = "pulseProgress",
    )

    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        repeat(3) { ring ->
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // Each ring runs a third of a cycle behind the previous one.
                        val t = FastOutSlowInEasing.transform((progress.value + ring / 3f) % 1f)
                        scaleX = 1f + 0.8f * t
                        scaleY = scaleX
                        alpha = 0.6f * (1f - t)
                    }
                    .border(2.dp, accent, CircleShape)
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(SurfaceRaised, CircleShape)
                .border(2.dp, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_phone),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(108.dp),
            )
        }
    }
}

@Composable
private fun TapHint(accent: Color) {
    val glow = rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    Row(
        modifier = Modifier
            .height(64.dp)
            .border(1.5.dp, Ivory.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(12.dp)
                .graphicsLayer { alpha = glow.value }
                .background(accent, CircleShape)
        )
        Text(
            text = stringResource(R.string.idle_cta),
            style = MaterialTheme.typography.bodyMedium,
            color = IvorySoft,
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun IdleScreenPreview() {
    AudioGuestTheme {
        IdleScreen(event = EventInfo.Sample, onTap = {})
    }
}
