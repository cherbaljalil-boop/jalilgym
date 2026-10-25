package com.chdjalil.jalilgym.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgCreatedByCredit
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.component.JgWordmark
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncStatus
import com.chdjalil.jalilgym.core.domain.repository.HealthAvailability
import com.chdjalil.jalilgym.core.domain.repository.HealthStatus
import com.chdjalil.jalilgym.core.health.HEALTH_CONNECT_STORE_URI
import com.chdjalil.jalilgym.core.health.healthPermissionContract
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.ui.EntryCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun ProfileScreen(
    onOpenAchievements: () -> Unit,
    onOpenLicenses: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val healthStatus by viewModel.healthStatus.collectAsStateWithLifecycle()
    val cloudStatus by viewModel.cloudStatus.collectAsStateWithLifecycle()
    val healthPermissionLauncher = rememberLauncherForActivityResult(healthPermissionContract()) { viewModel.onHealthPermissionsResult() }
    LaunchedEffect(Unit) { viewModel.refreshHealth() }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbar.showSnackbar(
                when (message) {
                    is BackupMessage.Exported -> resources.getString(R.string.feature_profile_backup_exported, message.rows)
                    BackupMessage.ExportFailed -> resources.getString(R.string.feature_profile_backup_export_failed)
                    is BackupMessage.Imported -> resources.getString(R.string.feature_profile_backup_imported, message.rows)
                    BackupMessage.ImportTooNew -> resources.getString(R.string.feature_profile_backup_too_new)
                    BackupMessage.ImportInvalid -> resources.getString(R.string.feature_profile_backup_invalid)
                },
            )
        }
    }
    // The system file picker: the user chooses where the backup goes (Drive, Downloads…).
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) { uri ->
        uri?.let { viewModel.exportTo(it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFrom(it.toString()) }
    }
    // Reminders need notification access on Android 13+; ask when one is first turned on.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val onEvent: (ProfileEvent) -> Unit = { event ->
        viewModel.onEvent(event)
        if (event is ProfileEvent.RemindersChanged &&
            (event.settings.workoutDays.isNotEmpty() || event.settings.weeklySummary) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Box(Modifier.fillMaxSize()) {
        ProfileContent(
            uiState = uiState,
            versionName = versionName,
            onEvent = onEvent,
            onOpenAchievements = onOpenAchievements,
            onOpenLicenses = onOpenLicenses,
            backupBusy = backupBusy,
            onExport = { exportLauncher.launch("jalilgym-backup-${LocalDate.now()}.json") },
            // Some providers label JSON as plain text or binary.
            onImport = { importLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "text/plain", "application/octet-stream")) },
            healthStatus = healthStatus,
            onHealthEnabled = { enabled ->
                viewModel.setHealthEnabled(enabled)
                if (enabled && !healthStatus.granted.containsAll(viewModel.healthPermissions)) {
                    healthPermissionLauncher.launch(viewModel.healthPermissions)
                }
            },
            onGrantHealth = { healthPermissionLauncher.launch(viewModel.healthPermissions) },
            onInstallHealth = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HEALTH_CONNECT_STORE_URI))) }
            },
            onSyncHealth = viewModel::syncHealthNow,
            cloudStatus = cloudStatus,
            cloudActions = CloudSyncActions(
                onSendCode = viewModel::sendCloudCode,
                onVerifyCode = viewModel::verifyCloudCode,
                onCancel = viewModel::cancelCloudSignIn,
                onSyncNow = viewModel::syncCloudNow,
                onSignOut = viewModel::signOutCloud,
                onDeleteAccount = viewModel::deleteCloudAccount,
            ),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private const val BACKUP_MIME_TYPE = "application/json"

@Composable
internal fun ProfileContent(
    uiState: ProfileUiState,
    versionName: String,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAchievements: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    backupBusy: Boolean = false,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    healthStatus: HealthStatus = HealthStatus(),
    onHealthEnabled: (Boolean) -> Unit = {},
    onGrantHealth: () -> Unit = {},
    onInstallHealth: () -> Unit = {},
    onSyncHealth: () -> Unit = {},
    cloudStatus: CloudSyncStatus = CloudSyncStatus(),
    cloudActions: CloudSyncActions = CloudSyncActions(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(title = stringResource(R.string.feature_profile_title))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EntryCard(
                title = stringResource(R.string.feature_profile_achievements),
                description = stringResource(R.string.feature_profile_achievements_desc),
                icon = JgIcons.Progress,
                onClick = onOpenAchievements,
            )
            if (uiState is ProfileUiState.Ready) {
                SettingsSection(uiState.preferences, onEvent)
                RemindersSection(uiState.preferences.reminders) { onEvent(ProfileEvent.RemindersChanged(it)) }
            }
            HealthSection(healthStatus, onHealthEnabled, onGrantHealth, onInstallHealth, onSyncHealth)
            CloudSyncSection(cloudStatus, cloudActions)
            DataSection(backupBusy, onExport, onImport)
            AboutSection(versionName, onOpenLicenses)
        }
    }
}

@Composable
private fun SettingsSection(preferences: UserPreferences, onEvent: (ProfileEvent) -> Unit) {
    SectionTitle(R.string.feature_profile_settings)
    ChoiceSetting(
        label = R.string.feature_profile_theme,
        options = listOf(
            ThemeMode.DARK to R.string.feature_profile_theme_dark,
            ThemeMode.LIGHT to R.string.feature_profile_theme_light,
            ThemeMode.SYSTEM to R.string.feature_profile_theme_system,
        ),
        selected = preferences.themeMode,
        onSelect = { onEvent(ProfileEvent.ThemeSelected(it)) },
    )
    ChoiceSetting(
        label = R.string.feature_profile_units,
        options = listOf(
            UnitSystem.METRIC to R.string.feature_profile_units_metric,
            UnitSystem.IMPERIAL to R.string.feature_profile_units_imperial,
        ),
        selected = preferences.units,
        onSelect = { onEvent(ProfileEvent.UnitsSelected(it)) },
    )
    ChoiceSetting(
        label = R.string.feature_profile_effort,
        options = listOf(
            EffortScale.RPE to R.string.feature_profile_effort_rpe,
            EffortScale.RIR to R.string.feature_profile_effort_rir,
            EffortScale.OFF to R.string.feature_profile_effort_off,
        ),
        selected = preferences.effortScale,
        onSelect = { onEvent(ProfileEvent.EffortScaleSelected(it)) },
    )
}

@Composable
private fun <T> ChoiceSetting(
    @StringRes label: Int,
    options: List<Pair<T, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(text))
                }
            }
        }
    }
}

@Composable
private fun AboutSection(versionName: String, onOpenLicenses: () -> Unit) {
    SectionTitle(R.string.feature_profile_about)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JgWordmark(style = MaterialTheme.typography.displaySmall)
            JgCreatedByCredit()
            if (versionName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.feature_profile_version, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.feature_profile_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.feature_profile_attributions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenLicenses) { Text(stringResource(R.string.feature_profile_licenses)) }
        }
    }
}

private val syncTimeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

@Composable
private fun HealthSection(
    status: HealthStatus,
    onEnabled: (Boolean) -> Unit,
    onGrant: () -> Unit,
    onInstall: () -> Unit,
    onSync: () -> Unit,
) {
    if (status.availability == HealthAvailability.OFF_IN_THIS_BUILD) return
    SectionTitle(R.string.feature_profile_health)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.feature_profile_health_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (status.availability) {
                HealthAvailability.OFF_IN_THIS_BUILD -> Unit
                HealthAvailability.NOT_SUPPORTED -> Text(
                    stringResource(R.string.feature_profile_health_unsupported),
                    style = MaterialTheme.typography.bodyMedium,
                )
                HealthAvailability.NEEDS_INSTALL_OR_UPDATE -> OutlinedButton(onClick = onInstall) {
                    Text(stringResource(R.string.feature_profile_health_install))
                }
                HealthAvailability.AVAILABLE -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = status.enabled, role = Role.Switch, onValueChange = onEnabled),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.feature_profile_health_toggle), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = status.enabled, onCheckedChange = null)
                    }
                    if (status.enabled && !status.granted.containsAll(status.required)) {
                        Text(
                            stringResource(R.string.feature_profile_health_needs_access),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedButton(onClick = onGrant) { Text(stringResource(R.string.feature_profile_health_grant)) }
                    }
                    if (status.ready) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                status.lastSyncedAt?.let {
                                    stringResource(
                                        R.string.feature_profile_health_synced,
                                        syncTimeFormat.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())),
                                    )
                                } ?: stringResource(R.string.feature_profile_health_not_synced),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onSync) { Text(stringResource(R.string.feature_profile_health_sync_now)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataSection(busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    SectionTitle(R.string.feature_profile_data)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.feature_profile_backup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport, enabled = !busy) { Text(stringResource(R.string.feature_profile_backup_export)) }
                OutlinedButton(onClick = { confirmImport = true }, enabled = !busy) {
                    Text(stringResource(R.string.feature_profile_backup_import))
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.feature_profile_backup_import_title)) },
            text = { Text(stringResource(R.string.feature_profile_backup_import_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    onImport()
                }) { Text(stringResource(R.string.feature_profile_backup_choose_file)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text(stringResource(R.string.feature_profile_cancel)) }
            },
        )
    }
}

@Composable
internal fun SectionTitle(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Preview
@Composable
private fun ProfilePreview() {
    JalilgymTheme {
        ProfileContent(ProfileUiState.Ready(UserPreferences()), versionName = "0.1.0", onEvent = {})
    }
}
