package com.example.pickup.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pickup.viewmodel.IN_TUNE_CENTS

// How far the needle can swing each way, in degrees, for ±50 cents
private const val MAX_ANGLE = 60f
private const val MAX_CENTS = 50f

// Extra UI feature (not in the labs): a tuner gauge drawn on a Canvas.
// The needle animates smoothly to each new reading and the in-tune zone
// turns green when the string is in tune.
// Reference: https://developer.android.com/develop/ui/compose/graphics/draw/overview
// Reference: https://developer.android.com/develop/ui/compose/animation/value-based
// Reference: GenAi see prompt(s) (4) in report
@Composable
fun TunerNeedle(
    cents: Float,
    hasReading: Boolean,
    inTune: Boolean,
    inTuneColour: Color,
    description: String, // read out by TalkBack, e.g. "12 cents flat"
    modifier: Modifier = Modifier
) {
    // Silence: the needle rests in the middle
    val target = if (hasReading) cents.coerceIn(-MAX_CENTS, MAX_CENTS) else 0f
    // A spring makes the needle glide (and settle) instead of jumping
    val animatedCents by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "needle"
    )
    val needleColour by animateColorAsState(
        targetValue = if (inTune) inTuneColour else MaterialTheme.colorScheme.onSurface,
        label = "needleColour"
    )
    val trackColour = MaterialTheme.colorScheme.outlineVariant
    val zoneColour = if (inTune) inTuneColour else inTuneColour.copy(alpha = 0.4f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = description }
    ) {
        val stroke = 10.dp.toPx()
        val radius = minOf(size.width / 2, size.height) * 0.85f
        val pivot = Offset(size.width / 2, size.height - stroke)
        val arcTopLeft = Offset(pivot.x - radius, pivot.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        // Grey track. Angles: 270° is straight up, so the arc runs 210°-330°.
        drawArc(
            color = trackColour,
            startAngle = 270f - MAX_ANGLE, sweepAngle = MAX_ANGLE * 2, useCenter = false,
            topLeft = arcTopLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
        )
        // The in-tune zone in the middle (±5 cents)
        val zoneAngle = IN_TUNE_CENTS / MAX_CENTS * MAX_ANGLE
        drawArc(
            color = zoneColour,
            startAngle = 270f - zoneAngle, sweepAngle = zoneAngle * 2, useCenter = false,
            topLeft = arcTopLeft, size = arcSize, style = Stroke(stroke)
        )

        // The needle: drawn pointing straight up, then rotated around the pivot
        rotate(degrees = animatedCents / MAX_CENTS * MAX_ANGLE, pivot = pivot) {
            drawLine(
                color = needleColour,
                start = pivot,
                end = Offset(pivot.x, pivot.y - radius + stroke * 1.5f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCircle(color = needleColour, radius = 8.dp.toPx(), center = pivot)
    }
}
