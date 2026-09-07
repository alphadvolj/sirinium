# Sirinium

<p align="center">
  <b>Современное, быстрое и удобное Android-приложение для просмотра расписания занятий</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Min_SDK-31-blue" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target_SDK-35-green" alt="Target SDK" />
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License" />
</p>

---

## 🌟 Основные возможности

- 📅 **Гибкое расписание**:
  - Просмотр расписания для учебных групп, преподавателей и аудиторий.
  - Быстрое переключение дней недели и удобный таймлайн пар с визуализацией текущего статуса занятия.
- ⚡ **Оффлайн-режим и автосинхронизация**:
  - Локальное кэширование в **Room Database** — расписание доступно даже без интернета.
  - Периодическая фоновая синхронизация с сервером через **WorkManager**.
- 🔔 **Уведомления и будильники**:
  - Точные напоминания о начале пары за 10–15 минут (**Exact Alarm**).
  - Автоматическое восстановление расписания будильников после перезагрузки устройства (**BootReceiver**).
- 📝 **Заметки и домашние задания**:
  - Персональные заметки к каждой паре с чек-листом домашних заданий и дедлайнами.
- 🔍 **Поиск свободных аудиторий**:
  - Быстрый просмотр незанятых кабинетов на выбранную пару или день.
- ⚖️ **Сравнение расписаний**:
  - Совмещение расписаний двух групп или преподавателей для поиска свободных окон и совместного времени.
- 📱 **Виджеты на рабочий стол (Jetpack Glance)**:
  - **Компактный (2x2)**: текущая / следующая пара с обратным отсчетом времени.
  - **Список (4x3)**: интерактивный список занятий на сегодня или завтра прямо на главном экране.
- 🎨 **Material You & Дизайн**:
  - Современный интерфейс Material Design 3 с поддержкой динамических цветов системы (Dynamic Colors).
  - Светлая и тёмная темы.
  - Кастомная альтернативная иконка приложения.

---

## 🛠 Стек технологий и архитектура

- **Язык**: [Kotlin](https://kotlinlang.org/) (версия 2.2+)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
- **Архитектура**: MVVM / Clean Architecture (Presentation, Domain, Data)
- **Dependency Injection**: [Koin](https://insert-koin.io/)
- **База данных**: [Room](https://developer.android.com/training/data-storage/room) (SQLite) с KSP-кодогенерацией
- **Сеть**: [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp 3](https://square.github.io/okhttp/) + [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Асинхронность**: Kotlin Coroutines & StateFlow / SharedFlow
- **Виджеты**: Jetpack Glance (App Widgets)
- **Фоновые задачи**: Android WorkManager & AlarmManager
- **Навигация**: Jetpack Navigation Compose

---

## 📁 Структура проекта

```text
app/src/main/java/com/dlab/sirinium/
├── alarm/            # Планировщик будильников и BroadcastReceiver
├── core/             # Утилиты, модели общих ресурсов, детекторы жестов
├── data/
│   ├── local/        # Room Database, DAO и сущности
│   ├── remote/       # Retrofit API, DTO и сетевой маппинг
│   └── repository/   # Реализации репозиториев
├── di/               # Koin-модули внедрения зависимостей
├── domain/           # Бизнес-модели и интерфейсы репозиториев
├── notification/     # Менеджер каналов и отправки push-уведомлений
├── sync/             # Фоновый WorkManager для синхронизации
├── ui/               # Экраны, компоненты Compose, ViewModels и тема
└── widget/           # Jetpack Glance виджеты для рабочего стола
```

---

## 🚀 Сборка и запуск

### Требования:
- **JDK**: 17 или выше
- **Android Studio**: Ladybug / Meerkat или новее
- **Android SDK**: Min SDK 31 (Android 12), Target SDK 35 (Android 15)

### Клонирование и локальная сборка:

```bash
# Клонирование репозитория
git clone https://github.com/alphadvolj/sirinium.git
cd sirinium

# Сборка Debug APK
./gradlew assembleDebug

# Запуск тестов
./gradlew test
```

Собранный APK будет находиться по пути:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 Лицензия

Проект распространяется под лицензией [MIT](LICENSE).
