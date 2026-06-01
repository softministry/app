package ro.church_office.teamleaf.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import ro.church_office.info.attendance.AttendanceRecordRepository;
import ro.church_office.info.attendance.AttendanceSession;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.church.DAO.ChurchInfo;
import ro.church_office.info.church.Repository.ChurchInfoRepository;
import ro.church_office.info.events.DAO.Event;
import ro.church_office.info.events.DAO.EventRepository;
import ro.church_office.info.events.DAO.EventTask;
import ro.church_office.info.events.DAO.EventTaskRepository;
import ro.church_office.info.events.EventStatus;
import ro.church_office.info.events.EventType;
import ro.church_office.info.events.Priority;
import ro.church_office.info.events.RecurrenceType;
import ro.church_office.info.finance.Transaction;
import ro.church_office.info.finance.TransactionRepository;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.groups.GroupType;
import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.visits.dao.Visit;
import ro.church_office.info.visits.dao.VisitRepository;
import ro.church_office.teamleaf.finance.TransactionChurchScopeRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CrudDbOperationsIT extends AbstractContainerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ChurchInfoRepository churchInfoRepository;
    @Autowired
    private ChurchContextService churchContextService;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private ChurchGroupRepository groupRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventTaskRepository eventTaskRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionChurchScopeRepository transactionScopeRepository;
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    private Long activeChurchId;

    @BeforeEach
    void setupChurchContext() {
        ChurchInfo church = IntegrationFixtures.ensureChurch(churchInfoRepository, "CRUD IT Church");
        activeChurchId = church.getId();
        churchContextService.setActiveChurchId(activeChurchId);
    }

    @Test
    void personCreateUpdateDeletePersistsCrudFlow() throws Exception {
        mockMvc.perform(post("/persons")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("firstName", "  Elena ")
                        .param("lastName", " Popescu ")
                        .param("phone", "0712345678")
                        .param("churchRole", "voluntar")
                        .param("address", "Strada Test 1")
                        .param("position", "cor")
                        .param("birthDate", "1990-02-03")
                        .param("memberType", "MEMBER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/persons/*"));

        Person created = latestPerson("Elena", "Popescu");
        assertEquals(activeChurchId, created.getChurchId());
        assertEquals(MemberType.MEMBER, created.getMemberType());
        assertEquals("0712345678", created.getPhone());

        mockMvc.perform(post("/persons/{id}", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("firstName", "Elena")
                        .param("lastName", "Ionescu")
                        .param("phone", "0799999999")
                        .param("churchRole", "secretar")
                        .param("address", "Strada Test 2")
                        .param("position", "admin")
                        .param("birthDate", "1990-02-03")
                        .param("memberType", "FRIEND"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/persons/*"));

        Person updated = personRepository.findByIdAndChurchId(created.getId(), activeChurchId).orElseThrow();
        assertEquals("Ionescu", updated.getLastName());
        assertEquals("0799999999", updated.getPhone());
        assertEquals(MemberType.FRIEND, updated.getMemberType());

        mockMvc.perform(post("/persons/{id}/delete", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/persons"));

        assertTrue(personRepository.findByIdAndChurchId(created.getId(), activeChurchId).isEmpty());
    }

    @Test
    void groupCreateUpdateDeletePersistsLeaderAndMembers() throws Exception {
        Person leader = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Lidia", "Leader");
        Person member = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Mihai", "Member");

        mockMvc.perform(post("/groups")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Grup CRUD")
                        .param("type", "SMALL_GROUP")
                        .param("leaderId", leader.getId().toString())
                        .param("description", "Initial")
                        .param("memberIds", leader.getId().toString(), member.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/groups"));

        ChurchGroup created = latestGroup("Grup CRUD");
        assertEquals(GroupType.SMALL_GROUP, created.getType());
        assertEquals(leader.getId(), created.getLeader().getId());
        assertEquals(Set.of(leader.getId(), member.getId()), memberIdsFor(created.getId(), leader.getId(), member.getId()));

        mockMvc.perform(post("/groups/{id}", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Grup CRUD Actualizat")
                        .param("type", "MINISTRY")
                        .param("description", "Updated")
                        .param("memberIds", member.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/groups"));

        ChurchGroup updated = groupRepository.findByIdAndChurchId(created.getId(), activeChurchId).orElseThrow();
        assertEquals("Grup CRUD Actualizat", updated.getName());
        assertEquals(GroupType.MINISTRY, updated.getType());
        assertEquals("Updated", updated.getDescription());
        assertEquals(Set.of(member.getId()), memberIdsFor(updated.getId(), leader.getId(), member.getId()));

        mockMvc.perform(post("/groups/{id}/delete", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/groups"));

        assertTrue(groupRepository.findByIdAndChurchId(created.getId(), activeChurchId).isEmpty());
    }

    @Test
    void visitCreateUpdateDeletePersistsCrudFlow() throws Exception {
        mockMvc.perform(post("/visits")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("personName", "Familia Test")
                        .param("address", "Adresa initiala")
                        .param("phone", "0711111111")
                        .param("visitDate", "2026-01-10")
                        .param("notes", "Prima vizita"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/visits"));

        Visit created = latestVisit("Familia Test");
        assertEquals(activeChurchId, created.getChurchId());
        assertEquals(LocalDate.of(2026, 1, 10), created.getVisitDate());

        mockMvc.perform(post("/visits/{id}", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("personName", "Familia Test")
                        .param("address", "Adresa actualizata")
                        .param("phone", "0722222222")
                        .param("visitDate", "2026-01-11")
                        .param("notes", "Vizita actualizata"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/visits"));

        Visit updated = visitRepository.findByIdAndChurchId(created.getId(), activeChurchId).orElseThrow();
        assertEquals("Adresa actualizata", updated.getAddress());
        assertEquals("Vizita actualizata", updated.getNotes());

        mockMvc.perform(post("/visits/{id}/delete", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/visits"));

        assertTrue(visitRepository.findByIdAndChurchId(created.getId(), activeChurchId).isEmpty());
    }

    @Test
    void financeCreateAndDeletePersistsTransactionScope() throws Exception {
        mockMvc.perform(post("/finance")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("type", "venit")
                        .param("name", "Donatie CRUD")
                        .param("description", "Test financiar")
                        .param("amount", "125.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/finance"));

        Transaction created = latestTransaction("Donatie CRUD");
        assertEquals("income", created.getType());
        assertEquals(125.50, created.getAmount());
        assertEquals(activeChurchId, transactionScopeRepository.findByTransactionId(created.getId()).orElseThrow().getChurchId());

        mockMvc.perform(post("/finance/{id}/delete", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/finance"));

        assertTrue(transactionRepository.findById(created.getId()).isEmpty());
        assertTrue(transactionScopeRepository.findByTransactionId(created.getId()).isEmpty());
    }

    @Test
    void attendanceRegisteredSessionCanBeDeletedFromTableContextMenuTarget() throws Exception {
        Person first = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Ioan", "Prezent");
        Person second = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Paul", "Absent");

        mockMvc.perform(post("/attendance")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("date", "2026-04-20")
                        .param("session", "MORNING")
                        .param("personIds", first.getId().toString(), second.getId().toString())
                        .param("presentPersonIds", first.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/attendance?date=2026-04-20&session=MORNING"));

        assertEquals(2, attendanceRecordRepository
                .findByChurchIdAndAttendanceDateAndSession(activeChurchId, LocalDate.of(2026, 4, 20), AttendanceSession.MORNING)
                .size());

        mockMvc.perform(get("/attendance")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-attendance-delete-url=\"/attendance/delete\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-attendance-date=\"2026-04-20\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-attendance-session=\"MORNING\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-attendance-context-delete")));

        mockMvc.perform(post("/attendance/delete")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("date", "2026-04-20")
                        .param("session", "MORNING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/attendance?date=2026-04-20&session=MORNING"));

        assertTrue(attendanceRecordRepository
                .findByChurchIdAndAttendanceDateAndSession(activeChurchId, LocalDate.of(2026, 4, 20), AttendanceSession.MORNING)
                .isEmpty());
    }

    @Test
    void renderedEventCreateFormSubmitsAndPersistsWithoutImplementer() throws Exception {
        Person activePerson = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Ana", "Activa");
        ChurchInfo otherChurch = IntegrationFixtures.ensureChurch(churchInfoRepository, "Other Event Church");
        IntegrationFixtures.createPerson(personRepository, otherChurch.getId(), "Mihai", "AltaBiserica");

        mockMvc.perform(get("/events/new")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/events\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(activePerson.getFirstName())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("AltaBiserica"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("required data-autocomplete-input"))));

        mockMvc.perform(post("/events")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("eventName", "Eveniment fara responsabil IT")
                        .param("status", "PLANNED")
                        .param("eventType", "OTHER")
                        .param("openDate", "2026-06-15")
                        .param("about", "Creat din formularul real")
                        .param("priority", "MEDIUM")
                        .param("recurrenceType", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*/edit"));

        Event created = latestEvent("Eveniment fara responsabil IT");
        assertEquals(activeChurchId, created.getChurchId());
        assertEquals(LocalDate.of(2026, 6, 15), created.getOpenDate());
        assertEquals(EventType.OTHER, created.getEventType());
        assertEquals(Priority.MEDIUM, created.getPriority());
    }

    @Test
    void eventAndTaskCreateUpdateDeletePersistCrudFlow() throws Exception {
        Person implementer = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Dorin", "Responsabil");

        mockMvc.perform(post("/events")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("eventName", "Eveniment CRUD")
                        .param("status", "PLANNED")
                        .param("eventType", "EVENT")
                        .param("openDate", "2026-02-01")
                        .param("endDate", "2026-02-02")
                        .param("about", "Plan initial")
                        .param("priority", "HIGH")
                        .param("recurrenceType", "NONE")
                        .param("frontReminderEnabled", "true")
                        .param("frontReminderDaysBefore", "2")
                        .param("implementedById", implementer.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*/edit"));

        Event created = latestEvent("Eveniment CRUD");
        assertEquals(activeChurchId, created.getChurchId());
        assertEquals(EventStatus.PLANNED.name(), created.getStatus());
        assertEquals(EventType.EVENT, created.getEventType());
        assertEquals(Priority.HIGH, created.getPriority());
        assertEquals(RecurrenceType.NONE, created.getRecurrenceType());
        assertEquals(implementer.getId(), created.getImplementedBy().getId());

        mockMvc.perform(post("/events/{id}/tasks", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Task CRUD")
                        .param("notes", "Task initial")
                        .param("dueDate", "2026-01-31")
                        .param("status", "TODO")
                        .param("assignedToId", implementer.getId().toString())
                        .param("orderIndex", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*/edit"));

        EventTask createdTask = eventTaskRepository.findByEventIdOrderByOrderIndexAscIdAsc(created.getId()).getFirst();
        assertEquals("Task CRUD", createdTask.getTitle());
        assertEquals(implementer.getId(), createdTask.getAssignedTo().getId());

        mockMvc.perform(post("/events/{id}/tasks/{taskId}", created.getId(), createdTask.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Task CRUD Actualizat")
                        .param("notes", "Task actualizat")
                        .param("dueDate", "2026-02-01")
                        .param("status", "DONE")
                        .param("orderIndex", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*/edit"));

        EventTask updatedTask = eventTaskRepository.findById(createdTask.getId()).orElseThrow();
        assertEquals("Task CRUD Actualizat", updatedTask.getTitle());
        assertEquals("DONE", updatedTask.getStatus());
        assertEquals(1, updatedTask.getOrderIndex());

        mockMvc.perform(post("/events/{id}", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("eventName", "Eveniment CRUD Actualizat")
                        .param("status", "DONE")
                        .param("eventType", "STUDY")
                        .param("openDate", "2026-02-03")
                        .param("about", "Plan actualizat")
                        .param("priority", "LOW")
                        .param("recurrenceType", "WEEKLY")
                        .param("recurrenceInterval", "2")
                        .param("recurrenceUntil", "2026-03-01")
                        .param("frontReminderEnabled", "false")
                        .param("implementedById", implementer.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));

        Event updated = eventRepository.findByIdAndChurchId(created.getId(), activeChurchId).orElseThrow();
        assertEquals("Eveniment CRUD Actualizat", updated.getEventName());
        assertEquals(EventStatus.DONE.name(), updated.getStatus());
        assertEquals(EventType.STUDY, updated.getEventType());
        assertEquals(Priority.LOW, updated.getPriority());
        assertEquals(RecurrenceType.WEEKLY, updated.getRecurrenceType());

        mockMvc.perform(post("/events/{id}/tasks/{taskId}/delete", created.getId(), createdTask.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*/edit"));

        assertTrue(eventTaskRepository.findById(createdTask.getId()).isEmpty());

        mockMvc.perform(post("/events/{id}/delete", created.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));

        assertTrue(eventRepository.findByIdAndChurchId(created.getId(), activeChurchId).isEmpty());
    }

    private Person latestPerson(String firstName, String lastName) {
        return personRepository.findAllByChurchId(activeChurchId, Sort.by(Sort.Order.desc("id"))).stream()
                .filter(person -> firstName.equals(trimmed(person.getFirstName())) && lastName.equals(trimmed(person.getLastName())))
                .findFirst()
                .orElseThrow();
    }

    private ChurchGroup latestGroup(String name) {
        return groupRepository.findAllByChurchId(activeChurchId, Sort.by(Sort.Order.desc("id"))).stream()
                .filter(group -> name.equals(group.getName()))
                .findFirst()
                .orElseThrow();
    }

    private Visit latestVisit(String personName) {
        return visitRepository.findAllByChurchIdOrderByVisitDateDescIdDesc(activeChurchId).stream()
                .filter(visit -> personName.equals(visit.getPersonName()))
                .max(Comparator.comparing(Visit::getId))
                .orElseThrow();
    }

    private Transaction latestTransaction(String name) {
        return transactionRepository.findAll().stream()
                .filter(transaction -> name.equals(transaction.getName()))
                .max(Comparator.comparing(Transaction::getId))
                .orElseThrow();
    }

    private Event latestEvent(String eventName) {
        return eventRepository.findAllByChurchId(activeChurchId, Sort.by(Sort.Order.desc("id"))).stream()
                .filter(event -> eventName.equals(event.getEventName()))
                .findFirst()
                .orElseThrow();
    }

    private Set<Long> memberIdsFor(Long groupId, Long... candidatePersonIds) {
        return groupRepository.findMembershipsForPersons(activeChurchId, Set.of(candidatePersonIds)).stream()
                .filter(membership -> groupId.equals(membership.getGroupId()))
                .map(ChurchGroupRepository.PersonGroupMembership::getPersonId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
