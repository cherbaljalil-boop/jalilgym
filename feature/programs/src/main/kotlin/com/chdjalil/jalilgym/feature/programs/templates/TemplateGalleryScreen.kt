package com.chdjalil.jalilgym.feature.programs.templates

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.domain.program.ProgramTemplates
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.feature.programs.R
import com.chdjalil.jalilgym.feature.programs.ui.BackTopBar

@Composable
internal fun TemplateGalleryScreen(
    onBack: () -> Unit,
    onProgramCreated: (String) -> Unit,
    viewModel: TemplateGalleryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val created by viewModel.createdProgramId.collectAsStateWithLifecycle()
    LaunchedEffect(created) { created?.let(onProgramCreated) }
    TemplateGalleryContent(uiState, onBack, onUse = viewModel::useTemplate, busy = created != null)
}

@Composable
internal fun TemplateGalleryContent(
    uiState: TemplateGalleryUiState,
    onBack: () -> Unit,
    onUse: (String) -> Unit,
    busy: Boolean = false,
) {
    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.feature_programs_templates_title), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.feature_programs_templates_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(uiState.templates, key = { it.id }) { template ->
                TemplateCard(template, enabled = !busy, onUse = { onUse(template.id) })
            }
        }
    }
}

@Composable
private fun TemplateCard(template: Program, enabled: Boolean, onUse: () -> Unit) {
    var expanded by rememberSaveable(template.id) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(template.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                pluralStringResource(R.plurals.feature_programs_days_per_week, template.daysPerWeek, template.daysPerWeek),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            templateDescription(template.id)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                template.days.forEach { day ->
                    Column(Modifier.padding(top = 4.dp)) {
                        Text(day.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            day.exercises.joinToString(", ") { it.exercise.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    stringResource(
                        if (expanded) R.string.feature_programs_hide_workouts else R.string.feature_programs_show_workouts,
                    ),
                )
            }
            JgPrimaryButton(onClick = onUse, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_programs_use_template))
            }
        }
    }
}

@Composable
private fun templateDescription(id: String): String? = when (id) {
    ProgramTemplates.FULL_BODY_3 -> stringResource(R.string.feature_programs_template_full_body_desc)
    ProgramTemplates.PPL_3 -> stringResource(R.string.feature_programs_template_ppl_3_desc)
    ProgramTemplates.PPL_6 -> stringResource(R.string.feature_programs_template_ppl_6_desc)
    ProgramTemplates.UPPER_LOWER_4 -> stringResource(R.string.feature_programs_template_upper_lower_desc)
    ProgramTemplates.STRENGTH_3 -> stringResource(R.string.feature_programs_template_strength_desc)
    ProgramTemplates.HOME_DUMBBELL_3 -> stringResource(R.string.feature_programs_template_home_desc)
    ProgramTemplates.BODYWEIGHT_3 -> stringResource(R.string.feature_programs_template_bodyweight_desc)
    else -> null
}
