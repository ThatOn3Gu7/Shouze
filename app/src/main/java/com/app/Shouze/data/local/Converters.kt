package com.app.shouze.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromGenresList(genres: List<String>): String = genres.joinToString("|")

    @TypeConverter
    fun toGenresList(genresString: String): List<String> =
        if (genresString.isBlank()) emptyList()
        else genresString.split("|").filter { it.isNotBlank() }
}
