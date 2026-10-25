package com.chdjalil.jalilgym.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncError
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Everything the Cloud sync section can ask for. */
data class CloudSyncActions(
    val onSendCode: (String) -> Unit = {},
    val onVerifyCode: (String) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onDeleteAccount: () -> Unit = {},
)

private val cloudTimeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/** Optional account for syncing between phones; only shown in builds with a Supabase project. */
@Composable
internal fun CloudSyncSection(status: CloudSyncStatus, actions: CloudSyncActions) {
    if (!status.available) return
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    SectionTitle(R.string.feature_profile_cloud)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val codeSentTo = status.codeSentTo
            when {
                status.signedIn -> SignedIn(status, actions, onDelete = { confirmDelete = true })
                codeSentTo != null -> EnterCode(codeSentTo, actions)
                else -> EnterEmail(actions)
            }
            status.error?.let { error ->
                Text(
                    stringResource(error.messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_profile_cloud_delete_title)) },
            text = { Text(stringResource(R.string.feature_profile_cloud_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDeleteAccount()
                }) { Text(stringResource(R.string.feature_profile_cloud_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_profile_cancel)) } },
        )
    }
}

private val CloudSyncError.messageRes: Int
    get() = when (this) {
        CloudSyncError.NETWORK -> R.string.feature_profile_cloud_error_network
        CloudSyncError.INVALID_EMAIL -> R.string.feature_profile_cloud_error_email
        CloudSyncError.INVALID_CODE -> R.string.feature_profile_cloud_error_code
        CloudSyncError.RATE_LIMITED -> R.string.feature_profile_cloud_error_rate
        CloudSyncError.SESSION_EXPIRED -> R.string.feature_profile_cloud_error_expired
        CloudSyncError.UNKNOWN -> R.string.feature_profile_cloud_error_unknown
    }

@Composable
private fun EnterEmail(actions: CloudSyncActions) {
    var email by rememberSaveable { mutableStateOf("") }
    Text(
        stringResource(R.string.feature_profile_cloud_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it.trim() },
        label = { Text(stringResource(R.string.feature_profile_cloud_email)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    JgPrimaryButton(onClick = { actions.onSendCode(email) }, enabled = "@" in email, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.feature_profile_cloud_send_code))
    }
}

@Composable
private fun EnterCode(email: String, actions: CloudSyncActions) {
    var code by rememberSaveable { mutableStateOf("") }
    Text(stringResource(R.string.feature_profile_cloud_code_sent, email), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.filter(Char::isDigit).take(CODE_LENGTH) },
        label = { Text(stringResource(R.string.feature_profile_cloud_code)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    JgPrimaryButton(onClick = { actions.onVerifyCode(code) }, enabled = code.length == CODE_LENGTH, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.feature_profile_cloud_sign_in))
    }
    Row {
        TextButton(onClick = { actions.onSendCode(email) }) { Text(stringResource(R.string.feature_profile_cloud_resend)) }
        TextButton(onClick = actions.onCancel) { Text(stringResource(R.string.feature_profile_cloud_other_email)) }
    }
}

@Composable
private fun SignedIn(status: CloudSyncStatus, actions: CloudSyncActions, onDelete: () -> Unit) {
    Text(stringResource(R.string.feature_profile_cloud_signed_in, status.email.orEmpty()), style = MaterialTheme.typography.bodyLarge)
    Text(
        status.lastSyncedAt?.let {
            stringResource(R.string.feature_profile_cloud_synced, cloudTimeFormat.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())))
        } ?: stringResource(R.string.feature_profile_cloud_not_synced),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (status.pendingChanges > 0) {
        Text(
            pluralStringResource(R.plurals.feature_profile_cloud_pending, status.pendingChanges, status.pendingChanges),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (status.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = actions.onSyncNow, enabled = !status.syncing) { Text(stringResource(R.string.feature_profile_cloud_sync_now)) }
        TextButton(onClick = actions.onSignOut) { Text(stringResource(R.string.feature_profile_cloud_sign_out)) }
    }
    TextButton(onClick = onDelete) {
        Text(stringResource(R.string.feature_profile_cloud_delete), color = MaterialTheme.colorScheme.error)
    }
}

private const val CODE_LENGTH = 6
