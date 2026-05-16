package ro.church_office.teamleaf.web;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ro.church_office.info.events.EventDTO;
import ro.church_office.info.events.Priority;
import ro.church_office.info.events.RecurrenceType;
import ro.church_office.info.events.DAO.EventService;

@Controller
public class CalendarWebController {

    private static final Locale RO_LOCALE = Locale.forLanguageTag("ro");

    private final EventService eventService;

    public CalendarWebController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/calendar")
    public String index(@RequestParam(value = "year", required = false) Integer year,
                        @RequestParam(value = "month", required = false) Integer month,
                        Model model) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = resolveMonth(year, month, today);
        LocalDate gridStart = currentMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = currentMonth.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<CalendarOccurrence> occurrences = eventService.getAllEvents().stream()
                .flatMap(event -> expandOccurrences(event, gridStart, gridEnd).stream())
                .sorted(Comparator
                        .comparing(CalendarOccurrence::date)
                        .thenComparingInt(item -> priorityOrder(item.event()))
                        .thenComparing(item -> safe(item.event().getEventName()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("monthLabel", monthLabel(currentMonth));
        model.addAttribute("weeks", buildWeeks(currentMonth, today, gridStart, gridEnd, occurrences));
        model.addAttribute("weekDays", List.of("Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă", "Duminică"));
        model.addAttribute("prevYear", currentMonth.minusMonths(1).getYear());
        model.addAttribute("prevMonth", currentMonth.minusMonths(1).getMonthValue());
        model.addAttribute("nextYear", currentMonth.plusMonths(1).getYear());
        model.addAttribute("nextMonth", currentMonth.plusMonths(1).getMonthValue());
        model.addAttribute("todayYear", today.getYear());
        model.addAttribute("todayMonth", today.getMonthValue());
        return "calendar/index";
    }

    private YearMonth resolveMonth(Integer year, Integer month, LocalDate today) {
        int resolvedYear = year == null ? today.getYear() : year;
        int resolvedMonth = month == null ? today.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            resolvedMonth = today.getMonthValue();
        }
        return YearMonth.of(resolvedYear, resolvedMonth);
    }

    private List<CalendarWeek> buildWeeks(YearMonth currentMonth,
                                          LocalDate today,
                                          LocalDate gridStart,
                                          LocalDate gridEnd,
                                          List<CalendarOccurrence> occurrences) {
        List<CalendarWeek> weeks = new ArrayList<>();
        LocalDate cursor = gridStart;
        while (!cursor.isAfter(gridEnd)) {
            List<CalendarDay> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate day = cursor;
                List<CalendarOccurrence> dayEvents = occurrences.stream()
                        .filter(item -> item.date().equals(day))
                        .toList();
                days.add(new CalendarDay(day, day.getMonthValue() == currentMonth.getMonthValue(), day.equals(today), dayEvents));
                cursor = cursor.plusDays(1);
            }
            weeks.add(new CalendarWeek(days));
        }
        return weeks;
    }

    private List<CalendarOccurrence> expandOccurrences(EventDTO event, LocalDate rangeStart, LocalDate rangeEnd) {
        List<CalendarOccurrence> occurrences = new ArrayList<>();
        LocalDate start = event.getEndDate() != null ? event.getEndDate() : event.getOpenDate();
        if (start == null) {
            return occurrences;
        }

        RecurrenceType recurrenceType = RecurrenceType.from(event.getRecurrenceType());
        if (recurrenceType == RecurrenceType.NONE) {
            addIfVisible(occurrences, event, start, rangeStart, rangeEnd);
            return occurrences;
        }

        int interval = event.getRecurrenceInterval() == null || event.getRecurrenceInterval() < 1
                ? 1
                : event.getRecurrenceInterval();
        LocalDate limit = event.getRecurrenceUntil() == null ? rangeEnd : min(event.getRecurrenceUntil(), rangeEnd);
        LocalDate cursor = start;
        int safety = 0;
        while (!cursor.isAfter(limit) && safety < 500) {
            addIfVisible(occurrences, event, cursor, rangeStart, rangeEnd);
            cursor = nextOccurrence(cursor, recurrenceType, interval);
            safety++;
        }
        return occurrences;
    }

    private void addIfVisible(List<CalendarOccurrence> occurrences,
                              EventDTO event,
                              LocalDate occurrenceDate,
                              LocalDate rangeStart,
                              LocalDate rangeEnd) {
        if (!occurrenceDate.isBefore(rangeStart) && !occurrenceDate.isAfter(rangeEnd)) {
            occurrences.add(new CalendarOccurrence(occurrenceDate, event));
        }
    }

    private LocalDate nextOccurrence(LocalDate current, RecurrenceType type, int interval) {
        return switch (type) {
            case DAILY -> current.plusDays(interval);
            case WEEKLY -> current.plusWeeks(interval);
            case MONTHLY -> current.plusMonths(interval);
            case YEARLY -> current.plusYears(interval);
            default -> current.plusYears(100);
        };
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private int priorityOrder(EventDTO event) {
        Priority priority = Priority.from(event == null ? null : event.getPriority());
        return priority == null ? Priority.MEDIUM.getValue() : priority.getValue();
    }

    private String monthLabel(YearMonth month) {
        String display = month.getMonth().getDisplayName(TextStyle.FULL, RO_LOCALE);
        return display.substring(0, 1).toUpperCase(RO_LOCALE) + display.substring(1) + " " + month.getYear();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record CalendarWeek(List<CalendarDay> days) {}

    public record CalendarDay(LocalDate date, boolean inCurrentMonth, boolean today, List<CalendarOccurrence> events) {}

    public record CalendarOccurrence(LocalDate date, EventDTO event) {}
}
