package ro.church_office.info.person.service;

import org.springframework.stereotype.Service;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.person.DTO.PersonDTO;

import java.util.List;

@Service
public class InMemoryPersonService implements PersonService {
    private final PersonRepository personRepository;

    public InMemoryPersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public List<PersonDTO> getAllPersons(){
        return personRepository.findAll().stream().map(PersonDTO::fromEntity).toList();
    }

    @Override
    public void deletePerson(Long id) {
        if (id != null) {
            personRepository.deleteById(id);
        }
    }
}
