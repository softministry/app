package ro.church_office.teamleaf.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ro.church_office.info.attendance.AttendanceRecord;
import ro.church_office.info.attendance.AttendanceRecordRepository;
import ro.church_office.info.attendance.AttendanceStatus;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.followup.PastoralFollowUpRepository;
import ro.church_office.info.followup.PastoralPrivateNote;
import ro.church_office.info.followup.PastoralPrivateNoteRepository;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.groups.ChurchGroupRepository.PersonGroupMembership;
import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;
import ro.church_office.info.person.service.PersonService;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.teamleaf.security.CurrentUserService;
import ro.church_office.info.visits.dao.Visit;
import ro.church_office.info.visits.dao.VisitRepository;

@Controller
@RequestMapping("/persons")
public class PersonWebController {

    private static final DateTimeFormatter NOTE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Set<String> NOTE_ROLE_OPTIONS = Set.of("ADMIN", "PASTOR", "ELDER", "GROUP_LEADER", "SECRETARY");
    private static final Set<String> NOTE_MANAGE_ROLES = Set.of("ADMIN", "PASTOR", "ELDER");
    private static final String ROWS_KEY = "rows_per_page";

    private final PersonRepository personRepository;
    private final ChurchContextService churchContextService;
    private final PersonService personService;
    private final ChurchGroupRepository groupRepository;
    private final PastoralFollowUpRepository followUpRepository;
    private final PastoralPrivateNoteRepository privateNoteRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final VisitRepository visitRepository;
    private final CurrentUserService currentUserService;
    private final GlobalSettingRepository globalSettingRepository;

    public PersonWebController(PersonRepository personRepository,
                               ChurchContextService churchContextService,
                               PersonService personService,
                               ChurchGroupRepository groupRepository,
                               PastoralFollowUpRepository followUpRepository,
                               PastoralPrivateNoteRepository privateNoteRepository,
                               AttendanceRecordRepository attendanceRecordRepository,
                               VisitRepository visitRepository,
                               CurrentUserService currentUserService,
                               GlobalSettingRepository globalSettingRepository) {
        this.personRepository = personRepository;
        this.churchContextService = churchContextService;
        this.personService = personService;
        this.groupRepository = groupRepository;
        this.followUpRepository = followUpRepository;
        this.privateNoteRepository = privateNoteRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.visitRepository = visitRepository;
        this.currentUserService = currentUserService;
        this.globalSettingRepository = globalSettingRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "memberType", required = false) MemberType memberType,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "scrollOnly", defaultValue = "false") boolean scrollOnly,
                       Model model) {
        populateListModel(q, memberType, page, size, scrollOnly, model);
        return "persons/list";
    }

    @PostMapping("/results")
    public String listResults(@RequestParam(value = "q", required = false) String q,
                              @RequestParam(value = "memberType", required = false) MemberType memberType,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "size", required = false) Integer size,
                              @RequestParam(value = "scrollOnly", defaultValue = "false") boolean scrollOnly,
                              Model model) {
        populateListModel(q, memberType, page, size, scrollOnly, model);
        return "persons/list :: resultsSection";
    }

    private void populateListModel(String q,
                                   MemberType memberType,
                                   int page,
                                   Integer size,
                                   boolean scrollOnly,
                                   Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Sort sort = Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName"));

        List<PersonDTO> persons = (q != null && !q.isBlank()
                ? personRepository.searchByChurchAndName(churchId, q, sort)
                : personRepository.findAllByChurchId(churchId, sort)).stream()
                .map(PersonDTO::fromEntity)
                .collect(Collectors.toList());

        if (memberType != null) {
            persons = persons.stream().filter(p -> memberType.equals(p.getMemberType())).toList();
        }

        int defaultSize = defaultRowsPerPage();
        int normalizedSize = normalizeSize(size == null ? defaultSize : size);
        int totalItems = persons.size();
        int totalPages = scrollOnly ? 1 : Math.max(1, (int) Math.ceil((double) totalItems / normalizedSize));
        int currentPage = scrollOnly ? 1 : Math.min(Math.max(page, 1), totalPages);
        int fromIndex = scrollOnly ? 0 : Math.min((currentPage - 1) * normalizedSize, totalItems);
        int toIndex = scrollOnly ? totalItems : Math.min(fromIndex + normalizedSize, totalItems);

        List<PersonDTO> visiblePersons = persons.subList(fromIndex, toIndex);
        model.addAttribute("persons", visiblePersons);
        model.addAttribute("personGroupsById", personGroupsById(churchId, visiblePersons));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("memberType", memberType);
        model.addAttribute("memberTypes", MemberType.values());
        model.addAttribute("page", currentPage);
        model.addAttribute("size", normalizedSize);
        model.addAttribute("defaultSize", defaultSize);
        model.addAttribute("scrollOnly", scrollOnly);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSizes", List.of(5, 7, 10, 20, 25, 50, 100));
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        PersonDTO dto = new PersonDTO();
        dto.setMemberType(MemberType.MEMBER);
        model.addAttribute("personDto", dto);
        attachMeta(model, null);
        return "persons/form";
    }

    @PostMapping
    @Transactional
    public String create(@ModelAttribute("personDto") PersonDTO dto,
                         @RequestParam(value = "childrenIds", required = false) List<String> childrenIdsParam,
                         RedirectAttributes redirectAttributes) {
        try {
            dto.setChildrenIds(parseIds(childrenIdsParam));
            Person saved = saveOrUpdatePerson(null, dto);
            applyRelations(saved.getId(), dto.getSpouseId(), dto.getChildrenIds());
            redirectAttributes.addFlashAttribute("success", "Persoana a fost adăugată.");
            return "redirect:/persons/" + saved.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut salva persoana." : ex.getMessage());
            return "redirect:/persons";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Optional<Person> opt = personRepository.findByIdAndChurchId(id, churchId);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Persoana nu a putut fi găsită.");
            return "redirect:/persons";
        }
        Person person = opt.get();
        PersonDTO dto = PersonDTO.fromEntity(person);
        List<PersonDTO> allPersons = personRepository.findAllByChurchId(churchId, Sort.unsorted()).stream()
                .map(PersonDTO::fromEntity)
                .toList();
        java.util.Map<Long, PersonDTO> familyMap = allPersons.stream()
                .filter(item -> item.getId() != null)
                .collect(java.util.stream.Collectors.toMap(PersonDTO::getId, item -> item, (left, right) -> left, java.util.LinkedHashMap::new));
        model.addAttribute("person", dto);
        model.addAttribute("personAgeYears", completedYears(dto.getBirthDate()));
        model.addAttribute("familyKey", FamilyWebController.familyKeyFor(dto, familyMap));
        model.addAttribute("spouseName", displayName(person.getSpouse()));
        model.addAttribute("children", person.getChildren() == null ? List.of() : person.getChildren().stream()
                .filter(p -> Objects.equals(p.getChurchId(), churchId))
                .sorted((a, b) -> ((a.getLastName() == null ? "" : a.getLastName()) + (a.getFirstName() == null ? "" : a.getFirstName()))
                        .compareTo((b.getLastName() == null ? "" : b.getLastName()) + (b.getFirstName() == null ? "" : b.getFirstName())))
                .map(PersonDTO::fromEntity)
                .toList());
        model.addAttribute("groups", groupRepository.findAllByChurchIdAndMembers_Id(
                churchId,
                id,
                Sort.by(Sort.Order.asc("type"), Sort.Order.asc("name"))));
        model.addAttribute("followUps", followUpRepository.findAllByChurchIdAndPerson_Id(
                churchId,
                id,
                Sort.by(Sort.Order.asc("status"), Sort.Order.asc("nextContactDate"), Sort.Order.desc("updatedAt"))));
        String currentRole = currentRole();
        String currentUsername = currentUsername();
        boolean canManagePrivateNotes = canManagePrivateNotes(currentRole);
        model.addAttribute("privateNotes", privateNoteRepository.findAllByChurchIdAndPerson_IdOrderByUpdatedAtDesc(churchId, id).stream()
                .filter(note -> canViewPrivateNote(note, currentRole, currentUsername))
                .map(note -> toPrivateNoteView(note, canManagePrivateNotes, currentUsername, currentRole))
                .toList());
        model.addAttribute("canManagePrivateNotes", canManagePrivateNotes);
        model.addAttribute("privateNoteRoleOptions", privateNoteRoleOptions());
        model.addAttribute("privateNoteForm", new PrivateNoteForm());
        model.addAttribute("pastoralTimeline", pastoralTimeline(churchId, person));
        return "persons/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Optional<Person> opt = personRepository.findByIdAndChurchId(id, churchId);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Persoana nu a putut fi găsită.");
            return "redirect:/persons";
        }
        PersonDTO dto = PersonDTO.fromEntity(opt.get());
        if (dto.getMemberType() == null) dto.setMemberType(MemberType.MEMBER);
        model.addAttribute("personDto", dto);
        attachMeta(model, id);
        return "persons/form";
    }

    @PostMapping("/{id}")
    @Transactional
    public String update(@PathVariable Long id,
                         @ModelAttribute("personDto") PersonDTO dto,
                         @RequestParam(value = "childrenIds", required = false) List<String> childrenIdsParam,
                         RedirectAttributes redirectAttributes) {
        try {
            dto.setChildrenIds(parseIds(childrenIdsParam));
            Person saved = saveOrUpdatePerson(id, dto);
            applyRelations(saved.getId(), dto.getSpouseId(), dto.getChildrenIds());

            redirectAttributes.addFlashAttribute("success", "Modificările au fost salvate.");
            return "redirect:/persons/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-au putut salva modificările." : ex.getMessage());
            return "redirect:/persons/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            personService.deletePerson(id);
            redirectAttributes.addFlashAttribute("success", "Persoana a fost ștearsă.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut șterge persoana." : ex.getMessage());
        }
        return "redirect:/persons";
    }

    @PostMapping("/{id}/private-notes")
    public String createPrivateNote(@PathVariable Long id,
                                    @ModelAttribute("privateNoteForm") PrivateNoteForm form,
                                    RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Optional<Person> personOpt = personRepository.findByIdAndChurchId(id, churchId);
        if (personOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Persoana nu a putut fi găsită.");
            return "redirect:/persons";
        }

        String currentRole = currentRole();
        if (!canManagePrivateNotes(currentRole)) {
            redirectAttributes.addFlashAttribute("error", "Nu ai permisiunea de a adăuga notițe pastorale private.");
            return "redirect:/persons/" + id;
        }

        String text = form.getText() == null ? "" : form.getText().trim();
        if (text.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Conținutul notiței nu poate fi gol.");
            return "redirect:/persons/" + id;
        }

        String normalizedRole = normalizeRole(currentRole);
        List<String> requestedRoles = form.getAllowedRoles() == null ? List.of() : form.getAllowedRoles();
        List<String> allowedRoles = sanitizeAllowedRoles(requestedRoles, normalizedRole);

        PastoralPrivateNote note = new PastoralPrivateNote();
        note.setChurchId(churchId);
        note.setPerson(personOpt.get());
        note.setNoteText(text);
        note.setAllowedRoles(String.join(",", allowedRoles));
        note.setCreatedByUsername(currentUsername());
        note.setCreatedByRole(normalizedRole);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        privateNoteRepository.save(note);

        redirectAttributes.addFlashAttribute("success", "Notița pastorală privată a fost adăugată.");
        return "redirect:/persons/" + id;
    }

    @PostMapping("/{id}/private-notes/{noteId}/delete")
    public String deletePrivateNote(@PathVariable Long id,
                                    @PathVariable Long noteId,
                                    RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        String currentRole = currentRole();
        if (!canManagePrivateNotes(currentRole)) {
            redirectAttributes.addFlashAttribute("error", "Nu ai permisiunea de a șterge notițe pastorale private.");
            return "redirect:/persons/" + id;
        }

        Optional<PastoralPrivateNote> note = privateNoteRepository.findByIdAndChurchIdAndPerson_Id(noteId, churchId, id);
        if (note.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Notița nu a putut fi găsită.");
            return "redirect:/persons/" + id;
        }

        if (!canDeletePrivateNote(note.get(), currentRole, currentUsername())) {
            redirectAttributes.addFlashAttribute("error", "Nu ai permisiunea de a șterge această notiță.");
            return "redirect:/persons/" + id;
        }

        privateNoteRepository.delete(note.get());
        redirectAttributes.addFlashAttribute("success", "Notița pastorală privată a fost ștearsă.");
        return "redirect:/persons/" + id;
    }

    private void attachMeta(Model model, Long currentId) {
        model.addAttribute("memberTypes", MemberType.values());
        model.addAttribute("allPersons", personService.getAllPersons().stream()
                .filter(p -> currentId == null || p.getId() == null || !p.getId().equals(currentId))
                .toList());
    }

    private int normalizeSize(int size) {
        return switch (size) {
            case 5, 7, 10, 20, 25, 50, 100 -> size;
            default -> 7;
        };
    }

    private int defaultRowsPerPage() {
        return normalizeSize(globalSettingRepository.findBySettingKey(ROWS_KEY)
                .map(setting -> setting.getIntValue())
                .orElse(7));
    }

    private Person saveOrUpdatePerson(Long id, PersonDTO dto) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();

        Person person;
        if (id == null) {
            person = new Person();
            person.setChurchId(churchId);
        } else {
            person = personRepository.findByIdAndChurchId(id, churchId)
                    .orElseThrow(() -> new IllegalArgumentException("Persoana nu există în biserica activă."));
        }

        person.setFirstName(dto.getFirstName());
        person.setLastName(dto.getLastName());
        person.setPhone(dto.getPhone());
        person.setChurchRole(dto.getChurchRole());
        person.setAddress(dto.getAddress());
        person.setPosition(dto.getPosition());
        person.setBirthDate(dto.getBirthDate());
        person.setMemberType(dto.getMemberType() == null ? MemberType.MEMBER : dto.getMemberType());
        person.setChurchId(churchId);

        return personRepository.save(person);
    }

    private void applyRelations(Long personId, Long spouseId, List<Long> childrenIds) {
        // Keep relationship updates consistent with the existing REST semantics in church-office-backend.
        setSpouse(personId, spouseId);
        setChildren(personId, childrenIds);
    }

    private void setSpouse(Long id, Long spouseId) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Person person = personRepository.findByIdAndChurchId(id, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Persoana nu există în biserica activă."));

        if (spouseId == null) {
            Person old = person.getSpouse();
            person.setSpouse(null);
            personRepository.save(person);
            if (old != null && Objects.equals(old.getChurchId(), churchId)) {
                old.setSpouse(null);
                personRepository.save(old);
            }
            return;
        }

        if (spouseId.equals(id)) throw new IllegalArgumentException("Nu poți seta aceeași persoană ca soț/soție.");
        Person spouse = personRepository.findByIdAndChurchId(spouseId, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Soțul/soția selectat(ă) nu există în biserica activă."));

        Person prev = person.getSpouse();
        if (prev != null && !prev.getId().equals(spouse.getId()) && Objects.equals(prev.getChurchId(), churchId)) {
            prev.setSpouse(null);
            personRepository.save(prev);
        }

        Person spousePrev = spouse.getSpouse();
        if (spousePrev != null && !spousePrev.getId().equals(person.getId()) && Objects.equals(spousePrev.getChurchId(), churchId)) {
            spousePrev.setSpouse(null);
            personRepository.save(spousePrev);
        }

        person.setSpouse(spouse);
        spouse.setSpouse(person);
        personRepository.save(person);
        personRepository.save(spouse);
    }

    @Transactional
    private void setChildren(Long id, List<Long> childrenIds) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Person parent = personRepository.findByIdAndChurchId(id, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Persoana nu există în biserica activă."));
        Set<Long> existingChildIds = parent.getChildren() == null ? Set.of() : parent.getChildren().stream()
                .map(Person::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Person> newChildren = childrenIds == null ? Set.of() : childrenIds.stream()
                .map(pid -> personRepository.findByIdAndChurchId(pid, churchId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (newChildren.stream().anyMatch(c -> Objects.equals(c.getId(), parent.getId()))) {
            throw new IllegalArgumentException("Nu poți seta aceeași persoană ca și copil.");
        }

        for (Person child : newChildren) {
            boolean isNewRelation = child.getId() != null && !existingChildIds.contains(child.getId());
            if (isNewRelation && isAncestor(child, parent, churchId)) {
                throw new IllegalArgumentException("Setarea acestor relații ar crea un ciclu în arborele familiei.");
            }
        }

        personRepository.deleteChildLinks(parent.getId());
        for (Person child : newChildren) {
            personRepository.insertChildLink(parent.getId(), child.getId());
        }

        Person spouse = parent.getSpouse();
        if (spouse != null && Objects.equals(spouse.getChurchId(), churchId)) {
            personRepository.deleteChildLinks(spouse.getId());
            for (Person child : newChildren) {
                personRepository.insertChildLink(spouse.getId(), child.getId());
            }
        }

        parent.getChildren().clear();
        parent.getChildren().addAll(newChildren);
        personRepository.save(parent);
        if (spouse != null && Objects.equals(spouse.getChurchId(), churchId)) {
            spouse.getChildren().clear();
            spouse.getChildren().addAll(newChildren);
            personRepository.save(spouse);
        }
    }

    private boolean isAncestor(Person node, Person target, Long churchId) {
        if (node == null || target == null) return false;
        Set<Long> visited = new HashSet<>();
        Deque<Person> stack = new ArrayDeque<>();
        if (node.getParents() != null) {
            node.getParents().stream().filter(p -> Objects.equals(p.getChurchId(), churchId)).forEach(stack::push);
        }

        while (!stack.isEmpty()) {
            Person cur = stack.pop();
            if (cur == null || cur.getId() == null) continue;
            if (visited.contains(cur.getId())) continue;
            if (cur.getId().equals(target.getId())) return true;
            visited.add(cur.getId());
            if (cur.getParents() != null) {
                cur.getParents().stream().filter(p -> Objects.equals(p.getChurchId(), churchId)).forEach(stack::push);
            }
        }
        return false;
    }

    private String displayName(Person person) {
        if (person == null) return null;
        String first = person.getFirstName() == null ? "" : person.getFirstName();
        String last = person.getLastName() == null ? "" : person.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? null : name;
    }

    private List<PastoralTimelineItem> pastoralTimeline(Long churchId, Person person) {
        List<PastoralTimelineItem> items = new ArrayList<>();
        Long personId = person.getId();

        for (AttendanceRecord record : attendanceRecordRepository.findTop20ByChurchIdAndPersonIdOrderByAttendanceDateDescSessionAsc(churchId, personId)) {
            String status = record.getStatus() == AttendanceStatus.PRESENT ? "Prezent" : "Absent";
            String detail = record.getSession() == null ? status : status + " · " + record.getSession().name();
            items.add(new PastoralTimelineItem(record.getAttendanceDate(), "Prezență", detail,
                    "/attendance?date=" + record.getAttendanceDate() + "&session=" + (record.getSession() == null ? "" : record.getSession().name())));
        }

        followUpRepository.findAllByChurchIdAndPerson_Id(churchId, personId, Sort.by(Sort.Order.desc("updatedAt"))).forEach(item -> {
            LocalDate date = item.getNextContactDate() != null
                    ? item.getNextContactDate()
                    : (item.getLastContactDate() != null ? item.getLastContactDate() : (item.getUpdatedAt() == null ? LocalDate.now() : item.getUpdatedAt().toLocalDate()));
            String status = item.getStatus() == null ? "Deschis" : switch (item.getStatus()) {
                case OPEN -> "Deschis";
                case IN_PROGRESS -> "În lucru";
                case DONE -> "Finalizat";
            };
            String notes = item.getNotes() == null || item.getNotes().isBlank() ? "Fără notițe" : item.getNotes();
            items.add(new PastoralTimelineItem(date, "Follow-up", status + " · " + notes, "/follow-ups/" + item.getId() + "/edit"));
        });

        String phone = normalize(person.getPhone());
        String fullName = normalize(displayName(person));
        for (Visit visit : visitRepository.findAll()) {
            boolean samePhone = !phone.isBlank() && phone.equals(normalize(visit.getPhone()));
            boolean sameName = !fullName.isBlank() && fullName.equals(normalize(visit.getPersonName()));
            if (samePhone || sameName) {
                String detail = visit.getNotes() == null || visit.getNotes().isBlank() ? "Vizită pastorală" : visit.getNotes();
                items.add(new PastoralTimelineItem(visit.getVisitDate(), "Vizită", detail, "/visits"));
            }
        }

        return items.stream()
                .filter(item -> item.date() != null)
                .sorted(Comparator.comparing(PastoralTimelineItem::date).reversed())
                .limit(12)
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private Integer completedYears(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private List<Long> parseIds(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        return rawValues.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, List<PersonGroupLink>> personGroupsById(Long churchId, List<PersonDTO> visiblePersons) {
        Set<Long> visibleIds = visiblePersons.stream()
                .map(PersonDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, List<PersonGroupLink>> groupsByPerson = new LinkedHashMap<>();
        for (Long id : visibleIds) {
            groupsByPerson.put(id, new ArrayList<>());
        }
        if (visibleIds.isEmpty()) {
            return groupsByPerson;
        }

        for (PersonGroupMembership membership : groupRepository.findMembershipsForPersons(churchId, visibleIds)) {
            PersonGroupLink link = new PersonGroupLink(
                    membership.getGroupId(),
                    membership.getGroupName(),
                    membership.getGroupType() == null ? "" : membership.getGroupType().name());
            groupsByPerson.computeIfAbsent(membership.getPersonId(), ignored -> new ArrayList<>()).add(link);
        }
        return groupsByPerson;
    }

    private List<RoleOption> privateNoteRoleOptions() {
        return List.of(
                new RoleOption("ADMIN", "Administrator"),
                new RoleOption("PASTOR", "Pastor"),
                new RoleOption("ELDER", "Prezbiter"),
                new RoleOption("GROUP_LEADER", "Lider grup"),
                new RoleOption("SECRETARY", "Secretar")
        );
    }

    private boolean canManagePrivateNotes(String role) {
        return NOTE_MANAGE_ROLES.contains(normalizeRole(role));
    }

    private boolean canViewPrivateNote(PastoralPrivateNote note, String role, String username) {
        if (note == null) {
            return false;
        }
        String normalizedRole = normalizeRole(role);
        String normalizedUsername = normalizeUsername(username);
        if (!normalizedUsername.isBlank()
                && normalizedUsername.equals(normalizeUsername(note.getCreatedByUsername()))) {
            return true;
        }
        return parseAllowedRoles(note.getAllowedRoles()).contains(normalizedRole);
    }

    private boolean canDeletePrivateNote(PastoralPrivateNote note, String role, String username) {
        if (note == null) {
            return false;
        }
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole) || "PASTOR".equals(normalizedRole)) {
            return true;
        }
        String normalizedUsername = normalizeUsername(username);
        return !normalizedUsername.isBlank()
                && normalizedUsername.equals(normalizeUsername(note.getCreatedByUsername()));
    }

    private PrivateNoteView toPrivateNoteView(PastoralPrivateNote note,
                                              boolean canManagePrivateNotes,
                                              String currentUsername,
                                              String currentRole) {
        LocalDateTime updatedAt = note.getUpdatedAt() == null ? note.getCreatedAt() : note.getUpdatedAt();
        String createdBy = note.getCreatedByUsername() == null || note.getCreatedByUsername().isBlank()
                ? "Necunoscut"
                : note.getCreatedByUsername();
        String role = note.getCreatedByRole() == null ? "" : note.getCreatedByRole();
        if (!role.isBlank()) {
            createdBy = createdBy + " · " + roleLabel(role);
        }
        String visibility = parseAllowedRoles(note.getAllowedRoles()).stream()
                .map(this::roleLabel)
                .collect(Collectors.joining(", "));
        if (visibility.isBlank()) {
            visibility = "Nedefinit";
        }
        boolean canDelete = canManagePrivateNotes && canDeletePrivateNote(note, currentRole, currentUsername);
        return new PrivateNoteView(
                note.getId(),
                note.getNoteText(),
                updatedAt == null ? "—" : NOTE_DATE_FORMAT.format(updatedAt),
                createdBy,
                visibility,
                canDelete
        );
    }

    private List<String> sanitizeAllowedRoles(List<String> requestedRoles, String fallbackRole) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        for (String role : requestedRoles) {
            String normalized = normalizeRole(role);
            if (NOTE_ROLE_OPTIONS.contains(normalized)) {
                roles.add(normalized);
            }
        }
        if (roles.isEmpty()) {
            String fallback = normalizeRole(fallbackRole);
            roles.add(NOTE_ROLE_OPTIONS.contains(fallback) ? fallback : "PASTOR");
        }
        return List.copyOf(roles);
    }

    private Set<String> parseAllowedRoles(String allowedRoles) {
        if (allowedRoles == null || allowedRoles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(allowedRoles.split(","))
                .map(this::normalizeRole)
                .filter(NOTE_ROLE_OPTIONS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String roleLabel(String role) {
        return switch (normalizeRole(role)) {
            case "ADMIN" -> "Administrator";
            case "PASTOR" -> "Pastor";
            case "ELDER" -> "Prezbiter";
            case "GROUP_LEADER" -> "Lider grup";
            case "SECRETARY" -> "Secretar";
            default -> normalizeRole(role).replace('_', ' ');
        };
    }

    private String currentRole() {
        return normalizeRole(currentUserService.currentRoleOr("VIEWER"));
    }

    private String currentUsername() {
        return currentUserService.currentUsernameOr("unknown-user");
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public record PastoralTimelineItem(LocalDate date, String type, String detail, String href) {}

    public record PersonGroupLink(Long id, String name, String type) {}

    public record RoleOption(String key, String label) {}

    public record PrivateNoteView(Long id, String text, String updatedAtLabel, String createdBy, String visibility, boolean canDelete) {}

    public static class PrivateNoteForm {
        private String text;
        private List<String> allowedRoles = new ArrayList<>();

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public List<String> getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles == null ? new ArrayList<>() : allowedRoles;
        }
    }
}
