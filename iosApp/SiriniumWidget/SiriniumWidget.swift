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
        let formatterDot = DateFormatter()
        formatterDot.dateFormat = "dd.MM.yyyy"
        formatterDot.locale = Locale(identifier: "ru_RU")
        formatterDot.timeZone = TimeZone.current
        let todayDot = formatterDot.string(from: date)

        let formatterDash = DateFormatter()
        formatterDash.dateFormat = "yyyy-MM-dd"
        formatterDash.locale = Locale(identifier: "ru_RU")
        formatterDash.timeZone = TimeZone.current
        let todayDash = formatterDash.string(from: date)

        return data.lessons.filter { $0.date == todayDot || $0.date == todayDash }
            .sorted { $0.startTime < $1.startTime }
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
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.timeZone = TimeZone.current

        if cleanDate.contains(".") {
            formatter.dateFormat = "dd.MM.yyyy HH:mm"
        } else {
            formatter.dateFormat = "yyyy-MM-dd HH:mm"
        }
        return formatter.date(from: "\(cleanDate) \(cleanTime)")
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
        VStack(alignment: .leading, spacing: 6) {
            // Header: Target & Status Badge
            HStack {
                Text(entry.target)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.white.opacity(0.85))
                    .lineLimit(1)

                Spacer()

                HStack(spacing: 4) {
                    Circle()
                        .fill(entry.state == .ongoing ? accentTeal : accentIndigo)
                        .frame(width: 6, height: 6)
                    Text(entry.state == .ongoing ? "СЕЙЧАС" : (entry.state == .upcoming ? "СКОРО" : "ОТДЫХ"))
                        .font(.system(size: 8, weight: .black))
                        .foregroundStyle(entry.state == .ongoing ? accentTeal : accentIndigo)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 3)
                .background(cardDark)
                .clipShape(Capsule())
            }

            Spacer()

            if let lesson = activeLesson {
                // Time & Pair Number
                HStack(spacing: 4) {
                    Text("\(lesson.startTime) – \(lesson.endTime)")
                        .font(.system(size: 11, weight: .semibold, design: .monospaced))
                        .foregroundStyle(accentIndigo)

                    if lesson.numberPair > 0 {
                        Text("• \(lesson.numberPair) пара")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(.white.opacity(0.6))
                    }
                }

                // Discipline Title
                Text(lesson.discipline)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer()

                // Classroom & Teacher
                HStack(spacing: 8) {
                    if !lesson.classroom.isEmpty {
                        HStack(spacing: 3) {
                            Image(systemName: "location.fill")
                                .font(.system(size: 9))
                                .foregroundStyle(accentTeal)
                            Text(lesson.classroom)
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                        }
                    }

                    if !lesson.teacher.isEmpty {
                        Text(lesson.teacher)
                            .font(.system(size: 9, weight: .medium))
                            .foregroundStyle(.white.opacity(0.65))
                            .lineLimit(1)
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Все пары завершены 🎉")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)

                    Text("На сегодня занятий больше нет")
                        .font(.system(size: 10))
                        .foregroundStyle(.white.opacity(0.6))
                }
                Spacer()
            }
        }
        .padding(12)
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
        HStack(spacing: 12) {
            // Left Column: Active / Next Lesson Hero Card
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(entry.target)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)

                    Spacer()

                    HStack(spacing: 4) {
                        Circle()
                            .fill(entry.state == .ongoing ? accentTeal : accentIndigo)
                            .frame(width: 6, height: 6)
                        Text(entry.state == .ongoing ? "ИДЁТ ПАРА" : (entry.state == .upcoming ? "СЛЕДУЮЩАЯ" : "ГОТОВО"))
                            .font(.system(size: 8, weight: .heavy))
                            .foregroundStyle(entry.state == .ongoing ? accentTeal : accentIndigo)
                    }
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(cardDark)
                    .clipShape(Capsule())
                }

                Spacer()

                if let lesson = activeLesson {
                    Text("\(lesson.startTime) – \(lesson.endTime)")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundStyle(accentIndigo)

                    Text(lesson.discipline)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(2)

                    HStack(spacing: 6) {
                        if !lesson.classroom.isEmpty {
                            Text("Ауд. \(lesson.classroom)")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(accentTeal)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(cardDark)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                        if !lesson.rawLessonType.isEmpty {
                            Text(lesson.rawLessonType)
                                .font(.system(size: 9, weight: .medium))
                                .foregroundStyle(.white.opacity(0.7))
                        }
                    }
                } else {
                    Text("Пар на сегодня нет")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)
                    Text("Отличного дня и отдыха!")
                        .font(.system(size: 11))
                        .foregroundStyle(.white.opacity(0.6))
                }

                Spacer()
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Divider()
                .background(Color.white.opacity(0.15))

            // Right Column: Subsequent lessons
            VStack(alignment: .leading, spacing: 6) {
                Text("ДАЛЕЕ СЕГОДНЯ")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(.white.opacity(0.5))

                if !entry.upcomingLessonsToday.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(entry.upcomingLessonsToday.prefix(2)) { lesson in
                            VStack(alignment: .leading, spacing: 2) {
                                HStack {
                                    Text(lesson.startTime)
                                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                                        .foregroundStyle(accentIndigo)
                                    Spacer()
                                    if !lesson.classroom.isEmpty {
                                        Text(lesson.classroom)
                                            .font(.system(size: 9, weight: .medium))
                                            .foregroundStyle(.white.opacity(0.65))
                                    }
                                }
                                Text(lesson.discipline)
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundStyle(.white)
                                    .lineLimit(1)
                            }
                        }
                    }
                } else {
                    Spacer()
                    Text("Больше пар нет")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(.white.opacity(0.45))
                    Spacer()
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
    }
}

// MARK: - Widget Configuration
struct SiriniumScheduleWidget: Widget {
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

// MARK: - Bundle
@main
struct SiriniumWidgetBundle: WidgetBundle {
    var body: some Widget {
        SiriniumScheduleWidget()
    }
}
