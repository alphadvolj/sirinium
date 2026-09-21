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
    let upcomingLessonsToday: [WidgetLesson]
    let state: LessonState
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
            upcomingLessonsToday: [],
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
        let todayLessons = filterTodayLessons(data: data, for: now)

        var entries: [ScheduleWidgetEntry] = []
        entries.append(createEntry(for: now, data: data, todayLessons: todayLessons))

        // Create timeline transition points for each lesson start & end today
        for lesson in todayLessons {
            if let startDate = parseDateTime(dateStr: lesson.date, timeStr: lesson.startTime), startDate > now {
                entries.append(createEntry(for: startDate, data: data, todayLessons: todayLessons))
            }
            if let endDate = parseDateTime(dateStr: lesson.date, timeStr: lesson.endTime), endDate > now {
                entries.append(createEntry(for: endDate, data: data, todayLessons: todayLessons))
            }
        }

        // Also add transition points for earliest upcoming future lessons
        let futureLessons = (data?.lessons ?? []).compactMap { lesson -> (WidgetLesson, Date)? in
            guard let start = parseDateTime(dateStr: lesson.date, timeStr: lesson.startTime), start > now else { return nil }
            return (lesson, start)
        }.sorted { $0.1 < $1.1 }.prefix(4)

        for (_, startDate) in futureLessons {
            entries.append(createEntry(for: startDate, data: data))
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

    private func filterTodayLessons(data: WidgetScheduleData?, for date: Date) -> [WidgetLesson] {
        guard let data = data else { return [] }
        let calendar = Calendar.current
        return data.lessons.filter { lesson in
            guard let lessonDate = parseDateTime(dateStr: lesson.date, timeStr: lesson.startTime) else { return false }
            return calendar.isDate(lessonDate, inSameDayAs: date)
        }.sorted { $0.startTime < $1.startTime }
    }

    private func createEntry(for date: Date, data: WidgetScheduleData? = nil, todayLessons: [WidgetLesson]? = nil) -> ScheduleWidgetEntry {
        let activeData = data ?? loadScheduleData()
        let target = activeData?.target.isEmpty == false ? activeData!.target : "Sirinium"
        let lessons = todayLessons ?? filterTodayLessons(data: activeData, for: date)

        var ongoing: WidgetLesson? = nil
        var upcoming: WidgetLesson? = nil
        var laterLessons: [WidgetLesson] = []

        for lesson in lessons {
            guard let startDate = parseDateTime(dateStr: lesson.date, timeStr: lesson.startTime),
                  let endDate = parseDateTime(dateStr: lesson.date, timeStr: lesson.endTime) else {
                continue
            }

            if date >= startDate && date < endDate {
                ongoing = lesson
            } else if date < startDate {
                if upcoming == nil {
                    upcoming = lesson
                } else {
                    laterLessons.append(lesson)
                }
            }
        }

        // If no ongoing and no upcoming found today, search across all available lessons for future lessons
        if ongoing == nil && upcoming == nil, let allLessons = activeData?.lessons {
            let future = allLessons.compactMap { lesson -> (WidgetLesson, Date)? in
                guard let start = parseDateTime(dateStr: lesson.date, timeStr: lesson.startTime) else { return nil }
                return start > date ? (lesson, start) : nil
            }.sorted { $0.1 < $1.1 }

            if let firstFuture = future.first {
                upcoming = firstFuture.0
                laterLessons = future.dropFirst().prefix(3).map { $0.0 }
            }
        }

        let state: LessonState
        if ongoing != nil {
            state = .ongoing
        } else if upcoming != nil {
            state = .upcoming
        } else {
            state = .completed
        }

        return ScheduleWidgetEntry(
            date: date,
            target: target,
            currentLesson: ongoing,
            nextLesson: upcoming,
            upcomingLessonsToday: laterLessons,
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

extension View {
    @ViewBuilder
    func widgetBackground(_ color: Color) -> some View {
        if #available(iOS 17.0, *) {
            self.containerBackground(color, for: .widget)
        } else {
            self.background(color)
        }
    }
}

// MARK: - Widget Views
struct SiriniumScheduleWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    var entry: SiriniumTimelineProvider.Entry

    private let bgDark = Color(red: 15/255, green: 23/255, blue: 42/255)       // #0F172A
    private let cardDark = Color(red: 30/255, green: 41/255, blue: 59/255)     // #1E293B
    private let accentIndigo = Color(red: 129/255, green: 140/255, blue: 248/255) // #818CF8
    private let accentTeal = Color(red: 45/255, green: 212/255, blue: 191/255)  // #2DD4BF

    var body: some View {
        Group {
            switch family {
            case .systemSmall:
                SmallWidgetView(entry: entry, bgDark: bgDark, cardDark: cardDark, accentIndigo: accentIndigo, accentTeal: accentTeal)
            case .systemMedium:
                MediumWidgetView(entry: entry, bgDark: bgDark, cardDark: cardDark, accentIndigo: accentIndigo, accentTeal: accentTeal)
            default:
                SmallWidgetView(entry: entry, bgDark: bgDark, cardDark: cardDark, accentIndigo: accentIndigo, accentTeal: accentTeal)
            }
        }
        .widgetBackground(bgDark)
        .widgetURL(URL(string: "sirinium://schedule"))
    }
}

// MARK: - Small Widget (Compact)
private struct SmallWidgetView: View {
    let entry: ScheduleWidgetEntry
    let bgDark: Color
    let cardDark: Color
    let accentIndigo: Color
    let accentTeal: Color

    var activeLesson: WidgetLesson? {
        entry.currentLesson ?? entry.nextLesson
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Header: Target & Minimal State Indicator (Circle for current, arrow for next)
            HStack(alignment: .center) {
                Text(entry.target)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.85))
                    .lineLimit(1)

                Spacer()

                if entry.state == .ongoing {
                    Circle()
                        .fill(accentTeal)
                        .frame(width: 8, height: 8)
                } else if entry.state == .upcoming {
                    Image(systemName: "arrow.right")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(accentIndigo)
                }
            }

            Spacer(minLength: 2)

            if let lesson = activeLesson {
                // Time & Pair Number
                HStack(spacing: 4) {
                    Text("\(lesson.startTime) – \(lesson.endTime)")
                        .font(.system(size: 11, weight: .semibold, design: .monospaced))
                        .foregroundStyle(accentIndigo)

                    if lesson.numberPair > 0 {
                        Text("• \(lesson.numberPair) пара")
                            .font(.system(size: 10, weight: .regular))
                            .foregroundStyle(.white.opacity(0.6))
                    }
                }

                // Discipline Title
                Text(lesson.discipline)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer(minLength: 2)

                // Classroom & Teacher
                HStack(spacing: 6) {
                    if !lesson.classroom.isEmpty {
                        HStack(spacing: 3) {
                            Image(systemName: "location.fill")
                                .font(.system(size: 9))
                                .foregroundStyle(accentTeal)
                            Text(lesson.classroom)
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                        }
                    }

                    if !lesson.teacher.isEmpty {
                        Text(lesson.teacher)
                            .font(.system(size: 9, weight: .regular))
                            .foregroundStyle(.white.opacity(0.65))
                            .lineLimit(1)
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Все пары завершены 🎉")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white)

                    Text("На сегодня занятий больше нет")
                        .font(.system(size: 10, weight: .regular))
                        .foregroundStyle(.white.opacity(0.6))
                }
                Spacer(minLength: 2)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
    }
}

// MARK: - Medium Widget (Split view)
private struct MediumWidgetView: View {
    let entry: ScheduleWidgetEntry
    let bgDark: Color
    let cardDark: Color
    let accentIndigo: Color
    let accentTeal: Color

    var activeLesson: WidgetLesson? {
        entry.currentLesson ?? entry.nextLesson
    }

    var body: some View {
        HStack(spacing: 10) {
            // Left Column: Active / Next Lesson Hero Card
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .center) {
                    Text(entry.target)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white)

                    Spacer()

                    if entry.state == .ongoing {
                        Circle()
                            .fill(accentTeal)
                            .frame(width: 8, height: 8)
                    } else if entry.state == .upcoming {
                        Image(systemName: "arrow.right")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(accentIndigo)
                    }
                }

                Spacer(minLength: 2)

                if let lesson = activeLesson {
                    Text("\(lesson.startTime) – \(lesson.endTime)")
                        .font(.system(size: 11, weight: .semibold, design: .monospaced))
                        .foregroundStyle(accentIndigo)

                    Text(lesson.discipline)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white)
                        .lineLimit(2)

                    HStack(spacing: 6) {
                        if !lesson.classroom.isEmpty {
                            Text("Ауд. \(lesson.classroom)")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(accentTeal)
                                .padding(.horizontal, 5)
                                .padding(.vertical, 2)
                                .background(cardDark)
                                .clipShape(RoundedRectangle(cornerRadius: 5))
                        }
                        if !lesson.rawLessonType.isEmpty {
                            Text(lesson.rawLessonType)
                                .font(.system(size: 9, weight: .regular))
                                .foregroundStyle(.white.opacity(0.7))
                        }
                    }
                } else {
                    Text("Пар на сегодня нет")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white)
                    Text("Отличного дня и отдыха!")
                        .font(.system(size: 10, weight: .regular))
                        .foregroundStyle(.white.opacity(0.6))
                }

                Spacer(minLength: 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Divider()
                .background(Color.white.opacity(0.15))

            // Right Column: Subsequent lessons
            VStack(alignment: .leading, spacing: 4) {
                Text("ДАЛЕЕ")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.5))

                if !entry.upcomingLessonsToday.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        ForEach(entry.upcomingLessonsToday.prefix(2)) { lesson in
                            VStack(alignment: .leading, spacing: 1) {
                                HStack {
                                    Text(lesson.startTime)
                                        .font(.system(size: 10, weight: .semibold, design: .monospaced))
                                        .foregroundStyle(accentIndigo)
                                    Spacer()
                                    if !lesson.classroom.isEmpty {
                                        Text(lesson.classroom)
                                            .font(.system(size: 9, weight: .regular))
                                            .foregroundStyle(.white.opacity(0.65))
                                    }
                                }
                                Text(lesson.discipline)
                                    .font(.system(size: 11, weight: .regular))
                                    .foregroundStyle(.white)
                                    .lineLimit(1)
                            }
                        }
                    }
                } else {
                    Spacer(minLength: 2)
                    Text("Больше пар нет")
                        .font(.system(size: 10, weight: .regular))
                        .foregroundStyle(.white.opacity(0.45))
                    Spacer(minLength: 2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
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
