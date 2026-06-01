package ro.church_office.info;

import org.junit.jupiter.api.Test;
import ro.church_office.info.attendance.*;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.DAO.ChurchInfo;
import ro.church_office.info.church.DTO.ChurchInfoDTO;
import ro.church_office.info.events.*;
import ro.church_office.info.events.DAO.Event;
import ro.church_office.info.events.DAO.EventService;
import ro.church_office.info.events.DAO.EventTask;
import ro.church_office.info.finance.Transaction;
import ro.church_office.info.finance.TransactionService;
import ro.church_office.info.followup.*;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.GroupType;
import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DTO.PersonDTO;
import ro.church_office.info.person.service.PersonService;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.visits.VisitDTO;
import ro.church_office.info.visits.VisitService;
import ro.church_office.info.visits.dao.Visit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompatibilityCoverageTest {
    @Test
    void coverInfoCompatLayer() {
        GlobalSetting gs = new GlobalSetting("k", "12");
        assertEquals("k", gs.getKey());
        assertEquals(12, gs.getIntValue());
        gs.setIntValue(7);
        assertEquals("7", gs.getStringValue());

        User u = new User();
        u.setId(1L); u.setUsername("john"); u.setPassword("p"); u.setRole("ADMIN"); u.setEmail("a@b.c");
        u.setSecurityAnswerOneHash("x"); u.setSecurityAnswerTwoHash("y"); u.setLastLogin(LocalDateTime.now());
        assertEquals("john", u.getUsername());

        ChurchInfoDTO dto = new ChurchInfoDTO();
        dto.id = 1L; dto.name = "Hope";
        assertEquals(1L, dto.getId());

        ChurchInfo ci = new ChurchInfo();
        ci.setId(3L); ci.setName("C1");
        assertEquals("C1", ci.getName());

        ChurchContextService ccs = new ChurchContextService() {};
        assertNotNull(ccs.getOrCreateActiveChurchId());
        ccs.setActiveChurchId(2L);

        ChurchInfoService cis = new ChurchInfoService() {};
        assertTrue(cis.getAll().isEmpty());
        assertEquals(dto, cis.save(dto));

        Person p = new Person();
        p.setId(11L); p.setChurchId(5L); p.setFirstName("Ana"); p.setLastName("Pop");
        p.setPhone("07"); p.setChurchRole("Leader"); p.setAddress("Street"); p.setPosition("Singer");
        p.setMemberType(MemberType.MEMBER); p.setBirthDate(LocalDate.now());
        Person spouse = new Person(); spouse.setId(12L); p.setSpouse(spouse);
        p.getChildren().add(spouse); p.getParents().add(spouse);
        assertEquals("Ana", p.getFirstName());

        PersonDTO pdto = PersonDTO.fromEntity(p);
        pdto.setParentIds(List.of(1L, 2L)); pdto.setChildrenIds(List.of(3L)); pdto.setSpouseId(12L);
        assertFalse(pdto.getFullName().isBlank());

        PersonService ps = new PersonService() {};
        assertTrue(ps.getAllPersons().isEmpty());
        ps.deletePerson(1L);

        ChurchGroup g = new ChurchGroup();
        g.setId(2L); g.setChurchId(5L); g.setName("G"); g.setType(GroupType.MINISTRY); g.setLeader(p);
        g.setDescription("desc"); g.getMembers().add(p);
        assertEquals(GroupType.MINISTRY, g.getType());

        AttendanceRecord ar = new AttendanceRecord();
        ar.setPersonId(p.getId()); ar.setStatus(AttendanceStatus.PRESENT); ar.setAttendanceDate(LocalDate.now());
        ar.setSession(AttendanceSession.MORNING);
        assertEquals(AttendanceStatus.PRESENT, ar.getStatus());

        AttendanceService as = new AttendanceService() {};
        assertEquals(AttendanceSession.MORNING, as.parseSession("x"));
        assertTrue(as.recordsByPerson(1L, LocalDate.now(), AttendanceSession.MORNING).isEmpty());

        Event e = new Event();
        e.setId(7L); e.setChurchId(5L); e.setEventType(EventType.EVENT); e.setEventName("N");
        e.setOpenDate(LocalDate.now()); e.setEndDate(LocalDate.now().plusDays(1)); e.setStatus("PLANNED");
        e.setAssociatedGroup(g); e.setImplementedBy(p);
        assertEquals(EventType.EVENT, e.getEventType());

        EventDTO edto = EventDTO.fromEntity(e);
        edto.setEventName("E2"); edto.setAbout("a"); edto.setStatus(EventStatus.PLANNED.name());
        edto.setEventType(EventType.STUDY.name()); edto.setPriority(Priority.HIGH.name());
        edto.setOpenDate(LocalDate.now()); edto.setRecurrenceType(RecurrenceType.WEEKLY.name());
        edto.setRecurrenceInterval(2); edto.setRecurrenceUntil(LocalDate.now().plusDays(10));
        edto.setGroupId(5L); edto.setImplementedBy(pdto); edto.setFrontReminderEnabled(true); edto.setFrontReminderDaysBefore(3);
        assertEquals("E2", edto.getEventName());
        assertEquals(RecurrenceType.NONE, RecurrenceType.from("bad"));
        assertEquals(Priority.MEDIUM, Priority.from("x"));

        EventTask task = new EventTask();
        task.setId(9L); task.setEvent(e); task.setTitle("t"); task.setNotes("n"); task.setDueDate(LocalDate.now());
        assertEquals("t", task.getTitle());
        assertNotNull(EventTaskDTO.fromEntity(task));

        EventService es = new EventService() {};
        assertTrue(es.getAllEvents().isEmpty());
        assertNotNull(es.saveEvent(edto));

        Transaction tx = new Transaction();
        tx.setId(1L); tx.setName("don"); tx.setDescription("desc"); tx.setAmount(12.5); tx.setDate(LocalDate.now());
        assertEquals(12.5, tx.getAmount());
        TransactionService ts = new TransactionService() {};
        assertTrue(ts.getAllTransactions().isEmpty());
        assertNotNull(ts.addTransaction(tx));

        PastoralFollowUp pf = new PastoralFollowUp();
        pf.setId(1L); pf.setChurchId(2L); pf.setPerson(p); pf.setGroup(g); pf.setStatus(PastoralFollowUpStatus.IN_PROGRESS);
        pf.setLastContactDate(LocalDate.now()); pf.setNextContactDate(LocalDate.now().plusDays(1)); pf.setContactMethod("CALL"); pf.setUpdatedAt(LocalDateTime.now());
        assertEquals("CALL", pf.getContactMethod());

        PastoralPrivateNote note = new PastoralPrivateNote();
        note.setChurchId(2L); note.setPerson(p); note.setNoteText("n"); note.setAllowedRoles("ADMIN"); note.setCreatedAt(LocalDateTime.now());
        assertEquals("n", note.getNote());

        PastoralRecommendationDismissal d = new PastoralRecommendationDismissal();
        d.setChurchId(2L); d.setPersonId(1L); d.setRecommendationType("Abs"); d.setDismissedUntil(LocalDate.now().plusDays(7));
        assertEquals("Abs", d.getRecommendationType());

        Visit v = new Visit();
        v.setId(1L); v.setPersonName("P"); v.setPhone("07"); v.setAddress("Addr"); v.setVisitDate(LocalDate.now()); v.setNotes("n");
        VisitDTO vd = new VisitDTO(v);
        assertEquals("P", vd.getPersonName());
        assertNotNull(vd.toEntity());
        VisitService vs = new VisitService() {};
        assertTrue(vs.getAllVisits(1L).isEmpty());
        assertNull(vs.getVisitById(1L, 1L));
    }
}
