package com.chdjalil.jalilgym.core.domain.repository

/** Keeps user-picked exercise photos in app-private storage. */
interface ExerciseImageStore {
    /** Copies the image behind [sourceUri] (e.g. from the photo picker) and returns its private file path. */
    suspend fun import(sourceUri: String): String

    suspend fun delete(path: String)
}
