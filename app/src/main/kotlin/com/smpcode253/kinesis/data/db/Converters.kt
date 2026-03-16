package com.smpcode253.kinesis.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room [TypeConverter]s that serialise/deserialise types that Room cannot
 * store natively.
 *
 * Currently handles `List<String>` ↔ JSON string so that
 * [com.smpcode253.kinesis.data.models.Preferences.trustedContacts] can be
 * stored in a single TEXT column.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>): String = gson.toJson(list)
}
