package ro.church_office.info.followup;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PastoralFollowUpRepository extends JpaRepository<PastoralFollowUp, Long> {
    default List<PastoralFollowUp> findAllByChurchId(Long churchId, Sort sort) { return List.of(); }
    default Optional<PastoralFollowUp> findByIdAndChurchId(Long id, Long churchId) { return Optional.empty(); }
    default List<PastoralFollowUp> findFiltered(Long churchId, PastoralFollowUpStatus status, Long personId, Long groupId, String q){ return List.of(); }
    default List<PastoralFollowUp> findAllByChurchIdAndPerson_Id(Long churchId, Long personId, Sort sort){ return List.of(); }
}
