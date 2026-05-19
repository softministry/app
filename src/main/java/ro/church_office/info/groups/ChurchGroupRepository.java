package ro.church_office.info.groups;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChurchGroupRepository extends JpaRepository<ChurchGroup, Long> {
    interface PersonGroupMembership {
        Long getPersonId();
        Long getGroupId();
        default String getGroupName(){ return ""; }
        default GroupType getGroupType(){ return GroupType.SMALL_GROUP; }
    }
    default List<PersonGroupMembership> findMembershipsByChurchId(Long churchId) {
        return findAllByChurchId(churchId, Sort.by(Sort.Order.asc("name"))).stream()
                .flatMap(group -> group.getMembers().stream().map(person -> membership(person.getId(), group.getId(), group.getName(), group.getType())))
                .toList();
    }
    default List<ChurchGroup> findAllByChurchId(Long churchId, Sort sort) {
        return findAll(sort).stream()
                .filter(group -> churchId != null && churchId.equals(group.getChurchId()))
                .toList();
    }
    default Optional<ChurchGroup> findByIdAndChurchId(Long id, Long churchId) {
        return findById(id).filter(group -> churchId != null && churchId.equals(group.getChurchId()));
    }
    default List<ChurchGroup> searchByChurchAndText(Long churchId, String q, Sort sort){
        String needle = q == null ? "" : q.trim().toLowerCase();
        return findAllByChurchId(churchId, sort).stream()
                .filter(group -> {
                    String name = group.getName() == null ? "" : group.getName().toLowerCase();
                    String desc = group.getDescription() == null ? "" : group.getDescription().toLowerCase();
                    return name.contains(needle) || desc.contains(needle);
                })
                .toList();
    }
    default List<ChurchGroup> findAllByChurchIdAndMembers_Id(Long churchId, Long personId, Sort sort){
        return findAllByChurchId(churchId, sort).stream()
                .filter(group -> group.getMembers().stream().anyMatch(member -> personId != null && personId.equals(member.getId())))
                .toList();
    }
    default List<PersonGroupMembership> findMembershipsForPersons(Long churchId, Set<Long> personIds){
        return findAllByChurchId(churchId, Sort.by(Sort.Order.asc("name"))).stream()
                .flatMap(group -> group.getMembers().stream()
                        .filter(person -> person.getId() != null && personIds.contains(person.getId()))
                        .map(person -> membership(person.getId(), group.getId(), group.getName(), group.getType())))
                .toList();
    }

    private static PersonGroupMembership membership(Long personId, Long groupId, String groupName, GroupType groupType) {
        return new PersonGroupMembership() {
            @Override public Long getPersonId() { return personId; }
            @Override public Long getGroupId() { return groupId; }
            @Override public String getGroupName() { return groupName; }
            @Override public GroupType getGroupType() { return groupType; }
        };
    }
}
