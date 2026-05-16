package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ro.church_office.info.attendance.AttendanceRecordRepository;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.followup.PastoralFollowUpRepository;
import ro.church_office.info.followup.PastoralPrivateNoteRepository;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;
import ro.church_office.info.person.service.PersonService;
import ro.church_office.info.visits.dao.VisitRepository;
import ro.church_office.teamleaf.security.CurrentUserService;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonWebControllerTest {

    private final PersonRepository personRepository = mock(PersonRepository.class);
    private final ChurchContextService churchContextService = mock(ChurchContextService.class);
    private final PersonService personService = mock(PersonService.class);
    private final ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);
    private final PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
    private final PastoralPrivateNoteRepository privateNoteRepository = mock(PastoralPrivateNoteRepository.class);
    private final AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private PersonWebController controller() {
        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        return new PersonWebController(
                personRepository,
                churchContextService,
                personService,
                groupRepository,
                followUpRepository,
                privateNoteRepository,
                attendanceRecordRepository,
                visitRepository,
                currentUserService);
    }

    @Test
    void createPersistsPersonWithActiveChurchAndDefaultMemberType() {
        AtomicReference<Person> savedPerson = new AtomicReference<>();
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            if (person.getId() == null) {
                person.setId(44L);
            }
            if (person.getChildren() == null) {
                person.setChildren(new LinkedHashSet<>());
            }
            savedPerson.set(person);
            return person;
        });
        when(personRepository.findByIdAndChurchId(eq(44L), eq(1L))).thenAnswer(invocation -> Optional.of(savedPerson.get()));

        PersonDTO dto = new PersonDTO();
        dto.setFirstName(" Ana ");
        dto.setLastName("Pop");
        dto.setPhone("0712");
        dto.setChurchRole("Voluntar");
        dto.setBirthDate(LocalDate.of(1990, 5, 3));

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller().create(dto, List.of(), redirect);

        assertEquals("redirect:/persons/44", view);
        assertNotNull(redirect.getFlashAttributes().get("success"));
        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Person firstSaved = captor.getAllValues().get(0);
        assertEquals(1L, firstSaved.getChurchId());
        assertEquals(" Ana ", firstSaved.getFirstName());
        assertEquals("Pop", firstSaved.getLastName());
        assertEquals(MemberType.MEMBER, firstSaved.getMemberType());
        verify(personRepository).deleteChildLinks(44L);
    }

    @Test
    void updatePersistsExistingPersonAndRedirectsToView() {
        Person existing = new Person();
        existing.setId(7L);
        existing.setChurchId(1L);
        existing.setChildren(new LinkedHashSet<>());

        when(personRepository.findByIdAndChurchId(7L, 1L)).thenReturn(Optional.of(existing));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonDTO dto = new PersonDTO();
        dto.setFirstName("Ioan");
        dto.setLastName("Ionescu");
        dto.setMemberType(MemberType.FREND);
        dto.setPhone("0733");

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller().update(7L, dto, List.of(), redirect);

        assertEquals("redirect:/persons/7", view);
        assertNotNull(redirect.getFlashAttributes().get("success"));
        assertEquals("Ioan", existing.getFirstName());
        assertEquals("Ionescu", existing.getLastName());
        assertEquals(MemberType.FREND, existing.getMemberType());
        verify(personRepository).deleteChildLinks(7L);
        verify(personRepository, org.mockito.Mockito.atLeastOnce()).save(existing);
    }

    @Test
    void updateMissingPersonAddsErrorAndDoesNotSave() {
        when(personRepository.findByIdAndChurchId(99L, 1L)).thenReturn(Optional.empty());

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller().update(99L, new PersonDTO(), List.of(), redirect);

        assertEquals("redirect:/persons/99/edit", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        verify(personRepository, never()).save(any(Person.class));
    }

    @Test
    void deleteDelegatesToServiceAndReportsSuccess() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller().delete(5L, redirect);

        assertEquals("redirect:/persons", view);
        assertNotNull(redirect.getFlashAttributes().get("success"));
        verify(personService).deletePerson(5L);
    }

    @Test
    void deleteReportsServiceFailure() {
        doThrow(new IllegalStateException("linked records")).when(personService).deletePerson(5L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller().delete(5L, redirect);

        assertEquals("redirect:/persons", view);
        assertEquals("linked records", redirect.getFlashAttributes().get("error"));
    }
}
