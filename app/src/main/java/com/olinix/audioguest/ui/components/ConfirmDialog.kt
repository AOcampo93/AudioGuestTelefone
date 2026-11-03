package com.olinix.audioguest.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.ui.theme.DialogScrim
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRed
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceRaised

/**
 * Confirmation for an action that cannot be undone, such as "¿Descartar este mensaje?".
 * The safe choice is the big gold button; the risky one is the red outline below it.
 */
@Composable
fun ConfirmDialog(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    keepText: String,
    confirmText: String,
    onKeep: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DialogScrim)
            // Swallow taps on the scrim so nothing underneath reacts.
            .clickable(interactionSource = null, indication = null, onClick = {})
            .padding(horizontal = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(SurfaceRaised)
                .border(1.5.dp, Ivory.copy(alpha = 0.14f), RoundedCornerShape(36.dp))
                .padding(start = 48.dp, end = 48.dp, top = 56.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(104.dp).background(RecordRed.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = RecordRedLight, modifier = Modifier.size(46.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 52.sp,
                textAlign = TextAlign.Center,
                color = Ivory,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = Sand,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )
            PillButton(
                text = keepText,
                onClick = onKeep,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
            )
            PillButton(
                text = confirmText,
                onClick = onConfirm,
                style = PillStyle.OUTLINE,
                height = 96.dp,
                contentColor = RecordRedLight,
                borderColor = RecordRedLight.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}
