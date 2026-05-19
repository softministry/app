package ro.church_office.teamleaf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.DTO.ChurchInfoDTO;
import ro.church_office.info.events.DAO.EventService;
import ro.church_office.info.events.EventDTO;
import ro.church_office.info.events.EventStatus;
import ro.church_office.info.events.EventType;
import ro.church_office.info.events.Priority;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.groups.GroupType;
import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Arrays;

@Component
@Profile({"dev-eng", "dev-ro"})
public class DevelopmentDataBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataBootstrap.class);

    private final PersonRepository personRepository;
    private final ChurchGroupRepository groupRepository;
    private final EventService eventService;
    private final ChurchInfoService churchInfoService;
    private final ChurchContextService churchContextService;
    private final Environment environment;

    public DevelopmentDataBootstrap(PersonRepository personRepository,
                                    ChurchGroupRepository groupRepository,
                                    EventService eventService,
                                    ChurchInfoService churchInfoService,
                                    ChurchContextService churchContextService,
                                    Environment environment) {
        this.personRepository = personRepository;
        this.groupRepository = groupRepository;
        this.eventService = eventService;
        this.churchInfoService = churchInfoService;
        this.churchContextService = churchContextService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long churchId = ensureChurch();
        if (churchId == null) {
            log.warn("Dev seed skipped: active church is missing.");
            return;
        }

        List<Person> existingPersons = personRepository.findAllByChurchId(churchId, Sort.unsorted());
        if (!existingPersons.isEmpty()) {
            log.info("Dev seed skipped: church {} already has {} persons.", churchId, existingPersons.size());
            return;
        }

        boolean romanianDataset = isRomanianProfile();
        Random random = new Random(romanianDataset ? 20260505L : 20260506L);

        List<Person> people = createPeople(churchId, romanianDataset, random);
        List<ChurchGroup> groups = createGroups(churchId, people, romanianDataset, random);
        createEvents(people, groups, romanianDataset, random);

        log.info("Dev seed completed for {}: {} persons, {} groups, {} events.",
                romanianDataset ? "dev-ro" : "dev-eng",
                people.size(),
                groups.size(),
                42);
    }

    private Long ensureChurch() {
        try {
            Long churchId = churchContextService.getOrCreateActiveChurchId();
            ChurchInfoDTO current = churchInfoService.get().orElse(null);
            if (current != null && current.id != null && current.name != null && !current.name.isBlank()) {
                return churchId;
            }
            if (current != null) {
                current.name = isRomanianProfile() ? "Biserica Speranța" : "Hope Community Church";
                churchInfoService.save(current);
            }
            return churchId;
        } catch (Exception ex) {
            log.warn("Could not ensure active church for dev seed: {}", ex.getMessage());
            return null;
        }
    }

    private boolean isRomanianProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(p -> p.toLowerCase(Locale.ROOT))
                .anyMatch("dev-ro"::equals);
    }

    private List<Person> createPeople(Long churchId, boolean ro, Random random) {
        List<String> firstNames = ro
                ? List.of("Andrei", "Mihai", "Cristian", "Alex", "Vlad", "Daniel", "Gabriel", "Raluca", "Ioana", "Ana", "Maria", "Elena")
                : List.of("James", "Daniel", "Michael", "David", "Samuel", "Noah", "Emma", "Olivia", "Sophia", "Grace", "Hannah", "Mia");
        List<String> lastNames = ro
                ? List.of("Popescu", "Ionescu", "Dumitrescu", "Marin", "Radu", "Matei", "Stan", "Nistor", "Georgescu", "Munteanu")
                : List.of("Smith", "Johnson", "Miller", "Davis", "Taylor", "Anderson", "White", "Clark", "Harris", "Lewis");
        List<String> roles = ro
                ? List.of("Pastor coordonator", "Prezbiter", "Diacon", "Lider tineret", "Lider închinare", "Învățător școala duminicală", "Membru")
                : List.of("Lead Pastor", "Elder", "Deacon", "Youth Leader", "Worship Leader", "Sunday School Teacher", "Member");

        int size = 120;
        List<Person> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Person p = new Person();
            p.setChurchId(churchId);
            p.setFirstName(firstNames.get(random.nextInt(firstNames.size())));
            p.setLastName(lastNames.get(random.nextInt(lastNames.size())));
            p.setPhone("+40 7" + (1000000 + random.nextInt(8999999)));
            p.setChurchRole(roles.get(random.nextInt(roles.size())));
            p.setAddress(ro ? "Str. Exemplu " + (1 + random.nextInt(120)) : (100 + random.nextInt(900)) + " Sample St");
            p.setPosition(random.nextBoolean()
                    ? (ro
                    ? List.of("Echipă închinare", "Cor bisericesc", "Echipă media", "Asistență pastorală", "Lucrare cu tinerii").get(random.nextInt(5))
                    : List.of("Worship Team", "Church Choir", "Media Team", "Pastoral Care", "Youth Ministry").get(random.nextInt(5)))
                    : null);
            p.setBirthDate(LocalDate.now().minusYears(14 + random.nextInt(55)).minusDays(random.nextInt(365)));
            p.setMemberType(random.nextDouble() < 0.14 ? MemberType.FREND : MemberType.MEMBER);
            result.add(personRepository.save(p));
        }
        return result;
    }

    private List<ChurchGroup> createGroups(Long churchId, List<Person> people, boolean ro, Random random) {
        List<String> names = ro
                ? List.of(
                "Comitet diaconi",
                "Consiliu prezbiteri",
                "Tineret penticostal",
                "Rugăciune și mijlocire",
                "Școala duminicală",
                "Cor și închinare")
                : List.of(
                "Deacons Committee",
                "Elders Council",
                "Pentecostal Youth",
                "Prayer and Intercession",
                "Sunday School Team",
                "Choir and Worship");
        List<String> descriptions = ro
                ? List.of(
                "Coordonare slujire practică, vizite și sprijin comunitar.",
                "Consiliere spirituală, disciplină și direcție doctrinară.",
                "Ucenicie, părtășie și proiecte de misiune pentru tineri.",
                "Întâlniri de rugăciune pentru biserică, familii și misiune.",
                "Planificare lecții biblice și activități pentru copii.",
                "Pregătire muzicală pentru serviciile divine și seri speciale.")
                : List.of(
                "Practical ministry coordination, visits, and community support.",
                "Spiritual counsel, discipline, and doctrinal direction.",
                "Discipleship, fellowship, and outreach projects for youth.",
                "Prayer meetings for church, families, and outreach.",
                "Bible lesson planning and activities for children.",
                "Music preparation for worship services and special evenings.");
        List<ChurchGroup> groups = new ArrayList<>();
        List<Person> maleLeaders = maleAssigneePool(people, ro);
        for (int i = 0; i < names.size(); i++) {
            ChurchGroup g = new ChurchGroup();
            g.setChurchId(churchId);
            g.setName(names.get(i));
            g.setType(GroupType.SMALL_GROUP);
            g.setDescription(descriptions.get(i));
            Person leader = maleLeaders.get(random.nextInt(maleLeaders.size()));
            g.setLeader(leader);

            java.util.LinkedHashSet<Person> members = new java.util.LinkedHashSet<>();
            while (members.size() < 12) {
                members.add(people.get(random.nextInt(people.size())));
            }
            members.add(leader);
            g.setMembers(members);
            groups.add(groupRepository.save(g));
        }
        return groups;
    }

    private void createEvents(List<Person> people, List<ChurchGroup> groups, boolean ro, Random random) {
        List<String> eventNames = ro
                ? List.of(
                "Serviciu divin duminică dimineața",
                "Studiu biblic doctrinar baptist",
                "Rugăciune de mijlocire penticostală",
                "Întâlnire comitet diaconi",
                "Repetiție fanfară și cor",
                "Cateheză pentru botez",
                "Evanghelizare de cartier",
                "Școala duminicală copii",
                "Conferință tineret penticostal",
                "Vizite pastorale familii",
                "Întâlnire slujitori și prezbiteri",
                "Seară de mărturii și închinare")
                : List.of(
                "Sunday Worship Service",
                "Baptist Doctrine Bible Study",
                "Pentecostal Intercessory Prayer",
                "Deacons Committee Meeting",
                "Brass Band and Choir Rehearsal",
                "Baptism Preparation Class",
                "Neighborhood Outreach",
                "Children Sunday School",
                "Pentecostal Youth Conference",
                "Pastoral Family Visits",
                "Elders and Ministers Meeting",
                "Testimony and Worship Evening");
        EventStatus[] statuses = EventStatus.values();
        EventType[] eventTypes = EventType.values();
        Priority[] priorities = Priority.values();
        List<Person> maleAssignees = maleAssigneePool(people, ro);

        for (int i = 0; i < 42; i++) {
            EventDTO dto = new EventDTO();
            dto.setEventName(eventNames.get(random.nextInt(eventNames.size())));
            dto.setAbout(ro
                    ? "Scenariu demo pentru biserică baptistă/penticostală: slujire, ucenicie, rugăciune și misiune."
                    : "Demo scenario for Baptist/Pentecostal church life: worship, discipleship, prayer, and outreach.");
            dto.setStatus(statuses[random.nextInt(statuses.length)].name());
            dto.setEventType(eventTypes[random.nextInt(eventTypes.length)].name());
            dto.setPriority(priorities[random.nextInt(priorities.length)].name());
            dto.setOpenDate(LocalDate.now().minusDays(random.nextInt(120)));
            ChurchGroup g = groups.get(random.nextInt(groups.size()));
            dto.setGroupId(g.getId());

            Person assignee = maleAssignees.get(random.nextInt(maleAssignees.size()));
            PersonDTO personDTO = new PersonDTO();
            personDTO.setId(assignee.getId());
            dto.setImplementedBy(personDTO);
            eventService.saveEvent(dto);
        }
    }

    private List<Person> maleAssigneePool(List<Person> people, boolean ro) {
        List<String> maleFirstNames = ro
                ? List.of("Andrei", "Mihai", "Cristian", "Alex", "Vlad", "Daniel", "Gabriel")
                : List.of("James", "Daniel", "Michael", "David", "Samuel", "Noah");
        List<Person> males = people.stream()
                .filter(person -> person.getFirstName() != null && maleFirstNames.contains(person.getFirstName()))
                .toList();
        return males.isEmpty() ? people : males;
    }
}
