package com.chdjalil.jalilgym.feature.nutrition.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.common.Units
import com.chdjalil.jalilgym.core.domain.nutrition.TargetResult
import com.chdjalil.jalilgym.core.domain.nutrition.computeTargets
import com.chdjalil.jalilgym.core.domain.nutrition.computeTdee
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.domain.units.toKg
import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Sex
import com.chdjalil.jalilgym.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Manual targets as typed; null fields fall back to the computed value. */
data class TargetOverride(val calories: String = "", val protein: String = "", val carbs: String = "", val fat: String = "")

data class SetupUiState(
    val isLoading: Boolean = true,
    val units: UnitSystem = UnitSystem.METRIC,
    val sex: Sex? = null,
    val birthDate: LocalDate? = null,
    /** In cm (metric) or inches (imperial). */
    val height: String = "",
    /** In kg or lb. */
    val weight: String = "",
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val goal: DietGoal = DietGoal.MAINTAIN,
    val style: DietStyle = DietStyle.BALANCED,
    val halalFilter: HalalFilter = HalalFilter.ALL,
    /** Null until the body questions are answered. */
    val computed: TargetResult? = null,
    val override: TargetOverride = TargetOverride(),
    val isEditing: Boolean = false,
    val saved: Boolean = false,
) {
    private fun String.int() = trim().toIntOrNull()?.takeIf { it >= 0 }

    val hasOverride: Boolean
        get() = listOf(override.calories, override.protein, override.carbs, override.fat).any { it.isNotBlank() }

    /** What will be saved: the computed targets with any manual values on top. */
    val finalTargets: MacroTargets?
        get() {
            val base = computed?.targets ?: return null
            return MacroTargets(
                calories = override.calories.int() ?: base.calories,
                proteinG = override.protein.int() ?: base.proteinG,
                carbsG = override.carbs.int() ?: base.carbsG,
                fatG = override.fat.int() ?: base.fatG,
            )
        }

    val canSave: Boolean get() = finalTargets != null
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val bodyRepository: BodyRepository,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /** Body weight already in the log, to avoid logging the same value again. */
    private var loggedWeightKg: Double? = null

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            val plan = nutritionRepository.observeActivePlan().first()
            val weight = bodyRepository.observeWeights().first().lastOrNull()?.weightKg
            loggedWeightKg = weight
            val units = prefs.units
            val profile = prefs.bodyProfile
            _uiState.value = SetupUiState(
                isLoading = false,
                units = units,
                sex = profile?.sex,
                birthDate = profile?.birthDate,
                height = profile?.heightCm?.let { formatNumber(if (units == UnitSystem.METRIC) it else Units.cmToInches(it)) }.orEmpty(),
                weight = weight?.let { formatNumber(units.fromKg(it)) }.orEmpty(),
                activity = profile?.activityLevel ?: ActivityLevel.MODERATE,
                goal = plan?.goal ?: DietGoal.MAINTAIN,
                style = plan?.style ?: DietStyle.BALANCED,
                halalFilter = prefs.halalFilter,
                isEditing = plan != null,
                // Keep earlier manual targets visible as overrides.
                override = if (plan?.isManualOverride == true) {
                    TargetOverride("${plan.targets.calories}", "${plan.targets.proteinG}", "${plan.targets.carbsG}", "${plan.targets.fatG}")
                } else {
                    TargetOverride()
                },
            ).recomputed()
        }
    }

    private fun String.number() = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    private fun SetupUiState.profile(): Pair<BodyProfile, Double>? {
        val sex = sex ?: return null
        val birthDate = birthDate ?: return null
        val heightCm = height.number()?.let { if (units == UnitSystem.METRIC) it else Units.inchesToCm(it) } ?: return null
        val weightKg = weight.number()?.let(units::toKg) ?: return null
        return BodyProfile(sex, birthDate, heightCm, activity) to weightKg
    }

    private fun SetupUiState.recomputed(): SetupUiState {
        val (profile, weightKg) = profile() ?: return copy(computed = null)
        val tdee = computeTdee(profile, weightKg, LocalDate.now())
        return copy(computed = computeTargets(goal, style, tdee, weightKg, profile.sex))
    }

    private fun edit(transform: SetupUiState.() -> SetupUiState) = _uiState.update { it.transform().recomputed() }

    fun setSex(value: Sex) = edit { copy(sex = value) }

    fun setBirthDate(value: LocalDate) = edit { copy(birthDate = value) }

    fun setHeight(value: String) = edit { copy(height = value) }

    fun setWeight(value: String) = edit { copy(weight = value) }

    fun setActivity(value: ActivityLevel) = edit { copy(activity = value) }

    fun setGoal(value: DietGoal) = edit { copy(goal = value) }

    fun setStyle(value: DietStyle) = edit { copy(style = value) }

    fun setHalalFilter(value: HalalFilter) = edit { copy(halalFilter = value) }

    fun setOverride(value: TargetOverride) = edit { copy(override = value) }

    fun clearOverride() = edit { copy(override = TargetOverride()) }

    fun save() {
        val state = _uiState.value
        val (profile, weightKg) = state.profile() ?: return
        val targets = state.finalTargets ?: return
        viewModelScope.launch {
            preferencesRepository.setBodyProfile(profile)
            preferencesRepository.setHalalFilter(state.halalFilter)
            if (loggedWeightKg == null || kotlin.math.abs(loggedWeightKg!! - weightKg) > 0.05) {
                bodyRepository.logWeight(LocalDate.now(), weightKg)
            }
            nutritionRepository.savePlan(
                goal = state.goal,
                style = state.style,
                halalOnly = state.halalFilter != HalalFilter.ALL,
                targets = targets,
                isManualOverride = state.hasOverride && targets != state.computed?.targets,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
