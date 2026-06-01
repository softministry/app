package ro.church_office.teamleaf.web;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.church_office.info.attendance.AttendanceRecord;
import ro.church_office.info.attendance.AttendanceService;
import ro.church_office.info.attendance.AttendanceSession;
import ro.church_office.info.attendance.AttendanceStatus;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class AttendanceWebController {

    private final ChurchContextService churchContextService;
    private final PersonRepository personRepository;
    private final AttendanceService attendanceService;
    private final ChurchGroupRepository groupRepository;

    public AttendanceWebController(ChurchContextService churchContextService,
                                   PersonRepository personRepository,
                                   AttendanceService attendanceService,
                                   ChurchGroupRepository groupRepository) {
        this.churchContextService = churchContextService;
        this.personRepository = personRepository;
        this.attendanceService = attendanceService;
        this.groupRepository = groupRepository;
    }

    @GetMapping("/attendance")
    public String attendance(@RequestParam(value = "date", required = false) String date,
                             @RequestParam(value = "session", required = false) String session,
                             @RequestParam(value = "groupId", required = false) Long groupId,
                             Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        LocalDate attendanceDate = parseDate(date);
        AttendanceSession attendanceSession = attendanceService.parseSession(session);
        ChurchGroup selectedGroup = resolveGroup(groupId, churchId);

        model.addAttribute("attendanceDate", attendanceDate);
        model.addAttribute("selectedSession", attendanceSession);
        model.addAttribute("selectedGroupId", selectedGroup == null ? null : selectedGroup.getId());
        model.addAttribute("selectedGroupName", selectedGroup == null ? null : selectedGroup.getName());
        model.addAttribute("groups", groupRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("type"), Sort.Order.asc("name"))));
        model.addAttribute("availableSessions", AttendanceSession.values());
        model.addAttribute(
                "registeredSessions",
                selectedGroup == null
                        ? attendanceService.registeredSessions(churchId)
                        : attendanceService.registeredSessionsForGroup(churchId, selectedGroup.getId()));
        return "attendance/index";
    }

    @GetMapping("/attendance/details")
    public String attendanceDetails(@RequestParam("date") String date,
                                    @RequestParam("session") String session,
                                    @RequestParam(value = "groupId", required = false) Long groupId,
                                    Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        LocalDate attendanceDate = parseDate(date);
        AttendanceSession attendanceSession = attendanceService.parseSession(session);
        ChurchGroup selectedGroup = resolveGroup(groupId, churchId);
        List<AttendanceRow> rows = attendanceRows(churchId, attendanceDate, attendanceSession, selectedGroup);

        model.addAttribute("attendanceDate", attendanceDate);
        model.addAttribute("selectedSession", attendanceSession);
        model.addAttribute("selectedGroupId", selectedGroup == null ? null : selectedGroup.getId());
        model.addAttribute("selectedGroupName", selectedGroup == null ? null : selectedGroup.getName());
        model.addAttribute("attendanceRows", rows);
        model.addAttribute("presentCount", rows.stream().filter(AttendanceRow::isPresent).count());
        model.addAttribute("absentCount", rows.stream().filter(row -> !row.isPresent()).count());
        return "attendance/details :: attendanceDetails";
    }

    @PostMapping("/attendance")
    public String saveAttendance(@RequestParam("date") String date,
                                 @RequestParam("session") String session,
                                 @RequestParam(value = "groupId", required = false) Long groupId,
                                 @RequestParam(value = "personIds", required = false) List<Long> personIds,
                                 @RequestParam(value = "presentPersonIds", required = false) List<Long> presentPersonIds,
                                 RedirectAttributes redirectAttributes) {
        LocalDate attendanceDate = parseDate(date);
        AttendanceSession attendanceSession = attendanceService.parseSession(session);
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        try {
            ChurchGroup selectedGroup = resolveGroup(groupId, churchId);
            attendanceService.savePresence(churchId, attendanceDate, attendanceSession, selectedGroup, personIds, presentPersonIds);
            redirectAttributes.addFlashAttribute("success", "Prezența a fost salvată.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut salva prezența." : ex.getMessage());
        }
        String redirect = "redirect:/attendance?date=" + attendanceDate + "&session=" + attendanceSession;
        return groupId == null ? redirect : redirect + "&groupId=" + groupId;
    }

    @PostMapping("/attendance/delete")
    public String deleteAttendanceSession(@RequestParam("date") String date,
                                          @RequestParam("session") String session,
                                          @RequestParam(value = "groupId", required = false) Long groupId,
                                          RedirectAttributes redirectAttributes) {
        LocalDate attendanceDate = parseDate(date);
        AttendanceSession attendanceSession = attendanceService.parseSession(session);
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        int deleted = attendanceService.deleteSession(churchId, attendanceDate, attendanceSession);
        if (deleted > 0) {
            redirectAttributes.addFlashAttribute("success", "Programul înregistrat a fost șters.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Programul înregistrat nu a fost găsit.");
        }
        String redirect = "redirect:/attendance?date=" + attendanceDate + "&session=" + attendanceSession;
        return groupId == null ? redirect : redirect + "&groupId=" + groupId;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value);
    }

    private List<AttendanceRow> attendanceRows(Long churchId,
                                               LocalDate attendanceDate,
                                               AttendanceSession attendanceSession,
                                               ChurchGroup selectedGroup) {
        Map<Long, AttendanceRecord> records = attendanceService.recordsByPerson(churchId, attendanceDate, attendanceSession);
        List<Person> persons = selectedGroup == null
                ? personRepository.findAllByChurchId(churchId, Sort.by("lastName", "firstName"))
                : selectedGroup.getMembers().stream()
                        .filter(person -> person.getChurchId() != null && person.getChurchId().equals(churchId))
                        .sorted((a, b) -> displayName(a).compareToIgnoreCase(displayName(b)))
                        .toList();
        return persons.stream()
                .map(person -> {
                    AttendanceRecord record = records.get(person.getId());
                    AttendanceStatus status = record == null ? AttendanceStatus.ABSENT : record.getStatus();
                    return new AttendanceRow(PersonDTO.fromEntity(person), status == AttendanceStatus.PRESENT);
                })
                .toList();
    }

    private ChurchGroup resolveGroup(Long groupId, Long churchId) {
        if (groupId == null) {
            return null;
        }
        return groupRepository.findByIdAndChurchId(groupId, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Grupul selectat nu există în biserica activă."));
    }

    private String displayName(Person person) {
        if (person == null) return "";
        String first = person.getFirstName() == null ? "" : person.getFirstName();
        String last = person.getLastName() == null ? "" : person.getLastName();
        return (first + " " + last).trim();
    }

    public static class AttendanceRow {
        private final PersonDTO person;
        private final boolean present;

        public AttendanceRow(PersonDTO person, boolean present) {
            this.person = person;
            this.present = present;
        }

        public PersonDTO getPerson() { return person; }
        public boolean isPresent() { return present; }
    }
}
