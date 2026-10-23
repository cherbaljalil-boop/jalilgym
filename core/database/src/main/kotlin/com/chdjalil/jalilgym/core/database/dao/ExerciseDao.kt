package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseImageEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseMuscleEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Transaction
    @Query("SELECT * FROM exercises WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PopulatedExercise>>

    /** Also returns archived/deleted rows so history screens can still show them. */
    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<PopulatedExercise?>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): PopulatedExercise?

    @Query("SELECT COUNT(*) FROM exercises WHERE deletedAt IS NULL AND isArchived = 0")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercise_muscles WHERE exerciseId IN (:exerciseIds)")
    suspend fun deleteMusclesFor(exerciseIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Query("DELETE FROM exercise_images WHERE exerciseId IN (:exerciseIds)")
    suspend fun deleteImagesFor(exerciseIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ExerciseImageEntity>)

    /** Writes an exercise with its muscles and images, replacing any previous ones. */
    @Transaction
    suspend fun replaceExercise(
        exercise: ExerciseEntity,
        muscles: List<ExerciseMuscleEntity>,
        images: List<ExerciseImageEntity>,
    ) {
        upsertExercises(listOf(exercise))
        deleteMusclesFor(listOf(exercise.id))
        insertMuscles(muscles)
        deleteImagesFor(listOf(exercise.id))
        insertImages(images)
    }

    @Query("UPDATE exercises SET deletedAt = :now, updatedAt = :now WHERE id = :id AND isBuiltIn = 0")
    suspend fun softDeleteCustom(id: String, now: Long)
}
