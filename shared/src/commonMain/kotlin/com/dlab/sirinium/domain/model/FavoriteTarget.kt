package com.dlab.sirinium.domain.model

data class FavoriteTarget(
    val target: String,
    val sectionType: String // "group", "teacher", "classroom"
) {
    fun toSerializedString(): String = "$sectionType:$target"

    companion object {
        fun fromSerializedString(str: String): FavoriteTarget? {
            val parts = str.split(":", limit = 2)
            if (parts.size != 2) return null
            return FavoriteTarget(sectionType = parts[0], target = parts[1])
        }
    }
}
