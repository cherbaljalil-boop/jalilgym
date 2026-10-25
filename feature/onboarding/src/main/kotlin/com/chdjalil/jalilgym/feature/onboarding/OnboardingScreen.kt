package com.chdjalil.jalilgym.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgCreatedByCredit
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.component.JgWordmark
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.ui.labelRes

private const val QUESTION_COUNT = 3

@Composable
internal fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.finished) { if (uiState.finished) onFinished() }
    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME) { viewModel.onEvent(OnboardingEvent.Back) }
    OnboardingContent(uiState, viewModel::onEvent)
}

@Composable
internal fun OnboardingContent(uiState: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (uiState.step != OnboardingStep.WELCOME) {
            TopRow(uiState, onEvent)
        }
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "onboarding-step",
        ) { step ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> Welcome()
                    OnboardingStep.GOAL -> Question(R.string.feature_onboarding_goal_title, R.string.feature_onboarding_goal_body) {
                        Goal.entries.forEach { goal ->
                            OptionCard(
                                title = stringResource(goal.labelRes),
                                description = stringResource(goal.descriptionRes),
                                selected = uiState.goal == goal,
                                onClick = { onEvent(OnboardingEvent.GoalSelected(goal)) },
                            )
                        }
                    }
                    OnboardingStep.EXPERIENCE -> Question(
                        R.string.feature_onboarding_experience_title,
                        R.string.feature_onboarding_experience_body,
                    ) {
                        ExperienceLevel.entries.forEach { level ->
                            OptionCard(
                                title = stringResource(level.labelRes),
                                description = stringResource(level.descriptionRes),
                                selected = uiState.experience == level,
                                onClick = { onEvent(OnboardingEvent.ExperienceSelected(level)) },
                            )
                        }
                    }
                    OnboardingStep.PROGRAM -> Question(
                        R.string.feature_onboarding_program_title,
                        R.string.feature_onboarding_program_body,
                    ) {
                        // Suggestion first, then the alternatives.
                        val suggested = uiState.suggestedProgram
                        (listOf(suggested) + StarterProgram.entries.filter { it != suggested }).forEach { program ->
                            OptionCard(
                                title = stringResource(program.labelRes),
                                description = stringResource(program.descriptionRes),
                                selected = uiState.program == program,
                                badge = if (program == suggested) stringResource(R.string.feature_onboarding_suggested) else null,
                                onClick = { onEvent(OnboardingEvent.ProgramSelected(program)) },
                            )
                        }
                    }
                }
            }
        }
        BottomActions(uiState, onEvent)
    }
}

@Composable
private fun TopRow(uiState: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onEvent(OnboardingEvent.Back) }) {
            Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_onboarding_back))
        }
        val progressLabel = stringResource(
            R.string.feature_onboarding_progress,
            uiState.questionNumber,
            QUESTION_COUNT,
        )
        LinearProgressIndicator(
            progress = { uiState.questionNumber / QUESTION_COUNT.toFloat() },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { this.contentDescription = progressLabel },
        )
        TextButton(onClick = { onEvent(OnboardingEvent.Skip) }, enabled = !uiState.isSaving) {
            Text(stringResource(R.string.feature_onboarding_skip))
        }
    }
}

@Composable
private fun Welcome() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        JgWordmark(style = MaterialTheme.typography.displayLarge)
        Text(
            stringResource(R.string.feature_onboarding_tagline),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.feature_onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        JgCreatedByCredit()
    }
}

@Composable
private fun Question(title: Int, body: Int, options: @Composable () -> Unit) {
    Text(
        stringResource(title).uppercase(),
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier
            .padding(top = 16.dp)
            .semantics { heading() },
    )
    Text(
        stringResource(body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) { options() }
}

@Composable
private fun OptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surfaceContainer,
            contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        ),
        border = if (selected) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (badge != null) {
                    Text(badge.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            if (selected) {
                Box(Modifier.padding(start = 12.dp)) {
                    Icon(painterResource(JgIcons.Check), contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomActions(uiState: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        JgPrimaryButton(
            onClick = { onEvent(OnboardingEvent.Continue) },
            enabled = uiState.canContinue && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when (uiState.step) {
                    OnboardingStep.WELCOME -> stringResource(R.string.feature_onboarding_get_started)
                    OnboardingStep.PROGRAM -> stringResource(R.string.feature_onboarding_finish)
                    else -> stringResource(R.string.feature_onboarding_continue)
                },
            )
        }
        if (uiState.step == OnboardingStep.WELCOME) {
            TextButton(
                onClick = { onEvent(OnboardingEvent.Skip) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(stringResource(R.string.feature_onboarding_skip_setup)) }
        }
    }
}

private val Goal.descriptionRes: Int
    get() = when (this) {
        Goal.BUILD_MUSCLE -> R.string.feature_onboarding_goal_build_muscle_desc
        Goal.LOSE_FAT -> R.string.feature_onboarding_goal_lose_fat_desc
        Goal.GET_STRONGER -> R.string.feature_onboarding_goal_get_stronger_desc
        Goal.GENERAL_FITNESS -> R.string.feature_onboarding_goal_general_fitness_desc
        Goal.ENDURANCE -> R.string.feature_onboarding_goal_endurance_desc
    }

private val ExperienceLevel.descriptionRes: Int
    get() = when (this) {
        ExperienceLevel.BEGINNER -> R.string.feature_onboarding_level_beginner_desc
        ExperienceLevel.INTERMEDIATE -> R.string.feature_onboarding_level_intermediate_desc
        ExperienceLevel.ADVANCED -> R.string.feature_onboarding_level_advanced_desc
    }

private val StarterProgram.descriptionRes: Int
    get() = when (this) {
        StarterProgram.FULL_BODY_3 -> R.string.feature_onboarding_program_full_body_desc
        StarterProgram.PPL_3 -> R.string.feature_onboarding_program_ppl_3_desc
        StarterProgram.PPL_6 -> R.string.feature_onboarding_program_ppl_6_desc
        StarterProgram.CUSTOM -> R.string.feature_onboarding_program_custom_desc
    }

@Preview
@Composable
private fun OnboardingPreview() {
    JalilgymTheme { OnboardingContent(OnboardingUiState(step = OnboardingStep.GOAL), onEvent = {}) }
}
