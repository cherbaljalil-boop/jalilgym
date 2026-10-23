package com.chdjalil.jalilgym.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.MediaType
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.MuscleRole
import com.chdjalil.jalilgym.core.model.PrType
import com.chdjalil.jalilgym.core.model.ProgramType
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.model.SessionStatus
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.TrackingType

// Conventions (see docs/BLUEPRINT.md §6): string UUID ids (stable slugs for
// built-in seed rows), weights in kg, timestamps in epoch millis (UTC), and
// createdAt/updatedAt/deletedAt on user-editable rows so sync can be added later.

@Entity(tableName = "exercises", indices = [Index("name")])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aliases: List<String>,
    val instructions: List<String>,
    val equipment: Equipment,
    val mechanics: Mechanics,
    val force: Force,
    val difficulty: Difficulty,
    val trackingType: TrackingType,
    val defaultIncrementKg: Double,
    val isBuiltIn: Boolean,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exerciseId", "muscle"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("muscle")],
)
data class ExerciseMuscleEntity(
    val exerciseId: String,
    val muscle: Muscle,
    val role: MuscleRole,
)

@Entity(
    tableName = "exercise_images",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class ExerciseImageEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    /** `asset://…` for bundled media, or an app-internal file path for custom exercises. */
    val uri: String,
    val mediaType: MediaType,
    val position: Int,
)

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: ProgramType,
    val daysPerWeek: Int,
    val goalTag: String?,
    val isTemplate: Boolean,
    val sourceTemplateId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "program_days",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programId")],
)
data class ProgramDayEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val dayIndex: Int,
    val name: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "program_exercises",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("programDayId"), Index("exerciseId")],
)
data class ProgramExerciseEntity(
    @PrimaryKey val id: String,
    val programDayId: String,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRpe: Double?,
    val restSeconds: Int,
    val supersetGroup: String?,
    val progressionRule: ProgressionRule,
    val incrementKg: Double,
    val deloadAfterFailures: Int = 2,
    val deloadPercent: Double = 0.10,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "progression_states",
    foreignKeys = [
        ForeignKey(
            entity = ProgramExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["programExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProgressionStateEntity(
    @PrimaryKey val programExerciseId: String,
    val nextWeightKg: Double,
    val nextTargetReps: Int,
    val consecutiveFailures: Int,
    val updatedAt: Long,
)

/** Single-row table: which program is active and where the rotation is. */
@Entity(
    tableName = "active_program",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programId")],
)
data class ActiveProgramEntity(
    @PrimaryKey val singleton: Int = 0,
    val programId: String,
    val nextDayIndex: Int,
    val startedAt: Long,
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("programDayId"), Index("startedAt"), Index("status")],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val programDayId: String?,
    val name: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: SessionStatus,
    val notes: String?,
    /** Denormalized for fast history lists; recomputed when the session finishes. */
    val totalVolumeKg: Double,
    val healthConnectId: String?,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ProgramExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["programExerciseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index("programExerciseId")],
)
data class SessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val position: Int,
    val supersetGroup: String?,
    val programExerciseId: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionExerciseId")],
)
data class SetLogEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val position: Int,
    val setType: SetType,
    val weightKg: Double,
    val reps: Int,
    val durationSec: Int?,
    val rpe: Double?,
    val rir: Int?,
    val completed: Boolean,
    val completedAt: Long?,
    val updatedAt: Long,
)

@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SetLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["setLogId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId", "prType"), Index("setLogId")],
)
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val setLogId: String,
    val prType: PrType,
    val value: Double,
    val achievedAt: Long,
)
