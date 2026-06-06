package com.callmind.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callmind.app.data.local.db.entity.ProcessingStage
import com.callmind.app.ui.theme.CallMissed
import com.callmind.app.ui.theme.GreenPrimary
import com.callmind.app.ui.theme.TextSecondary

private val StepLabels = listOf("Transcribe", "Analyze", "Index")

/**
 * Thin, animated pipeline progress bar.
 *
 * Motion is deliberately limited: an eased determinate fill that advances to the
 * active stage's fraction, plus a soft highlight that sweeps the filled portion
 * while a stage is in flight (an honest "working" signal — we don't fabricate the
 * sub-progress of a remote STT/LLM call).
 */
@Composable
fun ProcessingProgressBar(
    stage: ProcessingStage,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 6.dp,
    accent: Color = GreenPrimary
) {
    val failed = stage == ProcessingStage.FAILED
    val barColor = if (failed) CallMissed else accent
    val trackColor = barColor.copy(alpha = 0.16f)

    val progress by animateFloatAsState(
        targetValue = if (failed) 1f else stage.progress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "progressFill"
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(trackHeight)) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)

        drawRoundRect(color = trackColor, cornerRadius = radius)

        val fillW = (w * progress).coerceIn(0f, w)
        if (fillW > 0f) {
            drawRoundRect(color = barColor, size = Size(fillW, h), cornerRadius = radius)

            if (stage.isActive) {
                val bandW = w * 0.30f
                val travel = fillW + bandW
                val startX = shimmerPhase * travel - bandW
                val brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    startX = startX,
                    endX = startX + bandW
                )
                clipRect(left = 0f, top = 0f, right = fillW, bottom = h) {
                    drawRoundRect(brush = brush, size = Size(fillW, h), cornerRadius = radius)
                }
            }
        }
    }
}

/** Compact one-line indicator for the call list: active stage label + thin bar. */
@Composable
fun ProcessingRowCompact(
    stage: ProcessingStage,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(active = stage.isActive, color = GreenPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stage.activeLabel + if (stage.isActive) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimary,
                fontWeight = FontWeight.Medium
            )
            if (stage.isActive && stage.step in 1..ProcessingStage.STEP_COUNT) {
                Text(
                    text = "  ${stage.step}/${ProcessingStage.STEP_COUNT}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        ProcessingProgressBar(
            stage = stage,
            trackHeight = 4.dp,
            modifier = Modifier.width(150.dp)
        )
    }
}

/**
 * Full stepped indicator for the call detail screen: three labelled steps with
 * connectors, an emphasised active step, and the animated progress bar.
 */
@Composable
fun ProcessingStepper(
    stage: ProcessingStage,
    modifier: Modifier = Modifier
) {
    val activeStep = stage.step
    val allDone = stage == ProcessingStage.COMPLETED

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..ProcessingStage.STEP_COUNT) {
                val state = when {
                    allDone || activeStep > i -> StepState.DONE
                    activeStep == i -> StepState.ACTIVE
                    else -> StepState.PENDING
                }
                StepNode(label = StepLabels[i - 1], state = state)
                if (i < ProcessingStage.STEP_COUNT) {
                    StepConnector(filled = allDone || activeStep > i)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProcessingProgressBar(stage = stage)
    }
}

private enum class StepState { PENDING, ACTIVE, DONE }

@Composable
private fun androidx.compose.foundation.layout.RowScope.StepNode(label: String, state: StepState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        val labelColor = when (state) {
            StepState.DONE, StepState.ACTIVE -> GreenPrimary
            StepState.PENDING -> TextSecondary
        }
        Box(contentAlignment = Alignment.Center) {
            when (state) {
                StepState.DONE -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
                StepState.ACTIVE -> ActiveDot()
                StepState.PENDING -> RingDot(color = TextSecondary.copy(alpha = 0.5f))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            fontWeight = if (state == StepState.ACTIVE) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActiveDot() {
    val transition = rememberInfiniteTransition(label = "activeDot")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Canvas(modifier = Modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = GreenPrimary.copy(alpha = 0.18f * pulse), radius = size.minDimension / 2f)
        drawCircle(
            color = GreenPrimary,
            radius = size.minDimension / 2.6f,
            center = center,
            style = Stroke(width = 2.5.dp.toPx())
        )
        drawCircle(color = GreenPrimary.copy(alpha = pulse), radius = size.minDimension / 6f, center = center)
    }
}

@Composable
private fun RingDot(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        drawCircle(
            color = color,
            radius = size.minDimension / 2.6f,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StepConnector(filled: Boolean) {
    val color = if (filled) GreenPrimary else TextSecondary.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(bottom = 22.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(color)
    )
}

@Composable
private fun PulsingDot(active: Boolean, color: Color) {
    if (!active) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
