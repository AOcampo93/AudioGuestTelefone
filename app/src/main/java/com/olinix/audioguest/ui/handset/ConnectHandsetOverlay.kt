package com.olinix.audioguest.ui.handset

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.DialogScrim
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Surface
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.ui.theme.WarningOrange

/**
 * Screen 3 · "Conecta el auricular", on top of whatever screen is showing.
 * It has no buttons: it goes away by itself once the handset is plugged back in.
 */
@Composable
fun ConnectHandsetOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DialogScrim)
            // Swallow every tap so nothing underneath can be used without the handset.
            .clickable(interactionSource = null, indication = null, onClick = {})
            .padding(horizontal = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(36.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(40.dp, shape)
                .clip(shape)
                .background(SurfaceRaised)
                .border(1.5.dp, WarningOrange.copy(alpha = 0.5f), shape)
                .padding(start = 56.dp, end = 56.dp, top = 64.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HandsetWarningIcon()
            Text(
                text = stringResource(R.string.handset_connect_title),
                style = MaterialTheme.typography.displaySmall,
                fontSize = 60.sp,
                lineHeight = 63.sp,
                textAlign = TextAlign.Center,
                color = Ivory,
                modifier = Modifier.padding(top = 36.dp),
            )
            Text(
                text = stringResource(R.string.handset_connect_body),
                fontSize = 26.sp,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center,
                color = Taupe,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(max = 540.dp).padding(top = 18.dp),
            )
            Waiting(Modifier.padding(top = 40.dp))
        }
    }
}

/** The handset in a circle with an orange "!" badge. */
@Composable
private fun HandsetWarningIcon() {
    Box(Modifier.size(156.dp)) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF2E231A), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_phone), contentDescription = null, tint = Ivory, modifier = Modifier.size(72.dp))
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(60.dp)
                .background(SurfaceRaised, CircleShape)
                .padding(5.dp)
                .background(WarningOrange, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Surface, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Blinking orange dot and "Esperando conexión…". */
@Composable
private fun Waiting(modifier: Modifier = Modifier) {
    val blink = rememberInfiniteTransition(label = "waiting").animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "waitingDot",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).graphicsLayer { alpha = blink.value }.background(WarningOrange, CircleShape))
        Text(stringResource(R.string.handset_waiting), fontSize = 22.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun ConnectHandsetPreview() {
    AudioGuestTheme { ConnectHandsetOverlay() }
}
