package com.music.bitchord.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.PanelAccount
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/** Hora actual (ms, ya corregida con la del servidor) que se refresca cada segundo. */
@Composable
fun rememberPanelRemainingMs(): Long {
    val expires by AppSettings.panelExpires.collectAsStateWithLifecycle()
    val offset by AppSettings.panelOffsetMs.collectAsStateWithLifecycle()
    var remaining by remember(expires, offset) {
        mutableLongStateOf(PanelAccount.remainingMs(expires, offset))
    }
    LaunchedEffect(expires, offset) {
        while (expires > 0L) {
            remaining = PanelAccount.remainingMs(expires, offset)
            delay(1000)
        }
    }
    return remaining
}

/** Texto de la vigencia: fecha de vencimiento, o "Demo" con su contador. */
@Composable
fun panelValidityText(): String {
    val expires by AppSettings.panelExpires.collectAsStateWithLifecycle()
    val trial by AppSettings.panelTrial.collectAsStateWithLifecycle()
    val remaining = rememberPanelRemainingMs()
    return when {
        expires <= 0L -> stringResource(R.string.account_no_expiry)
        trial -> stringResource(R.string.account_demo_left, PanelAccount.formatCountdown(remaining))
        else -> PanelAccount.formatDate(expires)
    }
}

/**
 * Aviso de la cuenta: aparece al iniciar sesión y al abrir la app cuando a la
 * cuenta le quedan 5 días o menos (demos incluidos). Se compone solo mientras
 * hay sesión.
 */
@Composable
fun PanelAccountNotice() {
    val user by AppSettings.panelUser.collectAsStateWithLifecycle()
    val expires by AppSettings.panelExpires.collectAsStateWithLifecycle()
    val trial by AppSettings.panelTrial.collectAsStateWithLifecycle()
    val offset by AppSettings.panelOffsetMs.collectAsStateWithLifecycle()
    var dismissedFor by rememberSaveable { mutableStateOf("") }
    val remaining = rememberPanelRemainingMs()

    val show = user.isNotBlank() && dismissedFor != user &&
        expires > 0L && remaining > 0L && PanelAccount.daysLeft(remaining) <= PanelAccount.NOTICE_DAYS
    if (!show) return

    val days = PanelAccount.daysLeft(remaining)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = { dismissedFor = user }) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(width = 46.dp, height = 30.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(if (trial) R.string.notice_title_demo else R.string.notice_title_soon),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (trial) {
                Text(
                    text = PanelAccount.formatCountdown(remaining),
                    color = Color(0xFF1ED760),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.notice_demo_left),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 15.sp,
                )
            } else {
                Text(
                    text = if (days <= 1L && remaining < 86_400_000L) stringResource(R.string.notice_today)
                    else days.toString(),
                    color = Color(0xFF1ED760),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (!(days <= 1L && remaining < 86_400_000L)) {
                    Text(
                        text = stringResource(if (days == 1L) R.string.notice_day_left else R.string.notice_days_left),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 15.sp,
                    )
                }
            }
            Text(
                text = stringResource(R.string.notice_expires, PanelAccount.formatDate(expires)),
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.notice_renew),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { dismissedFor = user },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1ED760),
                    contentColor = Color.Black,
                ),
            ) {
                Text(stringResource(R.string.notice_close), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
