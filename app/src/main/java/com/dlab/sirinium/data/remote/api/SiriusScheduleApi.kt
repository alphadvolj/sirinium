package com.dlab.sirinium.data.remote.api

import com.dlab.sirinium.data.remote.dto.CheckGroupResponseDto
import com.dlab.sirinium.data.remote.dto.CheckTeacherResponseDto
import com.dlab.sirinium.data.remote.dto.RefreshResponseDto
import com.dlab.sirinium.data.remote.dto.ScheduleItemDto
import com.dlab.sirinium.data.remote.dto.StatusResponseDto
import com.dlab.sirinium.data.remote.dto.TeacherDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SiriusScheduleApi {

    @GET("api/schedule")
    suspend fun getSchedule(
        @Query("group") group: String? = null,
        @Query("teacher") teacher: String? = null,
        @Query("classroom") classroom: String? = null,
        @Query("date") date: String? = null,
        @Query("week_offset") weekOffset: Int? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("discipline") discipline: String? = null,
        @Query("section_type") sectionType: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = 500,
        @Query("offset") offset: Int? = 0
    ): List<ScheduleItemDto>

    @GET("api/schedule/group/{group_name}")
    suspend fun getGroupSchedule(
        @Path(value = "group_name", encoded = true) groupName: String,
        @Query("date") date: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): List<ScheduleItemDto>

    @GET("api/schedule/group")
    suspend fun getGroupScheduleByQuery(
        @Query("group") group: String? = null,
        @Query("name") name: String? = null,
        @Query("date") date: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): List<ScheduleItemDto>

    @GET("api/schedule/teacher/{teacher}")
    suspend fun getTeacherSchedule(
        @Path(value = "teacher", encoded = true) teacher: String,
        @Query("date") date: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): List<ScheduleItemDto>

    @GET("api/schedule/teacher")
    suspend fun getTeacherScheduleByQuery(
        @Query("teacher") teacher: String? = null,
        @Query("name") name: String? = null,
        @Query("date") date: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): List<ScheduleItemDto>

    @GET("api/schedule/classroom/{classroom_name}")
    suspend fun getClassroomSchedule(
        @Path(value = "classroom_name", encoded = true) classroomName: String,
        @Query("date") date: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): List<ScheduleItemDto>

    @GET("api/schedule/classroom")
    suspend fun getClassroomScheduleByQuery(
        @Query("classroom") classroom: String? = null,
        @Query("name") name: String? = null,
        @Query("date") date: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): List<ScheduleItemDto>

    @GET("api/groups")
    suspend fun getGroups(
        @Query("q") query: String? = null
    ): List<String>

    @GET("api/teachers")
    suspend fun getTeachers(
        @Query("q") query: String? = null
    ): List<TeacherDto>

    @GET("api/classrooms")
    suspend fun getClassrooms(
        @Query("q") query: String? = null
    ): List<String>

    @GET("api/status")
    suspend fun getStatus(): StatusResponseDto

    @POST("api/refresh")
    suspend fun triggerRefresh(): RefreshResponseDto

    @GET("api/check/group")
    suspend fun checkGroup(
        @Query("query") query: String? = null,
        @Query("q") q: String? = null
    ): CheckGroupResponseDto

    @GET("api/check/group/{group_query}")
    suspend fun checkGroupByPath(
        @Path(value = "group_query", encoded = true) groupQuery: String
    ): CheckGroupResponseDto

    @GET("api/check/teacher")
    suspend fun checkTeacher(
        @Query("query") query: String? = null,
        @Query("q") q: String? = null
    ): CheckTeacherResponseDto

    @GET("api/check/teacher/{teacher_query}")
    suspend fun checkTeacherByPath(
        @Path(value = "teacher_query", encoded = true) teacherQuery: String
    ): CheckTeacherResponseDto

    @GET("api/appupdate")
    suspend fun getAppUpdate(): com.dlab.sirinium.data.remote.dto.AppUpdateDto

    @GET("api/appinfo")
    suspend fun getAppInfo(): com.dlab.sirinium.data.remote.dto.AppInfoDto
}

