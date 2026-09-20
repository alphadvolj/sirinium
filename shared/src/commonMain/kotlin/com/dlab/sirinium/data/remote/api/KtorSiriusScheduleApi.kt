package com.dlab.sirinium.data.remote.api

import com.dlab.sirinium.data.remote.dto.AppInfoDto
import com.dlab.sirinium.data.remote.dto.AppUpdateDto
import com.dlab.sirinium.data.remote.dto.CheckGroupResponseDto
import com.dlab.sirinium.data.remote.dto.CheckTeacherResponseDto
import com.dlab.sirinium.data.remote.dto.RefreshResponseDto
import com.dlab.sirinium.data.remote.dto.ScheduleItemDto
import com.dlab.sirinium.data.remote.dto.StatusResponseDto
import com.dlab.sirinium.data.remote.dto.TeacherDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

class KtorSiriusScheduleApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://sirinium.avh-vless.work/"
) : SiriusScheduleApi {

    override suspend fun getSchedule(
        group: String?,
        teacher: String?,
        classroom: String?,
        date: String?,
        weekOffset: Int?,
        dateFrom: String?,
        dateTo: String?,
        discipline: String?,
        sectionType: String?,
        search: String?,
        limit: Int?,
        offset: Int?
    ): List<ScheduleItemDto> {
        return client.get("${baseUrl}api/schedule") {
            group?.let { parameter("group", it) }
            teacher?.let { parameter("teacher", it) }
            classroom?.let { parameter("classroom", it) }
            date?.let { parameter("date", it) }
            weekOffset?.let { parameter("week_offset", it) }
            dateFrom?.let { parameter("date_from", it) }
            dateTo?.let { parameter("date_to", it) }
            discipline?.let { parameter("discipline", it) }
            sectionType?.let { parameter("section_type", it) }
            search?.let { parameter("search", it) }
            limit?.let { parameter("limit", it) }
            offset?.let { parameter("offset", it) }
        }.body()
    }

    override suspend fun getGroupSchedule(
        groupName: String,
        date: String?,
        weekOffset: Int?
    ): List<ScheduleItemDto> {
        return client.get("${baseUrl}api/schedule/group/$groupName") {
            date?.let { parameter("date", it) }
            weekOffset?.let { parameter("week_offset", it) }
        }.body()
    }

    override suspend fun getGroupScheduleByQuery(
        group: String?,
        name: String?,
        date: String?,
        weekOffset: Int?
    ): List<ScheduleItemDto> {
        return client.get("${baseUrl}api/schedule/group") {
            group?.let { parameter("group", it) }
            name?.let { parameter("name", it) }
            date?.let { parameter("date", it) }
            weekOffset?.let { parameter("week_offset", it) }
        }.body()
    }

    override suspend fun getTeacherSchedule(
        teacher: String,
        date: String?,
        weekOffset: Int?
    ): List<ScheduleItemDto> {
        return client.get("${baseUrl}api/schedule/teacher/$teacher") {
            date?.let { parameter("date", it) }
            weekOffset?.let { parameter("week_offset", it) }
        }.body()
    }

    override suspend fun getTeacherScheduleByQuery(
        teacher: String?,
        name: String?,
        date: String?,
        weekOffset: Int?
    ): List<ScheduleItemDto> {
        return client.get("${baseUrl}api/schedule/teacher") {
            teacher?.let { parameter("teacher", it) }
            name?.let { parameter("name", it) }
            date?.let { parameter("date", it) }
            weekOffset?.let { parameter("week_offset", it) }
        }.body()
    }

    override suspend fun getClassroomSchedule(
        classroomName: String,
        date: String?,
        weekOffset: Int?
    ): List<ScheduleItemDto> {
        return client.get("${baseUrl}api/schedule/classroom/$classroomName") {
            date?.let { parameter("date", it) }
            weekOffset?.let { parameter("week_offset", it) }
        }.body()
    }

    override suspend fun getClassroomScheduleByQuery(
        classroom: String?,
        name: String?,
        date: String?,
        weekOffset: Int?
    ): List<ScheduleItemDto> {
        return client.get("${baseUrl}api/schedule/classroom") {
            classroom?.let { parameter("classroom", it) }
            name?.let { parameter("name", it) }
            date?.let { parameter("date", it) }
            weekOffset?.let { parameter("week_offset", it) }
        }.body()
    }

    override suspend fun getGroups(query: String?): List<String> {
        return client.get("${baseUrl}api/groups") {
            query?.let { parameter("q", it) }
        }.body()
    }

    override suspend fun getTeachers(query: String?): List<TeacherDto> {
        return client.get("${baseUrl}api/teachers") {
            query?.let { parameter("q", it) }
        }.body()
    }

    override suspend fun getClassrooms(query: String?): List<String> {
        return client.get("${baseUrl}api/classrooms") {
            query?.let { parameter("q", it) }
        }.body()
    }

    override suspend fun getStatus(): StatusResponseDto {
        return client.get("${baseUrl}api/status").body()
    }

    override suspend fun triggerRefresh(): RefreshResponseDto {
        return client.post("${baseUrl}api/refresh").body()
    }

    override suspend fun checkGroup(query: String?, q: String?): CheckGroupResponseDto {
        return client.get("${baseUrl}api/check/group") {
            query?.let { parameter("query", it) }
            q?.let { parameter("q", it) }
        }.body()
    }

    override suspend fun checkGroupByPath(groupQuery: String): CheckGroupResponseDto {
        return client.get("${baseUrl}api/check/group/$groupQuery").body()
    }

    override suspend fun checkTeacher(query: String?, q: String?): CheckTeacherResponseDto {
        return client.get("${baseUrl}api/check/teacher") {
            query?.let { parameter("query", it) }
            q?.let { parameter("q", it) }
        }.body()
    }

    override suspend fun checkTeacherByPath(teacherQuery: String): CheckTeacherResponseDto {
        return client.get("${baseUrl}api/check/teacher/$teacherQuery").body()
    }

    override suspend fun getAppUpdate(): AppUpdateDto {
        return client.get("${baseUrl}api/appupdate").body()
    }

    override suspend fun getAppInfo(): AppInfoDto {
        return client.get("${baseUrl}api/appinfo").body()
    }
}
