package com.chdjalil.jalilgym.core.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.chdjalil.jalilgym.core.common.Dispatcher
import com.chdjalil.jalilgym.core.common.JgDispatchers
import com.chdjalil.jalilgym.core.domain.repository.ExerciseImageStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Stores custom exercise photos under `filesDir/exercise_images`, downscaled
 * to at most [MAX_EDGE_PX] so a 12 MP photo doesn't cost megabytes per exercise.
 */
internal class PrivateExerciseImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(JgDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ExerciseImageStore {

    private val directory: File get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    override suspend fun import(sourceUri: String): String = withContext(ioDispatcher) {
        val uri = Uri.parse(sourceUri)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Cannot open $sourceUri")

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE_PX) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("Cannot decode $sourceUri")

        val file = File(directory, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        file.absolutePath
    }

    override suspend fun delete(path: String) {
        withContext(ioDispatcher) {
            val file = File(path)
            // Only ever delete files this store created; bundled assets are relative paths.
            if (file.isAbsolute && file.parentFile == directory) file.delete()
        }
    }

    private companion object {
        const val DIRECTORY = "exercise_images"
        const val MAX_EDGE_PX = 1080
        const val JPEG_QUALITY = 85
    }
}
