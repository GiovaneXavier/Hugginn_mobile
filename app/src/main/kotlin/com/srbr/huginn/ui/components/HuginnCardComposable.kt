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

private const val UNLOCKED_ROTATION = 22f  // graus de inclinação quando desbloqueado

/**
 * Card de credencial com face única.
 *
 * Quando desbloqueado:
 *  - Inclina UNLOCKED_ROTATION° no eixo Y com spring (overshoot + retorno suave)
 *  - Escala sobe 20% (1.2x)
 *  - Revela token e troca badge SRBR → ATIVO
 *  - Ao expirar (30s), reverte tudo para 0°
 */
@Composable
fun HuginnCard(
    card:                HuginnCard?,
    displayId:           String,
    isUnlocked:          Boolean,
    modifier:            Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    // Spring: vai até o ângulo alvo, passa um pouco (overshoot) e volta
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )

    val rotation by animateFloatAsState(
        targetValue      = if (isUnlocked) UNLOCKED_ROTATION else 0f,
        animationSpec    = springSpec,
        finishedListener = { if (isUnlocked) onAnimationComplete() },
        label            = "cardTilt"
    )

    val scale by animateFloatAsState(
        targetValue   = if (isUnlocked) 1.2f else 1f,
        animationSpec = springSpec,
        label         = "cardScale"
    )

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
                rotationY      = rotation
                scaleX         = scale
                scaleY         = scale
                cameraDistance = 8f * density   // câmera mais próxima = perspectiva mais dramática
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
