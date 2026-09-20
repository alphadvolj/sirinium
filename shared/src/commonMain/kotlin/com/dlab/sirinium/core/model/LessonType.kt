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
    EXAM(
        title = "Экзамен",
        shortTitle = "Экз",
        lightContainer = Color(0xFFFAF5FF), // Purple 50
        lightContent = Color(0xFF7E22CE),   // Purple 700 (Purple)
        lightBorder = Color(0xFFE9D5FF),    // Purple 200
        darkContainer = Color(0xFF3B0764),  // Purple 950
        darkContent = Color(0xFFC084FC),    // Purple 400
        darkBorder = Color(0xFF9333EA)     // Purple 600
    ),
    TESTING(
        title = "Тестирование",
        shortTitle = "Тест",
        lightContainer = Color(0xFFF5F3FF), // Violet 50
        lightContent = Color(0xFF6D28D9),   // Violet 700
        lightBorder = Color(0xFFDDD6FE),    // Violet 200
        darkContainer = Color(0xFF2E1065),  // Violet 950
        darkContent = Color(0xFFA78BFA),    // Violet 400
        darkBorder = Color(0xFF7C3AED)     // Violet 600
    ),
    LABORATORY(
        title = "Лабораторная работа",
        shortTitle = "Лаб",
        lightContainer = Color(0xFFECFEFF), // Cyan 50
        lightContent = Color(0xFF0E7490),   // Cyan 700 (Teal/Cyan)
        lightBorder = Color(0xFFA5F3FC),    // Cyan 200
        darkContainer = Color(0xFF083344),  // Cyan 950
        darkContent = Color(0xFF22D3EE),    // Cyan 400
        darkBorder = Color(0xFF0891B2)     // Cyan 600
    ),
    EVENT(
        title = "Мероприятие",
        shortTitle = "Меропр",
        lightContainer = Color(0xFFFFF1F2), // Rose 50
        lightContent = Color(0xFFBE123C),   // Rose 700
        lightBorder = Color(0xFFFECDD3),    // Rose 200
        darkContainer = Color(0xFF4C0519),  // Rose 950
        darkContent = Color(0xFFFB7185),    // Rose 400
        darkBorder = Color(0xFFE11D48)     // Rose 600
    ),
    OTHER(
        title = "Занятие",
        shortTitle = "Занятие",
        lightContainer = Color(0xFFF1F5F9), // Slate 100
        lightContent = Color(0xFF475569),   // Slate 600
        lightBorder = Color(0xFFCBD5E1),    // Slate 300
        darkContainer = Color(0xFF1E293B),  // Slate 800
        darkContent = Color(0xFF94A3B8),    // Slate 400
        darkBorder = Color(0xFF475569)     // Slate 600
    );

    fun containerColor(isDark: Boolean): Color = if (isDark) darkContainer else lightContainer
    fun contentColor(isDark: Boolean): Color = if (isDark) darkContent else lightContent
    fun borderColor(isDark: Boolean): Color = if (isDark) darkBorder else lightBorder

    companion object {
        fun fromString(raw: String?): LessonType {
            if (raw == null) return OTHER
            val normalized = raw.trim().lowercase()
            return when {
                normalized.contains("лек") -> LECTURE
                normalized.contains("прак") -> PRACTICE
                normalized.contains("сем") -> SEMINAR
                normalized.contains("конс") -> CONSULTATION
                normalized.contains("зач") -> CREDIT
                normalized.contains("экз") -> EXAM
                normalized.contains("тест") -> TESTING
                normalized.contains("лаб") -> LABORATORY
                normalized.contains("меропр") -> EVENT
                else -> OTHER
            }
        }
    }
}
