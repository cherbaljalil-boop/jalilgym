package com.chdjalil.jalilgym.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferencesData> {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override val defaultValue: UserPreferencesData = UserPreferencesData()

    override suspend fun readFrom(input: InputStream): UserPreferencesData = try {
        json.decodeFromString(UserPreferencesData.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read user preferences.", e)
    }

    override suspend fun writeTo(t: UserPreferencesData, output: OutputStream) {
        output.write(json.encodeToString(UserPreferencesData.serializer(), t).encodeToByteArray())
    }
}
