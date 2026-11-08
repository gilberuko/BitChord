package com.music.bitchord.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.PanelLogin
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_logo
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MESH_CYCLE_MILLIS
import com.music.bitchord.ui.player.WelcomeMeshPalettes
import com.music.bitchord.ui.player.cyclingMeshPalette
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Inicio de sesión con usuario y contraseña del panel ibo. Se dibuja encima de
 * todo, incluida la bienvenida; con una cuenta válida guarda la sesión y
 * [AppSettings.panelUser] deja de estar vacío, que es lo que la quita.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    /** Mensaje a mostrar al abrir (p. ej. la cuenta venció desde la última vez). */
    initialError: Int? = null,
    modifier: Modifier = Modifier,
) {
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(initialError) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val palette = cyclingMeshPalette(WelcomeMeshPalettes)

    fun submit() {
        val u = user.trim()
        val p = pass.trim()
        if (busy || u.isEmpty() || p.isEmpty()) return
        focusManager.clearFocus()
        busy = true
        error = null
        scope.launch {
            when (val result = PanelLogin.login(u, p)) {
                is PanelLogin.Result.Success -> AppSettings.setPanelSession(u, p, result.host)
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
            .background(Color.Black)
            // Se traga todo toque para que nada llegue a la app de abajo.
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        MeshGradientBackground(
            palette = palette,
            trackKey = 0,
            continuous = true,
            driftMillis = 6_000,
            colorFadeMillis = MESH_CYCLE_MILLIS,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 28.dp),
            ) {
                val keyboardUp = WindowInsets.isImeVisible
                AnimatedVisibility(
                    visible = !keyboardUp,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        Spacer(Modifier.height(56.dp))
                        Image(
                            painter = painterResource(Res.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier.size(width = 46.dp, height = 30.dp),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Title(stringResource(R.string.login_title))

                Spacer(Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.login_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 17.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                LoginField(
                    value = user,
                    onValueChange = { user = it.take(FIELD_LIMIT); error = null },
                    hint = stringResource(R.string.login_user_hint),
                    password = false,
                    imeAction = ImeAction.Next,
                    onAction = { focusManager.moveFocus(FocusDirection.Down) },
                )
                Spacer(Modifier.height(12.dp))
                LoginField(
                    value = pass,
                    onValueChange = { pass = it.take(FIELD_LIMIT); error = null },
                    hint = stringResource(R.string.login_pass_hint),
                    password = true,
                    imeAction = ImeAction.Done,
                    onAction = { submit() },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = error?.let { stringResource(it) } ?: stringResource(R.string.login_footer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (error != null) Color(0xFFFFB4AB) else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(28.dp))
                PillButton(
                    label = stringResource(if (busy) R.string.login_checking else R.string.login_button),
                    onClick = { submit() },
                    enabled = !busy && user.isNotBlank() && pass.isNotBlank(),
                    primary = true,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    password: Boolean,
    imeAction: ImeAction,
    onAction: () -> Unit,
) {
    val style = MaterialTheme.typography.displayLarge.copy(
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.6).sp,
        color = Color.White,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = hint,
                style = style,
                color = Color.White.copy(alpha = 0.38f),
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Color.White),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Ascii,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onNext = { onAction() }, onDone = { onAction() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val FIELD_LIMIT = 64
