package com.dlab.sirinium.ui.onboarding

/**
 * State holding user selections and configuration throughout the onboarding flow.
 */
data class OnboardingUiState(
    val selectedTarget: String = "",
    val selectedSectionType: String = "group", // "group", "teacher", "classroom"
    val availableGroups: List<String> = emptyList(),
    val availableTeachers: List<String> = emptyList(),
    val availableClassrooms: List<String> = emptyList(),
    val isCustomTarget: Boolean = false,
    val customTargetInput: String = "",
    val notifyDevelopersAboutCustom: Boolean = true,
    val themeMode: String = "system", // "system", "light", "dark"
    val dynamicColor: Boolean = true,
    val notifyLessonsEnabled: Boolean = true,
    val defaultAlarmMinutes: Int = 15,
    val shakeToReportEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val isLoadingEntities: Boolean = false,
    val isOnboardingCompleted: Boolean = false
) {
    val effectiveTarget: String
        get() = if (isCustomTarget && customTargetInput.isNotBlank()) customTargetInput.trim() else selectedTarget

    val effectiveSectionType: String
        get() = selectedSectionType
}
