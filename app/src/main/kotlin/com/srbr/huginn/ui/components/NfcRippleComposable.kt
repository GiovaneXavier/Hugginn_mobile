package com.srbr.huginn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

private const val RING_COUNT = 3

/**
 * Ondas NFC — 3 anéis expandindo com stagger de 800ms.
 *
 * [centerFractionY] define em qual fração da altura do Canvas o centro
 * das ondas será desenhado: 0f = topo, 0.5f = centro, 1f = base.
 * Padrão 0.5f (centro).
 */
@Composable
fun NfcRipple(
    visible:         Boolean,
    modifier:        Modifier = Modifier,
    color:           Color    = Color(0xFF1428A0),
    centerFractionY: Float    = 0.5f
) {
    if (!visible) return

    val transition = rememberInfiniteTransition(label = "ripple")
    val progresses = List(RING_COUNT) { i ->
        transition.animateFloat(
            initialValue  = 0f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(i * 800)
            ),
            label = "progress$i"
        )
    }

    Canvas(modifier = modifier) {
        val cx        = size.width  / 2f
        val cy        = size.height * centerFractionY
        val maxRadius = minOf(size.width, size.height) / 2f * 0.85f
        val minRadius = maxRadius * 0.25f

        for (i in 0 until RING_COUNT) {
            val p      = progresses[i].value
            val radius = minRadius + (maxRadius - minRadius) * p
            val alpha  = (1f - p) * 0.5f

            drawCircle(
                color  = color.copy(alpha = alpha),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style  = Stroke(width = 2.5f)
            )
        }
    }
}
