package com.srbr.huginn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

private const val RING_COUNT = 3

/**
 * NFC ripple animation — 3 expanding rings, each staggered.
 * Rendered entirely with Compose Canvas — no custom View needed.
 */
@Composable
fun NfcRipple(
    visible:  Boolean,
    modifier: Modifier = Modifier,
    color:    Color    = Color(0xFF1428A0)
) {
    if (!visible) return

    // Each ring gets its own infinite transition
    val transitions = List(RING_COUNT) { rememberInfiniteTransition(label = "ripple$it") }
    val progresses  = transitions.mapIndexed { i, transition ->
        transition.animateFloat(
            initialValue   = 0f,
            targetValue    = 1f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(i * 800)
            ),
            label = "progress$i"
        )
    }

    Canvas(modifier = modifier) {
        val cx        = size.width  / 2f
        val cy        = size.height / 2f
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
