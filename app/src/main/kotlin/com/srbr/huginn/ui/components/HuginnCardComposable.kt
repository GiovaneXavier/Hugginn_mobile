package com.srbr.huginn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srbr.huginn.core.security.HuginnCard
import com.srbr.huginn.ui.theme.*
import kotlinx.coroutines.launch

private const val TILT_TARGET   = 18f  // ângulo final quando desbloqueado
private const val TILT_OVERSHOOT = 23f  // ultrapassa levemente antes de assentar

/**
 * Card de credencial com face única — animação estilo Samsung Wallet.
 *
 * Desbloqueado:
 *  - Inclina até TILT_OVERSHOOT° (rápido), recua para TILT_TARGET° (suave)
 *  - Sem oscilação — movimento único, limpo
 *  - Escala sobe 20% em paralelo
 *  - Ao expirar, reverte suavemente para 0°
 */
@Composable
fun HuginnCard(
    card:                HuginnCard?,
    displayId:           String,
    isUnlocked:          Boolean,
    modifier:            Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val rotAnim   = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(isUnlocked) {
        if (!initialized) { initialized = true; return@LaunchedEffect }
        if (isUnlocked) {
            launch { scaleAnim.animateTo(1.2f, tween(420, easing = FastOutSlowInEasing)) }
            // Vai rápido até o overshoot, recua suave para o ângulo final
            rotAnim.animateTo(TILT_OVERSHOOT, tween(300, easing = FastOutLinearInEasing))
            rotAnim.animateTo(TILT_TARGET,    tween(180, easing = LinearOutSlowInEasing))
            onAnimationComplete()
        } else {
            launch { scaleAnim.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
            rotAnim.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        }
    }

    val cardColor = remember(card?.cardColor) {
        runCatching {
            Color(android.graphics.Color.parseColor(card?.cardColor ?: "#1428A0"))
        }.getOrElse { SamsungBlue }
    }

    Box(
        modifier = modifier
            .width(320.dp)
            .height(200.dp)
            .graphicsLayer {
                rotationY      = rotAnim.value
                scaleX         = scaleAnim.value
                scaleY         = scaleAnim.value
                cameraDistance = 8f * density
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(cardColor, SamsungBlueLight)))
    ) {
        // Cabeçalho: "Samsung Research Brasil" + "Huginn"
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text          = "Samsung Research Brasil",
                fontSize      = 10.sp,
                color         = Color.White.copy(alpha = 0.6f),
                fontWeight    = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Text(
                text       = "Huginn",
                fontSize   = 26.sp,
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Centro: nome do funcionário — aparece quando desbloqueado
        if (isUnlocked && card != null) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = card.employeeName,
                    modifier   = Modifier.padding(horizontal = 20.dp),
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }

        // Base: token (mascarado ou revelado) + badge (SRBR / ATIVO)
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = if (isUnlocked) displayId else "SRBR-••••-••••",
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 13.sp,
                    color      = Color.White.copy(alpha = if (isUnlocked) 0.85f else 0.5f)
                )
                CardBadge(isUnlocked = isUnlocked)
            }
        }
    }
}

@Composable
private fun CardBadge(isUnlocked: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SuccessGreen)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text          = "ATIVO",
                fontSize      = 10.sp,
                color         = Color.White,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        } else {
            Text(
                text          = "SRBR",
                fontSize      = 10.sp,
                color         = Color.White.copy(alpha = 0.7f),
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
