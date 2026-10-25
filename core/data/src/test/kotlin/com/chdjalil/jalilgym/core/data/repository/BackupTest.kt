package com.chdjalil.jalilgym.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.data.backup.BackupSerializer
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.entity.ProgressPhotoEntity
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.chdjalil.jalilgym.core.datastore.JgPreferencesDataSource
import com.chdjalil.jalilgym.core.datastore.UserPreferencesSerializer
import com.chdjalil.jalilgym.core.domain.repository.ImportResult
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BackupTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databases = mutableListOf<JalilgymDatabase>()
    private val today = LocalDate.now()

    @After
    fun tearDown() = databases.forEach { it.close() }

    private fun newDatabase() =
        Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build().also { databases += it }

    private fun TestScope.newPreferences(name: String) = JgPreferencesDataSource(
        DataStoreFactory.create(UserPreferencesSerializer, scope = backgroundScope) { File(tmpFolder.root, "$name.json") },
    )

    private fun nutrition(db: JalilgymDatabase) =
        OfflineFirstNutritionRepository(db, db.foodDao(), db.foodLogDao(), db.savedMealDao(), db.dietPlanDao())

    private fun body(db: JalilgymDatabase) = OfflineFirstBodyRepository(context, db.bodyDao(), kotlinx.coroutines.Dispatchers.Unconfined)

    /** A device with a little of everything. */
    private suspend fun TestScope.populatedDevice(): Pair<JalilgymDatabase, JgPreferencesDataSource> {
        val db = newDatabase()
        val prefs = newPreferences("source")
        val importer = SeedImporter(context, db)
        importer.importExercises(importer.readExerciseSeed())
        importer.importFoods(importer.readFoodSeed())
        prefs.setUnitSystem(UnitSystem.IMPERIAL)
        nutrition(db).savePlan(DietGoal.CUT, DietStyle.HIGH_PROTEIN, true, MacroTargets(2200, 190, 200, 70), isManualOverride = false)
        nutrition(db).logFood(today, MealType.BREAKFAST, "seed-oats", 80.0)
        nutrition(db).saveMealFromDiary("Oats", today, MealType.BREAKFAST)
        body(db).logWeight(today, 80.5)
        return db to prefs
    }

    @Test
    fun exportThenImportRestoresEverythingButKeepsPhotos() = runTest {
        val (source, sourcePrefs) = populatedDevice()
        val file = ByteArrayOutputStream()
        val exported = BackupSerializer(source, sourcePrefs).write(file, now = 1234L)
        assertThat(exported).isGreaterThan(100)

        val target = newDatabase()
        val targetPrefs = newPreferences("target")
        target.bodyDao().upsertPhoto(ProgressPhotoEntity("photo", today, PhotoPose.FRONT, "/x.jpg", null, updatedAt = 1))
        // Data already on the target is replaced, not merged.
        body(target).logWeight(today.minusDays(3), 99.0)

        val result = BackupSerializer(target, targetPrefs).read(file.toByteArray().inputStream())

        assertThat(result).isEqualTo(ImportResult.Success(exported, 1234L))
        assertThat(nutrition(target).observeDiary(today).first().entries.single().name).isEqualTo("Rolled oats")
        assertThat(nutrition(target).observeSavedMeals().first().single().items).hasSize(1)
        assertThat(nutrition(target).observeActivePlan().first()!!.targets.proteinG).isEqualTo(190)
        assertThat(body(target).observeWeights().first().map { it.weightKg }).containsExactly(80.5)
        assertThat(target.exerciseDao().observeAll().first()).isNotEmpty()
        assertThat(targetPrefs.userPreferences.first().units).isEqualTo(UnitSystem.IMPERIAL)
        assertThat(target.bodyDao().observePhotos().first().map { it.id }).containsExactly("photo")
    }

    @Test
    fun rejectsNewerAndForeignFiles() = runTest {
        val (source, prefs) = populatedDevice()
        val file = ByteArrayOutputStream().also { BackupSerializer(source, prefs).write(it) }.toString()
        val serializer = BackupSerializer(newDatabase(), newPreferences("t"))

        assertThat(serializer.read(file.replace(Regex("\"schemaVersion\":\\d+"), "\"schemaVersion\":99").byteInputStream()))
            .isEqualTo(ImportResult.TooNew)
        assertThat(serializer.read(file.replace(BACKUP_FORMAT_FOR_TEST, "something-else").byteInputStream()))
            .isEqualTo(ImportResult.Invalid)
        assertThat(serializer.read("not json at all".byteInputStream())).isEqualTo(ImportResult.Invalid)
        assertThat(serializer.read("{}".byteInputStream())).isEqualTo(ImportResult.Invalid)
    }

    @Test
    fun aBrokenRowRollsTheWholeImportBack() = runTest {
        val (source, prefs) = populatedDevice()
        val file = ByteArrayOutputStream().also { BackupSerializer(source, prefs).write(it) }.toString()
        // A diary entry pointing at a food that doesn't exist breaks a foreign key.
        val broken = file.replace("\"foodId\":\"seed-oats\"", "\"foodId\":\"missing-food\"")
        assertThat(broken).isNotEqualTo(file)

        val target = newDatabase()
        body(target).logWeight(today, 70.0)
        val result = BackupSerializer(target, newPreferences("t")).read(broken.byteInputStream())

        assertThat(result).isEqualTo(ImportResult.Invalid)
        assertThat(body(target).observeWeights().first().map { it.weightKg }).containsExactly(70.0)
    }

    private companion object {
        const val BACKUP_FORMAT_FOR_TEST = "jalilgym-backup"
    }
}
