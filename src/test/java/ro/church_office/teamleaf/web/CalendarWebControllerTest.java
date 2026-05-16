package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import ro.church_office.info.events.DAO.EventService;
import ro.church_office.info.events.EventDTO;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarWebControllerTest {

    @Test
    void buildsCalendarWithRecurringEventsAndNavigation() {
        EventService eventService = mock(EventService.class);
        CalendarWebController controller = new CalendarWebController(eventService);

        EventDTO weekly = new EventDTO();
        weekly.setEventName("Repetitie cor");
        weekly.setEndDate(LocalDate.of(2026, 5, 5));
        weekly.setRecurrenceType("WEEKLY");
        weekly.setRecurrenceInterval(1);
        weekly.setRecurrenceUntil(LocalDate.of(2026, 5, 31));

        EventDTO oneOff = new EventDTO();
        oneOff.setEventName("Conferinta tineret");
        oneOff.setEndDate(LocalDate.of(2026, 5, 10));
        oneOff.setRecurrenceType("NONE");

        when(eventService.getAllEvents()).thenReturn(List.of(weekly, oneOff));

        Model model = new ExtendedModelMap();
        String view = controller.index(2026, 5, model);

        assertEquals("calendar/index", view);
        assertEquals("Mai 2026", model.getAttribute("monthLabel"));
        assertEquals(2026, model.getAttribute("prevYear"));
        assertEquals(4, model.getAttribute("prevMonth"));
        assertEquals(2026, model.getAttribute("nextYear"));
        assertEquals(6, model.getAttribute("nextMonth"));

        @SuppressWarnings("unchecked")
        List<String> weekDays = (List<String>) model.getAttribute("weekDays");
        assertEquals(List.of("Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă", "Duminică"), weekDays);

        @SuppressWarnings("unchecked")
        List<CalendarWebController.CalendarWeek> weeks =
                (List<CalendarWebController.CalendarWeek>) model.getAttribute("weeks");
        assertNotNull(weeks);
        assertFalse(weeks.isEmpty());

        long totalOccurrences = weeks.stream()
                .flatMap(week -> week.days().stream())
                .flatMap(day -> day.events().stream())
                .count();
        assertEquals(5, totalOccurrences);

        boolean hasMayTenEvent = weeks.stream()
                .flatMap(week -> week.days().stream())
                .anyMatch(day -> day.date().equals(LocalDate.of(2026, 5, 10)) && !day.events().isEmpty());
        assertTrue(hasMayTenEvent);
    }
}
