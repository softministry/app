package ro.church_office.teamleaf.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ro.church_office.info.attendance.AttendanceRecord;
import ro.church_office.info.attendance.AttendanceRecordRepository;
import ro.church_office.info.attendance.AttendanceService;
import ro.church_office.info.attendance.AttendanceStatus;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.events.DAO.EventRepository;
import ro.church_office.info.followup.PastoralFollowUp;
import ro.church_office.info.followup.PastoralFollowUpRepository;
import ro.church_office.info.followup.PastoralFollowUpStatus;
import ro.church_office.info.followup.PastoralRecommendationDismissal;
import ro.church_office.info.followup.PastoralRecommendationDismissalRepository;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.info.visits.dao.VisitRepository;

@Controller
public class DashboardWebController {

    private final ChurchContextService churchContextService;
    private final PersonRepository personRepository;
    private final EventRepository eventRepository;
    private final VisitRepository visitRepository;
    private final AttendanceService attendanceService;
    private final PastoralFollowUpRepository followUpRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ChurchGroupRepository groupRepository;
    private final PastoralRecommendationDismissalRepository recommendationDismissalRepository;
    private final GlobalSettingRepository globalSettingRepository;

    public DashboardWebController(ChurchContextService churchContextService,
                                  PersonRepository personRepository,
                                  EventRepository eventRepository,
                                  VisitRepository visitRepository,
                                  AttendanceService attendanceService,
                                  PastoralFollowUpRepository followUpRepository,
                                  AttendanceRecordRepository attendanceRecordRepository,
                                  ChurchGroupRepository groupRepository,
                                  PastoralRecommendationDismissalRepository recommendationDismissalRepository,
                                  GlobalSettingRepository globalSettingRepository) {
        this.churchContextService = churchContextService;
        this.personRepository = personRepository;
        this.eventRepository = eventRepository;
        this.visitRepository = visitRepository;
        this.attendanceService = attendanceService;
        this.followUpRepository = followUpRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.groupRepository = groupRepository;
        this.recommendationDismissalRepository = recommendationDismissalRepository;
        this.globalSettingRepository = globalSettingRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        List<ro.church_office.info.person.DAO.Person> persons = personRepository.findAllByChurchId(churchId, org.springframework.data.domain.Sort.unsorted());
        model.addAttribute("personCount", persons.size());
        model.addAttribute("memberCount", persons.stream().filter(p -> p.getMemberType() == MemberType.MEMBER).count());
        model.addAttribute("childCount", persons.stream().filter(p -> p.getMemberType() == MemberType.CHILD).count());
        model.addAttribute("friendCount", persons.stream().filter(p -> p.getMemberType() == MemberType.FREND).count());
        model.addAttribute("eventCount", eventRepository.countByChurchId(churchId));
        model.addAttribute("visitCount", visitRepository.countByChurchId(churchId));
        model.addAttribute("attendanceSummaries", attendanceService.recentSummaries(churchId, 5));
        List<PastoralFollowUp> followUps = followUpRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("status"), Sort.Order.asc("nextContactDate"), Sort.Order.desc("updatedAt")));
        PastoralRecommendationSettings recommendationSettings = pastoralRecommendationSettings();
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        List<PastoralFollowUp> openFollowUps = followUps.stream()
                .filter(item -> item.getStatus() != PastoralFollowUpStatus.DONE)
                .toList();
        List<PastoralFollowUp> dueFollowUps = openFollowUps.stream()
                .filter(item -> item.getNextContactDate() != null && !item.getNextContactDate().isAfter(today))
                .toList();
        List<PastoralFollowUp> upcomingFollowUps = openFollowUps.stream()
                .filter(item -> item.getNextContactDate() != null && item.getNextContactDate().isAfter(today) && !item.getNextContactDate().isAfter(nextWeek))
                .toList();
        model.addAttribute("openFollowUpCount", openFollowUps.size());
        model.addAttribute("dueFollowUpCount", dueFollowUps.size());
        model.addAttribute("upcomingFollowUpCount", upcomingFollowUps.size());
        model.addAttribute("dueFollowUps", dueFollowUps.stream().limit(5).toList());
        List<PastoralRecommendation> recommendations = pastoralRecommendations(churchId, persons, today, followUps, recommendationSettings);
        model.addAttribute("pastoralRecommendations", recommendations);
        PastoralDashboardMetrics metrics = pastoralDashboardMetrics(followUps, recommendationSettings, today);
        model.addAttribute("pastoralPipeline", metrics.pipeline());
        model.addAttribute("pastoralSla", metrics.sla());
        model.addAttribute("pastoralReport", metrics.report());
        model.addAttribute("pastoralLeaders", metrics.activeCasesByLeader());
        model.addAttribute("pastoralInbox", pastoralDailyInbox(followUps, recommendations, recommendationSettings, today));
        model.addAttribute("pastoralRecommendationSettings", recommendationSettings);
        model.addAttribute("hiddenPastoralRecommendations", hiddenPastoralRecommendations(churchId, persons));
        return "dashboard/index";
    }

    @GetMapping("/birthdays")
    public String birthdays() {
        return "dashboard/birthdays";
    }

    @PostMapping("/dashboard/recommendations/dismiss")
    public String dismissRecommendation(@RequestParam("personId") Long personId,
                                        @RequestParam("type") String type,
                                        @RequestParam(value = "mode", defaultValue = "snooze") String mode,
                                        RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        LocalDate today = LocalDate.now();
        int snoozeDays = pastoralRecommendationSettings().snoozeDays();
        LocalDate dismissedUntil = "done".equals(mode) ? LocalDate.of(9999, 12, 31) : today.plusDays(snoozeDays);
        PastoralRecommendationDismissal dismissal = recommendationDismissalRepository
                .findByChurchIdAndPersonIdAndRecommendationType(churchId, personId, type)
                .orElseGet(PastoralRecommendationDismissal::new);
        dismissal.setChurchId(churchId);
        dismissal.setPersonId(personId);
        dismissal.setRecommendationType(type);
        dismissal.setDismissedUntil(dismissedUntil);
        dismissal.setUpdatedAt(java.time.LocalDateTime.now());
        recommendationDismissalRepository.save(dismissal);
        redirectAttributes.addFlashAttribute("success", "Recomandarea a fost " + ("done".equals(mode) ? "marcată ca tratată." : "amânată " + snoozeDays + " zile."));
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/recommendations/restore")
    public String restoreRecommendation(@RequestParam("dismissalId") Long dismissalId,
                                        RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        recommendationDismissalRepository.findByIdAndChurchId(dismissalId, churchId)
                .ifPresent(recommendationDismissalRepository::delete);
        redirectAttributes.addFlashAttribute("success", "Recomandarea a fost reactivată.");
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/inbox/check-in")
    public String quickCheckIn(@RequestParam("followUpId") Long followUpId,
                               @RequestParam(value = "status", defaultValue = "IN_PROGRESS") PastoralFollowUpStatus status,
                               @RequestParam(value = "redirect", defaultValue = "/dashboard") String redirect,
                               RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        PastoralFollowUp followUp = followUpRepository.findByIdAndChurchId(followUpId, churchId).orElse(null);
        if (followUp == null) {
            redirectAttributes.addFlashAttribute("error", "Cazul pastoral nu a putut fi găsit.");
            return safeDashboardRedirect(redirect);
        }
        followUp.setLastContactDate(LocalDate.now());
        followUp.setStatus(status);
        if (status != PastoralFollowUpStatus.DONE && followUp.getNextContactDate() == null) {
            followUp.setNextContactDate(LocalDate.now().plusDays(7));
        }
        followUp.setUpdatedAt(LocalDateTime.now());
        followUpRepository.save(followUp);
        redirectAttributes.addFlashAttribute("success", "Check-in pastoral salvat.");
        return safeDashboardRedirect(redirect);
    }

    private List<PastoralRecommendation> pastoralRecommendations(Long churchId,
                                                                 List<Person> persons,
                                                                 LocalDate today,
                                                                 List<PastoralFollowUp> followUps,
                                                                 PastoralRecommendationSettings settings) {
        List<PastoralRecommendation> recommendations = new ArrayList<>();
        Set<String> added = new HashSet<>();
        Set<String> dismissed = activeDismissalKeys(churchId, today);
        Set<Long> peopleWithOpenFollowUp = new HashSet<>();
        Set<Long> peopleWithAnyFollowUp = new HashSet<>();
        for (PastoralFollowUp followUp : followUps) {
            if (followUp.getPerson() != null && followUp.getPerson().getId() != null) {
                peopleWithAnyFollowUp.add(followUp.getPerson().getId());
            }
            if (followUp.getStatus() != PastoralFollowUpStatus.DONE && followUp.getPerson() != null && followUp.getPerson().getId() != null) {
                peopleWithOpenFollowUp.add(followUp.getPerson().getId());
            }
        }

        List<AttendanceRecord> recentRecords = attendanceRecordRepository.findByChurchIdAndAttendanceDateBetweenOrderByAttendanceDateDescServiceSessionAscRecordedAtDesc(
                churchId,
                today.minusDays(settings.analysisDays()),
                today
        );
        Map<Long, List<AttendanceRecord>> byPerson = new LinkedHashMap<>();
        for (AttendanceRecord record : recentRecords) {
            if (record.getPerson() == null || record.getPerson().getId() == null) continue;
            byPerson.computeIfAbsent(record.getPerson().getId(), ignored -> new ArrayList<>()).add(record);
        }
        Set<Long> peopleWithAttendance = byPerson.keySet();
        Set<Long> peopleInGroups = peopleInGroups(churchId);

        followUps.stream()
                .filter(ignored -> settings.overdueFollowUpEnabled())
                .filter(item -> item.getStatus() != PastoralFollowUpStatus.DONE)
                .filter(item -> item.getNextContactDate() != null && item.getNextContactDate().isBefore(today))
                .forEach(item -> {
                    Person person = item.getPerson();
                    if (person == null || person.getId() == null) return;
                    String detail = "Scadent din " + item.getNextContactDate();
                    addRecommendation(recommendations, added, dismissed, person, "Follow-up întârziat",
                            detail,
                            "/follow-ups?personId=" + person.getId(),
                            "Vezi follow-up",
                            null);
                });

        for (Map.Entry<Long, List<AttendanceRecord>> entry : byPerson.entrySet()) {
            List<AttendanceRecord> records = entry.getValue();
            if (records.size() < settings.absenceCount()) continue;
            List<AttendanceRecord> latestRecords = records.stream().limit(settings.absenceCount()).toList();
            boolean allAbsent = latestRecords.stream().allMatch(record -> record.getStatus() == AttendanceStatus.ABSENT);
            if (!allAbsent) continue;

            Person person = latestRecords.get(0).getPerson();
            if (person == null || person.getId() == null) continue;
            String type = person.getMemberType() == MemberType.CHILD ? "Copil absent" : "Absență";
            if ((person.getMemberType() == MemberType.CHILD && !settings.childAbsenceEnabled())
                    || (person.getMemberType() != MemberType.CHILD && !settings.absenceEnabled())) {
                continue;
            }
            String detail = person.getMemberType() == MemberType.CHILD
                    ? "Copil absent la ultimele " + settings.absenceCount() + " programe înregistrate"
                    : "Absent la ultimele " + settings.absenceCount() + " programe înregistrate";
            String actionLabel = peopleWithOpenFollowUp.contains(person.getId()) ? "Vezi follow-up" : "Creează follow-up";
            String actionHref = peopleWithOpenFollowUp.contains(person.getId())
                    ? "/follow-ups?personId=" + person.getId()
                    : "/follow-ups/new?personId=" + person.getId();
            addRecommendation(recommendations, added, dismissed, person, type, detail, actionHref, actionLabel, createFollowUpHref(person, type, detail, settings.templateFor(type)));
        }

        for (Person person : persons) {
            if (person.getId() == null) continue;
            if (settings.withoutGroupEnabled() && person.getMemberType() == MemberType.MEMBER && !peopleInGroups.contains(person.getId())) {
                String detail = "Membru fără grup sau slujire alocată";
                addRecommendation(recommendations, added, dismissed, person, "Fără grup",
                        detail,
                        "/groups",
                        "Vezi grupuri",
                        createFollowUpHref(person, "Fără grup", detail, settings.templateFor("Fără grup")));
            }
        }

        for (Person person : persons) {
            if (person.getId() == null) continue;
            if (settings.newPersonEnabled()
                    && person.getMemberType() == MemberType.FREND
                    && !peopleWithAttendance.contains(person.getId())
                    && !peopleWithAnyFollowUp.contains(person.getId())) {
                String detail = "Prieten/aparținător fără prezențe sau follow-up înregistrat";
                addRecommendation(recommendations, added, dismissed, person, "Persoană nouă",
                        detail,
                        createFollowUpHref(person, "Persoană nouă", detail, settings.templateFor("Persoană nouă")),
                        "Creează follow-up",
                        null);
            }
        }

        for (Person person : persons) {
            if (!settings.birthdayEnabled()) break;
            if (person.getId() == null || person.getBirthDate() == null) continue;
            long daysUntilBirthday = daysUntilBirthday(today, person.getBirthDate());
            if (daysUntilBirthday >= 0 && daysUntilBirthday <= settings.birthdayWindowDays()) {
                String detail = daysUntilBirthday == 0
                        ? "Zi de naștere astăzi"
                        : "Zi de naștere în " + daysUntilBirthday + " zile";
                addRecommendation(recommendations, added, dismissed, person, "Zi de naștere",
                        detail,
                        createFollowUpHref(person, "Zi de naștere", detail, settings.templateFor("Zi de naștere")),
                        "Creează follow-up",
                        null);
            }
        }

        return recommendations.stream().limit(settings.maxRecommendations()).toList();
    }

    private String displayName(Person person) {
        if (person == null) return "Persoană";
        String first = person.getFirstName() == null ? "" : person.getFirstName();
        String last = person.getLastName() == null ? "" : person.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "Persoană" : name;
    }

    public record PastoralRecommendation(Long personId,
                                         String personName,
                                         String type,
                                         String detail,
                                         String personHref,
                                         String actionHref,
                                         String actionLabel,
                                         String followUpHref) {}

    public record PastoralInboxItem(Long followUpId,
                                    Long personId,
                                    String personName,
                                    String caseType,
                                    String reason,
                                    String workflowStage,
                                    String riskLevel,
                                    int riskScore,
                                    String nextActionHref,
                                    String nextActionLabel,
                                    long overdueDays,
                                    String statusLabel) {}

    public record PastoralPipeline(int newCount,
                                   int contactedCount,
                                   int inProgressCount,
                                   int resolvedCount,
                                   int activeCount) {}

    public record PastoralSla(int highRiskCount,
                              int breachedCount,
                              int compliantCount,
                              int thresholdDays,
                              int complianceRate) {}

    public record PastoralReport(int closedLoopRate,
                                 int avgDaysToContact,
                                 int activeCases) {}

    public record LeaderCaseLoad(String leaderName, int activeCases) {}

    public record PastoralDashboardMetrics(PastoralPipeline pipeline,
                                           PastoralSla sla,
                                           PastoralReport report,
                                           List<LeaderCaseLoad> activeCasesByLeader) {}

    public record HiddenPastoralRecommendation(Long id,
                                               String personName,
                                               String type,
                                               String dismissedUntilLabel,
                                               String personHref) {}

    public record PastoralRecommendationSettings(int absenceCount,
                                                 int analysisDays,
                                                 int birthdayWindowDays,
                                                 int snoozeDays,
                                                 int slaDays,
                                                 int maxRecommendations,
                                                 boolean absenceEnabled,
                                                 boolean childAbsenceEnabled,
                                                 boolean overdueFollowUpEnabled,
                                                 boolean withoutGroupEnabled,
                                                 boolean newPersonEnabled,
                                                 boolean birthdayEnabled,
                                                 String absenceTemplate,
                                                 String childAbsenceTemplate,
                                                 String overdueFollowUpTemplate,
                                                 String withoutGroupTemplate,
                                                 String newPersonTemplate,
                                                 String birthdayTemplate) {
        public String templateFor(String type) {
            return switch (type) {
                case "Absență" -> absenceTemplate;
                case "Copil absent" -> childAbsenceTemplate;
                case "Follow-up întârziat" -> overdueFollowUpTemplate;
                case "Fără grup" -> withoutGroupTemplate;
                case "Persoană nouă" -> newPersonTemplate;
                case "Zi de naștere" -> birthdayTemplate;
                default -> "";
            };
        }
    }

    private void addRecommendation(List<PastoralRecommendation> recommendations,
                                   Set<String> added,
                                   Set<String> dismissed,
                                   Person person,
                                   String type,
                                   String detail,
                                   String actionHref,
                                   String actionLabel,
                                   String followUpHref) {
        String key = type + ":" + person.getId();
        if (dismissed.contains(key)) return;
        if (!added.add(key)) return;
        recommendations.add(new PastoralRecommendation(
                person.getId(),
                displayName(person),
                type,
                detail,
                "/persons/" + person.getId(),
                actionHref,
                actionLabel,
                followUpHref == null ? actionHref : followUpHref
        ));
    }

    private Set<Long> peopleInGroups(Long churchId) {
        Set<Long> ids = new HashSet<>();
        for (ChurchGroup group : groupRepository.findAllByChurchId(churchId, Sort.unsorted())) {
            if (group.getMembers() == null) continue;
            group.getMembers().stream()
                    .filter(person -> person.getId() != null)
                    .forEach(person -> ids.add(person.getId()));
        }
        return ids;
    }

    private long daysUntilBirthday(LocalDate today, LocalDate birthDate) {
        LocalDate nextBirthday = birthDate.withYear(today.getYear());
        if (nextBirthday.isBefore(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }
        return ChronoUnit.DAYS.between(today, nextBirthday);
    }

    private String createFollowUpHref(Person person, String type, String detail, String template) {
        String notes = "Recomandare: " + type + "\nMotiv: " + detail;
        if (template != null && !template.isBlank()) {
            notes += "\n\nPas recomandat: " + template.trim();
        }
        return "/follow-ups/new?personId=" + person.getId()
                + "&source=dashboard"
                + "&reason=" + URLEncoder.encode(notes, StandardCharsets.UTF_8);
    }

    private PastoralRecommendationSettings pastoralRecommendationSettings() {
        return new PastoralRecommendationSettings(
                intSetting("pastoral_absence_count", 3, 1, 10),
                intSetting("pastoral_analysis_days", 90, 7, 365),
                intSetting("pastoral_birthday_window_days", 14, 0, 90),
                intSetting("pastoral_snooze_days", 7, 1, 90),
                intSetting("pastoral_sla_days", 7, 1, 30),
                intSetting("pastoral_max_recommendations", 12, 1, 50),
                booleanSetting("pastoral_enable_absence", true),
                booleanSetting("pastoral_enable_child_absence", true),
                booleanSetting("pastoral_enable_overdue_follow_up", true),
                booleanSetting("pastoral_enable_without_group", true),
                booleanSetting("pastoral_enable_new_person", true),
                booleanSetting("pastoral_enable_birthday", true),
                stringSetting("pastoral_template_absence", defaultTemplate("Absență")),
                stringSetting("pastoral_template_child_absence", defaultTemplate("Copil absent")),
                stringSetting("pastoral_template_overdue_follow_up", defaultTemplate("Follow-up întârziat")),
                stringSetting("pastoral_template_without_group", defaultTemplate("Fără grup")),
                stringSetting("pastoral_template_new_person", defaultTemplate("Persoană nouă")),
                stringSetting("pastoral_template_birthday", defaultTemplate("Zi de naștere"))
        );
    }

    private int intSetting(String key, int fallback, int min, int max) {
        int value = globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getIntValue)
                .filter(item -> item != null && item >= min)
                .orElse(fallback);
        return Math.min(max, Math.max(min, value));
    }

    private boolean booleanSetting(String key, boolean fallback) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .map(value -> {
                    String normalized = value == null ? "" : value.trim().toLowerCase();
                    return !("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized));
                })
                .orElse(fallback);
    }

    private String stringSetting(String key, String fallback) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(fallback);
    }

    private String defaultTemplate(String type) {
        return switch (type) {
            case "Absență" -> "Contactează persoana pentru a verifica dacă are nevoie de sprijin și notează următorul pas.";
            case "Copil absent" -> "Ia legătura cu părinții și verifică dacă există o nevoie practică sau pastorală.";
            case "Follow-up întârziat" -> "Reia contactul și actualizează statusul follow-up-ului după discuție.";
            case "Fără grup" -> "Verifică dacă persoana poate fi invitată într-un grup sau într-o slujire potrivită.";
            case "Persoană nouă" -> "Programează un prim contact și identifică un grup potrivit pentru integrare.";
            case "Zi de naștere" -> "Trimite un mesaj de felicitare și notează dacă apare o nevoie de follow-up.";
            default -> "Stabilește următorul pas pastoral și notează rezultatul.";
        };
    }

    private Set<String> activeDismissalKeys(Long churchId, LocalDate today) {
        Set<String> keys = new HashSet<>();
        for (PastoralRecommendationDismissal dismissal : recommendationDismissalRepository.findAllByChurchIdAndDismissedUntilGreaterThanEqual(churchId, today)) {
            keys.add(dismissal.getRecommendationType() + ":" + dismissal.getPersonId());
        }
        return keys;
    }

    private List<HiddenPastoralRecommendation> hiddenPastoralRecommendations(Long churchId, List<Person> persons) {
        Map<Long, Person> peopleById = new LinkedHashMap<>();
        for (Person person : persons) {
            if (person.getId() != null) {
                peopleById.put(person.getId(), person);
            }
        }
        return recommendationDismissalRepository.findTop12ByChurchIdOrderByUpdatedAtDesc(churchId).stream()
                .map(item -> {
                    Person person = peopleById.get(item.getPersonId());
                    String until = item.getDismissedUntil() != null && item.getDismissedUntil().getYear() >= 9999
                            ? "tratat"
                            : "amânat până la " + item.getDismissedUntil();
                    return new HiddenPastoralRecommendation(
                            item.getId(),
                            person == null ? "Persoană #" + item.getPersonId() : displayName(person),
                            item.getRecommendationType(),
                            until,
                            "/persons/" + item.getPersonId()
                    );
                })
                .toList();
    }

    private List<PastoralInboxItem> pastoralDailyInbox(List<PastoralFollowUp> followUps,
                                                       List<PastoralRecommendation> recommendations,
                                                       PastoralRecommendationSettings settings,
                                                       LocalDate today) {
        List<PastoralInboxItem> items = new ArrayList<>();
        Set<Long> peopleWithActiveFollowUp = new HashSet<>();
        for (PastoralFollowUp followUp : followUps) {
            if (followUp.getStatus() == PastoralFollowUpStatus.DONE || followUp.getPerson() == null || followUp.getPerson().getId() == null) {
                continue;
            }
            peopleWithActiveFollowUp.add(followUp.getPerson().getId());
            long overdueDays = overdueDays(followUp, today);
            int score = followUpRiskScore(followUp, overdueDays, today, settings.slaDays());
            items.add(new PastoralInboxItem(
                    followUp.getId(),
                    followUp.getPerson().getId(),
                    displayName(followUp.getPerson()),
                    "Follow-up pastoral",
                    inboxReason(followUp, overdueDays),
                    workflowStageLabel(followUp),
                    riskLevelLabel(score),
                    score,
                    "/follow-ups/" + followUp.getId() + "/edit",
                    "Deschide cazul",
                    overdueDays,
                    workflowStageLabel(followUp)));
        }

        for (PastoralRecommendation recommendation : recommendations) {
            if (recommendation.personId() == null || peopleWithActiveFollowUp.contains(recommendation.personId())) {
                continue;
            }
            int score = recommendationRiskScore(recommendation.type());
            items.add(new PastoralInboxItem(
                    null,
                    recommendation.personId(),
                    recommendation.personName(),
                    recommendation.type(),
                    recommendation.detail(),
                    "New",
                    riskLevelLabel(score),
                    score,
                    recommendation.actionHref(),
                    recommendation.actionLabel(),
                    0,
                    "New"));
        }

        return items.stream()
                .sorted(Comparator
                        .comparingInt(PastoralInboxItem::riskScore).reversed()
                        .thenComparingLong(PastoralInboxItem::overdueDays).reversed()
                        .thenComparing(PastoralInboxItem::personName, String.CASE_INSENSITIVE_ORDER))
                .limit(settings.maxRecommendations())
                .toList();
    }

    private PastoralDashboardMetrics pastoralDashboardMetrics(List<PastoralFollowUp> followUps,
                                                              PastoralRecommendationSettings settings,
                                                              LocalDate today) {
        int newCount = 0;
        int contactedCount = 0;
        int inProgressCount = 0;
        int resolvedCount = 0;

        Map<String, Integer> leaderCases = new LinkedHashMap<>();
        List<PastoralFollowUp> highRiskActive = new ArrayList<>();
        List<Long> firstContactDays = new ArrayList<>();

        for (PastoralFollowUp followUp : followUps) {
            if (followUp.getStatus() == PastoralFollowUpStatus.DONE) {
                resolvedCount++;
            } else if (followUp.getStatus() == PastoralFollowUpStatus.IN_PROGRESS) {
                inProgressCount++;
            } else if (followUp.getLastContactDate() == null) {
                newCount++;
            } else {
                contactedCount++;
            }

            if (followUp.getStatus() != PastoralFollowUpStatus.DONE) {
                String leader = "Nealocat";
                if (followUp.getGroup() != null && followUp.getGroup().getLeader() != null) {
                    leader = displayName(followUp.getGroup().getLeader());
                }
                leaderCases.merge(leader, 1, Integer::sum);
                if (isHighRisk(followUp, today, settings.slaDays())) {
                    highRiskActive.add(followUp);
                }
            }

            if (followUp.getLastContactDate() != null) {
                long days = ChronoUnit.DAYS.between(followUp.getCreatedAt().toLocalDate(), followUp.getLastContactDate());
                if (days >= 0 && days <= 3650) {
                    firstContactDays.add(days);
                }
            }
        }

        int activeCount = newCount + contactedCount + inProgressCount;
        int totalCases = followUps.size();
        int closedLoopRate = totalCases == 0 ? 100 : (int) Math.round((resolvedCount * 100.0) / totalCases);
        DoubleSummaryStatistics contactStats = firstContactDays.stream().mapToDouble(Long::doubleValue).summaryStatistics();
        int avgDaysToContact = contactStats.getCount() == 0 ? 0 : (int) Math.round(contactStats.getAverage());

        int breached = (int) highRiskActive.stream()
                .filter(item -> {
                    LocalDate lastContact = item.getLastContactDate() != null ? item.getLastContactDate() : item.getCreatedAt().toLocalDate();
                    return lastContact.isBefore(today.minusDays(settings.slaDays()));
                })
                .count();
        int highRiskCount = highRiskActive.size();
        int compliantCount = Math.max(0, highRiskCount - breached);
        int complianceRate = highRiskCount == 0 ? 100 : (int) Math.round((compliantCount * 100.0) / highRiskCount);

        List<LeaderCaseLoad> activeCasesByLeader = leaderCases.entrySet().stream()
                .map(entry -> new LeaderCaseLoad(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingInt(LeaderCaseLoad::activeCases).reversed()
                        .thenComparing(LeaderCaseLoad::leaderName, String.CASE_INSENSITIVE_ORDER))
                .limit(6)
                .toList();

        return new PastoralDashboardMetrics(
                new PastoralPipeline(newCount, contactedCount, inProgressCount, resolvedCount, activeCount),
                new PastoralSla(highRiskCount, breached, compliantCount, settings.slaDays(), complianceRate),
                new PastoralReport(closedLoopRate, avgDaysToContact, activeCount),
                activeCasesByLeader);
    }

    private long overdueDays(PastoralFollowUp followUp, LocalDate today) {
        if (followUp.getNextContactDate() == null || !followUp.getNextContactDate().isBefore(today)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(followUp.getNextContactDate(), today);
    }

    private int followUpRiskScore(PastoralFollowUp followUp, long overdueDays, LocalDate today, int slaDays) {
        int score = followUp.getStatus() == PastoralFollowUpStatus.OPEN ? 40 : 30;
        if (followUp.getLastContactDate() == null) {
            score += 15;
        }
        if (followUp.getNextContactDate() == null) {
            score += 10;
        }
        if (overdueDays > 0) {
            score += Math.min(40, 8 + (int) (overdueDays * 4));
        }
        LocalDate lastContact = followUp.getLastContactDate() != null ? followUp.getLastContactDate() : followUp.getCreatedAt().toLocalDate();
        if (lastContact.isBefore(today.minusDays(slaDays))) {
            score += 20;
        }
        return Math.min(100, score);
    }

    private int recommendationRiskScore(String type) {
        return switch (type) {
            case "Follow-up întârziat" -> 95;
            case "Absență" -> 85;
            case "Copil absent" -> 80;
            case "Fără grup" -> 65;
            case "Persoană nouă" -> 58;
            case "Zi de naștere" -> 45;
            default -> 50;
        };
    }

    private String riskLevelLabel(int score) {
        if (score >= 90) return "Critic";
        if (score >= 70) return "Ridicat";
        if (score >= 50) return "Mediu";
        return "Scăzut";
    }

    private String workflowStageLabel(PastoralFollowUp followUp) {
        if (followUp.getStatus() == PastoralFollowUpStatus.DONE) {
            return "Resolved";
        }
        if (followUp.getStatus() == PastoralFollowUpStatus.IN_PROGRESS) {
            return "In Progress";
        }
        if (followUp.getLastContactDate() != null) {
            return "Contacted";
        }
        return "New";
    }

    private String inboxReason(PastoralFollowUp followUp, long overdueDays) {
        if (overdueDays > 0) {
            return "Următorul contact este întârziat cu " + overdueDays + " zile.";
        }
        if (followUp.getLastContactDate() == null) {
            return "Nu există încă un contact înregistrat pentru caz.";
        }
        if (followUp.getNextContactDate() != null) {
            return "Următor contact planificat: " + followUp.getNextContactDate() + ".";
        }
        return "Caz activ, fără dată pentru următorul contact.";
    }

    private boolean isHighRisk(PastoralFollowUp followUp, LocalDate today, int slaDays) {
        long overdueDays = overdueDays(followUp, today);
        if (overdueDays >= 1) {
            return true;
        }
        LocalDate lastContact = followUp.getLastContactDate() != null ? followUp.getLastContactDate() : followUp.getCreatedAt().toLocalDate();
        return lastContact.isBefore(today.minusDays(slaDays));
    }

    private String safeDashboardRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || !redirect.startsWith("/")) {
            return "redirect:/dashboard";
        }
        if (redirect.startsWith("//") || redirect.contains("://")) {
            return "redirect:/dashboard";
        }
        return "redirect:" + redirect;
    }
}
