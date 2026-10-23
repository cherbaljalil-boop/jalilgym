package com.chdjalil.jalilgym.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Enums use Room's built-in name mapping; these cover the remaining column types. */
internal class Converters {
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun stringListToJson(list: List<String>?): String? = list?.let { Json.encodeToString(it) }

    @TypeConverter
    fun jsonToStringList(value: String?): List<String>? = value?.let { Json.decodeFromString(it) }
}
