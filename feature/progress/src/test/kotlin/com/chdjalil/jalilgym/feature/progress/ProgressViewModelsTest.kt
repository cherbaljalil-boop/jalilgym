package com.chdjalil.jalilgym.feature.progress

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.domain.progress.weekStart
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.StrengthPoint
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeBodyRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeProgressRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.feature.progress.body.BodyViewModel
import com.chdjalil.jalilgym.feature.progress.hub.ProgressViewModel
import com.chdjalil.jalilgym.feature.progress.photos.PhotosViewModel
import com.chdjalil.jalilgym.feature.progress.strength.ChartRange
import com.chdjalil.jalilgym.feature.progress.strength.ExerciseProgressViewModel
import com.chdjalil.jalilgym.feature.progress.strength.StrengthMetric
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progress = FakeProgressRepository()
    private val body = FakeBodyRepository()
    private val prefs = FakeUserPreferencesRepository()
    private val today = LocalDate.now()

    @Test
    fun `hub navigates weeks but not into the future`() = runTest {
        val thisWeek = today.weekStart()
        progress.weeklySets.value = mapOf(
            thisWeek to mapOf(Muscle.CHEST to 12.0),
            thisWeek.minusWeeks(1) to mapOf(Muscle.QUADS to 6.0),
        )
        val viewModel = ProgressViewModel(progress, body, prefs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.week!!.isCurrentWeek).isTrue()
        assertThat(viewModel.uiState.value.week!!.sets).containsExactly(Muscle.CHEST, 12.0)

        viewModel.nextWeek()
        assertThat(viewModel.uiState.value.week!!.weekStart).isEqualTo(thisWeek)

        viewModel.previousWeek()
        assertThat(viewModel.uiState.value.week!!.isCurrentWeek).isFalse()
        assertThat(viewModel.uiState.value.week!!.sets).containsExactly(Muscle.QUADS, 6.0)
    }

    @Test
    fun `hub logs weight in the user's unit and shows the trend`() = runTest {
        prefs.setUnitSystem(UnitSystem.IMPERIAL)
        body.logWeight(today.minusDays(10), 80.0)
        val viewModel = ProgressViewModel(progress, body, prefs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.logWeight(176.0)

        val state = viewModel.uiState.value
        assertThat(state.latestWeight!!.weightKg).isWithin(0.01).of(79.83)
        assertThat(state.recentWeights).hasSize(2)
        assertThat(state.weightChangeKg).isNotNull()
    }

    @Test
    fun `exercise chart follows metric and range`() = runTest {
        val now = System.currentTimeMillis()
        fun daysAgo(days: Long) = now - TimeUnit.DAYS.toMillis(days)
        progress.series.value = mapOf(
            "bench" to listOf(
                StrengthPoint("old", daysAgo(200), 90.0, 80.0, 2000.0),
                StrengthPoint("recent", daysAgo(10), 100.0, 85.0, 2400.0),
            ),
        )
        val viewModel = ExerciseProgressViewModel(
            SavedStateHandle(mapOf("exerciseId" to "bench")),
            FakeExerciseRepository(listOf(sampleExercise("bench", "Bench Press"))),
            progress,
            prefs,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.exercise!!.name).isEqualTo("Bench Press")
        assertThat(viewModel.uiState.value.points.map { it.second }).containsExactly(100.0)

        viewModel.selectRange(ChartRange.ALL)
        viewModel.selectMetric(StrengthMetric.TOP_WEIGHT)
        assertThat(viewModel.uiState.value.points.map { it.second }).containsExactly(80.0, 85.0).inOrder()
        assertThat(viewModel.uiState.value.sessionCount).isEqualTo(2)
    }

    @Test
    fun `body converts measurements and summarizes changes`() = runTest {
        prefs.setUnitSystem(UnitSystem.IMPERIAL)
        body.logMeasurement(today.minusDays(7), MeasurementType.WAIST, 86.36)
        val viewModel = BodyViewModel(body, prefs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.logMeasurement(MeasurementType.WAIST, 33.0)
        viewModel.logMeasurement(MeasurementType.BODY_FAT, 18.0)

        val summaries = viewModel.uiState.value.measurements.associateBy { it.type }
        assertThat(summaries.getValue(MeasurementType.WAIST).latest.value).isWithin(0.001).of(83.82)
        assertThat(summaries.getValue(MeasurementType.WAIST).change).isWithin(0.001).of(-2.54)
        // Body fat is a percentage in every unit system.
        assertThat(summaries.getValue(MeasurementType.BODY_FAT).latest.value).isEqualTo(18.0)
    }

    @Test
    fun `photo selection keeps two and orders the comparison by date`() = runTest {
        body.addPhoto("a", PhotoPose.FRONT, today.minusDays(60))
        body.addPhoto("b", PhotoPose.FRONT, today.minusDays(30))
        body.addPhoto("c", PhotoPose.SIDE, today)
        val viewModel = PhotosViewModel(body, prefs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val ids = body.photos.value.associate { it.filePath to it.id }

        viewModel.toggle(ids.getValue("b"))
        viewModel.toggle(ids.getValue("c"))
        viewModel.toggle(ids.getValue("a"))
        assertThat(viewModel.uiState.value.selected).containsExactly(ids.getValue("c"), ids.getValue("a")).inOrder()
        assertThat(viewModel.comparePair()).isEqualTo(ids.getValue("a") to ids.getValue("c"))

        viewModel.selectPose(PhotoPose.FRONT)
        assertThat(viewModel.uiState.value.photos.map { it.filePath }).containsExactly("b", "a").inOrder()
        assertThat(viewModel.uiState.value.selected).isEmpty()

        viewModel.toggle(ids.getValue("a"))
        viewModel.deleteSelected()
        assertThat(body.photos.value.map { it.filePath }).containsExactly("b", "c")
    }
}
