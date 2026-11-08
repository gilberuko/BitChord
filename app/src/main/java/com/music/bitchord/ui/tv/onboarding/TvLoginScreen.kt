package com.music.bitchord.ui.tv.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.focusGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.data.PanelLogin
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.tv.TvAppMark
import com.music.bitchord.ui.tv.components.TvButton
import com.music.bitchord.ui.tv.components.TvTextField
import com.music.bitchord.ui.tv.components.tvInitialFocus
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType
import kotlinx.coroutines.launch

/**
 * Inicio de sesión con usuario y contraseña del panel ibo para la app de TV.
 * Se dibuja en lugar de la app; con una cuenta válida guarda la sesión y
 * [AppSettings.panelUser] deja de estar vacío, que es lo que la quita.
 * Se maneja con el control remoto: el teclado del sistema se abre al aceptar
 * sobre un campo.
 */
@Composable
fun TvLoginScreen(
    initialError: Int? = null,
    modifier: Modifier = Modifier,
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(initialError) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun submit() {
        val u = user.trim()
        val p = pass.trim()
        if (busy || u.isEmpty() || p.isEmpty()) return
        busy = true
        error = null
        scope.launch {
            when (val result = PanelLogin.login(u, p)) {
                is PanelLogin.Result.Success -> AppSettings.setPanelSession(
                    u, p, result.host, result.expiresAt, result.trial, result.serverOffsetMs,
                )
                PanelLogin.Result.Invalid -> error = R.string.login_error_invalid
                PanelLogin.Result.Expired -> error = R.string.login_error_expired
                PanelLogin.Result.NoConnection -> error = R.string.login_error_connection
            }
            busy = false
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B0B2E), Color(0xFF3A1250), Color(0xFF0A0510)),
                ),
            )
            .focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TvAppMark(size = 72.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.login_title),
                style = TvType.LargeTitle,
                color = TvGlass.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = error?.let { stringResource(it) } ?: stringResource(R.string.login_footer),
                style = TvType.Callout,
                color = if (error != null) Color(0xFFFFB4AB) else TvGlass.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            TvTextField(
                value = user,
                onValueChange = { user = it.take(64); error = null },
                placeholder = stringResource(R.string.login_user_hint),
                modifier = Modifier.fillMaxWidth().tvInitialFocus(),
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Ascii,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            )
            TvTextField(
                value = pass,
                onValueChange = { pass = it.take(64); error = null },
                placeholder = stringResource(R.string.login_pass_hint),
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                onImeAction = { submit() },
            )
            Spacer(Modifier.height(4.dp))
            TvButton(
                text = stringResource(if (busy) R.string.login_checking else R.string.login_button),
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true,
                enabled = !busy && user.isNotBlank() && pass.isNotBlank(),
            )
        }
    }
}
