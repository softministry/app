package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.events.EventDTO;
import ro.church_office.info.events.DAO.Event;
import ro.church_office.info.events.DAO.EventRepository;
import ro.church_office.info.events.DAO.EventService;
import ro.church_office.info.events.DAO.EventTaskRepository;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.service.PersonService;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventWebControllerTest {
    @Test
    void createAllowsEventWithoutImplementedBy() {
        EventService eventService = mock(EventService.class);
        Event saved = new Event();
        saved.setId(42L);
        when(eventService.saveEvent(any(EventDTO.class))).thenReturn(saved);

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        EventWebController controller = new EventWebController(
                eventService,
                mock(PersonService.class),
                mock(EventRepository.class),
                mock(EventTaskRepository.class),
                mock(PersonRepository.class),
                mock(ChurchContextService.class),
                mock(ChurchInfoService.class),
                mock(ChurchGroupRepository.class),
                mock(GlobalSettingRepository.class));

        EventDTO dto = new EventDTO();
        dto.setEventName("Eveniment fara responsabil");
        dto.setStatus("PLANNED");
        dto.setEventType("OTHER");
        dto.setOpenDate(LocalDate.of(2026, 6, 1));
        dto.setRecurrenceType("NONE");

        String view = controller.create(
                dto,
                bindingResult,
                "",
                new ExtendedModelMap(),
                new RedirectAttributesModelMap());

        assertEquals("redirect:/events/42/edit", view);
        verify(eventService).saveEvent(any(EventDTO.class));
    }
}
