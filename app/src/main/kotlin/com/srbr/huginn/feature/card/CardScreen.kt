package com.srbr.huginn.feature.card

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srbr.huginn.ui.components.HuginnCard
import com.srbr.huginn.ui.components.NfcRipple
import com.srbr.huginn.ui.theme.*

@Composable
fun CardScreen(
    onRequestBiometric: (onSuccess: () -> Unit) -> Unit,
    onBack:             () -> Unit,
    viewModel:          CardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Se desbloqueado: trava o cartão e fica na tela. Se bloqueado: navega para trás.
    BackHandler {
        if (state.isUnlocked) viewModel.onExpire() else onBack()
    }

    // Vibração suave enquanto NFC está ativo
    val context = LocalContext.current
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
    DisposableEffect(Unit) {
        onDispose { vibrator?.cancel() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackground()
                vibrator?.cancel() // cancela imediatamente sem aguardar recomposição
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Show loading indicator while card hasn't loaded yet
    if (!state.hasLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SamsungBlue)
        }
        return
    }

    // Ondas aparecem apenas após a animação de desbloqueio terminar
    var rippleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.isUnlocked) {
        if (!state.isUnlocked) rippleVisible = false
    }

    // Posição vertical do topo do cartão (fração de 0..1) para centrar as ondas
    val configuration  = LocalConfiguration.current
    val density        = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    var cardTopFraction by remember { mutableStateOf(0.35f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Botão voltar — topo esquerdo
        IconButton(
            onClick  = { if (state.isUnlocked) viewModel.onExpire() else onBack() },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint               = SubtleText
            )
        }

        // Ondas NFC — centradas no topo do cartão, aparecem após animação
        NfcRipple(
            visible         = rippleVisible,
            modifier        = Modifier.fillMaxSize(),
            color           = SamsungBlue,
            centerFractionY = cardTopFraction
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {

            // Label superior
            Text(
                text          = "SRBR · HUGGINN",
                fontSize      = 11.sp,
                color         = SubtleText,
                letterSpacing = 2.sp,
                fontWeight    = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Cartão
            state.card?.let { card ->
                HuginnCard(
                    card        = card,
                    displayId   = state.displayId,
                    isUnlocked  = state.isUnlocked,
                    onAnimationComplete = {
                        if (state.isUnlocked) rippleVisible = true
                    },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        // Fração vertical do topo do cartão na tela
                        val topPx = coords.positionInRoot().y
                        cardTopFraction = (topPx / screenHeightPx).coerceIn(0.05f, 0.95f)
                    }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Status
            AnimatedContent(
                targetState    = state.isUnlocked,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label          = "status"
            ) { unlocked ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = if (unlocked) "Aproxime do leitor Heimdall"
                                     else "Toque para autenticar",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (unlocked) SuccessGreen else MutedText,
                        textAlign  = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text      = if (unlocked) "NFC ativo · Biometria confirmada"
                                    else "Sua biometria confirma que você é o proprietário",
                        fontSize  = 12.sp,
                        color     = SubtleText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botão de autenticação ou countdown
            AnimatedVisibility(visible = !state.isUnlocked) {
                Button(
                    onClick = {
                        onRequestBiometric { viewModel.onBiometricSuccess() }
                    },
                    modifier = Modifier
                        .width(280.dp)
                        .height(52.dp),
                    shape  = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue)
                ) {
                    Text(
                        "🔒  Desbloquear com Biometria",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = state.isUnlocked) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(280.dp)
                ) {
                    LinearProgressIndicator(
                        progress   = { state.countdownPct },
                        modifier   = Modifier.fillMaxWidth().height(4.dp),
                        color      = SuccessGreen,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "${state.countdown}s",
                        fontSize = 13.sp,
                        color    = SubtleText
                    )
                }
            }
        }
    }
}
