package com.chdjalil.jalilgym.core.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Copies an image from a content/file URI into [directory] as a JPEG, downscaled
 * so its longer edge is at most [maxEdgePx]. Returns the new file.
 */
internal fun importDownscaledJpeg(context: Context, sourceUri: String, directory: File, maxEdgePx: Int, quality: Int = 85): File {
    val uri = Uri.parse(sourceUri)
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IOException("Cannot open $sourceUri")

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdgePx) sample *= 2
    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: throw IOException("Cannot decode $sourceUri")

    directory.mkdirs()
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    bitmap.recycle()
    return file
}
