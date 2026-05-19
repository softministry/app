package ro.church_office.info.person.service;

import ro.church_office.info.person.DTO.PersonDTO;

import java.util.List;

public interface PersonService {
    default List<PersonDTO> search(Long churchId, String query){ return List.of(); }
    default List<PersonDTO> getAllPersons(){ return List.of(); }
    default void deletePerson(Long id) {}
}
