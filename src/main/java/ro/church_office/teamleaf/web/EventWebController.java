package ro.church_office.teamleaf.web;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.events.DAO.Event;
import ro.church_office.info.events.DAO.EventRepository;
import ro.church_office.info.events.DAO.EventService;
import ro.church_office.info.events.DAO.EventTask;
import ro.church_office.info.events.DAO.EventTaskRepository;
import ro.church_office.info.events.EventDTO;
import ro.church_office.info.events.EventMigrationDTO;
import ro.church_office.info.events.EventStatus;
import ro.church_office.info.events.EventTaskDTO;
import ro.church_office.info.events.RecurrenceType;
import ro.church_office.info.events.EventType;
import ro.church_office.info.events.Priority;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.DTO.ChurchInfoDTO;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;
import ro.church_office.info.person.service.PersonService;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.util.Optional;

@Controller
@RequestMapping("/events")
public class EventWebController {
    private static final Logger log = LoggerFactory.getLogger(EventWebController.class);
    private static final String ROWS_KEY = "rows_per_page";

    private final EventService eventService;
    private final PersonService personService;
    private final EventRepository eventRepository;
    private final EventTaskRepository eventTaskRepository;
    private final PersonRepository personRepository;
    private final ChurchContextService churchContextService;
    private final ChurchInfoService churchInfoService;
    private final ChurchGroupRepository groupRepository;
    private final GlobalSettingRepository globalSettingRepository;

    public EventWebController(EventService eventService,
                              PersonService personService,
                              EventRepository eventRepository,
                              EventTaskRepository eventTaskRepository,
                              PersonRepository personRepository,
                              ChurchContextService churchContextService,
                              ChurchInfoService churchInfoService,
                              ChurchGroupRepository groupRepository,
                              GlobalSettingRepository globalSettingRepository) {
        this.eventService = eventService;
        this.personService = personService;
        this.eventRepository = eventRepository;
        this.eventTaskRepository = eventTaskRepository;
        this.personRepository = personRepository;
        this.churchContextService = churchContextService;
        this.churchInfoService = churchInfoService;
        this.groupRepository = groupRepository;
        this.globalSettingRepository = globalSettingRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "eventType", required = false) String eventType,
                       @RequestParam(value = "groupId", required = false) Long groupId,
                       @RequestParam(value = "sort", defaultValue = "openDateAsc") String sort,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "scrollOnly", defaultValue = "false") boolean scrollOnly,
                       Model model) {
        populateListModel(q, status, eventType, groupId, sort, page, size, scrollOnly, model);
        attachMeta(model);
        return "events/list";
    }

    @PostMapping("/results")
    public String listResults(@RequestParam(value = "q", required = false) String q,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "eventType", required = false) String eventType,
                              @RequestParam(value = "groupId", required = false) Long groupId,
                              @RequestParam(value = "sort", defaultValue = "openDateAsc") String sort,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "size", required = false) Integer size,
                              @RequestParam(value = "scrollOnly", defaultValue = "false") boolean scrollOnly,
                              Model model) {
        populateListModel(q, status, eventType, groupId, sort, page, size, scrollOnly, model);
        attachMeta(model);
        return "events/list :: resultsSection";
    }

    private void populateListModel(String q,
                                   String status,
                                   String eventType,
                                   Long groupId,
                                   String sort,
                                   int page,
                                   Integer size,
                                   boolean scrollOnly,
                                   Model model) {
        List<EventDTO> events = eventService.getAllEvents().stream()
                .filter(event -> matches(event, q, status, eventType, groupId))
                .sorted(comparatorFor(sort))
                .toList();

        int defaultSize = defaultRowsPerPage();
        int normalizedSize = normalizeSize(size == null ? defaultSize : size);
        int totalItems = events.size();
        int totalPages = scrollOnly ? 1 : Math.max(1, (int) Math.ceil((double) totalItems / normalizedSize));
        int currentPage = scrollOnly ? 1 : Math.min(Math.max(page, 1), totalPages);
        int fromIndex = scrollOnly ? 0 : Math.min((currentPage - 1) * normalizedSize, totalItems);
        int toIndex = scrollOnly ? totalItems : Math.min(fromIndex + normalizedSize, totalItems);

        model.addAttribute("events", events.subList(fromIndex, toIndex));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("eventType", eventType == null ? "" : eventType);
        model.addAttribute("groupId", groupId);
        model.addAttribute("sort", sort);
        model.addAttribute("page", currentPage);
        model.addAttribute("size", normalizedSize);
        model.addAttribute("defaultSize", defaultSize);
        model.addAttribute("scrollOnly", scrollOnly);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSizes", List.of(5, 7, 10, 20, 25, 50, 100));
        if (!model.containsAttribute("eventDto")) {
            EventDTO dto = new EventDTO();
            dto.setStatus(EventStatus.PLANNED.name());
            dto.setEventType(EventType.OTHER.name());
            dto.setOpenDate(LocalDate.now());
            ensureImplementer(dto);
            model.addAttribute("eventDto", dto);
        }
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(value = "openDate", required = false) LocalDate openDate,
                             Model model) {
        EventDTO dto = new EventDTO();
        dto.setStatus(EventStatus.PLANNED.name());
        dto.setEventType(EventType.OTHER.name());
        dto.setOpenDate(openDate != null ? openDate : LocalDate.now());
        ensureImplementer(dto);
        model.addAttribute("eventDto", dto);
        attachMeta(model);
        return "events/form";
    }

    @GetMapping("/move")
    public String moveForm(@RequestParam(value = "eventId", required = false) Long eventId,
                           Model model) {
        List<EventDTO> events = eventService.getAllEvents().stream()
                .sorted(comparatorFor("nameAsc"))
                .toList();
        List<ChurchInfoDTO> churches = churchInfoService.getAll();

        EventDTO selectedEvent = events.stream()
                .filter(event -> eventId != null && eventId.equals(event.getId()))
                .findFirst()
                .orElse(events.isEmpty() ? null : events.get(0));

        Long selectedEventId = selectedEvent == null ? null : selectedEvent.getId();
        List<ChurchInfoDTO> availableTargets = selectedEvent == null
                ? List.of()
                : churches.stream()
                        .filter(church -> church.id != null && !church.id.equals(selectedEvent.getChurchId()))
                        .toList();
        Long targetChurchId = availableTargets.isEmpty() ? null : availableTargets.get(0).id;

        model.addAttribute("events", events);
        model.addAttribute("churches", churches);
        model.addAttribute("currentEvent", selectedEvent);
        String currentChurchName = churches.stream()
                .filter(church -> selectedEvent != null
                        && church.id != null
                        && church.id.equals(selectedEvent.getChurchId()))
                .map(church -> church.name)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("Biserica activă");
        model.addAttribute("currentChurchName", currentChurchName);
        model.addAttribute("availableTargets", availableTargets);
        model.addAttribute("moveForm", new MoveEventForm(selectedEventId, targetChurchId, true));
        return "events/move";
    }

    @PostMapping("/move")
    public String move(@ModelAttribute MoveEventForm moveForm,
                       RedirectAttributes redirectAttributes) {
        try {
            if (moveForm.getEventId() == null) {
                throw new IllegalArgumentException("Selectează un eveniment.");
            }
            EventMigrationDTO dto = new EventMigrationDTO();
            dto.setTargetChurchId(moveForm.getTargetChurchId());
            dto.setClearImplementedBy(moveForm.getClearImplementedBy() == null || moveForm.getClearImplementedBy());
            eventService.moveEvent(moveForm.getEventId(), dto);
            redirectAttributes.addFlashAttribute("success", "Evenimentul a fost mutat.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut muta evenimentul." : ex.getMessage());
        }
        return "redirect:/events/move";
    }

    @PostMapping
    public String create(@ModelAttribute("eventDto") EventDTO eventDto,
                         BindingResult bindingResult,
                         @RequestParam(value = "implementedById", required = false) String implementedByIdRaw,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("error", "Datele formularului nu sunt valide. Verifică valorile introduse.");
                model.addAttribute("eventDto", eventDto);
                attachMeta(model);
                return "events/form";
            }
            Map<String, String> fieldErrors = validateEventForm(eventDto, implementedByIdRaw, false);
            if (!fieldErrors.isEmpty()) {
                model.addAttribute("error", "Corectează câmpurile marcate și încearcă din nou.");
                model.addAttribute("eventFieldErrors", fieldErrors);
                model.addAttribute("eventDto", eventDto);
                attachMeta(model);
                return "events/form";
            }
            Long implementedById = parseOptionalLong(implementedByIdRaw, "Responsabil invalid.");
            assignImplementer(eventDto, implementedById);
            validateEvent(eventDto);
            Event savedEvent = eventService.saveEvent(eventDto);
            redirectAttributes.addFlashAttribute("success", "Evenimentul a fost adăugat.");
            return "redirect:/events/" + savedEvent.getId() + "/edit";
        } catch (IllegalArgumentException ex) {
            log.warn("Event create rejected: name='{}', status='{}', type='{}', openDate={}, groupId={}, implementedByRaw='{}', reason='{}'",
                    eventDto == null ? null : eventDto.getEventName(),
                    eventDto == null ? null : eventDto.getStatus(),
                    eventDto == null ? null : eventDto.getEventType(),
                    eventDto == null ? null : eventDto.getOpenDate(),
                    eventDto == null ? null : eventDto.getGroupId(),
                    implementedByIdRaw,
                    ex.getMessage());
            model.addAttribute("error", messageOf(ex, "Nu s-a putut salva evenimentul."));
            model.addAttribute("eventDto", eventDto);
            attachMeta(model);
            return "events/form";
        } catch (Exception ex) {
            log.error("Unexpected error while creating event", ex);
            model.addAttribute("error", "A apărut o eroare neașteptată la salvare.");
            model.addAttribute("eventDto", eventDto);
            attachMeta(model);
            return "events/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Optional<Event> optional = eventRepository.findByIdAndChurchId(id, churchId);
        if (optional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Evenimentul nu a putut fi găsit.");
            return "redirect:/events";
        }
        EventDTO dto = EventDTO.fromEntity(optional.get());
        dto.setImplementedBy(ensurePerson(dto.getImplementedBy()));
        model.addAttribute("eventDto", dto);
        attachEventDetails(model, id, churchId);
        attachMeta(model);
        return "events/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("eventDto") EventDTO eventDto,
                         BindingResult bindingResult,
                         @RequestParam(value = "implementedById", required = false) String implementedByIdRaw,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("error", "Datele formularului nu sunt valide. Verifică valorile introduse.");
                model.addAttribute("eventDto", eventDto);
                Long churchId = churchContextService.getOrCreateActiveChurchId();
                attachEventDetails(model, id, churchId);
                attachMeta(model);
                return "events/form";
            }
            Map<String, String> fieldErrors = validateEventForm(eventDto, implementedByIdRaw, false);
            if (!fieldErrors.isEmpty()) {
                model.addAttribute("error", "Corectează câmpurile marcate și încearcă din nou.");
                model.addAttribute("eventFieldErrors", fieldErrors);
                model.addAttribute("eventDto", eventDto);
                Long churchId = churchContextService.getOrCreateActiveChurchId();
                attachEventDetails(model, id, churchId);
                attachMeta(model);
                return "events/form";
            }
            eventDto.setId(id);
            Long implementedById = parseOptionalLong(implementedByIdRaw, "Responsabil invalid.");
            assignImplementer(eventDto, implementedById);
            validateEvent(eventDto);
            eventService.updateEvent(eventDto.toEntity());
            redirectAttributes.addFlashAttribute("success", "Modificările au fost salvate.");
            return "redirect:/events";
        } catch (IllegalArgumentException ex) {
            log.warn("Event update rejected: id={}, name='{}', status='{}', type='{}', openDate={}, groupId={}, implementedByRaw='{}', reason='{}'",
                    id,
                    eventDto == null ? null : eventDto.getEventName(),
                    eventDto == null ? null : eventDto.getStatus(),
                    eventDto == null ? null : eventDto.getEventType(),
                    eventDto == null ? null : eventDto.getOpenDate(),
                    eventDto == null ? null : eventDto.getGroupId(),
                    implementedByIdRaw,
                    ex.getMessage());
            model.addAttribute("error", messageOf(ex, "Nu s-au putut salva modificările."));
            model.addAttribute("eventDto", eventDto);
            Long churchId = churchContextService.getOrCreateActiveChurchId();
            attachEventDetails(model, id, churchId);
            attachMeta(model);
            return "events/form";
        } catch (Exception ex) {
            log.error("Unexpected error while updating event {}", id, ex);
            model.addAttribute("error", "A apărut o eroare neașteptată la salvare.");
            model.addAttribute("eventDto", eventDto);
            Long churchId = churchContextService.getOrCreateActiveChurchId();
            attachEventDetails(model, id, churchId);
            attachMeta(model);
            return "events/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(value = "redirect", required = false) String redirect,
                         RedirectAttributes redirectAttributes) {
        eventService.deleteEventById(id);
        redirectAttributes.addFlashAttribute("success", "Evenimentul a fost șters.");
        if (redirect != null && redirect.startsWith("/")) {
            return "redirect:" + redirect;
        }
        return "redirect:/events";
    }

    @PostMapping("/{id}/tasks")
    public String createTask(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam(value = "notes", required = false) String notes,
                             @RequestParam(value = "dueDate", required = false) String dueDate,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "assignedToId", required = false) Long assignedToId,
                             @RequestParam(value = "orderIndex", required = false) Integer orderIndex,
                             RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Event event = eventRepository.findByIdAndChurchId(id, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Evenimentul nu a putut fi găsit."));

        EventTask task = new EventTask();
        task.setEvent(event);
        task.setTitle(requiredText(title, "Titlul task-ului este obligatoriu."));
        task.setNotes(blankToNull(notes));
        task.setDueDate(parseDate(dueDate));
        task.setStatus(blankToDefault(status, "TODO"));
        task.setOrderIndex(orderIndex);
        task.setAssignedTo(resolveAssignedPerson(assignedToId, churchId));
        eventTaskRepository.save(task);

        redirectAttributes.addFlashAttribute("success", "Task-ul a fost adăugat.");
        return "redirect:/events/" + id + "/edit";
    }

    @PostMapping("/{id}/tasks/{taskId}")
    public String updateTask(@PathVariable Long id,
                             @PathVariable Long taskId,
                             @RequestParam String title,
                             @RequestParam(value = "notes", required = false) String notes,
                             @RequestParam(value = "dueDate", required = false) String dueDate,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "assignedToId", required = false) Long assignedToId,
                             @RequestParam(value = "orderIndex", required = false) Integer orderIndex,
                             RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        EventTask task = eventTaskRepository.findById(taskId)
                .filter(item -> item.getEvent() != null
                        && item.getEvent().getId() != null
                        && id.equals(item.getEvent().getId())
                        && churchId.equals(item.getEvent().getChurchId()))
                .orElseThrow(() -> new IllegalArgumentException("Task-ul nu a putut fi găsit."));

        task.setTitle(requiredText(title, "Titlul task-ului este obligatoriu."));
        task.setNotes(blankToNull(notes));
        task.setDueDate(parseDate(dueDate));
        task.setStatus(blankToDefault(status, "TODO"));
        task.setOrderIndex(orderIndex);
        task.setAssignedTo(resolveAssignedPerson(assignedToId, churchId));
        eventTaskRepository.save(task);

        redirectAttributes.addFlashAttribute("success", "Task-ul a fost actualizat.");
        return "redirect:/events/" + id + "/edit";
    }

    @PostMapping("/{id}/tasks/{taskId}/delete")
    public String deleteTask(@PathVariable Long id,
                             @PathVariable Long taskId,
                             RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        EventTask task = eventTaskRepository.findById(taskId)
                .filter(item -> item.getEvent() != null
                        && item.getEvent().getId() != null
                        && id.equals(item.getEvent().getId())
                        && churchId.equals(item.getEvent().getChurchId()))
                .orElseThrow(() -> new IllegalArgumentException("Task-ul nu a putut fi găsit."));
        eventTaskRepository.delete(task);
        redirectAttributes.addFlashAttribute("success", "Task-ul a fost șters.");
        return "redirect:/events/" + id + "/edit";
    }

    private void attachMeta(Model model) {
        model.addAttribute("eventTypes", EventType.values());
        model.addAttribute("eventStatuses", EventStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        model.addAttribute("persons", personRepository.findAllByChurchId(
                        churchId,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("lastName"), org.springframework.data.domain.Sort.Order.asc("firstName")))
                .stream()
                .map(PersonDTO::fromEntity)
                .sorted(Comparator.comparing(PersonDTO::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList());
        model.addAttribute("taskStatuses", List.of("TODO", "IN_PROGRESS", "DONE"));
        model.addAttribute("groups", groupRepository.findAllByChurchId(
                churchId,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("type"), org.springframework.data.domain.Sort.Order.asc("name"))));
    }

    private void attachEventDetails(Model model, Long eventId, Long churchId) {
        model.addAttribute("tasks", eventTaskRepository.findByEventIdOrderByOrderIndexAscIdAsc(eventId).stream()
                .map(EventTaskDTO::fromEntity)
                .toList());
        model.addAttribute("taskStatuses", List.of("TODO", "IN_PROGRESS", "DONE"));
    }

    private void assignImplementer(EventDTO dto, Long implementedById) {
        if (dto == null) return;
        if (implementedById == null) {
            dto.setImplementedBy(null);
            return;
        }
        PersonDTO person = new PersonDTO();
        person.setId(implementedById);
        dto.setImplementedBy(person);
    }

    private void ensureImplementer(EventDTO dto) {
        if (dto == null) return;
        dto.setImplementedBy(ensurePerson(dto.getImplementedBy()));
    }

    private PersonDTO ensurePerson(PersonDTO person) {
        return person == null ? new PersonDTO() : person;
    }

    private boolean matches(EventDTO event, String q, String status, String eventType, Long groupId) {
        String query = q == null ? "" : q.trim().toLowerCase();
        if (!query.isEmpty()) {
            String haystack = String.join(" ",
                    safe(event.getEventName()),
                    safe(event.getAbout()),
                    safe(event.getStatus()),
                    safe(event.getEventType()),
                    safe(event.getGroupName()),
                    safe(event.getGroupType()),
                    event.getImplementedBy() == null ? "" : safe(event.getImplementedBy().getFullName())).toLowerCase();
            if (!haystack.contains(query)) {
                return false;
            }
        }

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase(safe(event.getStatus()))) {
            return false;
        }

        if (eventType != null && !eventType.isBlank() && !eventType.equalsIgnoreCase(safe(event.getEventType()))) {
            return false;
        }

        return groupId == null || groupId.equals(event.getGroupId());
    }

    private Comparator<EventDTO> comparatorFor(String sort) {
        return switch (sort == null ? "" : sort) {
            case "priorityAsc" -> Comparator.comparingInt(this::priorityOrder)
                    .thenComparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER);
            case "priorityDesc" -> Comparator.comparingInt(this::priorityOrder).reversed()
                    .thenComparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER);
            case "nameAsc" -> Comparator.comparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER);
            case "nameDesc" -> Comparator.comparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER).reversed();
            case "statusAsc" -> Comparator.comparing((EventDTO event) -> safe(event.getStatus()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER);
            case "statusDesc" -> Comparator.comparing((EventDTO event) -> safe(event.getStatus()), String.CASE_INSENSITIVE_ORDER).reversed()
                    .thenComparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER);
            case "openDateDesc" -> Comparator.comparing(EventDTO::getOpenDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(EventDTO::getOpenDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingInt(this::priorityOrder)
                    .thenComparing(this::eventNameSafe, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private int priorityOrder(EventDTO event) {
        Priority priority = Priority.from(event == null ? null : event.getPriority());
        return priority == null ? Priority.MEDIUM.getValue() : priority.getValue();
    }

    private String eventNameSafe(EventDTO event) {
        return safe(event == null ? null : event.getEventName());
    }

    private void validateEvent(EventDTO eventDto) {
        if (eventDto == null) {
            throw new IllegalArgumentException("Datele evenimentului lipsesc.");
        }
        eventDto.setEventName(requiredText(eventDto.getEventName(), "Numele evenimentului este obligatoriu."));
        eventDto.setStatus(normalizeEventStatus(eventDto.getStatus()));
        eventDto.setOpenDate(eventDto.getOpenDate() == null ? LocalDate.now() : eventDto.getOpenDate());
        eventDto.setEventType(normalizeEventType(eventDto.getEventType()));
        if (eventDto.getEndDate() != null && eventDto.getEndDate().isBefore(eventDto.getOpenDate())) {
            throw new IllegalArgumentException("Data de închidere nu poate fi înainte de data de deschidere.");
        }

        RecurrenceType recurrenceType = RecurrenceType.from(eventDto.getRecurrenceType());
        eventDto.setRecurrenceType(recurrenceType.name());
        if (recurrenceType == RecurrenceType.NONE) {
            eventDto.setRecurrenceInterval(1);
            eventDto.setRecurrenceUntil(null);
        } else {
            Integer interval = eventDto.getRecurrenceInterval();
            if (interval == null) {
                interval = 1;
                eventDto.setRecurrenceInterval(1);
            }
            if (interval < 1) {
                throw new IllegalArgumentException("Intervalul de recurență trebuie să fie cel puțin 1.");
            }
            if (eventDto.getRecurrenceUntil() != null && eventDto.getRecurrenceUntil().isBefore(eventDto.getOpenDate())) {
                throw new IllegalArgumentException("Data de final a recurenței nu poate fi înainte de data de deschidere.");
            }
        }

        boolean reminderEnabled = Boolean.TRUE.equals(eventDto.getFrontReminderEnabled());
        if (!reminderEnabled) {
            eventDto.setFrontReminderDaysBefore(1);
        } else if (eventDto.getFrontReminderDaysBefore() == null || eventDto.getFrontReminderDaysBefore() < 0) {
            throw new IllegalArgumentException("Numărul de zile pentru reminder trebuie să fie 0 sau mai mare.");
        }
    }

    private Person resolveAssignedPerson(Long personId, Long churchId) {
        if (personId == null) {
            return null;
        }
        return personRepository.findByIdAndChurchId(personId, churchId)
                .orElseThrow(() -> new IllegalArgumentException("Persoana selectată nu există în biserica activă."));
    }

    private java.time.LocalDate parseDate(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : java.time.LocalDate.parse(normalized);
    }

    private String requiredText(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String blankToDefault(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeEventStatus(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return EventStatus.PLANNED.name();
        }
        try {
            return EventStatus.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            return EventStatus.PLANNED.name();
        }
    }

    private String normalizeEventType(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return EventType.OTHER.name();
        }
        try {
            return EventType.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            return EventType.OTHER.name();
        }
    }

    private Long parseOptionalLong(String raw, String invalidMessage) {
        String normalized = blankToNull(raw);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(invalidMessage);
        }
    }

    private String messageOf(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
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

    private Map<String, String> validateEventForm(EventDTO eventDto, String implementedByIdRaw, boolean requireImplementedBy) {
        Map<String, String> errors = new HashMap<>();
        if (eventDto == null) {
            errors.put("eventName", "Datele evenimentului lipsesc.");
            return errors;
        }

        if (blankToNull(eventDto.getEventName()) == null) {
            errors.put("eventName", "Numele evenimentului este obligatoriu.");
        }

        LocalDate openDate = eventDto.getOpenDate() == null ? LocalDate.now() : eventDto.getOpenDate();
        if (eventDto.getEndDate() != null && eventDto.getEndDate().isBefore(openDate)) {
            errors.put("endDate", "Data de închidere nu poate fi înainte de data de deschidere.");
        }

        RecurrenceType recurrenceType = RecurrenceType.from(eventDto.getRecurrenceType());
        if (recurrenceType != RecurrenceType.NONE) {
            if (eventDto.getRecurrenceInterval() == null) {
                eventDto.setRecurrenceInterval(1);
            } else if (eventDto.getRecurrenceInterval() < 1) {
                errors.put("recurrenceInterval", "Intervalul de recurență trebuie să fie cel puțin 1.");
            }
            if (eventDto.getRecurrenceUntil() != null && eventDto.getRecurrenceUntil().isBefore(openDate)) {
                errors.put("recurrenceUntil", "Data de final a recurenței nu poate fi înainte de data de deschidere.");
            }
        }

        if (Boolean.TRUE.equals(eventDto.getFrontReminderEnabled())
                && (eventDto.getFrontReminderDaysBefore() == null || eventDto.getFrontReminderDaysBefore() < 0)) {
            errors.put("frontReminderDaysBefore", "Numărul de zile pentru reminder trebuie să fie 0 sau mai mare.");
        }

        String normalizedImplementedBy = blankToNull(implementedByIdRaw);
        if (requireImplementedBy && normalizedImplementedBy == null) {
            errors.put("implementedById", "Responsabilul este obligatoriu.");
        }
        if (normalizedImplementedBy != null) {
            Long implementedById = parseOptionalLong(implementedByIdRaw, "Responsabil invalid.");
            Long churchId = churchContextService.getOrCreateActiveChurchId();
            if (personRepository.findByIdAndChurchId(implementedById, churchId).isEmpty()) {
                errors.put("implementedById", "Responsabilul selectat nu există în biserica activă.");
            }
        }

        return errors;
    }

    public static class MoveEventForm {
        private Long eventId;
        private Long targetChurchId;
        private Boolean clearImplementedBy;

        public MoveEventForm() {}

        public MoveEventForm(Long eventId, Long targetChurchId, Boolean clearImplementedBy) {
            this.eventId = eventId;
            this.targetChurchId = targetChurchId;
            this.clearImplementedBy = clearImplementedBy;
        }

        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        public Long getTargetChurchId() { return targetChurchId; }
        public void setTargetChurchId(Long targetChurchId) { this.targetChurchId = targetChurchId; }
        public Boolean getClearImplementedBy() { return clearImplementedBy; }
        public void setClearImplementedBy(Boolean clearImplementedBy) { this.clearImplementedBy = clearImplementedBy; }
    }
}
