package com.chdjalil.jalilgym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import java.io.File

/**
 * Maps a stored image path to something Coil can load: bundled seed images are
 * paths inside `assets/`, custom photos are absolute app-private file paths.
 */
fun exerciseImageModel(path: String): Any =
    if (path.startsWith("/")) File(path) else "file:///android_asset/$path"

/** Exercise photo with a branded placeholder when there's no image (or it fails to load). */
@Composable
fun ExerciseImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIconSize: Dp = 28.dp,
) {
    val placeholder: @Composable () -> Unit = {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(JgIcons.Train),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(placeholderIconSize),
            )
        }
    }
    if (path == null) {
        Box(modifier) { placeholder() }
    } else {
        SubcomposeAsyncImage(
            model = exerciseImageModel(path),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = { placeholder() },
            error = { placeholder() },
        )
    }
}
