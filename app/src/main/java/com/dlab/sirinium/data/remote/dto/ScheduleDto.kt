package com.dlab.sirinium.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class ScheduleItemDto(
    @SerialName("section_type") val sectionType: String? = null,
    @SerialName("target") val target: String? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("day_of_week") val dayOfWeek: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("discipline") val discipline: String? = null,
    @SerialName("lesson_type") val lessonType: String? = null,
    @SerialName("teacher") val teacher: String? = null,
    @SerialName("classroom") val classroom: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("online_url") val onlineUrl: String? = null,
    @SerialName("group") val group: String? = null,
    @SerialName("number_pair") val numberPair: Int? = null
)

@Serializable
data class StatusResponseDto(
    @SerialName("status") val status: String,
    @SerialName("last_run_at") val lastRunAt: String? = null,
    @SerialName("next_run_at") val nextRunAt: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Double? = null,
    @SerialName("total_records") val totalRecords: Int = 0,
    @SerialName("groups_count") val groupsCount: Int = 0,
    @SerialName("teachers_count") val teachersCount: Int = 0,
    @SerialName("classrooms_count") val classroomsCount: Int = 0,
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class RefreshResponseDto(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String
)

@Serializable(with = TeacherDtoSerializer::class)
data class TeacherDto(
    val id: String = "",
    val fio: String = "",
    val department: String = ""
) {
    val displayName: String
        get() = fio.ifBlank { id }
}

object TeacherDtoSerializer : KSerializer<TeacherDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TeacherDto") {
        element<String>("id", isOptional = true)
        element<String>("fio", isOptional = true)
        element<String>("department", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): TeacherDto {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            return when (val element = jsonDecoder.decodeJsonElement()) {
                is JsonPrimitive -> {
                    val str = element.content
                    TeacherDto(id = str, fio = str)
                }
                is JsonObject -> {
                    val id = element["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val fio = element["fio"]?.jsonPrimitive?.contentOrNull
                        ?: element["name"]?.jsonPrimitive?.contentOrNull
                        ?: id
                    val department = element["department"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    TeacherDto(id = id, fio = fio, department = department)
                }
                else -> TeacherDto()
            }
        }
        val str = decoder.decodeString()
        return TeacherDto(id = str, fio = str)
    }

    override fun serialize(encoder: Encoder, value: TeacherDto) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            val jsonObject = buildJsonObject {
                put("id", value.id)
                put("fio", value.fio)
                put("department", value.department)
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        } else {
            encoder.encodeString(value.displayName)
        }
    }
}

@Serializable
data class CheckGroupResponseDto(
    @SerialName("query") val query: String = "",
    @SerialName("normalized_query") val normalizedQuery: String? = null,
    @SerialName("found") val found: Boolean = false,
    @SerialName("count") val count: Int = 0,
    @SerialName("results") val results: List<String> = emptyList()
)

@Serializable
data class CheckTeacherItemDto(
    @SerialName("id") val id: String = "",
    @SerialName("fio") val fio: String = "",
    @SerialName("department") val department: String? = null
)

@Serializable
data class CheckTeacherResponseDto(
    @SerialName("query") val query: String = "",
    @SerialName("normalized_query") val normalizedQuery: String? = null,
    @SerialName("found") val found: Boolean = false,
    @SerialName("count") val count: Int = 0,
    @SerialName("results") val results: List<CheckTeacherItemDto> = emptyList()
)

