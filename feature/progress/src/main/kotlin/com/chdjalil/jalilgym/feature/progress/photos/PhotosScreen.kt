package com.chdjalil.jalilgym.feature.progress.photos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.ProgressPhoto
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.progress.R
import com.chdjalil.jalilgym.feature.progress.strength.EmptyMessage
import com.chdjalil.jalilgym.feature.progress.ui.BackBar
import com.chdjalil.jalilgym.feature.progress.ui.formatDate
import com.chdjalil.jalilgym.feature.progress.ui.formatWeight
import java.io.File

/** Photos are portrait; the grid and compare view use the same frame. */
private const val PHOTO_ASPECT = 3f / 4f

@Composable
internal fun PhotosScreen(
    onBack: () -> Unit,
    onCompare: (before: String, after: String) -> Unit,
    viewModel: PhotosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingUri = uri.toString()
    }

    Scaffold(
        topBar = {
            BackBar(stringResource(R.string.feature_progress_photos_title), onBack) {
                if (uiState.selected.isNotEmpty()) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(painterResource(JgIcons.Delete), stringResource(R.string.feature_progress_delete))
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.selected.size == 2) {
                val label = stringResource(R.string.feature_progress_compare)
                ExtendedFloatingActionButton(
                    onClick = { viewModel.comparePair()?.let { (before, after) -> onCompare(before, after) } },
                    modifier = Modifier.semantics { contentDescription = label },
                    text = { Text(label) },
                    icon = { Icon(painterResource(JgIcons.Check), null) },
                )
            } else {
                val label = stringResource(R.string.feature_progress_add_photo)
                ExtendedFloatingActionButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.semantics { contentDescription = label },
                    text = { Text(label) },
                    icon = { Icon(painterResource(JgIcons.AddPhoto), null) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.pose == null,
                    onClick = { viewModel.selectPose(null) },
                    label = { Text(stringResource(R.string.feature_progress_all_poses)) },
                )
                PhotoPose.entries.forEach { pose ->
                    FilterChip(
                        selected = uiState.pose == pose,
                        onClick = { viewModel.selectPose(pose) },
                        label = { Text(stringResource(pose.labelRes)) },
                    )
                }
            }
            Text(
                stringResource(R.string.feature_progress_photos_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                uiState.photos.isEmpty() -> EmptyMessage(stringResource(R.string.feature_progress_photos_empty))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.photos, key = { it.id }) { photo ->
                        PhotoTile(photo, selected = photo.id in uiState.selected, onClick = { viewModel.toggle(photo.id) })
                    }
                }
            }
        }
    }

    pendingUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            title = { Text(stringResource(R.string.feature_progress_choose_pose)) },
            text = {
                Column {
                    PhotoPose.entries.forEach { pose ->
                        TextButton(
                            onClick = {
                                pendingUri = null
                                viewModel.addPhoto(uri, pose)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(pose.labelRes)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pendingUri = null }) { Text(stringResource(R.string.feature_progress_cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_progress_delete_photos_title)) },
            text = { Text(stringResource(R.string.feature_progress_delete_photos_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteSelected()
                }) { Text(stringResource(R.string.feature_progress_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_progress_cancel)) } },
        )
    }
    if (uiState.importFailed) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            text = { Text(stringResource(R.string.feature_progress_photo_failed)) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.feature_progress_ok)) } },
        )
    }
}

@Composable
private fun PhotoTile(photo: ProgressPhoto, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column {
        AsyncImage(
            model = File(photo.filePath),
            contentDescription = stringResource(R.string.feature_progress_photo_desc, stringResource(photo.pose.labelRes), formatDate(photo.date)),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PHOTO_ASPECT)
                .clip(shape)
                .then(if (selected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), shape) else Modifier)
                // Announced as a checkbox so TalkBack says whether it's picked for comparison.
                .selectable(selected = selected, role = Role.Checkbox, onClick = onClick),
        )
        Text(formatDate(photo.date), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun PhotoCompareScreen(onBack: () -> Unit, viewModel: PhotoCompareViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var split by rememberSaveable { mutableFloatStateOf(0.5f) }
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_progress_compare), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val before = uiState.before
        val after = uiState.after
        if (before == null || after == null) {
            EmptyMessage(stringResource(R.string.feature_progress_photo_missing), Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(PHOTO_ASPECT)
                    .clip(RoundedCornerShape(12.dp))
                    .clipToBounds(),
            ) {
                AsyncImage(
                    model = File(before.filePath),
                    contentDescription = stringResource(R.string.feature_progress_before),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // The newer photo covers everything right of the divider.
                AsyncImage(
                    model = File(after.filePath),
                    contentDescription = stringResource(R.string.feature_progress_after),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipRect(left = size.width * split) { this@drawWithContent.drawContent() }
                        },
                )
            }
            Slider(value = split, onValueChange = { split = it })
            Row(Modifier.fillMaxWidth()) {
                PhotoCaption(stringResource(R.string.feature_progress_before), before, uiState, Modifier.weight(1f))
                PhotoCaption(stringResource(R.string.feature_progress_after), after, uiState, Modifier.weight(1f), Alignment.End)
            }
        }
    }
}

@Composable
private fun PhotoCaption(
    label: String,
    photo: ProgressPhoto,
    uiState: CompareUiState,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(formatDate(photo.date), style = MaterialTheme.typography.bodySmall)
        photo.weightKgAtTime?.let {
            Text(uiState.units.formatWeight(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
