package com.chdjalil.jalilgym.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Every schema version must migrate to the next without losing data
 * (docs/BLUEPRINT.md §9). Add a test here with each new version.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), JalilgymDatabase::class.java)

    private val name = "migration-test"

    @Test
    fun migrate1To2KeepsDiaryEntriesAndAddsSaturatedFat() {
        helper.createDatabase(name, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO food_log_entries
                (id, date, meal, foodId, recipeId, quantityG, nameSnapshot, kcal, proteinG, carbsG, fatG,
                 fiberG, sugarG, sodiumMg, loggedAt, updatedAt, deletedAt)
                VALUES ('e1', '2026-10-01', 'LUNCH', NULL, NULL, 150.0, 'Rice bowl', 520.0, 30.0, 70.0, 12.0,
                 4.0, 3.0, 800.0, 1, 1, NULL)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(name, 2, true).use { db ->
            db.query("SELECT nameSnapshot, kcal, satFatG FROM food_log_entries WHERE id = 'e1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("Rice bowl")
                assertThat(cursor.getDouble(1)).isEqualTo(520.0)
                assertThat(cursor.isNull(2)).isTrue()
            }
        }
    }

    @Test
    fun migrate2To3KeepsDataAndAddsTheWaterLog() {
        helper.createDatabase(name, 2).use { db ->
            db.execSQL("INSERT INTO body_weights (id, date, weightKg, healthConnectId, updatedAt, deletedAt) VALUES ('w1', '2026-10-01', 80.5, NULL, 1, NULL)")
        }

        helper.runMigrationsAndValidate(name, 3, true).use { db ->
            db.query("SELECT weightKg FROM body_weights WHERE id = 'w1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getDouble(0)).isEqualTo(80.5)
            }
            db.execSQL("INSERT INTO water_logs (id, date, ml, loggedAt, updatedAt, deletedAt) VALUES ('d1', '2026-10-01', 250, 1, 1, NULL)")
            db.query("SELECT SUM(ml) FROM water_logs").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(250)
            }
        }
    }

    @Test
    fun migratedDatabaseOpensWithTheCurrentSchema() = runTest {
        helper.createDatabase(name, 1).close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Room validates the schema on open and fails if a migration is missing.
        val database = Room.databaseBuilder(context, JalilgymDatabase::class.java, name).allowMainThreadQueries().build()
        try {
            assertThat(database.foodLogDao().observeDay(LocalDate.of(2026, 10, 1)).first()).isEmpty()
            assertThat(database.waterDao().observeDay(LocalDate.of(2026, 10, 1)).first()).isEmpty()
            assertThat(database.openHelper.readableDatabase.version).isEqualTo(3)
        } finally {
            database.close()
        }
    }
}
