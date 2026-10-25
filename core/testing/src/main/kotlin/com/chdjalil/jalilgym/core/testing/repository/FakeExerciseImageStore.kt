package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.ExerciseImageStore

class FakeExerciseImageStore : ExerciseImageStore {
    /** When true, [import] throws like an unreadable picker result. */
    var failImports = false
    val deleted = mutableListOf<String>()

    override suspend fun import(sourceUri: String): String {
        if (failImports) throw java.io.IOException("Cannot open $sourceUri")
        return "/data/exercise_images/${sourceUri.substringAfterLast('/')}.jpg"
    }

    override suspend fun delete(path: String) {
        deleted += path
    }
}
