package com.olinix.audioguest.ui.delivery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.data.DeliveryChannel
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.components.BackPill
import com.olinix.audioguest.ui.components.ChoiceToggle
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.ToggleChoice
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.FieldBackground
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Muted
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Taupe

/** Most guests are in Portugal, so the number starts with its country code; they can change it. */
private const val PHONE_PREFIX = "+351 "

/**
 * "Receba a sua mensagem": the guest leaves an e-mail or a WhatsApp number, never leaving the app.
 * The host sends the message by hand in the next days.
 *
 * @param onSubmit returns false if the address does not look right, so the form says so.
 * @param onTyping typing does not touch the screen, so it has to keep the form from timing out.
 */
@Composable
fun DeliveryFormScreen(
    onSubmit: (DeliveryChannel, String, Boolean) -> Boolean,
    onBack: () -> Unit,
    onTyping: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var channel by rememberSaveable { mutableStateOf(DeliveryChannel.EMAIL) }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf(PHONE_PREFIX) }
    var wantsNews by rememberSaveable { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }
    val isEmail = channel == DeliveryChannel.EMAIL
    val submit = { invalid = !onSubmit(channel, if (isEmail) email else phone, wantsNews) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            // The keyboard pushes the form up, and it scrolls if it no longer fits.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 56.dp, end = 56.dp, top = 48.dp, bottom = 56.dp),
    ) {
        BackPill(onClick = onBack)
        Text(
            text = stringResource(R.string.delivery_title),
            style = MaterialTheme.typography.displayMedium,
            fontSize = 68.sp,
            lineHeight = 72.sp,
            color = Ivory,
            modifier = Modifier.padding(top = 44.dp),
        )
        Text(
            text = stringResource(R.string.delivery_body),
            style = MaterialTheme.typography.bodyLarge,
            color = IvorySoft,
            modifier = Modifier.padding(top = 12.dp),
        )

        ChoiceToggle(
            choices = listOf(
                ToggleChoice(DeliveryChannel.EMAIL, R.drawable.ic_mail, stringResource(R.string.delivery_email)),
                ToggleChoice(DeliveryChannel.WHATSAPP, R.drawable.ic_chat, stringResource(R.string.delivery_whatsapp)),
            ),
            selected = channel,
            onSelect = {
                channel = it
                invalid = false
            },
            modifier = Modifier.padding(top = 40.dp),
        )

        AddressField(
            value = if (isEmail) email else phone,
            onValueChange = { typed ->
                if (isEmail) email = typed else phone = typed
                invalid = false
                onTyping()
            },
            channel = channel,
            onDone = submit,
            modifier = Modifier.padding(top = 24.dp),
        )
        if (invalid) {
            Text(
                text = stringResource(if (isEmail) R.string.delivery_email_invalid else R.string.delivery_phone_invalid),
                fontSize = 24.sp,
                color = RecordRedLight,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        NewsCheckbox(checked = wantsNews, onCheckedChange = { wantsNews = it }, modifier = Modifier.padding(top = 32.dp))

        Text(
            text = stringResource(R.string.delivery_when),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            color = Sand,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            text = stringResource(R.string.delivery_privacy),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 10.dp),
        )
        PillButton(
            text = stringResource(R.string.delivery_send),
            onClick = submit,
            icon = if (isEmail) R.drawable.ic_mail else R.drawable.ic_chat,
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        )
    }
}

/** Big one-line field with the keyboard for e-mails or phone numbers. It takes the focus, so the keyboard opens at once. */
@Composable
private fun AddressField(
    value: String,
    onValueChange: (String) -> Unit,
    channel: DeliveryChannel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(channel) { focus.requestFocus() }
    val shape = RoundedCornerShape(20.dp)
    val isEmail = channel == DeliveryChannel.EMAIL
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 34.sp, color = Ivory),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = if (isEmail) KeyboardType.Email else KeyboardType.Phone,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { onDone() }),
        modifier = modifier.fillMaxWidth().height(104.dp).focusRequester(focus),
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(FieldBackground)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), shape)
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(stringResource(R.string.delivery_email_hint), fontSize = 34.sp, color = Muted, style = MaterialTheme.typography.bodyLarge)
                }
                field()
            }
        },
    )
}

/** "☐ Quero receber novidades da Vokraft". Unticked unless the guest ticks it: consent must be their choice. */
@Composable
private fun NewsCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(if (checked) Modifier.background(accent) else Modifier.border(2.dp, Ivory.copy(alpha = 0.4f), RoundedCornerShape(12.dp))),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Charcoal, modifier = Modifier.size(30.dp))
        }
        Text(stringResource(R.string.delivery_news), fontSize = 26.sp, color = IvorySoft, style = MaterialTheme.typography.bodyLarge)
    }
}

/** "Pronto!", repeating the e-mail or number, then back to the invitation on its own. */
@Composable
fun DeliveryDoneScreen(state: SessionState.DeliveryDone, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val pop = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(modifier.fillMaxSize().screenBackground()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, bottom = 140.dp),
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
                Icon(painterResource(R.drawable.ic_mail), contentDescription = null, tint = Charcoal, modifier = Modifier.size(96.dp))
            }
            Text(
                text = stringResource(R.string.delivery_done_title),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 112.sp,
                lineHeight = 116.sp,
                color = Ivory,
                modifier = Modifier.padding(top = 56.dp),
            )
            Text(
                text = stringResource(R.string.delivery_done_body, state.address),
                fontSize = 30.sp,
                lineHeight = 42.sp,
                color = IvorySoft,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ProgressBar(
                progress = (state.secondsLeft - 1f) / state.totalSeconds,
                color = accent,
                height = 6.dp,
                modifier = Modifier.width(320.dp),
            )
            Text(text = stringResource(R.string.saved_back_in, state.secondsLeft), color = Taupe, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun DeliveryFormPreview() {
    AudioGuestTheme { DeliveryFormScreen(onSubmit = { _, _, _ -> true }, onBack = {}, onTyping = {}) }
}
