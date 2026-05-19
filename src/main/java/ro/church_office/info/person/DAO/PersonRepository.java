package ro.church_office.info.person.DAO;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    default List<Person> findAllByChurchId(Long churchId, Sort sort){ return List.of(); }
    default Optional<Person> findByIdAndChurchId(Long id, Long churchId){ return Optional.empty(); }
    default List<Person> searchByChurchAndName(Long churchId, String q, Sort sort){ return List.of(); }
    default void deleteChildLinks(Long parentId) {}
    default void insertChildLink(Long parentId, Long childId) {}
    default List<Person> findAllByChurchIdAndBirthDateIsNotNull(Long churchId, Sort sort){ return List.of(); }
}
