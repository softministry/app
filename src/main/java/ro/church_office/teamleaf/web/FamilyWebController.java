package ro.church_office.teamleaf.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;

@Controller
@RequestMapping("/families")
public class FamilyWebController {

    private final PersonRepository personRepository;
    private final ChurchContextService churchContextService;

    public FamilyWebController(PersonRepository personRepository,
                               ChurchContextService churchContextService) {
        this.personRepository = personRepository;
        this.churchContextService = churchContextService;
    }

    @GetMapping("/{key}")
    public String view(@PathVariable String key, Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        List<PersonDTO> persons = personRepository.findAllByChurchId(churchId, Sort.unsorted()).stream()
                .map(PersonDTO::fromEntity)
                .toList();

        Map<Long, PersonDTO> byId = new LinkedHashMap<>();
        persons.stream()
                .filter(person -> person.getId() != null)
                .forEach(person -> byId.put(person.getId(), person));

        List<PersonDTO> members = persons.stream()
                .filter(person -> key.equals(computeFamilyKey(person, byId)))
                .toList();

        List<FamilyMemberNode> roots = buildTree(members);

        model.addAttribute("familyKey", key);
        model.addAttribute("title", familyTitle(members));
        model.addAttribute("members", members);
        model.addAttribute("roots", roots);
        return "families/view";
    }

    public static String familyKeyFor(PersonDTO person, Map<Long, PersonDTO> map) {
        return computeFamilyKey(person, map);
    }

    private static String computeFamilyKey(PersonDTO person, Map<Long, PersonDTO> map) {
        if (person == null || person.getId() == null) {
            return "";
        }
        if (person.getSpouseId() != null) {
            return pairKey(person.getId(), person.getSpouseId());
        }
        if (person.getParentIds() != null && !person.getParentIds().isEmpty()) {
            Long firstParentId = person.getParentIds().get(0);
            PersonDTO parent = map.get(firstParentId);
            if (parent != null && parent.getSpouseId() != null) {
                return pairKey(parent.getId(), parent.getSpouseId());
            }
            return "p-" + firstParentId;
        }
        return "s-" + person.getId();
    }

    private static String pairKey(Long a, Long b) {
        if (a == null || b == null) {
            return "";
        }
        return a < b ? a + "-" + b : b + "-" + a;
    }

    private List<FamilyMemberNode> buildTree(List<PersonDTO> members) {
        Map<Long, FamilyMemberNode> nodeMap = new LinkedHashMap<>();
        for (PersonDTO member : members) {
            if (member.getId() != null) {
                nodeMap.put(member.getId(), new FamilyMemberNode(member));
            }
        }

        Set<Long> attached = new LinkedHashSet<>();
        for (FamilyMemberNode node : nodeMap.values()) {
            List<Long> parentIds = node.person().getParentIds();
            if (parentIds == null) {
                continue;
            }
            for (Long parentId : parentIds) {
                FamilyMemberNode parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.children().add(node);
                    attached.add(node.person().getId());
                }
            }
        }

        List<FamilyMemberNode> roots = new ArrayList<>();
        for (FamilyMemberNode node : nodeMap.values()) {
            if (node.person().getId() == null || !attached.contains(node.person().getId())) {
                roots.add(node);
            }
        }
        return roots;
    }

    private String familyTitle(List<PersonDTO> members) {
        if (members == null || members.isEmpty()) {
            return "Familie";
        }
        PersonDTO first = members.get(0);
        String lastName = first.getLastName() == null ? "" : first.getLastName().trim();
        if (!lastName.isEmpty()) {
            return "Familia " + lastName;
        }
        return first.getFullName().trim().isEmpty() ? "Familie" : first.getFullName().trim();
    }

    public static String displayName(PersonDTO person) {
        if (person == null) {
            return "—";
        }
        String fullName = person.getFullName() == null ? "" : person.getFullName().trim();
        return fullName.isEmpty() ? "Persoană fără nume" : fullName;
    }

    public static boolean isSpouseInTree(PersonDTO person, List<PersonDTO> members) {
        return person != null
                && person.getSpouseId() != null
                && members.stream().anyMatch(item -> Objects.equals(item.getId(), person.getSpouseId()));
    }

    public record FamilyMemberNode(PersonDTO person, List<FamilyMemberNode> children) {
        public FamilyMemberNode(PersonDTO person) {
            this(person, new ArrayList<>());
        }
    }
}
