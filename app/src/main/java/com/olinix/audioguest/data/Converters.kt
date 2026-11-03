package com.olinix.audioguest.data

import androidx.room.TypeConverter
import java.time.LocalDate

/** How Room stores the types SQLite does not know. Enums need nothing: Room keeps their names. */
class Converters {

    /** As ISO text (2027-06-14), readable when looking at the database by hand. */
    @TypeConverter
    fun fromDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toDate(text: String): LocalDate = LocalDate.parse(text)
}
