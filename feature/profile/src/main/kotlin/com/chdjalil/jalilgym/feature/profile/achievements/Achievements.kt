package com.chdjalil.jalilgym.feature.profile.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.repository.AchievementRepository
import com.chdjalil.jalilgym.core.model.AchievementCategory
import com.chdjalil.jalilgym.core.model.AchievementStatus
import com.chdjalil.jalilgym.core.ui.achievementText
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.profile.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val byCategory: Map<AchievementCategory, List<AchievementStatus>> = emptyMap(),
    val unlocked: Int = 0,
    val total: Int = 0,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(achievementRepository: AchievementRepository) : ViewModel() {
    val uiState: StateFlow<AchievementsUiState> = achievementRepository.observeAchievements()
        .map { list ->
            AchievementsUiState(
                isLoading = false,
                byCategory = list.groupBy { it.category },
                unlocked = list.count { it.isUnlocked },
                total = list.size,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
internal fun AchievementsScreen(onBack: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AchievementsContent(uiState, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AchievementsContent(uiState: AchievementsUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_profile_achievements)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_profile_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.feature_profile_achievements_count, uiState.unlocked, uiState.total),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.byCategory.forEach { (category, badges) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(category.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics { heading() },
                    )
                }
                items(badges, key = { it.id }) { BadgeCard(it) }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: AchievementStatus) {
    val (title, description) = achievementText(badge.id)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                painterResource(if (badge.isUnlocked) JgIcons.Check else JgIcons.Progress),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (badge.isUnlocked) 1f else 0.5f),
            )
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val unlockedAt = badge.unlockedAt
            if (unlockedAt != null) {
                Text(
                    stringResource(
                        R.string.feature_profile_achievement_unlocked,
                        dateFormat.format(Instant.ofEpochMilli(unlockedAt).atZone(ZoneId.systemDefault())),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                LinearProgressIndicator(progress = { badge.fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
