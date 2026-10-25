package com.chdjalil.jalilgym.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.entity.WaterLogEntity
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncError
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class MemorySessionStore : SessionStore {
    override var session: SupabaseSession? = null
    override var codeSentTo: String? = null
    override var lastSyncedAt: Long? = null
    override var settingsJson: String? = null
    override var settingsSyncedAt: Long? = null
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SupabaseCloudSyncTest {

    private val server = MockWebServer()
    private lateinit var database: JalilgymDatabase
    private lateinit var cloud: SupabaseCloudSync
    private val store = MemorySessionStore()
    private val preferences = FakeUserPreferencesRepository()
    private val farFuture = System.currentTimeMillis() / 1_000 + 3_600

    @Before
    fun setUp() {
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()
        val config = SupabaseConfig("http://127.0.0.1:${server.port}", "sb_publishable_test")
        cloud = SupabaseCloudSync(config, SupabaseClient(OkHttpClient(), config), SyncEngine(database), store, preferences)
    }

    @After
    fun tearDown() {
        server.close()
        database.close()
    }

    private fun respond(body: String, code: Int = 200) = server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun `signing in uploads what is already on the phone`() = runTest {
        database.waterDao().insert(WaterLogEntity("d1", LocalDate.of(2026, 10, 24), 250, loggedAt = 1, updatedAt = 1))

        assertThat(cloud.sendCode("not an email")).isFalse()
        assertThat(cloud.observeStatus().first().error).isEqualTo(CloudSyncError.INVALID_EMAIL)

        respond("{}")
        assertThat(cloud.sendCode(" a@b.co ")).isTrue()
        assertThat(cloud.observeStatus().first().codeSentTo).isEqualTo("a@b.co")

        respond("""{"access_token":"at","refresh_token":"rt","expires_at":$farFuture,"user":{"id":"u1","email":"a@b.co"}}""")
        respond("[]") // pull
        respond("") // push rows
        respond("") // push settings
        assertThat(cloud.verifyCode("123 456")).isTrue()

        val status = cloud.observeStatus().first()
        assertThat(status.email).isEqualTo("a@b.co")
        assertThat(status.pendingChanges).isEqualTo(0)
        assertThat(status.lastSyncedAt).isNotNull()
        server.takeRequest() // otp
        assertThat(server.takeRequest().body!!.utf8()).contains("\"token\":\"123456\"")
        assertThat(server.takeRequest().url.encodedPath).isEqualTo("/rest/v1/sync_rows")
        val push = server.takeRequest()
        assertThat(push.url.encodedPath).isEqualTo("/rest/v1/rpc/push_rows")
        assertThat(push.body!!.utf8()).contains("\"row_key\":\"d1\"")
        val settings = server.takeRequest().body!!.utf8()
        assertThat(settings).contains("\"table_name\":\"_settings\"")
        assertThat(store.settingsJson).isNotNull()
    }

    @Test
    fun `a wrong code is reported and an expired refresh signs out`() = runTest {
        respond("{}")
        cloud.sendCode("a@b.co")
        respond("""{"msg":"Token has expired or is invalid"}""", code = 403)
        assertThat(cloud.verifyCode("000000")).isFalse()
        assertThat(cloud.observeStatus().first().error).isEqualTo(CloudSyncError.INVALID_CODE)

        store.session = SupabaseSession("old", "rt", expiresAt = 0, userId = "u1", email = "a@b.co")
        cloud.initialize()
        respond("""{"msg":"Invalid Refresh Token"}""", code = 400)
        assertThat(cloud.sync()).isFalse()
        val status = cloud.observeStatus().first()
        assertThat(status.signedIn).isFalse()
        assertThat(status.error).isEqualTo(CloudSyncError.SESSION_EXPIRED)
        assertThat(store.session).isNull()
    }

    @Test
    fun `offline sync keeps changes for later`() = runTest {
        store.session = SupabaseSession("at", "rt", expiresAt = farFuture, userId = "u1", email = "a@b.co")
        cloud.initialize()
        database.waterDao().insert(WaterLogEntity("d1", LocalDate.of(2026, 10, 24), 250, loggedAt = 1, updatedAt = 1))
        server.close()
        assertThat(cloud.sync()).isFalse()
        val status = cloud.observeStatus().first()
        assertThat(status.error).isEqualTo(CloudSyncError.NETWORK)
        assertThat(status.pendingChanges).isEqualTo(1)
        assertThat(status.signedIn).isTrue()
    }

    @Test
    fun `a new phone takes the account's settings`() = runTest {
        store.session = SupabaseSession("at", "rt", expiresAt = farFuture, userId = "u1", email = "a@b.co")
        cloud.initialize()
        respond(
            """[{"table_name":"_settings","row_key":"settings","deleted":false,"updated_at":5,"seq":7,
               "data":{"onboardingComplete":true,"units":"IMPERIAL","waterGoalMl":3000,"reminderDays":[1,3],"reminderTime":"07:30",
                       "sex":"FEMALE","birthDate":"1995-04-02","heightCm":168.0,"activityLevel":"MODERATE"}}]""",
        )
        assertThat(cloud.sync()).isTrue()

        val prefs = preferences.current
        assertThat(prefs.units).isEqualTo(UnitSystem.IMPERIAL)
        assertThat(prefs.waterGoalMl).isEqualTo(3000)
        assertThat(prefs.onboardingComplete).isTrue()
        assertThat(prefs.bodyProfile!!.heightCm).isEqualTo(168.0)
        assertThat(prefs.reminders.workoutDays).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        assertThat(prefs.reminders.workoutTime).isEqualTo(LocalTime.of(7, 30))
        // Nothing to upload: this phone now matches the account.
        assertThat(store.settingsSyncedAt).isEqualTo(5)
        server.takeRequest()
        assertThat(server.requestCount).isEqualTo(1)
    }
}
