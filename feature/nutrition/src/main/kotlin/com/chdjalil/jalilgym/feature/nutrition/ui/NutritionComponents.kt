package com.chdjalil.jalilgym.feature.nutrition.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JgTheme
import com.chdjalil.jalilgym.core.domain.nutrition.progressFraction
import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.feature.nutrition.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_nutrition_back))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

internal fun formatKcal(value: Double): String = "${value.roundToInt()}"

/** Grams with one decimal under 10 g, whole grams above. */
internal fun formatGrams(value: Double): String =
    if (value < 10) "${(value * 10).roundToInt() / 10.0}".removeSuffix(".0") else "${value.roundToInt()}"

/** Remaining calories in the middle, eaten so far as the arc. */
@Composable
internal fun CalorieRing(eaten: Double, target: Int?, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val over = target != null && eaten > target
    val arc = if (over) JgTheme.extendedColors.warning else MaterialTheme.colorScheme.primary
    val fraction = target?.let { progressFraction(eaten, it) } ?: 0f
    val description = if (target == null) {
        stringResource(R.string.feature_nutrition_ring_desc_no_target, formatKcal(eaten))
    } else {
        stringResource(R.string.feature_nutrition_ring_desc, formatKcal(eaten), target)
    }
    Box(modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 12.dp.toPx()
            val style = Stroke(width = stroke, cap = StrokeCap.Round)
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
            if (fraction > 0f) {
                drawArc(arc, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (target != null) {
                val left = target - eaten
                Text(formatKcal(kotlin.math.abs(left)), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(if (left >= 0) R.string.feature_nutrition_kcal_left else R.string.feature_nutrition_kcal_over),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(formatKcal(eaten), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.feature_nutrition_kcal_eaten),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun MacroBar(label: String, value: Double, target: Int?, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                if (target != null) {
                    stringResource(R.string.feature_nutrition_grams_of, formatGrams(value), target)
                } else {
                    stringResource(R.string.feature_nutrition_grams, formatGrams(value))
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { target?.let { progressFraction(value, it) } ?: 0f },
            color = color,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Careful wording: a label, a flag or nothing — never "halal" without a label. */
@Composable
internal fun HalalBadge(status: HalalStatus) {
    val text = when (status) {
        HalalStatus.CERTIFIED -> R.string.feature_nutrition_halal_labeled
        HalalStatus.USER_MARKED -> R.string.feature_nutrition_halal_marked
        HalalStatus.FLAGGED -> R.string.feature_nutrition_halal_flagged
        HalalStatus.UNKNOWN -> return
    }
    val color = if (status == HalalStatus.FLAGGED) JgTheme.extendedColors.warning else JgTheme.extendedColors.success
    SuggestionChip(
        onClick = {},
        label = { Text(stringResource(text)) },
        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = color),
    )
}

internal val MealType.labelRes: Int
    get() = when (this) {
        MealType.BREAKFAST -> R.string.feature_nutrition_meal_breakfast
        MealType.LUNCH -> R.string.feature_nutrition_meal_lunch
        MealType.DINNER -> R.string.feature_nutrition_meal_dinner
        MealType.SNACK -> R.string.feature_nutrition_meal_snacks
    }

internal val DietGoal.labelRes: Int
    get() = when (this) {
        DietGoal.CUT -> R.string.feature_nutrition_goal_cut
        DietGoal.LEAN_BULK -> R.string.feature_nutrition_goal_bulk
        DietGoal.MAINTAIN -> R.string.feature_nutrition_goal_maintain
    }

internal val DietGoal.descriptionRes: Int
    get() = when (this) {
        DietGoal.CUT -> R.string.feature_nutrition_goal_cut_desc
        DietGoal.LEAN_BULK -> R.string.feature_nutrition_goal_bulk_desc
        DietGoal.MAINTAIN -> R.string.feature_nutrition_goal_maintain_desc
    }

internal val DietStyle.labelRes: Int
    get() = when (this) {
        DietStyle.BALANCED -> R.string.feature_nutrition_style_balanced
        DietStyle.HIGH_PROTEIN -> R.string.feature_nutrition_style_high_protein
    }

internal val ActivityLevel.labelRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.feature_nutrition_activity_sedentary
        ActivityLevel.LIGHT -> R.string.feature_nutrition_activity_light
        ActivityLevel.MODERATE -> R.string.feature_nutrition_activity_moderate
        ActivityLevel.VERY_ACTIVE -> R.string.feature_nutrition_activity_very
        ActivityLevel.EXTRA_ACTIVE -> R.string.feature_nutrition_activity_extra
    }

internal val HalalFilter.labelRes: Int
    get() = when (this) {
        HalalFilter.ALL -> R.string.feature_nutrition_halal_filter_all
        HalalFilter.HIDE_FLAGGED -> R.string.feature_nutrition_halal_filter_hide
        HalalFilter.CERTIFIED_ONLY -> R.string.feature_nutrition_halal_filter_certified
    }
