package ro.church_office.info.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PastoralPrivateNoteRepository extends JpaRepository<PastoralPrivateNote, Long> {
    default List<PastoralPrivateNote> findAllByChurchIdAndPerson_IdOrderByUpdatedAtDesc(Long churchId, Long personId){ return List.of(); }
    default Optional<PastoralPrivateNote> findByIdAndChurchIdAndPerson_Id(Long id, Long churchId, Long personId){ return Optional.empty(); }
}
