package com.chdjalil.jalilgym.feature.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.chdjalil.jalilgym.core.domain.hydration.mlToFlOz
import com.chdjalil.jalilgym.core.domain.hydration.quickWaterAmountsMl
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WaterRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.model.UnitSystem
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first

/** Widgets are created by the system, so they reach the app's singletons through this. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun preferences(): UserPreferencesRepository

    fun programs(): ProgramRepository

    fun workouts(): WorkoutRepository

    fun progress(): ProgressRepository

    fun nutrition(): NutritionRepository

    fun water(): WaterRepository

    fun body(): BodyRepository
}

private fun Context.widgetSources(): TodayWidgetSources {
    val deps = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
    return TodayWidgetSources(deps.preferences(), deps.programs(), deps.workouts(), deps.progress(), deps.nutrition(), deps.water(), deps.body())
}

/** Today at a glance: the next workout, and on wider sizes calories, protein and water. */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sources = context.widgetSources()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val initial = sources.observeState(today, zone).first()
        provideContent {
            val state by remember { sources.observeState(today, zone) }.collectAsState(initial)
            GlanceTheme { TodayWidgetContent(state) }
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 80.dp)
        private val WIDE = DpSize(250.dp, 110.dp)

        /** Keeps placed widgets in step with the app's data while the app is running. */
        @OptIn(FlowPreview::class)
        suspend fun refreshOnChanges(context: Context) {
            val zone = ZoneId.systemDefault()
            context.widgetSources().observeState(LocalDate.now(zone), zone)
                .distinctUntilChanged()
                .drop(1)
                .debounce(1_000)
                .collect { TodayWidget().updateAll(context) }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/** The widget's "+ water" button. */
internal class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val units = deps.preferences().userPreferences.first().units
        deps.water().add(LocalDate.now(), quickWaterAmountsMl(units).first())
        TodayWidget().updateAll(context)
    }
}

private fun water(ml: Int, units: UnitSystem): String = when (units) {
    UnitSystem.METRIC -> String.format(Locale.getDefault(), "%,d ml", ml)
    UnitSystem.IMPERIAL -> "${mlToFlOz(ml).roundToInt()} fl oz"
}

@Composable
private fun TodayWidgetContent(state: TodayWidgetState) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= 250.dp
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ?: Intent()
    val workoutLine = when {
        state.workoutInProgress && state.workoutName != null -> context.getString(R.string.feature_widgets_in_progress, state.workoutName)
        state.trainedToday -> context.getString(R.string.feature_widgets_trained_today)
        state.workoutName != null -> state.workoutName
        else -> context.getString(R.string.feature_widgets_no_program)
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(launch)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            context.getString(if (state.trainedToday) R.string.feature_widgets_today else R.string.feature_widgets_next_workout).uppercase(),
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium),
        )
        Text(
            workoutLine,
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
        if (state.streakWeeks > 0) {
            Text(
                context.resources.getQuantityString(R.plurals.feature_widgets_streak, state.streakWeeks, state.streakWeeks),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
        if (wide) {
            Spacer(GlanceModifier.height(8.dp))
            Text(
                state.kcalTarget?.let { target ->
                    context.getString(R.string.feature_widgets_food, state.kcalEaten, target, state.proteinEaten, state.proteinTarget ?: 0)
                } ?: context.getString(R.string.feature_widgets_food_setup),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
            )
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    context.getString(R.string.feature_widgets_water, water(state.waterMl, state.units), water(state.waterGoalMl, state.units)),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
                val add = context.getString(R.string.feature_widgets_add_water, water(state.waterStepMl, state.units))
                Button(text = add, onClick = actionRunCallback<AddWaterAction>())
            }
        }
    }
}
