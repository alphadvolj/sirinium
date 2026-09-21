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
    let rawLessonType: String
    let numberPair: Int
    let date: String
}

enum LessonState {
    case ongoing
    case upcoming
    case completed
}

struct ScheduleWidgetEntry: TimelineEntry {
    let date: Date
    let target: String
    let currentLesson: WidgetLesson?
    let nextLesson: WidgetLesson?
    let upcomingLessons: [WidgetLesson]
    let state: LessonState
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

// MARK: - Timeline Provider
struct SiriniumTimelineProvider: TimelineProvider {
    typealias Entry = ScheduleWidgetEntry

    func placeholder(in context: Context) -> ScheduleWidgetEntry {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        let todayStr = formatter.string(from: Date())
        return ScheduleWidgetEntry(
            date: Date(),
            target: "К1609-241",
            currentLesson: WidgetLesson(
                id: "1",
                discipline: "Высшая математика",
                startTime: "08:45",
                endTime: "10:20",
                classroom: "302",
                teacher: "Иванов И.И.",
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
    private func loadScheduleData() -> WidgetScheduleData? {
        let sharedDefaults = UserDefaults(suiteName: "group.com.dlab.sirinium")
        let jsonString = sharedDefaults?.string(forKey: "widget_schedule_data")
            ?? UserDefaults.standard.string(forKey: "widget_schedule_data")

        guard let jsonString = jsonString, let jsonData = jsonString.data(using: .utf8) else {
            return nil
        }
        return try? JSONDecoder().decode(WidgetScheduleData.self, from: jsonData)
    }

    private func createEntry(for date: Date, data: WidgetScheduleData? = nil) -> ScheduleWidgetEntry {
        let activeData = data ?? loadScheduleData()
        let target = activeData?.target.isEmpty == false ? activeData!.target : "Sirinium"
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
            target: target,
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

        var normalizedTime = cleanTime
        if let colonIndex = cleanTime.firstIndex(of: ":") {
            let hourPart = cleanTime[..<colonIndex]
            if hourPart.count == 1 {
                normalizedTime = "0" + cleanTime
            }
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.timeZone = TimeZone.current

        if cleanDate.contains(".") {
            let parts = cleanDate.split(separator: ".")
            if parts.count == 3 {
                let d = parts[0].count == 1 ? "0" + parts[0] : String(parts[0])
                let m = parts[1].count == 1 ? "0" + parts[1] : String(parts[1])
                let y = String(parts[2])
                let normalizedDate = "\(d).\(m).\(y)"
                formatter.dateFormat = "dd.MM.yyyy HH:mm"
                return formatter.date(from: "\(normalizedDate) \(normalizedTime)")
            }
        } else if cleanDate.contains("-") {
            let parts = cleanDate.split(separator: "-")
            if parts.count == 3 {
                if parts[0].count == 4 {
                    let y = String(parts[0])
                    let m = parts[1].count == 1 ? "0" + parts[1] : String(parts[1])
                    let d = parts[2].count == 1 ? "0" + parts[2] : String(parts[2])
                    let normalizedDate = "\(y)-\(m)-\(d)"
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
            // Header: Target & Indicator
            HStack(alignment: .center) {
                Text(entry.target)
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)

                Spacer()

                if entry.state == .ongoing {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 7, height: 7)
                } else if entry.state == .upcoming {
                    Image(systemName: "arrow.right")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(Color.blue)
                }
            }
            .padding(.bottom, 6)

            if let lesson = activeLesson {
                // Status / Relative Day + Time Range
                let dayLabel = relativeDayText(lesson.date, relativeTo: entry.date)
                HStack(spacing: 4) {
                    if entry.state == .ongoing {
                        Text("СЕЙЧАС")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color.green)
                    } else if dayLabel != "Сегодня" {
                        Text(dayLabel.uppercased())
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color.blue)
                    }

                    Text("\(lesson.startTime) – \(lesson.endTime)")
                        .font(.caption.monospacedDigit().weight(.medium))
                        .foregroundStyle(.secondary)
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

                // Bottom Metadata: Room & Pair Number / Type
                HStack(spacing: 6) {
                    if !lesson.classroom.isEmpty {
                        HStack(spacing: 2) {
                            Image(systemName: "location.fill")
                                .font(.system(size: 7))
                            Text(lesson.classroom)
                                .font(.caption2.weight(.semibold))
                        }
                        .foregroundStyle(.primary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color(uiColor: .tertiarySystemFill))
                        .clipShape(Capsule())
                    }

                    if lesson.numberPair > 0 {
                        Text("\(lesson.numberPair) пара")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    } else if !lesson.rawLessonType.isEmpty {
                        Text(lesson.rawLessonType)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
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
                // Header: Target & Indicator
                HStack(alignment: .center) {
                    Text(entry.target)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)

                    Spacer()

                    if entry.state == .ongoing {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 7, height: 7)
                    } else if entry.state == .upcoming {
                        Image(systemName: "arrow.right")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundStyle(Color.blue)
                    }
                }
                .padding(.bottom, 6)

                if let lesson = activeLesson {
                    let dayLabel = relativeDayText(lesson.date, relativeTo: entry.date)
                    HStack(spacing: 4) {
                        if entry.state == .ongoing {
                            Text("СЕЙЧАС")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(Color.green)
                        } else if dayLabel != "Сегодня" {
                            Text(dayLabel.uppercased())
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(Color.blue)
                        }

                        Text("\(lesson.startTime) – \(lesson.endTime)")
                            .font(.caption.monospacedDigit().weight(.medium))
                            .foregroundStyle(.secondary)
                    }
                    .lineLimit(1)
                    .padding(.bottom, 3)

                    Text(lesson.discipline)
                        .font(.system(.subheadline, design: .default, weight: .bold))
                        .foregroundStyle(.primary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)

                    Spacer(minLength: 4)

                    HStack(spacing: 6) {
                        if !lesson.classroom.isEmpty {
                            HStack(spacing: 2) {
                                Image(systemName: "location.fill")
                                    .font(.system(size: 7))
                                Text("Ауд. \(lesson.classroom)")
                                    .font(.caption2.weight(.semibold))
                            }
                            .foregroundStyle(.primary)
                            .padding(.horizontal, 6)
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
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(alignment: .center) {
                                    Text(item.startTime)
                                        .font(.caption.monospacedDigit().weight(.semibold))
                                        .foregroundStyle(Color.blue)

                                    let itemDay = relativeDayText(item.date, relativeTo: entry.date)
                                    if itemDay != dayHeader && itemDay != "Сегодня" {
                                        Text(itemDay)
                                            .font(.caption2)
                                            .foregroundStyle(.secondary)
                                    }

                                    Spacer()

                                    if !item.classroom.isEmpty {
                                        Text(item.classroom)
                                            .font(.caption2.weight(.medium))
                                            .foregroundStyle(.secondary)
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
