package com.olinix.audioguest.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.Surface

/** Big pill-shaped button. The style decides colors; every size comes from the mockups. */
enum class PillStyle { ACCENT, LIGHT, OUTLINE, QUIET }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.ACCENT,
    height: Dp = 104.dp,
    @DrawableRes icon: Int? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    contentColor: Color? = null,
    borderColor: Color = Ivory.copy(alpha = 0.35f),
    /** Off, the pill is faded and ignores taps. */
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(percent = 50)
    val (background, foreground) = when (style) {
        PillStyle.ACCENT -> MaterialTheme.colorScheme.primary to Surface
        PillStyle.LIGHT -> Ivory to Surface
        PillStyle.OUTLINE -> Color.Transparent to Ivory
        PillStyle.QUIET -> Ivory.copy(alpha = 0.04f) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val color = contentColor ?: foreground
    // Small 72 dp pills, like those in the host panel, use smaller text and icons.
    val small = height < 96.dp
    val border = if (style == PillStyle.OUTLINE) Modifier.border(2.dp, borderColor, shape) else Modifier

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .height(height)
            .clip(shape)
            .background(background)
            .then(border)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(if (small) 24.dp else 32.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = when {
                height >= 120.dp -> 34.sp
                small -> 22.sp
                else -> 28.sp
            },
            fontWeight = if (style == PillStyle.QUIET) FontWeight.Medium else FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** "← Volver" pill in the top-left corner. */
@Composable
fun BackPill(onClick: () -> Unit, modifier: Modifier = Modifier, text: String = stringResource(R.string.back)) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .border(1.5.dp, Ivory.copy(alpha = 0.22f), shape)
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = null, tint = Ivory, modifier = Modifier.size(30.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Ivory)
    }
}

/** Small translucent chip such as "🎙 Mensaje de voz" on the countdown. */
@Composable
fun InfoChip(text: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(Ivory.copy(alpha = 0.08f))
            .border(1.5.dp, Ivory.copy(alpha = 0.16f), shape)
            .padding(start = 20.dp, end = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Ivory, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Filled accent circle with a dark icon, used for play buttons and the menu cards. */
@Composable
fun AccentCircle(
    @DrawableRes icon: Int,
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = Charcoal, modifier = Modifier.size(iconSize))
    }
}

/** Outlined circle for secondary round buttons, such as previous and next in the player. */
@Composable
fun OutlineCircle(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    iconSize: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, Ivory.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = Ivory, modifier = Modifier.size(iconSize))
    }
}
