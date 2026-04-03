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

/**
 * Card de credencial com face única.
 *
 * Quando desbloqueado:
 *  - Gira até 90° no eixo Y (fica de lado / invisível)
 *  - Troca o conteúdo (token revelado, badge ATIVO) enquanto está de lado
 *  - Spring de volta a 0° com overshoot suave
 *  - Escala sobe 20% durante o estado desbloqueado
 *  - Ao travar novamente, reverte tudo
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

    // Controla qual conteúdo mostrar (troca enquanto o cartão está de lado)
    var showUnlocked by remember { mutableStateOf(false) }
    // Evita rodar animação na composição inicial
    var initialized  by remember { mutableStateOf(false) }

    LaunchedEffect(isUnlocked) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        if (isUnlocked) {
            // Scale sobe + gira até 90° (fica de lado) em paralelo
            launch { scaleAnim.animateTo(1.2f, tween(320, easing = FastOutSlowInEasing)) }
            rotAnim.animateTo(90f, tween(200, easing = FastOutLinearInEasing))

            // Troca conteúdo enquanto o cartão está invisível (90°)
            showUnlocked = true

            // Spring de volta a 0° com overshoot — "passa um pouco e volta"
            rotAnim.animateTo(
                targetValue   = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            )
            onAnimationComplete()
        } else {
            // Reverte: gira para fora até 90°, troca, volta suave
            launch { scaleAnim.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
            rotAnim.animateTo(90f, tween(180, easing = FastOutLinearInEasing))
            showUnlocked = false
            rotAnim.animateTo(0f, tween(220, easing = LinearOutSlowInEasing))
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
                cameraDistance = 12f * density
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
        if (showUnlocked && card != null) {
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
                    text       = if (showUnlocked) displayId else "SRBR-••••-••••",
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 13.sp,
                    color      = Color.White.copy(alpha = if (showUnlocked) 0.85f else 0.5f)
                )
                CardBadge(isUnlocked = showUnlocked)
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
