package com.dlab.sirinium

import com.dlab.sirinium.core.util.AppIconManager
import com.dlab.sirinium.domain.model.FavoriteTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasterEggTest {

    @Test
    fun testFavoriteTargetTeacherCondition() {
        val favGroup = FavoriteTarget(target = "К1609-241", sectionType = "group")
        val favRoom = FavoriteTarget(target = "3.2", sectionType = "classroom")
        val favTeacher = FavoriteTarget(target = "Иванов И.И.", sectionType = "teacher")

        val listWithoutTeacher = listOf(favGroup, favRoom)
        val hasTeacherInListWithout = listWithoutTeacher.any { it.sectionType == "teacher" }
        assertFalse("List without teacher should return false for hasTeacher", hasTeacherInListWithout)

        val listWithTeacher = listOf(favGroup, favTeacher, favRoom)
        val hasTeacherInListWith = listWithTeacher.any { it.sectionType == "teacher" }
        assertTrue("List with teacher should return true for hasTeacher", hasTeacherInListWith)
    }

    @Test
    fun testFavoriteTargetSerialization() {
        val serializedTeacher = "teacher:Иванов И.И."
        val parsed = FavoriteTarget.fromSerializedString(serializedTeacher)
        assertNotNull(parsed)
        assertEquals("teacher", parsed?.sectionType)
        assertEquals("Иванов И.И.", parsed?.target)
    }

    @Test
    fun testRawSetTeacherDetection() {
        val rawSetWithTeacher = setOf("group:К1609-241", "teacher:Петров П.П.", "classroom:2.1")
        val favoritesWith = rawSetWithTeacher.mapNotNull { FavoriteTarget.fromSerializedString(it) }
        val hasTeacherWith = favoritesWith.any { it.sectionType.equals("teacher", ignoreCase = true) } ||
                rawSetWithTeacher.any { it.startsWith("teacher:", ignoreCase = true) }
        assertTrue("Should detect teacher in rawSet", hasTeacherWith)

        val rawSetWithoutTeacher = setOf("group:К1609-241", "classroom:2.1")
        val favoritesWithout = rawSetWithoutTeacher.mapNotNull { FavoriteTarget.fromSerializedString(it) }
        val hasTeacherWithout = favoritesWithout.any { it.sectionType.equals("teacher", ignoreCase = true) } ||
                rawSetWithoutTeacher.any { it.startsWith("teacher:", ignoreCase = true) }
        assertFalse("Should not detect teacher when none exists", hasTeacherWithout)
    }

    @Test
    fun testAppIconManagerConstants() {
        assertEquals("com.dlab.sirinium.MainActivityDefault", AppIconManager.ALIAS_DEFAULT)
        assertEquals("com.dlab.sirinium.MainActivityCustom", AppIconManager.ALIAS_CUSTOM)
    }
}
