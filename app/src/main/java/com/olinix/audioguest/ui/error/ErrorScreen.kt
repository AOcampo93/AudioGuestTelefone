package com.olinix.audioguest.ui.error

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.ErrorReason
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.ui.theme.WarningOrange

/** Something went wrong: say what happened and offer a way out. */
@Composable
fun ErrorScreen(reason: ErrorReason, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val message = stringResource(
        when (reason) {
            ErrorReason.MICROPHONE_UNAVAILABLE -> R.string.error_microphone
            ErrorReason.RECORDING_FAILED -> R.string.error_recording
        }
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(156.dp).border(3.dp, WarningOrange, CircleShape), contentAlignment = Alignment.Center) {
            Text("!", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = WarningOrange, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = message,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            color = Ivory,
            modifier = Modifier.padding(top = 40.dp),
        )
        Text(
            text = stringResource(R.string.error_hint),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Taupe,
            modifier = Modifier.padding(top = 18.dp),
        )
        PillButton(
            text = stringResource(R.string.error_back_to_start),
            onClick = onBack,
            modifier = Modifier.padding(top = 56.dp),
        )
    }
}
