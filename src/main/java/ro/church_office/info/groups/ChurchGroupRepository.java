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
    default List<PersonGroupMembership> findMembershipsByChurchId(Long churchId) { return List.of(); }
    default List<ChurchGroup> findAllByChurchId(Long churchId, Sort sort) { return List.of(); }
    default Optional<ChurchGroup> findByIdAndChurchId(Long id, Long churchId) { return Optional.empty(); }
    default List<ChurchGroup> searchByChurchAndText(Long churchId, String q, Sort sort){ return List.of(); }
    default List<ChurchGroup> findAllByChurchIdAndMembers_Id(Long churchId, Long personId, Sort sort){ return List.of(); }
    default List<PersonGroupMembership> findMembershipsForPersons(Long churchId, Set<Long> personIds){ return List.of(); }
}
