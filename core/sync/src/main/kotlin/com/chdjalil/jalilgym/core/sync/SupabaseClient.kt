package com.chdjalil.jalilgym.core.sync

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** The project the app talks to; blank in builds without cloud sync. */
data class SupabaseConfig(val url: String, val publishableKey: String) {
    val isConfigured: Boolean get() = url.startsWith("https://") && publishableKey.isNotBlank()
}

@Serializable
data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    /** Epoch seconds. */
    val expiresAt: Long,
    val userId: String,
    val email: String,
)

/** An HTTP failure from Supabase, with the status code (0 for no connection). */
class SupabaseException(val status: Int, message: String) : IOException(message)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: UserResponse,
)

@Serializable
private data class UserResponse(val id: String, val email: String? = null)

private val JSON_TYPE = "application/json".toMediaType()

/**
 * Supabase Auth (email one-time codes) and the two sync endpoints, over plain
 * HTTPS with the shared OkHttp client. Only the publishable key ships in the
 * app; Row Level Security keeps each account to its own rows.
 */
class SupabaseClient(
    private val client: OkHttpClient,
    private val config: SupabaseConfig,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun request(path: String, accessToken: String? = null) = Request.Builder()
        .url(config.url + path)
        .header("apikey", config.publishableKey)
        .apply { accessToken?.let { header("Authorization", "Bearer $it") } }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) throw SupabaseException(response.code, body.take(300))
                body
            }
        } catch (e: SupabaseException) {
            throw e
        } catch (e: IOException) {
            throw SupabaseException(0, e.message ?: "No connection")
        }
    }

    private fun JsonObject.body() = toString().toRequestBody(JSON_TYPE)

    /** Sends a 6-digit code to [email], creating the account if it's new. */
    suspend fun sendCode(email: String) {
        execute(
            request("/auth/v1/otp").post(
                buildJsonObject {
                    put("email", email)
                    put("create_user", true)
                }.body(),
            ).build(),
        )
    }

    suspend fun verifyCode(email: String, code: String): SupabaseSession = session(
        execute(
            request("/auth/v1/verify").post(
                buildJsonObject {
                    put("type", "email")
                    put("email", email)
                    put("token", code)
                }.body(),
            ).build(),
        ),
        fallbackEmail = email,
    )

    suspend fun refresh(session: SupabaseSession): SupabaseSession = session(
        execute(
            request("/auth/v1/token?grant_type=refresh_token")
                .post(buildJsonObject { put("refresh_token", session.refreshToken) }.body())
                .build(),
        ),
        fallbackEmail = session.email,
    )

    suspend fun signOut(session: SupabaseSession) {
        execute(request("/auth/v1/logout", session.accessToken).post(ByteArray(0).toRequestBody(null)).build())
    }

    suspend fun deleteAccount(session: SupabaseSession) {
        execute(request("/rest/v1/rpc/delete_my_account", session.accessToken).post(JsonObject(emptyMap()).body()).build())
    }

    suspend fun push(session: SupabaseSession, rows: List<SyncRow>) {
        val payload = buildJsonObject {
            put(
                "rows",
                buildJsonArray {
                    rows.forEach { row ->
                        add(
                            buildJsonObject {
                                put("table_name", row.table)
                                put("row_key", row.key)
                                put("data", row.data ?: JsonNull)
                                put("deleted", row.data == null)
                                put("updated_at", row.updatedAt)
                            },
                        )
                    }
                },
            )
        }
        execute(request("/rest/v1/rpc/push_rows", session.accessToken).post(payload.body()).build())
    }

    suspend fun pull(session: SupabaseSession, afterSeq: Long, limit: Int): List<SyncRow> {
        val url = (config.url + "/rest/v1/sync_rows").toHttpUrl().newBuilder()
            .addQueryParameter("select", "table_name,row_key,data,deleted,updated_at,seq")
            .addQueryParameter("seq", "gt.$afterSeq")
            .addQueryParameter("order", "seq.asc")
            .addQueryParameter("limit", "$limit")
            .build()
        val body = execute(
            Request.Builder().url(url)
                .header("apikey", config.publishableKey)
                .header("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build(),
        )
        return json.parseToJsonElement(body).jsonArray.map { element ->
            val row = element.jsonObject
            val deleted = row["deleted"]?.jsonPrimitive?.boolean == true
            SyncRow(
                table = row.getValue("table_name").jsonPrimitive.content,
                key = row.getValue("row_key").jsonPrimitive.content,
                data = if (deleted) null else row["data"] as? JsonObject,
                updatedAt = row.getValue("updated_at").jsonPrimitive.long,
                seq = row.getValue("seq").jsonPrimitive.long,
            )
        }
    }

    private fun session(body: String, fallbackEmail: String): SupabaseSession {
        val token = json.decodeFromString(TokenResponse.serializer(), body)
        return SupabaseSession(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAt = token.expiresAt ?: (nowSeconds() + token.expiresIn),
            userId = token.user.id,
            email = token.user.email ?: fallbackEmail,
        )
    }
}
