package no.nav.syfo.util

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime

fun gsonSerializer() =
    GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime::class.java,  LocalDateTimeAdapter())
        .create()

class LocalDateTimeAdapter: JsonSerializer<LocalDateTime> {
    override fun serialize(
            toSerialize: LocalDateTime?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?)
    : JsonElement? =
        toSerialize?.let { JsonPrimitive(toSerialize.toString()) }
}
