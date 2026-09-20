package com.dlab.sirinium.data.remote.api

import com.dlab.sirinium.data.remote.dto.AppInfoDto
import com.dlab.sirinium.data.remote.dto.AppUpdateDto
import com.dlab.sirinium.data.remote.dto.CheckGroupResponseDto
import com.dlab.sirinium.data.remote.dto.CheckTeacherResponseDto
import com.dlab.sirinium.data.remote.dto.RefreshResponseDto
import com.dlab.sirinium.data.remote.dto.ScheduleItemDto
import com.dlab.sirinium.data.remote.dto.StatusResponseDto
import com.dlab.sirinium.data.remote.dto.TeacherDto

interface SiriusScheduleApi {

    suspend fun getSchedule(
        group: String? = null,
        teacher: String? = null,
        classroom: String? = null,
        date: String? = null,
        weekOffset: Int? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        discipline: String? = null,
        sectionType: String? = null,
        search: String? = null,
        limit: Int? = 500,
        offset: Int? = 0
    ): List<ScheduleItemDto>

    suspend fun getGroupSchedule(
        groupName: String,
        date: String? = null,
        weekOffset: Int? = null
    ): List<ScheduleItemDto>

    suspend fun getGroupScheduleByQuery(
        group: String? = null,
        name: String? = null,
        date: String? = null,
        weekOffset: Int? = null
    ): List<ScheduleItemDto>

    suspend fun getTeacherSchedule(
        teacher: String,
        date: String? = null,
        weekOffset: Int? = null
    ): List<ScheduleItemDto>

    suspend fun getTeacherScheduleByQuery(
        teacher: String? = null,
        name: String? = null,
        date: String? = null,
        weekOffset: Int? = null
    ): List<ScheduleItemDto>

    suspend fun getClassroomSchedule(
        classroomName: String,
        date: String? = null,
        weekOffset: Int? = null
    ): List<ScheduleItemDto>

    suspend fun getClassroomScheduleByQuery(
        classroom: String? = null,
        name: String? = null,
        date: String? = null,
        weekOffset: Int? = null
    ): List<ScheduleItemDto>

    suspend fun getGroups(
        query: String? = null
    ): List<String>

    suspend fun getTeachers(
        query: String? = null
    ): List<TeacherDto>

    suspend fun getClassrooms(
        query: String? = null
    ): List<String>

    suspend fun getStatus(): StatusResponseDto

    suspend fun triggerRefresh(): RefreshResponseDto

    suspend fun checkGroup(
        query: String? = null,
        q: String? = null
    ): CheckGroupResponseDto

    suspend fun checkGroupByPath(
        groupQuery: String
    ): CheckGroupResponseDto

    suspend fun checkTeacher(
        query: String? = null,
        q: String? = null
    ): CheckTeacherResponseDto

    suspend fun checkTeacherByPath(
        teacherQuery: String
    ): CheckTeacherResponseDto

    suspend fun getAppUpdate(): AppUpdateDto

    suspend fun getAppInfo(): AppInfoDto
}
