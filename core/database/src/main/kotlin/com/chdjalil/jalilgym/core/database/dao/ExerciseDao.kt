package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseMuscleEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Transaction
    @Query("SELECT * FROM exercises WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PopulatedExercise>>

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
}
