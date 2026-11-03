package com.olinix.audioguest.ui.saved

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Taupe

/**
 * Screen 9 · "¡Gracias!" with a gold check, then back to Idle on its own.
 * When the host allows it, "Receber a minha mensagem" asks for an e-mail or a WhatsApp number.
 */
@Composable
fun SavedScreen(state: SessionState.Saved, event: EventInfo, modifier: Modifier = Modifier, onReceive: () -> Unit = {}) {
    val accent = MaterialTheme.colorScheme.primary
    val pop = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box(modifier.fillMaxSize().screenBackground()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 64.dp, end = 64.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Charcoal, modifier = Modifier.size(100.dp))
            }
            Text(
                text = stringResource(R.string.saved_title),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 128.sp,
                lineHeight = 128.sp,
                color = Ivory,
                modifier = Modifier.padding(top = 56.dp),
            )
            Text(
                text = stringResource(R.string.saved_for, event.hosts),
                fontSize = 30.sp,
                lineHeight = 40.sp,
                color = IvorySoft,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp),
            )
            if (state.recordingId != null) {
                PillButton(
                    text = stringResource(R.string.saved_receive),
                    onClick = onReceive,
                    style = PillStyle.OUTLINE,
                    icon = R.drawable.ic_mail,
                    borderColor = accent.copy(alpha = 0.6f),
                    contentColor = accent,
                    modifier = Modifier.padding(top = 56.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_phone), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.saved_hang_up), color = Taupe, style = MaterialTheme.typography.bodyMedium)
            }
            ProgressBar(
                progress = (state.secondsLeft - 1f) / state.totalSeconds,
                color = accent,
                height = 6.dp,
                modifier = Modifier.width(320.dp),
            )
            Text(
                text = stringResource(R.string.saved_back_in, state.secondsLeft),
                color = Sand,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun SavedPreview() {
    AudioGuestTheme { SavedScreen(SessionState.Saved(secondsLeft = 4, totalSeconds = 5), EventInfo.Sample) }
}
