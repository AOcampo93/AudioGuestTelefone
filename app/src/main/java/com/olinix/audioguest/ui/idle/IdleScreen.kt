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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
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
            .screenBackground()
            .clickable(interactionSource = null, indication = null, onClick = onTap),
    ) {
        // Giant handset silhouette as a watermark.
        Icon(
            painter = painterResource(R.drawable.ic_phone_filled),
            contentDescription = null,
            tint = accent.copy(alpha = 0.05f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 230.dp, y = 170.dp)
                .size(720.dp)
                .rotate(-16f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 64.dp, end = 64.dp, top = 128.dp, bottom = 104.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            EventHeadline(event = event, accent = accent)
            PulsingPhone(accent = accent)
            Invitation(accent = accent)
        }
    }
}

@Composable
private fun EventHeadline(event: EventInfo, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = event.date.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
        Spacer(Modifier.height(40.dp))
        Text(
            text = event.label,
            style = MaterialTheme.typography.headlineMedium,
            color = Taupe,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = event.hosts,
            // Long names wrap onto two balanced lines instead of leaving one word alone.
            style = MaterialTheme.typography.displayLarge.copy(lineBreak = LineBreak.Heading),
            fontSize = 108.sp,
            lineHeight = 108.sp,
            textAlign = TextAlign.Center,
            color = Ivory,
        )
        Box(
            Modifier
                .padding(top = 48.dp)
                .size(width = 120.dp, height = 2.dp)
                .background(accent)
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

    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
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
                modifier = Modifier.size(116.dp),
            )
        }
    }
}

@Composable
private fun Invitation(accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.idle_title),
            style = MaterialTheme.typography.displaySmall,
            fontSize = 56.sp,
            lineHeight = 59.sp,
            textAlign = TextAlign.Center,
            color = Ivory,
            modifier = Modifier.widthIn(max = 620.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.idle_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Sand,
        )
        Spacer(Modifier.height(48.dp))
        TapHint(accent = accent)
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
            .height(68.dp)
            .border(1.5.dp, Ivory.copy(alpha = 0.2f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 34.dp),
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

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun IdleScreenPreview() {
    AudioGuestTheme {
        IdleScreen(event = EventInfo.Sample, onTap = {})
    }
}
