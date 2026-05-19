package ro.church_office.info.followup;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PastoralFollowUpRepository extends JpaRepository<PastoralFollowUp, Long> {
    default List<PastoralFollowUp> findAllByChurchId(Long churchId, Sort sort) {
        return findAll(sort).stream()
                .filter(item -> churchId != null && churchId.equals(item.getChurchId()))
                .toList();
    }
    default Optional<PastoralFollowUp> findByIdAndChurchId(Long id, Long churchId) {
        return findById(id).filter(item -> churchId != null && churchId.equals(item.getChurchId()));
    }
    default List<PastoralFollowUp> findFiltered(Long churchId, PastoralFollowUpStatus status, Long groupId, Long personId, String q){
        String needle = q == null ? null : q.replace("%", "").trim().toLowerCase();
        return findAllByChurchId(churchId, Sort.by(Sort.Order.desc("id"))).stream()
                .filter(item -> status == null || status == item.getStatus())
                .filter(item -> groupId == null || (item.getGroup() != null && groupId.equals(item.getGroup().getId())))
                .filter(item -> personId == null || (item.getPerson() != null && personId.equals(item.getPerson().getId())))
                .filter(item -> {
                    if (needle == null || needle.isBlank()) {
                        return true;
                    }
                    String notes = item.getNotes() == null ? "" : item.getNotes().toLowerCase();
                    String first = item.getPerson() == null || item.getPerson().getFirstName() == null ? "" : item.getPerson().getFirstName().toLowerCase();
                    String last = item.getPerson() == null || item.getPerson().getLastName() == null ? "" : item.getPerson().getLastName().toLowerCase();
                    String phone = item.getPerson() == null || item.getPerson().getPhone() == null ? "" : item.getPerson().getPhone().toLowerCase();
                    return notes.contains(needle) || first.contains(needle) || last.contains(needle) || phone.contains(needle);
                })
                .toList();
    }
    default List<PastoralFollowUp> findAllByChurchIdAndPerson_Id(Long churchId, Long personId, Sort sort){
        return findAllByChurchId(churchId, sort).stream()
                .filter(item -> item.getPerson() != null && personId != null && personId.equals(item.getPerson().getId()))
                .toList();
    }
}
