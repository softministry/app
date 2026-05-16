package ro.church_office.teamleaf.web;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.followup.PastoralFollowUp;
import ro.church_office.info.followup.PastoralFollowUpRepository;
import ro.church_office.info.followup.PastoralFollowUpStatus;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/follow-ups")
public class PastoralFollowUpWebController {

    private final ChurchContextService churchContextService;
    private final PastoralFollowUpRepository followUpRepository;
    private final PersonRepository personRepository;
    private final ChurchGroupRepository groupRepository;

    public PastoralFollowUpWebController(ChurchContextService churchContextService,
                                         PastoralFollowUpRepository followUpRepository,
                                         PersonRepository personRepository,
                                         ChurchGroupRepository groupRepository) {
        this.churchContextService = churchContextService;
        this.followUpRepository = followUpRepository;
        this.personRepository = personRepository;
        this.groupRepository = groupRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) PastoralFollowUpStatus status,
                       @RequestParam(value = "due", defaultValue = "false") boolean due,
                       @RequestParam(value = "groupId", required = false) Long groupId,
                       @RequestParam(value = "personId", required = false) Long personId,
                       @RequestParam(value = "q", required = false) String q,
                       Model model) {
        populateListModel(status, due, groupId, personId, q, model);
        return "follow-ups/list";
    }

    @PostMapping("/results")
    public String listResults(@RequestParam(value = "status", required = false) PastoralFollowUpStatus status,
                              @RequestParam(value = "due", defaultValue = "false") boolean due,
                              @RequestParam(value = "groupId", required = false) Long groupId,
                              @RequestParam(value = "personId", required = false) Long personId,
                              @RequestParam(value = "q", required = false) String q,
                              Model model) {
        populateListModel(status, due, groupId, personId, q, model);
        return "follow-ups/list :: resultsSection";
    }

    private void populateListModel(PastoralFollowUpStatus status,
                                   boolean due,
                                   Long groupId,
                                   Long personId,
                                   String q,
                                   Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        List<PastoralFollowUp> followUps = filteredFollowUps(churchId, status, due, groupId, personId, q);
        model.addAttribute("followUps", followUps);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("due", due);
        model.addAttribute("selectedGroupId", groupId);
        model.addAttribute("selectedPersonId", personId);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("currentRedirect", currentRedirect(status, due, groupId, personId, q));
        model.addAttribute("newCases", followUpsByStage(followUps, "NEW"));
        model.addAttribute("contactedCases", followUpsByStage(followUps, "CONTACTED"));
        model.addAttribute("inProgressCases", followUpsByStage(followUps, "IN_PROGRESS"));
        model.addAttribute("resolvedCases", followUpsByStage(followUps, "RESOLVED"));
        model.addAttribute("statuses", PastoralFollowUpStatus.values());
        model.addAttribute("groups", groupRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("type"), Sort.Order.asc("name"))));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam(value = "status", required = false) PastoralFollowUpStatus status,
                                         @RequestParam(value = "due", defaultValue = "false") boolean due,
                                         @RequestParam(value = "groupId", required = false) Long groupId,
                                         @RequestParam(value = "personId", required = false) Long personId,
                                         @RequestParam(value = "q", required = false) String q) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        String csv = followUpsCsv(filteredFollowUps(churchId, status, due, groupId, personId, q));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"follow-up-pastoral.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body('\ufeff' + csv);
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(value = "personId", required = false) Long personId,
                             @RequestParam(value = "reason", required = false) String reason,
                             @RequestParam(value = "source", required = false) String source,
                             Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        FollowUpForm form = new FollowUpForm();
        form.setStatus(PastoralFollowUpStatus.OPEN);
        form.setNextContactDate(LocalDate.now().plusDays(7));
        if (personId != null && personRepository.findByIdAndChurchId(personId, churchId).isPresent()) {
            form.setPersonId(personId);
        }
        if (reason != null && !reason.isBlank()) {
            form.setNotes(reason);
        }
        if ("dashboard".equals(source)) {
            form.setRedirect("/dashboard");
        }
        model.addAttribute("followUpForm", form);
        attachMeta(model, churchId);
        return "follow-ups/form";
    }

    @PostMapping
    public String create(@ModelAttribute("followUpForm") FollowUpForm form,
                         RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        try {
            PastoralFollowUp followUp = new PastoralFollowUp();
            followUp.setChurchId(churchId);
            applyForm(followUp, form, churchId);
            followUpRepository.save(followUp);
            redirectAttributes.addFlashAttribute("success", "Follow-up-ul pastoral a fost adăugat.");
            return safeRedirect(form.getRedirect());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/follow-ups/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        PastoralFollowUp followUp = followUpRepository.findByIdAndChurchId(id, churchId).orElse(null);
        if (followUp == null) {
            redirectAttributes.addFlashAttribute("error", "Follow-up-ul nu a putut fi găsit.");
            return "redirect:/follow-ups";
        }
        model.addAttribute("followUpForm", FollowUpForm.fromEntity(followUp));
        attachMeta(model, churchId);
        return "follow-ups/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("followUpForm") FollowUpForm form,
                         RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        PastoralFollowUp followUp = followUpRepository.findByIdAndChurchId(id, churchId).orElse(null);
        if (followUp == null) {
            redirectAttributes.addFlashAttribute("error", "Follow-up-ul nu a putut fi găsit.");
            return "redirect:/follow-ups";
        }
        try {
            applyForm(followUp, form, churchId);
            followUpRepository.save(followUp);
            redirectAttributes.addFlashAttribute("success", "Follow-up-ul pastoral a fost actualizat.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/follow-ups/" + id + "/edit";
        }
        return safeRedirect(form.getRedirect());
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        followUpRepository.findByIdAndChurchId(id, churchId).ifPresent(followUpRepository::delete);
        redirectAttributes.addFlashAttribute("success", "Follow-up-ul pastoral a fost șters.");
        return "redirect:/follow-ups";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") PastoralFollowUpStatus status,
                               @RequestParam(value = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        PastoralFollowUp followUp = followUpRepository.findByIdAndChurchId(id, churchId).orElse(null);
        if (followUp == null) {
            redirectAttributes.addFlashAttribute("error", "Follow-up-ul nu a putut fi găsit.");
            return safeRedirect(redirect);
        }
        followUp.setStatus(status);
        if (status == PastoralFollowUpStatus.DONE && followUp.getLastContactDate() == null) {
            followUp.setLastContactDate(LocalDate.now());
        }
        followUp.setUpdatedAt(LocalDateTime.now());
        followUpRepository.save(followUp);
        redirectAttributes.addFlashAttribute("success", "Statusul follow-up-ului a fost actualizat.");
        return safeRedirect(redirect);
    }

    @PostMapping("/{id}/contacted")
    public String markContacted(@PathVariable Long id,
                                @RequestParam(value = "redirect", required = false) String redirect,
                                RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        PastoralFollowUp followUp = followUpRepository.findByIdAndChurchId(id, churchId).orElse(null);
        if (followUp == null) {
            redirectAttributes.addFlashAttribute("error", "Follow-up-ul nu a putut fi găsit.");
            return safeRedirect(redirect);
        }
        followUp.setStatus(PastoralFollowUpStatus.OPEN);
        followUp.setLastContactDate(LocalDate.now());
        if (followUp.getNextContactDate() == null) {
            followUp.setNextContactDate(LocalDate.now().plusDays(7));
        }
        followUp.setUpdatedAt(LocalDateTime.now());
        followUpRepository.save(followUp);
        redirectAttributes.addFlashAttribute("success", "Cazul a fost marcat ca «Contactat».");
        return safeRedirect(redirect);
    }

    @PostMapping("/stage")
    public String updateStage(@RequestParam("id") Long id,
                              @RequestParam("stage") String stage,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        PastoralFollowUp followUp = followUpRepository.findByIdAndChurchId(id, churchId).orElse(null);
        if (followUp == null) {
            redirectAttributes.addFlashAttribute("error", "Follow-up-ul nu a putut fi găsit.");
            return safeRedirect(redirect);
        }
        try {
            applyStage(followUp, stage);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return safeRedirect(redirect);
        }
        followUp.setUpdatedAt(LocalDateTime.now());
        followUpRepository.save(followUp);
        redirectAttributes.addFlashAttribute("success", "Stadiul cazului a fost actualizat.");
        return safeRedirect(redirect);
    }

    private void applyForm(PastoralFollowUp followUp, FollowUpForm form, Long churchId) {
        Person person = personRepository.findByIdAndChurchId(form.getPersonId(), churchId)
                .orElseThrow(() -> new IllegalArgumentException("Selectează o persoană validă."));
        ChurchGroup group = null;
        if (form.getGroupId() != null) {
            group = groupRepository.findByIdAndChurchId(form.getGroupId(), churchId)
                    .orElseThrow(() -> new IllegalArgumentException("Selectează un grup valid."));
        }
        followUp.setPerson(person);
        followUp.setGroup(group);
        followUp.setStatus(form.getStatus() == null ? PastoralFollowUpStatus.OPEN : form.getStatus());
        followUp.setContactMethod(form.getContactMethod());
        followUp.setLastContactDate(form.getLastContactDate());
        followUp.setNextContactDate(form.getNextContactDate());
        followUp.setNotes(form.getNotes());
        followUp.setUpdatedAt(LocalDateTime.now());
    }

    private void attachMeta(Model model, Long churchId) {
        model.addAttribute("statuses", PastoralFollowUpStatus.values());
        model.addAttribute("persons", personRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName"))));
        model.addAttribute("groups", groupRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("type"), Sort.Order.asc("name"))));
    }

    private List<PastoralFollowUp> filteredFollowUps(Long churchId,
                                                     PastoralFollowUpStatus status,
                                                     boolean due,
                                                     Long groupId,
                                                     Long personId,
                                                     String q) {
        String normalizedQuery = (q == null || q.isBlank()) ? null : normalize(q);
        String queryPattern = normalizedQuery == null ? null : "%" + normalizedQuery + "%";
        List<PastoralFollowUp> followUps = followUpRepository.findFiltered(churchId, status, groupId, personId, queryPattern);
        if (due) {
            LocalDate today = LocalDate.now();
            followUps = followUps.stream()
                    .filter(item -> item.getStatus() != PastoralFollowUpStatus.DONE)
                    .filter(item -> item.getNextContactDate() != null && !item.getNextContactDate().isAfter(today))
                    .toList();
        }
        return followUps.stream()
                .sorted(Comparator
                        .comparing(PastoralFollowUp::getStatus)
                        .thenComparing(PastoralFollowUp::getNextContactDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PastoralFollowUp::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<PastoralFollowUp> followUpsByStage(List<PastoralFollowUp> followUps, String stage) {
        return followUps.stream()
                .filter(item -> switch (stage) {
                    case "NEW" -> item.getStatus() == PastoralFollowUpStatus.OPEN && item.getLastContactDate() == null;
                    case "CONTACTED" -> item.getStatus() == PastoralFollowUpStatus.OPEN && item.getLastContactDate() != null;
                    case "IN_PROGRESS" -> item.getStatus() == PastoralFollowUpStatus.IN_PROGRESS;
                    case "RESOLVED" -> item.getStatus() == PastoralFollowUpStatus.DONE;
                    default -> false;
                })
                .sorted(Comparator
                        .comparing(PastoralFollowUp::getNextContactDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PastoralFollowUp::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void applyStage(PastoralFollowUp followUp, String stage) {
        String normalized = normalize(stage);
        switch (normalized) {
            case "new" -> {
                followUp.setStatus(PastoralFollowUpStatus.OPEN);
                followUp.setLastContactDate(null);
            }
            case "contacted" -> {
                followUp.setStatus(PastoralFollowUpStatus.OPEN);
                if (followUp.getLastContactDate() == null) {
                    followUp.setLastContactDate(LocalDate.now());
                }
                if (followUp.getNextContactDate() == null) {
                    followUp.setNextContactDate(LocalDate.now().plusDays(7));
                }
            }
            case "in_progress" -> followUp.setStatus(PastoralFollowUpStatus.IN_PROGRESS);
            case "resolved" -> followUp.setStatus(PastoralFollowUpStatus.DONE);
            default -> throw new IllegalArgumentException("Stadiul selectat nu este valid.");
        }
    }

    private String followUpsCsv(List<PastoralFollowUp> followUps) {
        StringBuilder csv = new StringBuilder("Persoana,Telefon,Grup,Status,Metoda contact,Ultimul contact,Urmator contact,Notite\n");
        for (PastoralFollowUp item : followUps) {
            Person person = item.getPerson();
            ChurchGroup group = item.getGroup();
            csv.append(csvLine(
                    personName(person),
                    person == null ? "" : value(person.getPhone()),
                    group == null ? "" : value(group.getName()),
                    statusLabel(item.getStatus()),
                    value(item.getContactMethod()),
                    value(item.getLastContactDate()),
                    value(item.getNextContactDate()),
                    value(item.getNotes())));
        }
        return csv.toString();
    }

    private String csvLine(String... values) {
        return java.util.Arrays.stream(values)
                .map(this::csvCell)
                .collect(Collectors.joining(",")) + "\n";
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String personName(Person person) {
        if (person == null) {
            return "";
        }
        return (value(person.getFirstName()) + " " + value(person.getLastName())).trim();
    }

    private String statusLabel(PastoralFollowUpStatus status) {
        if (status == PastoralFollowUpStatus.IN_PROGRESS) {
            return "În lucru";
        }
        if (status == PastoralFollowUpStatus.DONE) {
            return "Finalizat";
        }
        return "Deschis";
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || !redirect.startsWith("/")) {
            return "redirect:/follow-ups";
        }
        if (redirect.startsWith("//") || redirect.contains("://")) {
            return "redirect:/follow-ups";
        }
        return "redirect:" + redirect;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String currentRedirect(PastoralFollowUpStatus status, boolean due, Long groupId, Long personId, String q) {
        StringBuilder redirect = new StringBuilder("/follow-ups");
        String separator = "?";
        if (status != null) {
            redirect.append(separator).append("status=").append(status.name());
            separator = "&";
        }
        if (due) {
            redirect.append(separator).append("due=true");
            separator = "&";
        }
        if (groupId != null) {
            redirect.append(separator).append("groupId=").append(groupId);
            separator = "&";
        }
        if (personId != null) {
            redirect.append(separator).append("personId=").append(personId);
            separator = "&";
        }
        if (q != null && !q.isBlank()) {
            redirect.append(separator).append("q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8));
        }
        return redirect.toString();
    }

    public static class FollowUpForm {
        private Long id;
        private Long personId;
        private Long groupId;
        private PastoralFollowUpStatus status = PastoralFollowUpStatus.OPEN;
        private String contactMethod;
        private LocalDate lastContactDate;
        private LocalDate nextContactDate;
        private String notes;
        private String redirect;

        public static FollowUpForm fromEntity(PastoralFollowUp followUp) {
            FollowUpForm form = new FollowUpForm();
            form.setId(followUp.getId());
            form.setPersonId(followUp.getPerson() == null ? null : followUp.getPerson().getId());
            form.setGroupId(followUp.getGroup() == null ? null : followUp.getGroup().getId());
            form.setStatus(followUp.getStatus());
            form.setContactMethod(followUp.getContactMethod());
            form.setLastContactDate(followUp.getLastContactDate());
            form.setNextContactDate(followUp.getNextContactDate());
            form.setNotes(followUp.getNotes());
            return form;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPersonId() { return personId; }
        public void setPersonId(Long personId) { this.personId = personId; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public PastoralFollowUpStatus getStatus() { return status; }
        public void setStatus(PastoralFollowUpStatus status) { this.status = status; }
        public String getContactMethod() { return contactMethod; }
        public void setContactMethod(String contactMethod) { this.contactMethod = contactMethod; }
        public LocalDate getLastContactDate() { return lastContactDate; }
        public void setLastContactDate(LocalDate lastContactDate) { this.lastContactDate = lastContactDate; }
        public LocalDate getNextContactDate() { return nextContactDate; }
        public void setNextContactDate(LocalDate nextContactDate) { this.nextContactDate = nextContactDate; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getRedirect() { return redirect; }
        public void setRedirect(String redirect) { this.redirect = redirect; }
    }
}
