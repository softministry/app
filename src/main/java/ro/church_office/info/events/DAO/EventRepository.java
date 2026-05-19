package ro.church_office.info.events.DAO;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    default long countByChurchId(Long churchId) {
        return findAll().stream().filter(event -> churchId != null && churchId.equals(event.getChurchId())).count();
    }
    default Optional<Event> findByIdAndChurchId(Long id, Long churchId){
        return findById(id).filter(event -> churchId != null && churchId.equals(event.getChurchId()));
    }
    default List<Event> findAllByChurchIdAndAssociatedGroup_Id(Long churchId, Long groupId, Sort sort){
        return findAllByChurchId(churchId, sort).stream()
                .filter(event -> event.getAssociatedGroup() != null && groupId != null && groupId.equals(event.getAssociatedGroup().getId()))
                .toList();
    }
    default List<Event> findAllByChurchId(Long churchId, Sort sort){
        return findAll(sort).stream()
                .filter(event -> churchId != null && churchId.equals(event.getChurchId()))
                .toList();
    }
}
