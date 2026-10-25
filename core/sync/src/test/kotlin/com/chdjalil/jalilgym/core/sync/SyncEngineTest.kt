package com.chdjalil.jalilgym.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.entity.BodyWeightEntity
import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeIngredientEntity
import com.chdjalil.jalilgym.core.database.entity.WaterLogEntity
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The server's behavior from schema.sql: newest change per row wins, every write gets the next seq. */
class FakeServer : SyncRemote {
    val rows = linkedMapOf<Pair<String, String>, SyncRow>()
    private var seq = 0L

    override suspend fun push(rows: List<SyncRow>) {
        rows.forEach { row ->
            val existing = this.rows[row.table to row.key]
            if (existing == null || existing.updatedAt <= row.updatedAt) this.rows[row.table to row.key] = row.copy(seq = ++seq)
        }
    }

    override suspend fun pull(afterSeq: Long, limit: Int): List<SyncRow> =
        rows.values.filter { it.seq > afterSeq }.sortedBy { it.seq }.take(limit)
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SyncEngineTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var phoneA: JalilgymDatabase
    private lateinit var phoneB: JalilgymDatabase
    private val server = FakeServer()
    private val day = LocalDate.of(2026, 10, 24)

    private fun newDatabase() = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()

    @Before
    fun setUp() {
        phoneA = newDatabase()
        phoneB = newDatabase()
    }

    @After
    fun tearDown() {
        phoneA.close()
        phoneB.close()
    }

    private fun food(id: String) = FoodEntity(
        id = id, name = id, brand = null, barcode = null, source = FoodSource.CUSTOM, offCode = null,
        kcalPer100g = 100.0, proteinPer100g = 10.0, carbsPer100g = 10.0, fatPer100g = 2.0,
        fiberPer100g = null, sugarPer100g = null, satFatPer100g = null, sodiumMgPer100g = null,
        halalStatus = HalalStatus.UNKNOWN, lastUsedAt = null, cachedAt = null, createdAt = 1, updatedAt = 1,
    )

    @Test
    fun `rows already on the phone go up on first sign-in and arrive on another phone`() = runTest {
        phoneA.bodyDao().upsertWeight(BodyWeightEntity("w1", day, 80.5, null, updatedAt = 1))
        val a = SyncEngine(phoneA)
        a.enableTracking(markEverything = true)
        assertThat(a.pendingChanges()).isEqualTo(1)

        assertThat(a.sync(server)).isEqualTo(SyncReport(pulled = 0, pushed = 1))
        assertThat(a.pendingChanges()).isEqualTo(0)

        val b = SyncEngine(phoneB)
        b.enableTracking(markEverything = true)
        b.sync(server)
        assertThat(phoneB.bodyDao().observeWeights().first().single().weightKg).isEqualTo(80.5)
        // What B received isn't sent back.
        assertThat(b.pendingChanges()).isEqualTo(0)
    }

    @Test
    fun `edits and deletions travel both ways`() = runTest {
        val a = SyncEngine(phoneA).apply { enableTracking(markEverything = true) }
        val b = SyncEngine(phoneB).apply { enableTracking(markEverything = true) }

        phoneA.waterDao().insert(WaterLogEntity("d1", day, 250, loggedAt = 1, updatedAt = 1))
        phoneA.waterDao().insert(WaterLogEntity("d2", day, 500, loggedAt = 2, updatedAt = 2))
        a.sync(server)
        b.sync(server)
        assertThat(phoneB.waterDao().observeDay(day).first().map { it.ml }).containsExactly(250, 500)

        phoneB.waterDao().deleteLatest(day)
        b.sync(server)
        a.sync(server)
        assertThat(phoneA.waterDao().observeDay(day).first().map { it.id }).containsExactly("d1")
        assertThat(a.pendingChanges()).isEqualTo(0)
    }

    @Test
    fun `the newer of two conflicting edits wins on both phones`() = runTest {
        val a = SyncEngine(phoneA).apply { enableTracking(markEverything = true) }
        val b = SyncEngine(phoneB).apply { enableTracking(markEverything = true) }
        phoneA.bodyDao().upsertWeight(BodyWeightEntity("w1", day, 80.0, null, updatedAt = 1))
        a.sync(server)
        b.sync(server)

        phoneA.bodyDao().upsertWeight(BodyWeightEntity("w1", day, 81.0, null, updatedAt = 2))
        Thread.sleep(5)
        phoneB.bodyDao().upsertWeight(BodyWeightEntity("w1", day, 79.0, null, updatedAt = 3))

        a.sync(server)
        // B's edit is newer than A's upload, so B keeps it and uploads it.
        b.sync(server)
        a.sync(server)
        assertThat(phoneA.bodyDao().observeWeights().first().single().weightKg).isEqualTo(79.0)
        assertThat(phoneB.bodyDao().observeWeights().first().single().weightKg).isEqualTo(79.0)
    }

    @Test
    fun `rows with composite keys and parents arrive together`() = runTest {
        val a = SyncEngine(phoneA).apply { enableTracking(markEverything = true) }
        val b = SyncEngine(phoneB).apply { enableTracking(markEverything = true) }
        phoneA.foodDao().upsertFoods(listOf(food("oats"), food("milk")))
        phoneA.recipeDao().replaceRecipes(
            listOf(RecipeEntity("r1", "Porridge", 1, emptyList(), emptyList(), false, false, emptyList(), 1, 1)),
            listOf(RecipeIngredientEntity("r1", 0, "oats", 60.0), RecipeIngredientEntity("r1", 1, "milk", 200.0)),
        )
        a.sync(server)
        b.sync(server)

        val recipe = phoneB.recipeDao().get("r1")!!
        assertThat(recipe.ingredients.map { it.ingredient.foodId }).containsExactly("oats", "milk")

        // Deleting the recipe on B removes it and its ingredients on A.
        phoneB.openHelper.writableDatabase.execSQL("DELETE FROM recipes WHERE id = 'r1'")
        b.sync(server)
        a.sync(server)
        assertThat(phoneA.recipeDao().get("r1")).isNull()
        assertThat(
            phoneA.openHelper.readableDatabase.query("SELECT COUNT(*) FROM recipe_ingredients").use {
                it.moveToFirst()
                it.getInt(0)
            },
        ).isEqualTo(0)
    }

    @Test
    fun `signing out stops tracking and forgets the pull position`() = runTest {
        val a = SyncEngine(phoneA).apply { enableTracking(markEverything = true) }
        phoneA.waterDao().insert(WaterLogEntity("d1", day, 250, loggedAt = 1, updatedAt = 1))
        a.disableTracking()
        assertThat(a.pendingChanges()).isEqualTo(0)
        phoneA.waterDao().insert(WaterLogEntity("d2", day, 250, loggedAt = 2, updatedAt = 2))

        // Signing in again uploads everything, as on a first sign-in.
        a.enableTracking(markEverything = true)
        assertThat(a.pendingChanges()).isEqualTo(2)
    }
}
