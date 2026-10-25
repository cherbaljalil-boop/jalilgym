package com.chdjalil.jalilgym.core.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class SupabaseClientTest {

    private val server = MockWebServer()
    private lateinit var client: SupabaseClient
    private val session = SupabaseSession("access", "refresh", expiresAt = 2_000, userId = "u1", email = "a@b.co")

    @Before
    fun setUp() {
        server.start()
        val config = SupabaseConfig(server.url("/").toString().trimEnd('/'), "sb_publishable_test")
        client = SupabaseClient(OkHttpClient(), config, nowSeconds = { 1_000 })
    }

    @After
    fun tearDown() = server.close()

    private fun respond(body: String, code: Int = 200) = server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun `sign-in sends the code request and turns the verify reply into a session`() = runTest {
        respond("{}")
        client.sendCode("a@b.co")
        val otp = server.takeRequest()
        assertThat(otp.url.encodedPath).isEqualTo("/auth/v1/otp")
        assertThat(otp.headers["apikey"]).isEqualTo("sb_publishable_test")
        assertThat(otp.headers["Authorization"]).isNull()
        assertThat(otp.body!!.utf8()).contains("\"create_user\":true")

        respond("""{"access_token":"at","refresh_token":"rt","expires_in":3600,"user":{"id":"u1","email":"a@b.co"},"token_type":"bearer"}""")
        val signedIn = client.verifyCode("a@b.co", "123456")
        assertThat(signedIn).isEqualTo(SupabaseSession("at", "rt", expiresAt = 4_600, userId = "u1", email = "a@b.co"))
        val verify = server.takeRequest()
        assertThat(verify.url.encodedPath).isEqualTo("/auth/v1/verify")
        val body = Json.parseToJsonElement(verify.body!!.utf8()).jsonObject
        assertThat(body["type"]?.jsonPrimitive?.content).isEqualTo("email")
        assertThat(body["token"]?.jsonPrimitive?.content).isEqualTo("123456")
    }

    @Test
    fun `push sends rows to the RPC with the user's token`() = runTest {
        respond("")
        client.push(
            session,
            listOf(
                SyncRow("body_weights", "w1", JsonObject(mapOf("id" to JsonPrimitive("w1"), "weightKg" to JsonPrimitive(80.5))), updatedAt = 10),
                SyncRow("water_logs", "d1", null, updatedAt = 11),
            ),
        )
        val request = server.takeRequest()
        assertThat(request.url.encodedPath).isEqualTo("/rest/v1/rpc/push_rows")
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer access")
        val rows = Json.parseToJsonElement(request.body!!.utf8()).jsonObject.getValue("rows").jsonArray
        assertThat(rows).hasSize(2)
        assertThat(rows[0].jsonObject["data"]!!.jsonObject["weightKg"]!!.jsonPrimitive.content).isEqualTo("80.5")
        assertThat(rows[1].jsonObject["deleted"]!!.jsonPrimitive.content).isEqualTo("true")
    }

    @Test
    fun `pull asks for rows after the cursor and reads deletions`() = runTest {
        respond(
            """[{"table_name":"body_weights","row_key":"w1","data":{"id":"w1"},"deleted":false,"updated_at":10,"seq":41},
               {"table_name":"water_logs","row_key":"d1","data":null,"deleted":true,"updated_at":11,"seq":42}]""",
        )
        val rows = client.pull(session, afterSeq = 40, limit = 1_000)
        assertThat(rows.map { it.seq }).containsExactly(41L, 42L).inOrder()
        assertThat(rows[0].data).isNotNull()
        assertThat(rows[1].data).isNull()
        val request = server.takeRequest()
        assertThat(request.url.queryParameter("seq")).isEqualTo("gt.40")
        assertThat(request.url.queryParameter("order")).isEqualTo("seq.asc")
    }

    @Test
    fun `errors carry the status code`() = runTest {
        respond("""{"msg":"Token has expired or is invalid"}""", code = 403)
        val error = assertThrows(SupabaseException::class.java) { kotlinx.coroutines.runBlocking { client.verifyCode("a@b.co", "000000") } }
        assertThat(error.status).isEqualTo(403)
    }
}
