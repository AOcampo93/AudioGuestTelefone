package com.olinix.audioguest.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.components.BackPill
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe

/** How long the host holds the hidden corner before the PIN pad opens. */
private const val HOLD_MILLIS = 3_000L

private val KeyHeight = 104.dp
private val KeyShape = RoundedCornerShape(24.dp)

/**
 * Invisible square in the top-right corner of the invitation. Holding it for 3 s opens the
 * PIN pad; a short tap does nothing, so guests who touch it by chance notice nothing.
 */
@Composable
fun HostCorner(onHold: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnHold by rememberUpdatedState(onHold)
    Box(
        modifier
            .size(140.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Taken from the invitation underneath, so this touch never starts a session.
                    awaitFirstDown().consume()
                    val lifted = withTimeoutOrNull(HOLD_MILLIS) {
                        waitForUpOrCancellation()?.consume()
                        true
                    }
                    if (lifted == null) currentOnHold()
                }
            },
    )
}

/** Host access · "Acceso del anfitrión" with a 4-digit PIN pad. */
@Composable
fun HostLoginScreen(
    state: SessionState.HostLogin,
    pinLength: Int,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .padding(horizontal = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterVertically),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).border(1.5.dp, accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = accent, modifier = Modifier.size(42.dp))
            }
            Text(
                text = stringResource(R.string.host_login_title),
                style = MaterialTheme.typography.displayMedium,
                color = Ivory,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 36.dp),
            )
            Text(
                text = stringResource(R.string.host_login_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Sand,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp).widthIn(max = 460.dp),
            )
            PinDots(filled = state.digits, length = pinLength, modifier = Modifier.padding(top = 48.dp))
            // Always laid out, so the pad does not jump when the message appears,
            // but hidden from screen readers until then.
            Text(
                text = stringResource(R.string.host_login_wrong),
                fontSize = 22.sp,
                color = RecordRedLight,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .alpha(if (state.wrongPin) 1f else 0f)
                    .then(if (state.wrongPin) Modifier else Modifier.clearAndSetSemantics {}),
            )
        }
        Keypad(onDigit = onDigit, onErase = onErase)
        BackPill(onClick = onBack, text = stringResource(R.string.host_back_to_kiosk))
    }
}

/** ● ● ○ ○ */
@Composable
private fun PinDots(filled: Int, length: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(length) { index ->
            val dot = Modifier.size(30.dp)
            if (index < filled) {
                Box(dot.background(MaterialTheme.colorScheme.primary, CircleShape))
            } else {
                Box(dot.border(2.dp, Ivory.copy(alpha = 0.35f), CircleShape))
            }
        }
    }
}

/** 1–9, then 0 and "Borrar" on the last row. */
@Composable
private fun Keypad(onDigit: (Int) -> Unit, onErase: () -> Unit) {
    Column(Modifier.width(440.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        for (row in listOf(1..3, 4..6, 7..9)) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                for (digit in row) DigitKey(digit, onDigit, Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.weight(1f))
            DigitKey(0, onDigit, Modifier.weight(1f))
            Box(
                Modifier
                    .weight(1f)
                    .height(KeyHeight)
                    .clip(KeyShape)
                    .clickable(onClick = onErase),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.host_login_erase),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Taupe,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Int, onDigit: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(KeyHeight)
            .clip(KeyShape)
            .background(SurfaceRaised)
            .border(1.5.dp, Ivory.copy(alpha = 0.12f), KeyShape)
            .clickable { onDigit(digit) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$digit",
            fontSize = 42.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ivory,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
        )
    }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun HostLoginPreview() {
    AudioGuestTheme {
        HostLoginScreen(SessionState.HostLogin(digits = 2), pinLength = 4, onDigit = {}, onErase = {}, onBack = {})
    }
}
