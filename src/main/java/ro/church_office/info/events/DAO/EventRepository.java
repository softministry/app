package ro.church_office.info.events.DAO;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    default long countByChurchId(Long churchId) { return 0L; }
    default Optional<Event> findByIdAndChurchId(Long id, Long churchId){ return Optional.empty(); }
    default List<Event> findAllByChurchIdAndAssociatedGroup_Id(Long churchId, Long groupId, Sort sort){ return List.of(); }
    default List<Event> findAllByChurchId(Long churchId, Sort sort){ return List.of(); }
}
