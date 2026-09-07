package com.dlab.sirinium.core.model

import androidx.compose.ui.graphics.Color

enum class LessonType(
    val title: String,
    val shortTitle: String,
    val lightContainer: Color,
    val lightContent: Color,
    val lightBorder: Color,
    val darkContainer: Color,
    val darkContent: Color,
    val darkBorder: Color
) {
    LECTURE(
        title = "Лекция",
        shortTitle = "Лек",
        lightContainer = Color(0xFFECFDF5), // Emerald 50
        lightContent = Color(0xFF047857),   // Emerald 700 (Green)
        lightBorder = Color(0xFFA7F3D0),    // Emerald 200
        darkContainer = Color(0xFF064E3B),  // Emerald 950
        darkContent = Color(0xFF34D399),    // Emerald 400
        darkBorder = Color(0xFF059669)     // Emerald 600
    ),
    PRACTICE(
        title = "Практика",
        shortTitle = "Прак",
        lightContainer = Color(0xFFEFF6FF), // Blue 50
        lightContent = Color(0xFF1D4ED8),   // Blue 700 (Blue)
        lightBorder = Color(0xFFBFDBFE),    // Blue 200
        darkContainer = Color(0xFF172554),  // Blue 950
        darkContent = Color(0xFF60A5FA),    // Blue 400
        darkBorder = Color(0xFF2563EB)     // Blue 600
    ),
    SEMINAR(
        title = "Семинар",
        shortTitle = "Сем",
        lightContainer = Color(0xFFFEFCE8), // Yellow 50
        lightContent = Color(0xFFA16207),   // Yellow 700 (Yellow)
        lightBorder = Color(0xFFFDE047),    // Yellow 300
        darkContainer = Color(0xFF422006),  // Yellow 950
        darkContent = Color(0xFFFACC15),    // Yellow 400
        darkBorder = Color(0xFFCA8A04)     // Yellow 600
    ),
    CONSULTATION(
        title = "Консультация",
        shortTitle = "Конс",
        lightContainer = Color(0xFFFFF7ED), // Orange 50
        lightContent = Color(0xFFC2410C),   // Orange 700 (Orange)
        lightBorder = Color(0xFFFED7AA),    // Orange 200
        darkContainer = Color(0xFF431407),  // Orange 950
        darkContent = Color(0xFFFB923C),    // Orange 400
        darkBorder = Color(0xFFEA580C)     // Orange 600
    ),
    CREDIT(
        title = "Зачет",
        shortTitle = "Зачет",
        lightContainer = Color(0xFFFEF2F2), // Red 50
        lightContent = Color(0xFFB91C1C),   // Red 700 (Red)
        lightBorder = Color(0xFFFECACA),    // Red 200
        darkContainer = Color(0xFF450A0A),  // Red 950
        darkContent = Color(0xFFF87171),    // Red 400
        darkBorder = Color(0xFFDC2626)     // Red 600
    ),
    CREDIT_DIFF(
        title = "Дифф. зачет",
        shortTitle = "Дифф",
        lightContainer = Color(0xFFFEF2F2), // Red 50
        lightContent = Color(0xFFB91C1C),   // Red 700 (Red)
        lightBorder = Color(0xFFFECACA),    // Red 200
        darkContainer = Color(0xFF450A0A),  // Red 950
        darkContent = Color(0xFFF87171),    // Red 400
        darkBorder = Color(0xFFDC2626)     // Red 600
    ),
    EXAM(
        title = "Экзамен",
        shortTitle = "Экз",
        lightContainer = Color(0xFFFEF2F2), // Red 50
        lightContent = Color(0xFFB91C1C),   // Red 700 (Red)
        lightBorder = Color(0xFFFECACA),    // Red 200
        darkContainer = Color(0xFF450A0A),  // Red 950
        darkContent = Color(0xFFF87171),    // Red 400
        darkBorder = Color(0xFFDC2626)     // Red 600
    ),
    CONTROL_WORK(
        title = "Контрольная работа",
        shortTitle = "К/Р",
        lightContainer = Color(0xFFFAF5FF), // Purple 50
        lightContent = Color(0xFF7E22CE),   // Purple 700 (Purple)
        lightBorder = Color(0xFFE9D5FF),    // Purple 200
        darkContainer = Color(0xFF3B0764),  // Purple 950
        darkContent = Color(0xFFC084FC),    // Purple 400
        darkBorder = Color(0xFF9333EA)     // Purple 600
    ),
    EXTRA_EVENT(
        title = "Внеучебное мероприятие",
        shortTitle = "Событие",
        lightContainer = Color(0xFFFAF5FF), // Purple 50
        lightContent = Color(0xFF7E22CE),   // Purple 700 (Purple)
        lightBorder = Color(0xFFE9D5FF),    // Purple 200
        darkContainer = Color(0xFF3B0764),  // Purple 950
        darkContent = Color(0xFFC084FC),    // Purple 400
        darkBorder = Color(0xFF9333EA)     // Purple 600
    ),
    LAB(
        title = "Лабораторная",
        shortTitle = "Лаб",
        lightContainer = Color(0xFFF0FDFA), // Teal 50
        lightContent = Color(0xFF0F766E),   // Teal 700
        lightBorder = Color(0xFF99F6E4),    // Teal 200
        darkContainer = Color(0xFF042F2E),  // Teal 950
        darkContent = Color(0xFF2DD4BF),    // Teal 400
        darkBorder = Color(0xFF0D9488)     // Teal 600
    ),
    OTHER(
        title = "Занятие",
        shortTitle = "Пара",
        lightContainer = Color(0xFFF1F5F9), // Slate 100
        lightContent = Color(0xFF475569),   // Slate 600
        lightBorder = Color(0xFFCBD5E1),    // Slate 300
        darkContainer = Color(0xFF1E293B),  // Slate 800
        darkContent = Color(0xFF94A3B8),    // Slate 400
        darkBorder = Color(0xFF64748B)     // Slate 600
    );

    companion object {
        fun fromString(raw: String?): LessonType {
            if (raw.isNullOrBlank()) return OTHER
            val lower = raw.trim().lowercase()
            return when {
                lower.contains("контр") || lower.contains("коллок") -> CONTROL_WORK
                lower.contains("внеучеб") || lower.contains("мероприят") -> EXTRA_EVENT
                lower.contains("дифф") -> CREDIT_DIFF
                lower.contains("зачет") || lower.contains("зачёт") -> CREDIT
                lower.contains("экзам") || lower.contains("аттест") -> EXAM
                lower.contains("консульт") -> CONSULTATION
                lower.contains("семин") -> SEMINAR
                lower.contains("лекц") -> LECTURE
                lower.contains("практ") -> PRACTICE
                lower.contains("лаб") -> LAB
                else -> OTHER
            }
        }
    }
}
