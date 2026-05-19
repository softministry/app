package ro.church_office.info.person.DAO;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    default List<Person> findAllByChurchId(Long churchId, Sort sort){
        return findAll(sort).stream()
                .filter(person -> churchId != null && churchId.equals(person.getChurchId()))
                .toList();
    }
    default Optional<Person> findByIdAndChurchId(Long id, Long churchId){
        return findById(id).filter(person -> churchId != null && churchId.equals(person.getChurchId()));
    }
    default List<Person> searchByChurchAndName(Long churchId, String q, Sort sort){
        String needle = q == null ? "" : q.trim().toLowerCase();
        return findAllByChurchId(churchId, sort).stream()
                .filter(person -> {
                    String first = person.getFirstName() == null ? "" : person.getFirstName().toLowerCase();
                    String last = person.getLastName() == null ? "" : person.getLastName().toLowerCase();
                    String full = (first + " " + last).trim();
                    return first.contains(needle) || last.contains(needle) || full.contains(needle);
                })
                .toList();
    }
    default void deleteChildLinks(Long parentId) {}
    default void insertChildLink(Long parentId, Long childId) {}
    default List<Person> findAllByChurchIdAndBirthDateIsNotNull(Long churchId, Sort sort){
        return findAllByChurchId(churchId, sort).stream()
                .filter(person -> person.getBirthDate() != null)
                .toList();
    }
}
