package ro.church_office.info.visits.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    default long countByChurchId(Long churchId) {
        return findAll().stream().filter(visit -> churchId != null && churchId.equals(visit.getChurchId())).count();
    }
    default Optional<Visit> findByIdAndChurchId(Long id, Long churchId){
        return findById(id).filter(visit -> churchId != null && churchId.equals(visit.getChurchId()));
    }
    default List<Visit> findAllByChurchIdOrderByVisitDateDescIdDesc(Long churchId){
        return findAll().stream()
                .filter(visit -> churchId != null && churchId.equals(visit.getChurchId()))
                .sorted(java.util.Comparator
                        .comparing(Visit::getVisitDate, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(Visit::getId, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }
}
