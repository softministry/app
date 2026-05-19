package ro.church_office.info.visits.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    default long countByChurchId(Long churchId) { return 0L; }
    default Optional<Visit> findByIdAndChurchId(Long id, Long churchId){ return Optional.empty(); }
    default List<Visit> findAllByChurchIdOrderByVisitDateDescIdDesc(Long churchId){ return List.of(); }
}
