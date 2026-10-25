package com.chdjalil.jalilgym.core.ui.body

import android.graphics.Matrix
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.model.Muscle

enum class BodySide { FRONT, BACK }

/**
 * Front and back body figures with each muscle tinted by its intensity (0..1).
 * Used for muscle targeting on exercise detail, for picking muscles in the
 * library filter, and later for weekly volume.
 *
 * Pass [onMuscleClick] to make regions tappable. Always pair a tappable heatmap
 * with an accessible alternative (e.g. chips), since regions aren't focusable.
 */
@Composable
fun BodyHeatmap(
    intensities: Map<Muscle, Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onMuscleClick: ((Muscle) -> Unit)? = null,
) {
    Row(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BodyFigure(BodySide.FRONT, intensities, onMuscleClick, Modifier.weight(1f))
        BodyFigure(BodySide.BACK, intensities, onMuscleClick, Modifier.weight(1f))
    }
}

@Immutable
private class FigureShapes(
    val silhouette: List<Path>,
    val regions: List<Pair<Muscle, Path>>,
    /** Hit regions in viewport units × [HIT_SCALE] (Region works on integer coordinates). */
    val hitRegions: List<Pair<Muscle, Region>>,
)

private const val HIT_SCALE = 10f

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

private fun buildShapes(side: BodySide): FigureShapes {
    val source = if (side == BodySide.FRONT) BodyArtwork.front else BodyArtwork.back
    val regions = source.flatMap { (muscle, paths) -> paths.map { muscle to parse(it) } }
    val clip = Region(
        0,
        0,
        (BodyArtwork.VIEWPORT_WIDTH * HIT_SCALE).toInt(),
        (BodyArtwork.VIEWPORT_HEIGHT * HIT_SCALE).toInt(),
    )
    val scale = Matrix().apply { setScale(HIT_SCALE, HIT_SCALE) }
    val hit = regions.map { (muscle, path) ->
        val scaled = android.graphics.Path(path.asAndroidPath()).apply { transform(scale) }
        muscle to Region().apply { setPath(scaled, clip) }
    }
    return FigureShapes(BodyArtwork.silhouette.map(::parse), regions, hit)
}

@Composable
private fun BodyFigure(
    side: BodySide,
    intensities: Map<Muscle, Float>,
    onMuscleClick: ((Muscle) -> Unit)?,
    modifier: Modifier,
) {
    val shapes = remember(side) { buildShapes(side) }
    val colors = MaterialTheme.colorScheme
    val baseColor = colors.surfaceContainerHigh
    val idleColor = colors.surfaceContainerHighest
    val activeColor = colors.primary
    val gapColor = colors.background

    val tapModifier = if (onMuscleClick == null) {
        Modifier
    } else {
        Modifier.pointerInput(shapes, onMuscleClick) {
            detectTapGestures { offset ->
                val unitsPerPx = BodyArtwork.VIEWPORT_WIDTH / size.width
                val x = (offset.x * unitsPerPx * HIT_SCALE).toInt()
                val y = (offset.y * unitsPerPx * HIT_SCALE).toInt()
                shapes.hitRegions.lastOrNull { (_, region) -> region.contains(x, y) }
                    ?.let { (muscle, _) -> onMuscleClick(muscle) }
            }
        }
    }

    Canvas(
        modifier
            .aspectRatio(BodyArtwork.VIEWPORT_WIDTH / BodyArtwork.VIEWPORT_HEIGHT)
            .then(tapModifier),
    ) {
        val factor = size.width / BodyArtwork.VIEWPORT_WIDTH
        scale(factor, pivot = Offset.Zero) {
            shapes.silhouette.forEach { drawPath(it, baseColor) }
            drawCircle(
                color = baseColor,
                radius = BodyArtwork.HEAD_RADIUS,
                center = Offset(BodyArtwork.HEAD_CENTER_X, BodyArtwork.HEAD_CENTER_Y),
            )
            shapes.regions.forEach { (muscle, path) ->
                drawPath(path, regionColor(intensities[muscle] ?: 0f, idleColor, activeColor))
                drawPath(path, gapColor, style = Stroke(width = 0.6f))
            }
        }
    }
}

/** Any trained muscle is clearly visible (at least 35% of the way to the accent color). */
internal fun regionColor(intensity: Float, idle: Color, active: Color): Color =
    if (intensity <= 0f) idle else lerp(idle, active, 0.35f + 0.65f * intensity.coerceIn(0f, 1f))

/** Intensities for showing an exercise's targets: primary muscles full, secondary half. */
fun muscleTargetIntensities(primary: Collection<Muscle>, secondary: Collection<Muscle>): Map<Muscle, Float> =
    secondary.associateWith { 0.5f } + primary.associateWith { 1f }
