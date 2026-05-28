package ro.church_office.teamleaf.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.events.DAO.EventRepository;
import ro.church_office.info.events.EventDTO;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.groups.GroupType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/groups")
public class GroupWebController {

    private static final String ROWS_KEY = "rows_per_page";

    private final ChurchGroupRepository groupRepository;
    private final PersonRepository personRepository;
    private final EventRepository eventRepository;
    private final ChurchContextService churchContextService;
    private final GlobalSettingRepository globalSettingRepository;

    public GroupWebController(ChurchGroupRepository groupRepository,
                              PersonRepository personRepository,
                              EventRepository eventRepository,
                              ChurchContextService churchContextService,
                              GlobalSettingRepository globalSettingRepository) {
        this.groupRepository = groupRepository;
        this.personRepository = personRepository;
        this.eventRepository = eventRepository;
        this.churchContextService = churchContextService;
        this.globalSettingRepository = globalSettingRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "type", required = false) GroupType type,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "size", required = false) Integer size,
                       Model model) {
        populateListModel(q, type, page, size, model);
        attachMeta(model);
        return "groups/list";
    }

    @PostMapping("/results")
    public String listResults(@RequestParam(value = "q", required = false) String q,
                              @RequestParam(value = "type", required = false) GroupType type,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "size", required = false) Integer size,
                              Model model) {
        populateListModel(q, type, page, size, model);
        attachMeta(model);
        return "groups/list :: resultsSection";
    }

    private void populateListModel(String q, GroupType type, int page, Integer size, Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Sort sort = Sort.by(Sort.Order.asc("type"), Sort.Order.asc("name"));
        List<ChurchGroup> groups = q != null && !q.isBlank()
                ? groupRepository.searchByChurchAndText(churchId, q.trim(), sort)
                : groupRepository.findAllByChurchId(churchId, sort);

        if (type != null) {
            groups = groups.stream().filter(group -> type.equals(group.getType())).toList();
        }

        int defaultSize = defaultRowsPerPage();
        int normalizedSize = normalizeSize(size == null ? defaultSize : size);
        int totalItems = groups.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / normalizedSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * normalizedSize, totalItems);
        int toIndex = Math.min(fromIndex + normalizedSize, totalItems);

        List<ChurchGroup> visibleGroups = groups.subList(fromIndex, toIndex);
        model.addAttribute("groups", visibleGroups);
        model.addAttribute("groupForm", defaultGroupForm());
        model.addAttribute("groupEditDataById", groupEditDataById(visibleGroups));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("type", type);
        model.addAttribute("page", currentPage);
        model.addAttribute("size", normalizedSize);
        model.addAttribute("defaultSize", defaultSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSizes", List.of(5, 10, 20, 25, 50, 100));
    }

    private int normalizeSize(int size) {
        return switch (size) {
            case 5, 10, 20, 25, 50, 100 -> size;
            default -> 10;
        };
    }

    private int defaultRowsPerPage() {
        return normalizeSize(globalSettingRepository.findBySettingKey(ROWS_KEY)
                .map(setting -> setting.getIntValue())
                .orElse(5));
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("groupForm", defaultGroupForm());
        attachMeta(model);
        return "groups/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("groupForm") GroupForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        System.out.println("=== CREATE GROUP DEBUG ===");
        System.out.println("Form name: " + form.getName());
        System.out.println("Form type: " + form.getType());
        System.out.println("Form leaderId: " + form.getLeaderId());
        System.out.println("Form memberIds: " + form.getMemberIds());
        System.out.println("Form description: " + form.getDescription());
        System.out.println("Has binding errors: " + bindingResult.hasErrors());
        if (bindingResult.hasErrors()) {
            System.out.println("Binding errors: " + bindingResult.getAllErrors());
            attachMeta(model);
            return "groups/form";
        }
        try {
            ChurchGroup saved = saveGroup(null, form);
            System.out.println("Saved group type: " + saved.getType());
            System.out.println("=== END DEBUG ===");
            redirectAttributes.addFlashAttribute("success", "Grupul a fost creat.");
            return "redirect:/groups";
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
            System.out.println("=== END DEBUG ===");
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut crea grupul." : ex.getMessage());
            return "redirect:/groups";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        ChurchGroup group = groupRepository.findByIdAndChurchId(id, churchId).orElse(null);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Grupul nu a putut fi găsit.");
            return "redirect:/groups";
        }

        model.addAttribute("group", group);
        model.addAttribute("members", group.getMembers().stream()
                .filter(person -> Objects.equals(person.getChurchId(), churchId))
                .sorted((a, b) -> displayName(a).compareToIgnoreCase(displayName(b)))
                .toList());
        model.addAttribute("events", eventRepository.findAllByChurchIdAndAssociatedGroup_Id(
                churchId,
                id,
                Sort.by(Sort.Order.asc("openDate"), Sort.Order.asc("eventName"))).stream()
                .map(EventDTO::fromEntity)
                .toList());
        return "groups/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        ChurchGroup group = groupRepository.findByIdAndChurchId(id, churchId).orElse(null);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Grupul nu a putut fi găsit.");
            return "redirect:/groups";
        }

        model.addAttribute("groupForm", GroupForm.from(group));
        attachMeta(model);
        return "groups/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("groupForm") GroupForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            attachMeta(model);
            return "groups/form";
        }
        try {
            saveGroup(id, form);
            redirectAttributes.addFlashAttribute("success", "Grupul a fost actualizat.");
            return "redirect:/groups";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut salva grupul." : ex.getMessage());
            return "redirect:/groups";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        try {
            ChurchGroup group = groupRepository.findByIdAndChurchId(id, churchId)
                    .orElseThrow(() -> new IllegalArgumentException("Grupul nu există în biserica activă."));
            groupRepository.delete(group);
            redirectAttributes.addFlashAttribute("success", "Grupul a fost șters.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut șterge grupul." : ex.getMessage());
        }
        return "redirect:/groups";
    }

    private ChurchGroup saveGroup(Long id, GroupForm form) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        ChurchGroup group = id == null ? new ChurchGroup() : groupRepository.findByIdAndChurchId(id, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Grupul nu există în biserica activă."));

        String name = blankToNull(form.getName());
        if (name == null) {
            throw new IllegalArgumentException("Numele grupului este obligatoriu.");
        }

        group.setChurchId(churchId);
        group.setName(name);
        group.setType(form.getType() == null ? GroupType.SMALL_GROUP : form.getType());
        group.setDescription(blankToNull(form.getDescription()));
        group.setLeader(findPersonInActiveChurch(form.getLeaderId(), churchId));
        group.setMembers(resolveMembers(form.getMemberIds(), churchId));

        return groupRepository.save(group);
    }

    private Person findPersonInActiveChurch(Long personId, Long churchId) {
        if (personId == null) return null;
        return personRepository.findByIdAndChurchId(personId, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Liderul selectat nu există în biserica activă."));
    }

    private Set<Person> resolveMembers(List<Long> memberIds, Long churchId) {
        Set<Person> members = new LinkedHashSet<>();
        if (memberIds == null) {
            return members;
        }
        for (Long memberId : memberIds) {
            if (memberId == null) continue;
            personRepository.findByIdAndChurchId(memberId, churchId).ifPresent(members::add);
        }
        return members;
    }

    private void attachMeta(Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        model.addAttribute("groupTypes", GroupType.values());
        model.addAttribute("allPersons", personRepository.findAllByChurchId(
                churchId,
                Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName"))).stream()
                .map(PersonDTO::fromEntity)
                .toList());
    }

    private GroupForm defaultGroupForm() {
        GroupForm form = new GroupForm();
        form.setType(GroupType.SMALL_GROUP);
        return form;
    }

    private Map<Long, GroupEditData> groupEditDataById(List<ChurchGroup> groups) {
        return groups.stream()
                .collect(Collectors.toMap(
                        ChurchGroup::getId,
                        GroupEditData::from,
                        (left, right) -> left));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String displayName(Person person) {
        if (person == null) return "—";
        String first = person.getFirstName() == null ? "" : person.getFirstName();
        String last = person.getLastName() == null ? "" : person.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "Persoană fără nume" : name;
    }

    public record GroupEditData(Long id,
                                String name,
                                String type,
                                Long leaderId,
                                String description,
                                String memberIdsCsv) {
        public static GroupEditData from(ChurchGroup group) {
            String memberIdsCsv = group.getMembers() == null ? "" : group.getMembers().stream()
                    .filter(member -> member.getId() != null)
                    .map(member -> member.getId().toString())
                    .collect(Collectors.joining(","));
            return new GroupEditData(
                    group.getId(),
                    group.getName(),
                    group.getType() == null ? GroupType.SMALL_GROUP.name() : group.getType().name(),
                    group.getLeader() == null ? null : group.getLeader().getId(),
                    group.getDescription() == null ? "" : group.getDescription(),
                    memberIdsCsv);
        }
    }

    public static class GroupForm {
        private Long id;
        
        @jakarta.validation.constraints.NotBlank(message = "Numele grupului este obligatoriu")
        @jakarta.validation.constraints.Size(min = 2, max = 100, message = "Numele trebuie să aibă între 2 și 100 caractere")
        private String name;
        
        @jakarta.validation.constraints.NotNull(message = "Tipul grupului este obligatoriu")
        private GroupType type = GroupType.SMALL_GROUP;
        
        @jakarta.validation.constraints.NotNull(message = "Liderul este obligatoriu")
        private Long leaderId;
        
        @jakarta.validation.constraints.Size(max = 1000, message = "Descrierea nu poate depăși 1000 caractere")
        private String description;
        
        @jakarta.validation.constraints.NotEmpty(message = "Cel puțin un membru este obligatoriu")
        private List<Long> memberIds = List.of();

        public static GroupForm from(ChurchGroup group) {
            GroupForm form = new GroupForm();
            form.setId(group.getId());
            form.setName(group.getName());
            form.setType(group.getType());
            form.setLeaderId(group.getLeader() == null ? null : group.getLeader().getId());
            form.setDescription(group.getDescription());
            form.setMemberIds(group.getMembers().stream()
                    .filter(member -> member.getId() != null)
                    .map(Person::getId)
                    .toList());
            return form;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public GroupType getType() { return type; }
        public void setType(GroupType type) { this.type = type; }
        public Long getLeaderId() { return leaderId; }
        public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Long> getMemberIds() { return memberIds; }
        public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
    }
}
