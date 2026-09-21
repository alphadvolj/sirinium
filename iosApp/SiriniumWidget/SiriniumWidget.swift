import WidgetKit
import SwiftUI

// MARK: - Data Models
struct WidgetScheduleData: Codable {
    let target: String
    let sectionType: String
    let updatedAt: Int64
    let lessons: [WidgetLesson]
}

struct WidgetLesson: Codable, Identifiable {
    let id: String
    let discipline: String
    let startTime: String
    let endTime: String
    let classroom: String
    let teacher: String
    let group: String?
    let rawLessonType: String
    let numberPair: Int
    let date: String

    enum CodingKeys: String, CodingKey {
        case id, discipline, startTime, endTime, classroom, teacher, group, rawLessonType, numberPair, date
    }

    init(
        id: String,
        discipline: String,
        startTime: String,
        endTime: String,
        classroom: String,
        teacher: String,
        group: String? = nil,
        rawLessonType: String,
        numberPair: Int,
        date: String
    ) {
        self.id = id
        self.discipline = discipline
        self.startTime = startTime
        self.endTime = endTime
        self.classroom = classroom
        self.teacher = teacher
        self.group = group
        self.rawLessonType = rawLessonType
        self.numberPair = numberPair
        self.date = date
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        discipline = try container.decode(String.self, forKey: .discipline)
        startTime = try container.decode(String.self, forKey: .startTime)
        endTime = try container.decode(String.self, forKey: .endTime)
        classroom = try container.decode(String.self, forKey: .classroom)
        teacher = try container.decode(String.self, forKey: .teacher)
        group = try container.decodeIfPresent(String.self, forKey: .group) ?? ""
        rawLessonType = try container.decode(String.self, forKey: .rawLessonType)
        numberPair = try container.decode(Int.self, forKey: .numberPair)
        date = try container.decode(String.self, forKey: .date)
    }
}

enum LessonState {
    case ongoing
    case upcoming
    case completed
}

struct ScheduleWidgetEntry: TimelineEntry {
    let date: Date
    let displayTarget: String
    let targetIconName: String
    let sectionType: String
    let currentLesson: WidgetLesson?
    let nextLesson: WidgetLesson?
    let upcomingLessons: [WidgetLesson]
    let state: LessonState

    var target: String { displayTarget }
}

// MARK: - Date Formatting Helpers
func relativeDayText(_ dateStr: String, relativeTo: Date = Date()) -> String {
    let cleanDate = dateStr.trimmingCharacters(in: .whitespacesAndNewlines)
    let parts: [Int]
    if cleanDate.contains(".") {
        parts = cleanDate.split(separator: ".").compactMap { Int($0) }
    } else if cleanDate.contains("-") {
        let raw = cleanDate.split(separator: "-").compactMap { Int($0) }
        if raw.count == 3 && raw[0] > 1000 {
            parts = [raw[2], raw[1], raw[0]]
        } else {
            parts = raw
        }
    } else {
        return cleanDate
    }

    guard parts.count == 3 else { return cleanDate }
    var comps = DateComponents()
    comps.day = parts[0]
    comps.month = parts[1]
    comps.year = parts[2]

    let calendar = Calendar.current
    guard let targetDate = calendar.date(from: comps) else { return cleanDate }

    if calendar.isDate(targetDate, inSameDayAs: relativeTo) {
        return "Сегодня"
    } else if let tomorrow = calendar.date(byAdding: .day, value: 1, to: relativeTo),
              calendar.isDate(targetDate, inSameDayAs: tomorrow) {
        return "Завтра"
    } else {
        let df = DateFormatter()
        df.locale = Locale(identifier: "ru_RU")
        df.dateFormat = "E, d MMM"
        return df.string(from: targetDate).capitalized
    }
}

// MARK: - Lesson Styling & Formatting Helpers
func formatLocation(_ raw: String) -> String {
    var clean = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if clean.isEmpty { return "" }
    clean = clean.replacingOccurrences(of: "\"\"", with: "\"")
    return clean
}

func lessonColor(for rawType: String, discipline: String = "") -> Color {
    let text = (rawType + " " + discipline).trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

    // 1. Зачет, Зачет дифференцированный, Экзамен -> Красный
    if text.contains("зачет") || text.contains("зачёт") || text.contains("экзамен") || text.contains("экз") || text.contains("дифф") || text.contains("аттестац") {
        return Color.red
    }

    // 2. Лекции -> Зеленый
    if text.contains("лекц") || text.contains("лек") {
        return Color.green
    }

    // 3. Семинарские занятия -> Желтый
    if text.contains("семин") || text.contains("сем") {
        return Color.yellow
    }

    // 4. Консультация -> Фиолетовый
    if text.contains("консульт") || text.contains("конс") {
        return Color.purple
    }

    // 5. Внеучебное мероприятие -> Оранжевый
    if text.contains("внеучеб") || text.contains("меропр") || text.contains("куратор") || text.contains("событ") {
        return Color.orange
    }

    // 6. Практические занятия -> Синий
    if text.contains("практ") || text.contains("прак") || text.contains("лаб") {
        return Color.blue
    }

    // По умолчанию -> Синий
    return Color.blue
}

func formatTeacherInitials(_ raw: String) -> String {
    let clean = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if clean.isEmpty { return "" }

    let normalized = clean.replacingOccurrences(of: "\u{00a0}", with: " ")
    let components = normalized.components(separatedBy: .whitespaces).filter { !$0.isEmpty }
    guard !components.isEmpty else { return "" }

    let lastName = components[0]
    if components.count == 1 {
        return lastName
    }

    var initials: [String] = []
    for part in components.dropFirst() {
        let subParts = part.components(separatedBy: ".").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        for sub in subParts {
            if let firstLetter = sub.first {
                initials.append("\(firstLetter).")
            }
        }
    }

    if initials.isEmpty {
        return lastName
    }
    return "\(lastName) " + initials.joined(separator: " ")
}

struct LessonBadgeInfo: Identifiable {
    let id = UUID()
    let icon: String
    let text: String
}

func getLessonBadges(lesson: WidgetLesson, sectionType: String) -> [LessonBadgeInfo] {
    let cleanTeacher = formatTeacherInitials(lesson.teacher)
    let cleanClassroom = formatLocation(lesson.classroom)
    let cleanGroup = (lesson.group ?? "").trimmingCharacters(in: .whitespacesAndNewlines)

    var badges: [LessonBadgeInfo] = []

    switch sectionType.lowercased() {
    case "teacher":
        // Если выбран преподаватель: показывай группу и аудиторию
        if !cleanGroup.isEmpty {
            badges.append(LessonBadgeInfo(icon: "person.2.fill", text: cleanGroup))
        }
        if !cleanClassroom.isEmpty {
            badges.append(LessonBadgeInfo(icon: "location.fill", text: cleanClassroom))
        }
        if badges.isEmpty && !cleanTeacher.isEmpty {
            badges.append(LessonBadgeInfo(icon: "person.fill", text: cleanTeacher))
        }

    case "classroom", "auditorium":
        // Если выбрана аудитория: показывай Фамилию и инициалы преподавателя и группу
        if !cleanTeacher.isEmpty {
            badges.append(LessonBadgeInfo(icon: "person.fill", text: cleanTeacher))
        }
        if !cleanGroup.isEmpty {
            badges.append(LessonBadgeInfo(icon: "person.2.fill", text: cleanGroup))
        }
        if badges.isEmpty && !cleanClassroom.isEmpty {
            badges.append(LessonBadgeInfo(icon: "location.fill", text: cleanClassroom))
        }

    default:
        // По умолчанию (если выбрана группа): показывай Фамилию и инициалы преподавателя и аудиторию
        if !cleanTeacher.isEmpty {
            badges.append(LessonBadgeInfo(icon: "person.fill", text: cleanTeacher))
        }
        if !cleanClassroom.isEmpty {
            badges.append(LessonBadgeInfo(icon: "location.fill", text: cleanClassroom))
        }
        if badges.isEmpty && !cleanGroup.isEmpty {
            badges.append(LessonBadgeInfo(icon: "person.2.fill", text: cleanGroup))
        }
    }

    return badges
}

// MARK: - Timeline Provider
struct SiriniumTimelineProvider: TimelineProvider {
    typealias Entry = ScheduleWidgetEntry

    func placeholder(in context: Context) -> ScheduleWidgetEntry {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        let todayStr = formatter.string(from: Date())
        return ScheduleWidgetEntry(
            date: Date(),
            displayTarget: "К1609-241",
            targetIconName: "person.2.fill",
            sectionType: "group",
            currentLesson: WidgetLesson(
                id: "1",
                discipline: "Высшая математика",
                startTime: "08:45",
                endTime: "10:20",
                classroom: "302",
                teacher: "Иванов И.И.",
                group: "К1609-241",
                rawLessonType: "Лекция",
                numberPair: 1,
                date: todayStr
            ),
            nextLesson: WidgetLesson(
                id: "2",
                discipline: "Физика",
                startTime: "10:35",
                endTime: "12:10",
                classroom: "415",
                teacher: "Петров П.П.",
                group: "К1609-241",
                rawLessonType: "Практика",
                numberPair: 2,
                date: todayStr
            ),
            upcomingLessons: [],
            state: .ongoing
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (ScheduleWidgetEntry) -> Void) {
        if context.isPreview {
            completion(placeholder(in: context))
        } else {
            let entry = createEntry(for: Date())
            completion(entry)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ScheduleWidgetEntry>) -> Void) {
        let now = Date()
        let data = loadScheduleData()

        var entries: [ScheduleWidgetEntry] = []
        entries.append(createEntry(for: now, data: data))

        // Create timeline transition points for all upcoming lesson starts & ends
        let allLessons = data?.lessons ?? []
        for lesson in allLessons {
            if let startDate = parseDateTime(dateStr: lesson.date, timeStr: lesson.startTime), startDate > now {
                entries.append(createEntry(for: startDate, data: data))
            }
            if let endDate = parseDateTime(dateStr: lesson.date, timeStr: lesson.endTime), endDate > now {
                entries.append(createEntry(for: endDate, data: data))
            }
        }

        // Sort unique entries by date
        entries.sort { $0.date < $1.date }

        let refreshDate = Calendar.current.date(byAdding: .minute, value: 30, to: now) ?? now.addingTimeInterval(1800)
        let timeline = Timeline(entries: entries, policy: .after(refreshDate))
        completion(timeline)
    }

    // MARK: - Helpers
    private func getPossibleSuites() -> [String] {
        var suites = ["group.com.dlab.sirinium", "group.com.dlab.sirinium.ios"]
        if let bundleId = Bundle.main.bundleIdentifier {
            let stripped = bundleId.replacingOccurrences(of: ".widget", with: "")
                .replacingOccurrences(of: ".SiriniumWidgetExtension", with: "")
            suites.append("group.\(stripped)")
        }
        return suites
    }

    private func loadScheduleData() -> WidgetScheduleData? {
        let suites = getPossibleSuites()
        for suite in suites {
            if let sharedDefaults = UserDefaults(suiteName: suite),
               let jsonString = sharedDefaults.string(forKey: "widget_schedule_data"),
               let jsonData = jsonString.data(using: .utf8),
               let decoded = try? JSONDecoder().decode(WidgetScheduleData.self, from: jsonData) {
                return decoded
            }
            if let containerUrl = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: suite) {
                let fileUrl = containerUrl.appendingPathComponent("widget_schedule_data.json")
                if let fileData = try? Data(contentsOf: fileUrl),
                   let decoded = try? JSONDecoder().decode(WidgetScheduleData.self, from: fileData) {
                    return decoded
                }
            }
        }
        if let jsonString = UserDefaults.standard.string(forKey: "widget_schedule_data"),
           let jsonData = jsonString.data(using: .utf8),
           let decoded = try? JSONDecoder().decode(WidgetScheduleData.self, from: jsonData) {
            return decoded
        }
        return nil
    }

    private func loadActiveTarget() -> String? {
        let suites = getPossibleSuites()
        for suite in suites {
            if let sharedDefaults = UserDefaults(suiteName: suite) {
                if let t = sharedDefaults.string(forKey: "widget_active_target"), !t.isEmpty {
                    return t
                }
                if let t = sharedDefaults.string(forKey: "pref_current_target"), !t.isEmpty {
                    return t
                }
            }
            if let containerUrl = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: suite) {
                let fileUrl = containerUrl.appendingPathComponent("widget_active_target.txt")
                if let content = try? String(contentsOf: fileUrl, encoding: .utf8) {
                    let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
                    if !trimmed.isEmpty { return trimmed }
                }
            }
        }
        if let t = UserDefaults.standard.string(forKey: "widget_active_target"), !t.isEmpty {
            return t
        }
        return UserDefaults.standard.string(forKey: "pref_current_target")
    }

    private func loadActiveSection() -> String {
        let suites = getPossibleSuites()
        for suite in suites {
            if let sharedDefaults = UserDefaults(suiteName: suite) {
                if let s = sharedDefaults.string(forKey: "widget_active_section"), !s.isEmpty {
                    return s
                }
                if let s = sharedDefaults.string(forKey: "pref_current_section"), !s.isEmpty {
                    return s
                }
            }
            if let containerUrl = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: suite) {
                let fileUrl = containerUrl.appendingPathComponent("widget_active_section.txt")
                if let content = try? String(contentsOf: fileUrl, encoding: .utf8) {
                    let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
                    if !trimmed.isEmpty { return trimmed }
                }
            }
        }
        return UserDefaults.standard.string(forKey: "widget_active_section")
            ?? UserDefaults.standard.string(forKey: "pref_current_section")
            ?? "group"
    }

    private func createEntry(for date: Date, data: WidgetScheduleData? = nil) -> ScheduleWidgetEntry {
        let activeData = data ?? loadScheduleData()
        let rawTarget = (activeData?.target.isEmpty == false ? activeData?.target : nil) ?? loadActiveTarget() ?? ""
        let section = (activeData?.sectionType.isEmpty == false ? activeData?.sectionType : nil) ?? loadActiveSection()

        // Format entity title and symbol: NEVER "Sirinium"
        let displayTarget: String
        let iconName: String

        if rawTarget.isEmpty || rawTarget.caseInsensitiveCompare("Sirinium") == .orderedSame {
            displayTarget = "Расписание"
            iconName = "calendar"
        } else {
            switch section.lowercased() {
            case "classroom":
                displayTarget = rawTarget.lowercased().contains("ауд") ? rawTarget : "Ауд. \(rawTarget)"
                iconName = "building.2.fill"
            case "teacher":
                displayTarget = rawTarget
                iconName = "person.fill"
            default: // "group"
                displayTarget = rawTarget
                iconName = "person.2.fill"
            }
        }

        let allLessons = activeData?.lessons ?? []

        // Parse and sort all lessons chronologically
        let parsedLessons: [(lesson: WidgetLesson, start: Date, end: Date)] = allLessons.compactMap { lesson in
            guard let start = parseDateTime(dateStr: lesson.date, timeStr: lesson.startTime),
                  let end = parseDateTime(dateStr: lesson.date, timeStr: lesson.endTime) else {
                return nil
            }
            return (lesson, start, end)
        }.sorted { $0.start < $1.start }

        // Find currently ongoing lesson (date >= start && date < end)
        let ongoing = parsedLessons.first { date >= $0.start && date < $0.end }?.lesson

        // All future lessons starting strictly after `date`
        let future = parsedLessons.filter { $0.start > date }.map { $0.lesson }

        let nextLesson = future.first
        let subsequentLessons = Array(future.dropFirst().prefix(4))

        let state: LessonState
        if ongoing != nil {
            state = .ongoing
        } else if nextLesson != nil {
            state = .upcoming
        } else {
            state = .completed
        }

        return ScheduleWidgetEntry(
            date: date,
            displayTarget: displayTarget,
            targetIconName: iconName,
            sectionType: section,
            currentLesson: ongoing,
            nextLesson: nextLesson,
            upcomingLessons: subsequentLessons,
            state: state
        )
    }

    private func parseDateTime(dateStr: String, timeStr: String) -> Date? {
        let cleanDate = dateStr.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanTime = timeStr.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleanDate.isEmpty || cleanTime.isEmpty { return nil }

        let timeParts = cleanTime.split(separator: ":")
        guard timeParts.count >= 2 else { return nil }
        let h = timeParts[0].count == 1 ? "0" + timeParts[0] : String(timeParts[0])
        let m = timeParts[1].count == 1 ? "0" + timeParts[1] : String(timeParts[1])
        let normalizedTime = "\(h):\(m)"

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.timeZone = TimeZone.current

        if cleanDate.contains(".") {
            let parts = cleanDate.split(separator: ".")
            if parts.count == 3 {
                let d = parts[0].count == 1 ? "0" + parts[0] : String(parts[0])
                let month = parts[1].count == 1 ? "0" + parts[1] : String(parts[1])
                let y = String(parts[2])
                let normalizedDate = "\(d).\(month).\(y)"
                formatter.dateFormat = "dd.MM.yyyy HH:mm"
                return formatter.date(from: "\(normalizedDate) \(normalizedTime)")
            }
        } else if cleanDate.contains("-") {
            let parts = cleanDate.split(separator: "-")
            if parts.count == 3 {
                if parts[0].count == 4 {
                    let y = String(parts[0])
                    let month = parts[1].count == 1 ? "0" + parts[1] : String(parts[1])
                    let d = parts[2].count == 1 ? "0" + parts[2] : String(parts[2])
                    let normalizedDate = "\(y)-\(month)-\(d)"
                    formatter.dateFormat = "yyyy-MM-dd HH:mm"
                    return formatter.date(from: "\(normalizedDate) \(normalizedTime)")
                }
            }
        }

        formatter.dateFormat = "dd.MM.yyyy HH:mm"
        return formatter.date(from: "\(cleanDate) \(normalizedTime)")
    }
}

// MARK: - System Background Modifier
extension View {
    @ViewBuilder
    func widgetBackground() -> some View {
        if #available(iOSApplicationExtension 17.0, *) {
            self.containerBackground(for: .widget) {
                Color(uiColor: .secondarySystemGroupedBackground)
            }
        } else {
            self.background(Color(uiColor: .secondarySystemGroupedBackground))
        }
    }
}

// MARK: - Root Entry View
struct SiriniumScheduleWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    var entry: SiriniumTimelineProvider.Entry

    var body: some View {
        Group {
            switch family {
            case .systemSmall:
                SmallWidgetView(entry: entry)
            case .systemMedium:
                MediumWidgetView(entry: entry)
            default:
                SmallWidgetView(entry: entry)
            }
        }
        .widgetBackground()
        .widgetURL(URL(string: "sirinium://schedule"))
    }
}

// MARK: - Small Widget (Apple HIG Design)
private struct SmallWidgetView: View {
    let entry: ScheduleWidgetEntry

    var activeLesson: WidgetLesson? {
        entry.currentLesson ?? entry.nextLesson
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header: Entity Icon + Target & Indicator
            HStack(alignment: .center, spacing: 4) {
                Image(systemName: entry.targetIconName)
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(.secondary)

                Text(entry.displayTarget)
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)

                Spacer()

                if let lesson = activeLesson, lesson.numberPair > 0 {
                    Text("\(lesson.numberPair) пара")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }

                if entry.state == .ongoing {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 7, height: 7)
                } else if entry.state == .upcoming {
                    let arrowColor = activeLesson.map { lessonColor(for: $0.rawLessonType, discipline: $0.discipline) } ?? Color.blue
                    Image(systemName: "arrow.right")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(arrowColor)
                }
            }
            .padding(.bottom, 6)

            if let lesson = activeLesson {
                // Status / Relative Day + Time Range
                let dayLabel = relativeDayText(lesson.date, relativeTo: entry.date)
                let typeColor = lessonColor(for: lesson.rawLessonType, discipline: lesson.discipline)

                HStack(spacing: 4) {
                    if entry.state == .ongoing {
                        Text("СЕЙЧАС")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color.green)
                    } else if dayLabel != "Сегодня" {
                        Text(dayLabel.uppercased())
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(typeColor)
                    }

                    Text("\(lesson.startTime) – \(lesson.endTime)")
                        .font(.caption.monospacedDigit().weight(.semibold))
                        .foregroundStyle(typeColor)
                }
                .lineLimit(1)
                .padding(.bottom, 3)

                // Discipline Title
                Text(lesson.discipline)
                    .font(.system(.subheadline, design: .default, weight: .bold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer(minLength: 4)

                // Bottom Metadata: Entity Badges (Teacher, Room, Group)
                let badges = getLessonBadges(lesson: lesson, sectionType: entry.sectionType)
                HStack(spacing: 4) {
                    ForEach(badges) { badge in
                        HStack(spacing: 2) {
                            Image(systemName: badge.icon)
                                .font(.system(size: 7))
                            Text(badge.text)
                                .font(.caption2.weight(.semibold))
                                .lineLimit(1)
                        }
                        .foregroundStyle(.primary)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(Color(uiColor: .tertiarySystemFill))
                        .clipShape(Capsule())
                    }
                }
            } else {
                Spacer()
                VStack(alignment: .leading, spacing: 3) {
                    Image(systemName: "calendar.badge.checkmark")
                        .font(.system(size: 20))
                        .foregroundStyle(Color.blue)
                        .padding(.bottom, 2)

                    Text("Все пары завершены")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)

                    Text("Отдыхайте!")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
        }
        .padding(12)
    }
}

// MARK: - Medium Widget (Apple HIG Design)
private struct MediumWidgetView: View {
    let entry: ScheduleWidgetEntry

    var activeLesson: WidgetLesson? {
        entry.currentLesson ?? entry.nextLesson
    }

    // Subsequent lessons for right column
    var subsequentLessons: [WidgetLesson] {
        if entry.currentLesson != nil {
            var list: [WidgetLesson] = []
            if let next = entry.nextLesson {
                list.append(next)
            }
            list.append(contentsOf: entry.upcomingLessons)
            return Array(list.prefix(2))
        } else {
            return Array(entry.upcomingLessons.prefix(2))
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            // Left Column: Active / Next Lesson
            VStack(alignment: .leading, spacing: 0) {
                // Header: Entity Icon + Target & Indicator
                HStack(alignment: .center, spacing: 4) {
                    Image(systemName: entry.targetIconName)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(.secondary)

                    Text(entry.displayTarget)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)

                    Spacer()

                    if entry.state == .ongoing {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 7, height: 7)
                    } else if entry.state == .upcoming {
                        let arrowColor = activeLesson.map { lessonColor(for: $0.rawLessonType, discipline: $0.discipline) } ?? Color.blue
                        Image(systemName: "arrow.right")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundStyle(arrowColor)
                    }
                }
                .padding(.bottom, 6)

                if let lesson = activeLesson {
                    let dayLabel = relativeDayText(lesson.date, relativeTo: entry.date)
                    let typeColor = lessonColor(for: lesson.rawLessonType, discipline: lesson.discipline)

                    HStack(spacing: 4) {
                        if entry.state == .ongoing {
                            Text("СЕЙЧАС")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(Color.green)
                        } else if dayLabel != "Сегодня" {
                            Text(dayLabel.uppercased())
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(typeColor)
                        }

                        Text("\(lesson.startTime) – \(lesson.endTime)")
                            .font(.caption.monospacedDigit().weight(.semibold))
                            .foregroundStyle(typeColor)
                    }
                    .lineLimit(1)
                    .padding(.bottom, 3)

                    Text(lesson.discipline)
                        .font(.system(.subheadline, design: .default, weight: .bold))
                        .foregroundStyle(.primary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)

                    Spacer(minLength: 4)

                    let badges = getLessonBadges(lesson: lesson, sectionType: entry.sectionType)
                    HStack(spacing: 4) {
                        ForEach(badges) { badge in
                            HStack(spacing: 2) {
                                Image(systemName: badge.icon)
                                    .font(.system(size: 7))
                                Text(badge.text)
                                    .font(.caption2.weight(.semibold))
                                    .lineLimit(1)
                            }
                            .foregroundStyle(.primary)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(Color(uiColor: .tertiarySystemFill))
                            .clipShape(Capsule())
                        }

                        if !lesson.rawLessonType.isEmpty {
                            Text(lesson.rawLessonType)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                    }
                } else {
                    Spacer()
                    VStack(alignment: .leading, spacing: 2) {
                        Image(systemName: "calendar.badge.checkmark")
                            .font(.system(size: 18))
                            .foregroundStyle(Color.blue)
                        Text("Пар нет")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.primary)
                        Text("Отличного отдыха!")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Divider()

            // Right Column: Subsequent upcoming lessons
            VStack(alignment: .leading, spacing: 0) {
                let list = subsequentLessons

                if !list.isEmpty {
                    let firstItem = list[0]
                    let dayHeader = relativeDayText(firstItem.date, relativeTo: entry.date)
                    let headerTitle = (dayHeader == "Сегодня" ? "ДАЛЕЕ" : dayHeader).uppercased()

                    Text(headerTitle)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                        .padding(.bottom, 6)

                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(list) { item in
                            let itemColor = lessonColor(for: item.rawLessonType, discipline: item.discipline)
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(alignment: .center) {
                                    Text(item.startTime)
                                        .font(.caption.monospacedDigit().weight(.semibold))
                                        .foregroundStyle(itemColor)

                                    let itemDay = relativeDayText(item.date, relativeTo: entry.date)
                                    if itemDay != dayHeader && itemDay != "Сегодня" {
                                        Text(itemDay.uppercased())
                                            .font(.system(size: 9, weight: .bold))
                                            .foregroundStyle(itemColor)
                                    }

                                    Spacer()

                                    let itemBadges = getLessonBadges(lesson: item, sectionType: entry.sectionType)
                                    if !itemBadges.isEmpty {
                                        Text(itemBadges.map { $0.text }.joined(separator: " • "))
                                            .font(.caption2.weight(.medium))
                                            .foregroundStyle(.secondary)
                                            .lineLimit(1)
                                    }
                                }

                                Text(item.discipline)
                                    .font(.caption.weight(.regular))
                                    .foregroundStyle(.primary)
                                    .lineLimit(1)
                            }
                        }
                    }

                    Spacer(minLength: 0)
                } else {
                    Text("ДАЛЕЕ")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                        .padding(.bottom, 6)

                    Spacer()

                    Text("Больше пар нет")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Spacer()
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
    }
}

// MARK: - Widget Configurations
@available(iOSApplicationExtension 17.0, *)
struct SiriniumScheduleWidget17: Widget {
    let kind: String = "SiriniumScheduleWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SiriniumTimelineProvider()) { entry in
            SiriniumScheduleWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Расписание занятий")
        .description("Текущая и ближайшие пары для вашей группы, преподавателя или аудитории.")
        .supportedFamilies([.systemSmall, .systemMedium])
        .contentMarginsDisabled()
    }
}

struct SiriniumScheduleWidget16: Widget {
    let kind: String = "SiriniumScheduleWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SiriniumTimelineProvider()) { entry in
            SiriniumScheduleWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Расписание занятий")
        .description("Текущая и ближайшие пары для вашей группы, преподавателя или аудитории.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Bundles
@available(iOSApplicationExtension 17.0, *)
struct SiriniumWidgetBundle17: WidgetBundle {
    var body: some Widget {
        SiriniumScheduleWidget17()
    }
}

struct SiriniumWidgetBundle16: WidgetBundle {
    var body: some Widget {
        SiriniumScheduleWidget16()
    }
}

// MARK: - Entry Point
@main
struct SiriniumWidgetLauncher {
    static func main() {
        if #available(iOSApplicationExtension 17.0, *) {
            SiriniumWidgetBundle17.main()
        } else {
            SiriniumWidgetBundle16.main()
        }
    }
}
