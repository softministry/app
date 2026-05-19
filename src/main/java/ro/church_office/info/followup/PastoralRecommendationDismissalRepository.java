package ro.church_office.info.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PastoralRecommendationDismissalRepository extends JpaRepository<PastoralRecommendationDismissal, Long> {
    default Optional<PastoralRecommendationDismissal> findByChurchIdAndPersonIdAndRecommendationType(Long churchId, Long personId, String recommendationType) { return Optional.empty(); }
    default Optional<PastoralRecommendationDismissal> findByIdAndChurchId(Long id, Long churchId) { return Optional.empty(); }
    default List<PastoralRecommendationDismissal> findAllByChurchIdAndDismissedUntilGreaterThanEqual(Long churchId, LocalDate date){ return List.of(); }
    default List<PastoralRecommendationDismissal> findTop12ByChurchIdOrderByUpdatedAtDesc(Long churchId){ return List.of(); }
}
