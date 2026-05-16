package ro.church_office.teamleaf.web;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.church_office.info.attendance.AttendanceRecord;
import ro.church_office.info.attendance.AttendanceRecordRepository;
import ro.church_office.info.attendance.AttendanceStatus;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.events.DAO.Event;
import ro.church_office.info.events.DAO.EventRepository;
import ro.church_office.info.finance.Transaction;
import ro.church_office.info.finance.TransactionService;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.teamleaf.finance.TransactionChurchScopeService;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ReportsWebController {
    private static final String REPORTS_LAYOUT_ORDER_KEY = "reports_layout_order";
    private static final DateTimeFormatter RO_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ChurchContextService churchContextService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final PersonRepository personRepository;
    private final EventRepository eventRepository;
    private final TransactionService transactionService;
    private final ChurchGroupRepository groupRepository;
    private final GlobalSettingRepository globalSettingRepository;
    private final TransactionChurchScopeService transactionChurchScopeService;

    public ReportsWebController(ChurchContextService churchContextService,
                                AttendanceRecordRepository attendanceRecordRepository,
                                PersonRepository personRepository,
                                EventRepository eventRepository,
                                TransactionService transactionService,
                                ChurchGroupRepository groupRepository,
                                GlobalSettingRepository globalSettingRepository,
                                TransactionChurchScopeService transactionChurchScopeService) {
        this.churchContextService = churchContextService;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.personRepository = personRepository;
        this.eventRepository = eventRepository;
        this.transactionService = transactionService;
        this.groupRepository = groupRepository;
        this.globalSettingRepository = globalSettingRepository;
        this.transactionChurchScopeService = transactionChurchScopeService;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(value = "from", required = false) String from,
                          @RequestParam(value = "to", required = false) String to,
                          Model model) {
        populateReportsModel(from, to, model);
        return "reports/index";
    }

    @PostMapping("/reports/results")
    public String reportsResults(@RequestParam(value = "from", required = false) String from,
                                 @RequestParam(value = "to", required = false) String to,
                                 Model model) {
        populateReportsModel(from, to, model);
        return "reports/index :: resultsSection";
    }

    @PostMapping(value = "/reports/layout-order", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> saveLayoutOrder(@RequestParam("layoutOrder") String layoutOrder) {
        GlobalSetting setting = globalSettingRepository.findByKey(REPORTS_LAYOUT_ORDER_KEY)
                .orElseGet(() -> new GlobalSetting(REPORTS_LAYOUT_ORDER_KEY, layoutOrder));
        setting.setStringValue(normalizeReportsLayoutOrder(layoutOrder));
        globalSettingRepository.save(setting);
        return ResponseEntity.noContent().build();
    }

    private void populateReportsModel(String from, String to, Model model) {
        ReportData data = loadReportData(from, to);

        model.addAttribute("from", data.fromDate());
        model.addAttribute("to", data.toDate());
        model.addAttribute("attendanceRecords", data.attendanceRecords().size());
        model.addAttribute("presentCount", data.presentCount());
        model.addAttribute("absentCount", data.absentCount());
        model.addAttribute("attendanceRate", data.attendanceRecords().isEmpty() ? 0 : Math.round((data.presentCount() * 100.0) / data.attendanceRecords().size()));
        model.addAttribute("attendanceByGroup", data.attendanceByGroup());
        model.addAttribute("pastoralAttention", data.pastoralAttention());
        model.addAttribute("smallGroupHealth", data.smallGroupHealth());
        model.addAttribute("smallGroupMembersTotal", data.smallGroupHealth().stream().mapToInt(SmallGroupHealthRow::membersCount).sum());
        List<SmallGroupChartBand> smallGroupChartBands = smallGroupChartBands(data.smallGroupHealth(), data.persons().size());
        model.addAttribute("smallGroupChartBands", smallGroupChartBands);
        model.addAttribute("smallGroupPieStyle", smallGroupPieStyle(smallGroupChartBands));
        model.addAttribute("smallGroupSliceLabels", smallGroupSliceLabels(smallGroupChartBands));
        model.addAttribute("peopleCount", data.persons().size());
        model.addAttribute("peopleByType", mapRows(data.peopleByType()));
        List<AgeBandRow> ageBands = ageBands(data.persons(), data.toDate());
        model.addAttribute("peopleByAgeBands", ageBands);
        model.addAttribute("peopleByAgePieStyle", agePieStyle(ageBands));
        model.addAttribute("peopleByAgeSliceLabels", ageSliceLabels(ageBands));
        model.addAttribute("peopleWithoutPhone", data.peopleWithoutPhone());
        model.addAttribute("peopleWithoutAddress", data.peopleWithoutAddress());
        model.addAttribute("eventsCount", data.events().size());
        model.addAttribute("eventsByStatus", mapRows(data.eventsByStatus()));
        model.addAttribute("eventsByType", mapRows(data.eventsByType()));
        model.addAttribute("totalIncome", data.totalIncome());
        model.addAttribute("totalExpenses", data.totalExpenses());
        model.addAttribute("currentBalance", data.totalIncome() - data.totalExpenses());
        model.addAttribute("financeTransactionsCount", data.transactions().size());
        model.addAttribute("upcomingBirthdays", data.upcomingBirthdays());
        model.addAttribute("missingBirthDatePeople", data.missingBirthDatePeople());
        model.addAttribute("reportsLayoutOrder", resolveReportsLayoutOrder());
    }

    private String resolveReportsLayoutOrder() {
        String raw = globalSettingRepository.findByKey(REPORTS_LAYOUT_ORDER_KEY)
                .map(GlobalSetting::getStringValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse("attendance-period,attendance-groups,pastoral-follow-up,small-group-health,age-distribution,people-categories,key-moments,events-status,events-type,finance");
        String normalized = normalizeReportsLayoutOrder(raw);
        if (!normalized.equals(raw)) {
            GlobalSetting setting = globalSettingRepository.findByKey(REPORTS_LAYOUT_ORDER_KEY)
                    .orElseGet(() -> new GlobalSetting(REPORTS_LAYOUT_ORDER_KEY, normalized));
            setting.setStringValue(normalized);
            globalSettingRepository.save(setting);
        }
        return normalized;
    }

    private String normalizeReportsLayoutOrder(String value) {
        if (value == null || value.isBlank()) {
            return "attendance-period,attendance-groups,pastoral-follow-up,small-group-health,age-distribution,people-categories,key-moments,events-status,events-type,finance";
        }
        List<String> allowed = List.of(
                "attendance-period",
                "attendance-groups",
                "pastoral-follow-up",
                "small-group-health",
                "age-distribution",
                "people-categories",
                "key-moments",
                "events-status",
                "events-type",
                "finance");
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
        for (String token : value.split(",")) {
            String item = token == null ? "" : token.trim();
            if (allowed.contains(item)) {
                ordered.add(item);
            }
        }
        for (String key : allowed) {
            ordered.add(key);
        }
        return String.join(",", ordered);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<String> export(@RequestParam(value = "type", defaultValue = "attendance") String type,
                                         @RequestParam(value = "from", required = false) String from,
                                         @RequestParam(value = "to", required = false) String to) {
        ReportData data = loadReportData(from, to);
        String normalizedType = type == null ? "attendance" : type.trim().toLowerCase(Locale.ROOT);
        String csv = switch (normalizedType) {
            case "people" -> peopleCsv(data);
            case "events" -> eventsCsv(data);
            case "finance" -> financeCsv(data);
            case "attendance-groups" -> attendanceGroupsCsv(data);
            case "pastoral-attention" -> pastoralAttentionCsv(data);
            case "small-groups" -> smallGroupsCsv(data);
            case "key-moments" -> keyMomentsCsv(data);
            default -> attendanceCsv(data);
        };
        String filename = "raport-" + normalizedType.replaceAll("[^a-z0-9-]", "") + "-" + data.fromDate() + "-" + data.toDate() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body('\ufeff' + csv);
    }

    private ReportData loadReportData(String from, String to) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        LocalDate toDate = parseDate(to, LocalDate.now());
        LocalDate fromDate = parseDate(from, toDate.minusDays(30));
        if (fromDate.isAfter(toDate)) {
            LocalDate swap = fromDate;
            fromDate = toDate;
            toDate = swap;
        }

        List<AttendanceRecord> attendanceRecords = attendanceRecordRepository
                .findByChurchIdAndAttendanceDateBetweenOrderByAttendanceDateDescServiceSessionAscRecordedAtDesc(churchId, fromDate, toDate);
        long presentCount = attendanceRecords.stream().filter(record -> record.getStatus() == AttendanceStatus.PRESENT).count();
        long absentCount = attendanceRecords.stream().filter(record -> record.getStatus() == AttendanceStatus.ABSENT).count();

        List<Person> persons = personRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName")));
        Map<String, Long> peopleByType = new LinkedHashMap<>();
        for (MemberType type : MemberType.values()) {
            peopleByType.put(type.name(), persons.stream().filter(person -> type.equals(person.getMemberType())).count());
        }
        long peopleWithoutPhone = persons.stream().filter(person -> person.getPhone() == null || person.getPhone().isBlank()).count();
        long peopleWithoutAddress = persons.stream().filter(person -> person.getAddress() == null || person.getAddress().isBlank()).count();

        LocalDate periodFrom = fromDate;
        LocalDate periodTo = toDate;
        List<Event> events = eventRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("openDate"), Sort.Order.asc("eventName"))).stream()
                .filter(event -> event.getOpenDate() == null || (!event.getOpenDate().isBefore(periodFrom) && !event.getOpenDate().isAfter(periodTo)))
                .toList();
        Map<String, Long> eventsByStatus = events.stream().collect(Collectors.groupingBy(
                event -> blankToDefault(event.getStatus(), "Fără status"),
                LinkedHashMap::new,
                Collectors.counting()));
        Map<String, Long> eventsByType = events.stream().collect(Collectors.groupingBy(
                event -> event.getEventType() == null ? "Fără tip" : event.getEventType().name(),
                LinkedHashMap::new,
                Collectors.counting()));

        List<Transaction> allTransactions = transactionService.getAllTransactions();
        transactionChurchScopeService.ensureLegacyTransactionsAssigned(allTransactions, churchId);
        List<Transaction> transactions = transactionChurchScopeService.filterForChurch(allTransactions, churchId);
        double totalIncome = transactions.stream()
                .filter(transaction -> "income".equals(normalizeType(transaction.getType())))
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        double totalExpenses = transactions.stream()
                .filter(transaction -> "expense".equals(normalizeType(transaction.getType())))
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        List<GroupAttendanceRow> attendanceByGroup = attendanceByGroup(churchId, attendanceRecords);
        List<SmallGroupHealthRow> smallGroupHealth = smallGroupHealth(churchId, attendanceRecords);
        List<PastoralAttentionRow> pastoralAttention = pastoralAttention(attendanceRecords);
        List<BirthdayMomentRow> upcomingBirthdays = upcomingBirthdays(persons, toDate, 30);
        List<MissingBirthDateRow> missingBirthDatePeople = missingBirthDatePeople(persons);
        return new ReportData(fromDate, toDate, attendanceRecords, presentCount, absentCount, persons, peopleByType,
                peopleWithoutPhone, peopleWithoutAddress, events, eventsByStatus, eventsByType, transactions,
                totalIncome, totalExpenses, attendanceByGroup, pastoralAttention, smallGroupHealth,
                upcomingBirthdays, missingBirthDatePeople);
    }

    private List<GroupAttendanceRow> attendanceByGroup(Long churchId, List<AttendanceRecord> attendanceRecords) {
        Map<Long, GroupAttendanceAccumulator> grouped = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRecords) {
            if (record.getGroup() == null || record.getGroup().getId() == null) {
                continue;
            }
            Long groupId = record.getGroup().getId();
            GroupAttendanceAccumulator accumulator = grouped.computeIfAbsent(groupId, ignored -> new GroupAttendanceAccumulator(
                    record.getGroup().getName(),
                    record.getGroup().getType() == null ? "Fără tip" : record.getGroup().getType().name()));
            accumulator.total++;
            if (record.getStatus() == AttendanceStatus.PRESENT) accumulator.present++;
            if (record.getStatus() == AttendanceStatus.ABSENT) accumulator.absent++;
        }

        for (ChurchGroup group : groupRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("type"), Sort.Order.asc("name")))) {
            grouped.putIfAbsent(group.getId(), new GroupAttendanceAccumulator(
                    group.getName(),
                    group.getType() == null ? "Fără tip" : group.getType().name()));
        }

        return grouped.values().stream()
                .map(item -> new GroupAttendanceRow(item.name, item.type, item.present, item.absent, item.total))
                .toList();
    }

    private List<PastoralAttentionRow> pastoralAttention(List<AttendanceRecord> attendanceRecords) {
        Map<Long, PersonAttendanceAccumulator> grouped = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRecords) {
            if (record.getPerson() == null || record.getPerson().getId() == null) {
                continue;
            }
            Person person = record.getPerson();
            PersonAttendanceAccumulator accumulator = grouped.computeIfAbsent(person.getId(), ignored -> new PersonAttendanceAccumulator(
                    person.getId(),
                    personName(person),
                    value(person.getPhone())));
            accumulator.total++;
            if (record.getStatus() == AttendanceStatus.PRESENT) accumulator.present++;
            if (record.getStatus() == AttendanceStatus.ABSENT) accumulator.absent++;
        }

        return grouped.values().stream()
                .filter(item -> item.absent >= 2 || (item.total > 0 && item.present == 0))
                .sorted((a, b) -> {
                    int absentCompare = Long.compare(b.absent, a.absent);
                    if (absentCompare != 0) return absentCompare;
                    return a.name.compareToIgnoreCase(b.name);
                })
                .map(item -> new PastoralAttentionRow(
                        item.personId,
                        item.name,
                        item.phone,
                        item.present,
                        item.absent,
                        item.total,
                        item.present == 0 ? "Fără prezență în perioada selectată" : "Absențe recurente"))
                .toList();
    }

    private List<SmallGroupHealthRow> smallGroupHealth(Long churchId, List<AttendanceRecord> attendanceRecords) {
        Map<Long, SmallGroupAccumulator> grouped = new LinkedHashMap<>();
        for (ChurchGroup group : groupRepository.findAllByChurchId(churchId, Sort.by(Sort.Order.asc("name")))) {
            if (group.getType() == null || !"SMALL_GROUP".equalsIgnoreCase(group.getType().name())) {
                continue;
            }
            String leaderName = personName(group.getLeader());
            Set<Long> memberIds = group.getMembers() == null ? Set.of() : group.getMembers().stream()
                    .map(Person::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            grouped.put(group.getId(), new SmallGroupAccumulator(group.getName(), leaderName, memberIds));
        }

        for (AttendanceRecord record : attendanceRecords) {
            if (record.getGroup() == null || record.getGroup().getId() == null) {
                continue;
            }
            SmallGroupAccumulator accumulator = grouped.get(record.getGroup().getId());
            if (accumulator == null) {
                continue;
            }
            accumulator.total++;
            if (record.getStatus() == AttendanceStatus.PRESENT) {
                accumulator.present++;
                if (record.getPerson() != null && record.getPerson().getId() != null) {
                    accumulator.presentMemberIds.add(record.getPerson().getId());
                }
            } else if (record.getStatus() == AttendanceStatus.ABSENT) {
                accumulator.absent++;
            }
        }

        return grouped.values().stream()
                .map(item -> {
                    int rate = item.total == 0 ? 0 : (int) Math.round((item.present * 100.0) / item.total);
                    long inactiveMembers = item.memberIds.stream()
                            .filter(memberId -> !item.presentMemberIds.contains(memberId))
                            .count();
                    return new SmallGroupHealthRow(
                            item.name,
                            item.leaderName == null || item.leaderName.isBlank() ? "Fără lider" : item.leaderName,
                            item.memberIds.size(),
                            item.present,
                            item.absent,
                            item.total,
                            rate,
                            inactiveMembers);
                })
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    private List<BirthdayMomentRow> upcomingBirthdays(List<Person> persons, LocalDate referenceDate, int windowDays) {
        return persons.stream()
                .filter(Objects::nonNull)
                .filter(person -> person.getBirthDate() != null && !person.getBirthDate().isAfter(referenceDate))
                .map(person -> {
                    LocalDate birthDate = person.getBirthDate();
                    long daysUntil = daysUntilBirthday(referenceDate, birthDate);
                    if (daysUntil < 0 || daysUntil > windowDays) {
                        return null;
                    }
                    int turningAge = Period.between(birthDate, referenceDate.plusDays(daysUntil)).getYears();
                    return new BirthdayMomentRow(
                            person.getId(),
                            personName(person),
                            birthDate.format(RO_DATE),
                            turningAge,
                            (int) daysUntil);
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    int daysCompare = Integer.compare(a.daysUntil(), b.daysUntil());
                    if (daysCompare != 0) return daysCompare;
                    return a.name().compareToIgnoreCase(b.name());
                })
                .toList();
    }

    private List<MissingBirthDateRow> missingBirthDatePeople(List<Person> persons) {
        return persons.stream()
                .filter(Objects::nonNull)
                .filter(person -> person.getBirthDate() == null)
                .map(person -> new MissingBirthDateRow(person.getId(), personName(person), value(person.getPhone())))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    private List<SmallGroupChartBand> smallGroupChartBands(List<SmallGroupHealthRow> rows, int totalPeople) {
        if ((rows == null || rows.isEmpty()) && totalPeople <= 0) {
            return List.of();
        }
        String[] palette = {"#60a5fa", "#34d399", "#f59e0b", "#a78bfa", "#f87171", "#22d3ee", "#84cc16", "#fb7185"};
        long groupedMembers = rows == null ? 0 : rows.stream().mapToLong(SmallGroupHealthRow::membersCount).sum();
        long noGroupCount = Math.max(0, totalPeople - groupedMembers);
        long totalMembers = groupedMembers + noGroupCount;
        if (totalMembers <= 0) {
            return List.of();
        }
        List<SmallGroupChartBand> bands = new ArrayList<>();
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                SmallGroupHealthRow row = rows.get(i);
                int percent = percentOf(row.membersCount(), totalMembers);
                if (percent <= 0) {
                    continue;
                }
                bands.add(new SmallGroupChartBand(
                        row.name(),
                        row.membersCount(),
                        percent,
                        palette[i % palette.length],
                        row.attendanceRatePercent()));
            }
        }
        if (noGroupCount > 0) {
            int percent = percentOf(noGroupCount, totalMembers);
            bands.add(new SmallGroupChartBand(
                    "Fără grup",
                    noGroupCount,
                    percent,
                    "#9ca3af",
                    0));
        }
        return bands;
    }

    private String smallGroupPieStyle(List<SmallGroupChartBand> bands) {
        if (bands == null || bands.isEmpty()) {
            return "background: conic-gradient(#e5e7eb 0deg 360deg);";
        }
        int total = bands.stream().mapToInt(SmallGroupChartBand::percent).sum();
        if (total <= 0) {
            return "background: conic-gradient(#e5e7eb 0deg 360deg);";
        }

        StringBuilder gradient = new StringBuilder("conic-gradient(");
        double start = 0;
        for (int i = 0; i < bands.size(); i++) {
            SmallGroupChartBand band = bands.get(i);
            double share = (band.percent() * 360.0) / total;
            double end = (i == bands.size() - 1) ? 360.0 : Math.min(360.0, start + share);
            gradient.append(band.color()).append(" ").append(Math.round(start)).append("deg ").append(Math.round(end)).append("deg");
            if (i < bands.size() - 1) {
                gradient.append(", ");
            }
            start = end;
        }
        gradient.append(")");
        return "background: " + gradient + ";";
    }

    private List<AgeSliceLabel> smallGroupSliceLabels(List<SmallGroupChartBand> bands) {
        if (bands == null || bands.isEmpty()) {
            return List.of();
        }
        int total = bands.stream().mapToInt(SmallGroupChartBand::percent).sum();
        if (total <= 0) {
            return List.of();
        }
        List<AgeSliceLabel> labels = new ArrayList<>();
        double start = 0;
        for (SmallGroupChartBand band : bands) {
            if (band.percent() <= 0) {
                continue;
            }
            double share = (band.percent() * 360.0) / total;
            double end = Math.min(360.0, start + share);
            double midAngle = start + (end - start) / 2.0;
            double radians = Math.toRadians(midAngle);
            double radius = 33.5;
            double x = 50.0 + Math.sin(radians) * radius;
            double y = 50.0 - Math.cos(radians) * radius;
            labels.add(new AgeSliceLabel(band.percent(), x, y));
            start = end;
        }
        return labels;
    }

    private long daysUntilBirthday(LocalDate today, LocalDate birthDate) {
        LocalDate nextBirthday = birthDate.withYear(today.getYear());
        if (nextBirthday.isBefore(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }
        return ChronoUnit.DAYS.between(today, nextBirthday);
    }

    private String attendanceCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Data,Program,Grup,Persoana,Status\n");
        for (AttendanceRecord record : data.attendanceRecords()) {
            csv.append(csvLine(
                    value(record.getAttendanceDate()),
                    record.getServiceSession() == null ? "" : record.getServiceSession().name(),
                    record.getGroup() == null ? "Toată biserica" : value(record.getGroup().getName()),
                    personName(record.getPerson()),
                    record.getStatus() == null ? "" : record.getStatus().name()));
        }
        return csv.toString();
    }

    private String attendanceGroupsCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Grup,Tip,Prezenti,Absenti,Total,Rata prezenta\n");
        for (GroupAttendanceRow row : data.attendanceByGroup()) {
            csv.append(csvLine(
                    row.name(),
                    row.type(),
                    String.valueOf(row.present()),
                    String.valueOf(row.absent()),
                    String.valueOf(row.total()),
                    row.total() == 0 ? "0%" : Math.round((row.present() * 100.0) / row.total()) + "%"));
        }
        return csv.toString();
    }

    private String peopleCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Nume,Tip membru,Rol,Telefon,Adresa\n");
        for (Person person : data.persons()) {
            csv.append(csvLine(
                    personName(person),
                    person.getMemberType() == null ? "" : person.getMemberType().name(),
                    value(person.getChurchRole()),
                    value(person.getPhone()),
                    value(person.getAddress())));
        }
        return csv.toString();
    }

    private String pastoralAttentionCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Nume,Telefon,Prezente,Absente,Total,Motiv\n");
        for (PastoralAttentionRow row : data.pastoralAttention()) {
            csv.append(csvLine(
                    row.name(),
                    row.phone(),
                    String.valueOf(row.present()),
                    String.valueOf(row.absent()),
                    String.valueOf(row.total()),
                    row.reason()));
        }
        return csv.toString();
    }

    private String eventsCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Nume,Data inceput,Data final,Status,Tip,Grup,Responsabil\n");
        for (Event event : data.events()) {
            csv.append(csvLine(
                    value(event.getEventName()),
                    value(event.getOpenDate()),
                    value(event.getEndDate()),
                    blankToDefault(event.getStatus(), "Fără status"),
                    event.getEventType() == null ? "Fără tip" : event.getEventType().name(),
                    event.getAssociatedGroup() == null ? "" : value(event.getAssociatedGroup().getName()),
                    personName(event.getImplementedBy())));
        }
        return csv.toString();
    }

    private String financeCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Tip,Nume,Descriere,Suma\n");
        for (Transaction transaction : data.transactions()) {
            csv.append(csvLine(
                    normalizeType(transaction.getType()).equals("income") ? "Venit" : "Cheltuială",
                    value(transaction.getName()),
                    value(transaction.getDescription()),
                    transaction.getAmount() == null ? "0" : String.format(Locale.ROOT, "%.2f", transaction.getAmount())));
        }
        return csv.toString();
    }

    private String smallGroupsCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Grup,Lider,Membri,Prezenti,Absenti,Total,Rata prezenta,Membri inactivi\n");
        for (SmallGroupHealthRow row : data.smallGroupHealth()) {
            csv.append(csvLine(
                    row.name(),
                    row.leaderName(),
                    String.valueOf(row.membersCount()),
                    String.valueOf(row.present()),
                    String.valueOf(row.absent()),
                    String.valueOf(row.total()),
                    row.attendanceRatePercent() + "%",
                    String.valueOf(row.inactiveMembers())));
        }
        return csv.toString();
    }

    private String keyMomentsCsv(ReportData data) {
        StringBuilder csv = new StringBuilder("Tip,Nume,Detaliu,Telefon\n");
        for (BirthdayMomentRow row : data.upcomingBirthdays()) {
            String detail = row.daysUntil() == 0
                    ? "Aniversare astăzi (" + row.turningAge() + " ani)"
                    : "În " + row.daysUntil() + " zile (" + row.turningAge() + " ani)";
            csv.append(csvLine("Aniversare", row.name(), detail + " - născut " + row.birthDate(), ""));
        }
        for (MissingBirthDateRow row : data.missingBirthDatePeople()) {
            csv.append(csvLine("Date lipsă", row.name(), "Fără dată naștere", row.phone()));
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

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(value);
    }

    private List<ReportRow> mapRows(Map<String, Long> values) {
        List<ReportRow> rows = new ArrayList<>();
        values.forEach((label, value) -> rows.add(new ReportRow(label, value == null ? 0 : value)));
        return rows;
    }

    private List<AgeBandRow> ageBands(List<Person> persons, LocalDate referenceDate) {
        long upTo20 = 0;
        long from20To45 = 0;
        long from45To70 = 0;
        long over70 = 0;

        for (Person person : persons) {
            if (person == null || person.getBirthDate() == null || person.getBirthDate().isAfter(referenceDate)) {
                continue;
            }
            int age = Period.between(person.getBirthDate(), referenceDate).getYears();
            if (age <= 20) {
                upTo20++;
            } else if (age <= 45) {
                from20To45++;
            } else if (age <= 70) {
                from45To70++;
            } else {
                over70++;
            }
        }

        long knownAgeTotal = upTo20 + from20To45 + from45To70 + over70;
        return List.of(
                new AgeBandRow("0-20 ani", upTo20, percentOf(upTo20, knownAgeTotal), "#a5b4fc"),
                new AgeBandRow("20-45 ani", from20To45, percentOf(from20To45, knownAgeTotal), "#93c5fd"),
                new AgeBandRow("45-70 ani", from45To70, percentOf(from45To70, knownAgeTotal), "#86efac"),
                new AgeBandRow("vârstnici", over70, percentOf(over70, knownAgeTotal), "#fcd34d")
        );
    }

    private int percentOf(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((value * 100.0) / total);
    }

    private String agePieStyle(List<AgeBandRow> bands) {
        if (bands == null || bands.isEmpty()) {
            return "background: conic-gradient(#e5e7eb 0deg 360deg);";
        }
        int total = bands.stream().mapToInt(AgeBandRow::percent).sum();
        if (total <= 0) {
            return "background: conic-gradient(#e5e7eb 0deg 360deg);";
        }

        StringBuilder gradient = new StringBuilder("conic-gradient(");
        double start = 0;
        for (int i = 0; i < bands.size(); i++) {
            AgeBandRow band = bands.get(i);
            double share = (band.percent() * 360.0) / total;
            double end = (i == bands.size() - 1) ? 360.0 : Math.min(360.0, start + share);
            gradient.append(band.color()).append(" ").append(Math.round(start)).append("deg ").append(Math.round(end)).append("deg");
            if (i < bands.size() - 1) {
                gradient.append(", ");
            }
            start = end;
        }
        gradient.append(")");
        return "background: " + gradient + ";";
    }

    private List<AgeSliceLabel> ageSliceLabels(List<AgeBandRow> bands) {
        if (bands == null || bands.isEmpty()) {
            return List.of();
        }
        int total = bands.stream().mapToInt(AgeBandRow::percent).sum();
        if (total <= 0) {
            return List.of();
        }

        List<AgeSliceLabel> labels = new ArrayList<>();
        double start = 0;
        for (AgeBandRow band : bands) {
            if (band.percent() <= 0) {
                continue;
            }
            double share = (band.percent() * 360.0) / total;
            double end = Math.min(360.0, start + share);
            double midAngle = start + (end - start) / 2.0;
            double radians = Math.toRadians(midAngle);
            double radius = 33.5;
            // CSS conic-gradient uses 0deg at top and grows clockwise.
            // Convert to cartesian percentage coordinates in that system.
            double x = 50.0 + Math.sin(radians) * radius;
            double y = 50.0 - Math.cos(radians) * radius;
            labels.add(new AgeSliceLabel(band.percent(), x, y));
            start = end;
        }
        return labels;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeType(String value) {
        if (value == null) return "expense";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("income") || normalized.equals("venit") || normalized.equals("intrare") || normalized.contains("inc")) {
            return "income";
        }
        return "expense";
    }

    public record ReportRow(String label, long value) {}

    public record AgeBandRow(String label, long value, int percent, String color) {}
    public record AgeSliceLabel(int percent, double x, double y) {}

    public record GroupAttendanceRow(String name, String type, long present, long absent, long total) {}

    public record PastoralAttentionRow(Long personId, String name, String phone, long present, long absent, long total, String reason) {}
    public record SmallGroupHealthRow(String name, String leaderName, int membersCount, long present, long absent, long total,
                                      int attendanceRatePercent, long inactiveMembers) {}
    public record BirthdayMomentRow(Long personId, String name, String birthDate, int turningAge, int daysUntil) {}
    public record MissingBirthDateRow(Long personId, String name, String phone) {}
    public record SmallGroupChartBand(String label, long value, int percent, String color, int attendanceRatePercent) {}

    private static class SmallGroupAccumulator {
        private final String name;
        private final String leaderName;
        private final Set<Long> memberIds;
        private final Set<Long> presentMemberIds = new LinkedHashSet<>();
        private long present;
        private long absent;
        private long total;

        private SmallGroupAccumulator(String name, String leaderName, Set<Long> memberIds) {
            this.name = name == null || name.isBlank() ? "Grup fără nume" : name;
            this.leaderName = leaderName;
            this.memberIds = memberIds;
        }
    }

    private static class GroupAttendanceAccumulator {
        private final String name;
        private final String type;
        private long present;
        private long absent;
        private long total;

        private GroupAttendanceAccumulator(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    private static class PersonAttendanceAccumulator {
        private final Long personId;
        private final String name;
        private final String phone;
        private long present;
        private long absent;
        private long total;

        private PersonAttendanceAccumulator(Long personId, String name, String phone) {
            this.personId = personId;
            this.name = name;
            this.phone = phone;
        }
    }

    public record ReportData(LocalDate fromDate,
                             LocalDate toDate,
                             List<AttendanceRecord> attendanceRecords,
                             long presentCount,
                             long absentCount,
                             List<Person> persons,
                             Map<String, Long> peopleByType,
                             long peopleWithoutPhone,
                             long peopleWithoutAddress,
                             List<Event> events,
                             Map<String, Long> eventsByStatus,
                             Map<String, Long> eventsByType,
                             List<Transaction> transactions,
                             double totalIncome,
                             double totalExpenses,
                             List<GroupAttendanceRow> attendanceByGroup,
                             List<PastoralAttentionRow> pastoralAttention,
                             List<SmallGroupHealthRow> smallGroupHealth,
                             List<BirthdayMomentRow> upcomingBirthdays,
                             List<MissingBirthDateRow> missingBirthDatePeople) {}
}
