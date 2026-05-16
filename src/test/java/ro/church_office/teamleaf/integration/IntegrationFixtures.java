package ro.church_office.teamleaf.integration;

import ro.church_office.info.church.DAO.ChurchInfo;
import ro.church_office.info.church.Repository.ChurchInfoRepository;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

final class IntegrationFixtures {

    private IntegrationFixtures() {}

    static User ensureUser(UserRepository userRepository,
                           String username,
                           String passwordHash,
                           String role,
                           String answerOneHash,
                           String answerTwoHash) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordHash);
            user.setRole(role);
            user.setSecurityAnswerOneHash(answerOneHash);
            user.setSecurityAnswerTwoHash(answerTwoHash);
            return userRepository.save(user);
        });
    }

    static ChurchInfo ensureChurch(ChurchInfoRepository churchInfoRepository, String name) {
        return churchInfoRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            ChurchInfo church = new ChurchInfo();
            church.setName(name);
            return churchInfoRepository.save(church);
        });
    }

    static Person createPerson(PersonRepository personRepository, Long churchId, String firstName, String lastName) {
        Person person = new Person();
        person.setChurchId(churchId);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        return personRepository.save(person);
    }
}
