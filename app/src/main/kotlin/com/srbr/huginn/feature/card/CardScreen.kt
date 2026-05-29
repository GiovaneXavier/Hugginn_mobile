package com.srbr.huginn.feature.card

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srbr.huginn.credential.card.CardScreenScaffold
import com.srbr.huginn.credential.ui.CardUnlockMotion
import com.srbr.huginn.credential.ui.theme.SamsungBlue
import com.srbr.huginn.ui.components.NfcRipple

@Composable
fun CardScreen(
    onRequestBiometric: (onSuccess: () -> Unit) -> Unit,
    onBack:             () -> Unit,
    viewModel:          CardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Vibração suave enquanto o NFC está ativo.
    val context  = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 10, 1100), intArrayOf(0, 70, 0), 1)
            )
        } else {
            vibrator?.cancel()
        }
    }
    DisposableEffect(Unit) { onDispose { vibrator?.cancel() } }

    // Cancela a vibração imediatamente ao ir para background (sem aguardar recomposição).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vibrator?.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ondas aparecem só após a animação de desbloqueio terminar.
    var rippleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.isUnlocked) {
        if (!state.isUnlocked) rippleVisible = false
    }

    CardScreenScaffold(
        state                  = state,
        cardMotion             = CardUnlockMotion.ROTATE_TO_PORTRAIT,
        statusUnlockedTitle    = "Aproxime do leitor Heimdall",
        statusUnlockedSubtitle = "NFC ativo · Biometria confirmada",
        onRequestBiometric     = onRequestBiometric,
        onBiometricSuccess     = viewModel::onBiometricSuccess,
        onExpire               = viewModel::onExpire,
        onAppBackground        = viewModel::onAppBackground,
        onBack                 = onBack,
        showBackButton         = true,
        cardToStatusSpacing    = 80.dp,
        statusToControlSpacing = 40.dp,
        onUnlockAnimationComplete = { rippleVisible = true },
        background = { cardTopFraction ->
            NfcRipple(
                visible         = rippleVisible,
                modifier        = Modifier.fillMaxSize(),
                color           = SamsungBlue,
                centerFractionY = cardTopFraction
            )
        }
    )
}
