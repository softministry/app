package ro.church_office.teamleaf.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.church.DAO.ChurchInfo;
import ro.church_office.info.church.Repository.ChurchInfoRepository;
import ro.church_office.info.followup.PastoralFollowUp;
import ro.church_office.info.followup.PastoralFollowUpRepository;
import ro.church_office.info.followup.PastoralFollowUpStatus;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AdminAndFollowUpsIT extends AbstractContainerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChurchInfoRepository churchInfoRepository;
    @Autowired
    private ChurchContextService churchContextService;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private PastoralFollowUpRepository followUpRepository;

    private Long activeChurchId;

    @BeforeEach
    void setupChurchContext() {
        ChurchInfo church = IntegrationFixtures.ensureChurch(churchInfoRepository, "IT Church");
        activeChurchId = church.getId();
        churchContextService.setActiveChurchId(activeChurchId);
    }

    @Test
    void adminCreateUserPersistsNormalizedUserAndFallbackRole() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("username", "  New.Admin ")
                        .param("password", "Secret123!")
                        .param("role", "INVALID")
                        .param("answerOne", "Mama")
                        .param("answerTwo", "Oras"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User created = userRepository.findByUsername("new.admin").orElseThrow();
        assertEquals("VIEWER", created.getRole());
        assertNotNull(created.getSecurityAnswerOneHash());
        assertFalse(created.getSecurityAnswerOneHash().isBlank());
    }

    @Test
    void adminDeleteBlocksWhenOnlyOneUserExists() throws Exception {
        User only = IntegrationFixtures.ensureUser(userRepository, "solo.user", "x", "VIEWER", "h1", "h2");

        for (User u : userRepository.findAll()) {
            if (!u.getId().equals(only.getId())) {
                userRepository.delete(u);
            }
        }

        mockMvc.perform(post("/admin/users/{id}/delete", only.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertEquals(1, userRepository.count());
        userRepository.findById(only.getId()).orElseThrow();
    }

    @Test
    void followUpCreateThenStatusDonePersistsFlow() throws Exception {
        Person person = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Ana", "Pop");

        mockMvc.perform(post("/follow-ups")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("personId", person.getId().toString())
                        .param("status", "OPEN")
                        .param("contactMethod", "phone")
                        .param("nextContactDate", LocalDate.now().plusDays(3).toString())
                        .param("notes", "Follow-up initial"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/follow-ups"));

        List<PastoralFollowUp> created = followUpRepository.findAllByChurchId(activeChurchId,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("id")));
        PastoralFollowUp followUp = created.getFirst();

        mockMvc.perform(post("/follow-ups/{id}/status", followUp.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("status", "DONE")
                        .param("redirect", "/follow-ups"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/follow-ups"));

        PastoralFollowUp updated = followUpRepository.findByIdAndChurchId(followUp.getId(), activeChurchId).orElseThrow();
        assertEquals(PastoralFollowUpStatus.DONE, updated.getStatus());
        assertNotNull(updated.getLastContactDate());
    }

    @Test
    void adminUpdateUserChangesRoleAndSecurityAnswers() throws Exception {
        User existing = IntegrationFixtures.ensureUser(userRepository, "edit.user", "old-hash", "VIEWER", "a1", "a2");

        mockMvc.perform(post("/admin/users/{id}", existing.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("username", "  EDIT.USER  ")
                        .param("role", "PASTOR")
                        .param("answerOne", "updated-one")
                        .param("answerTwo", "updated-two"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User updated = userRepository.findById(existing.getId()).orElseThrow();
        assertEquals("edit.user", updated.getUsername());
        assertEquals("PASTOR", updated.getRole());
        assertNotNull(updated.getSecurityAnswerOneHash());
        assertNotNull(updated.getSecurityAnswerTwoHash());
    }

    @Test
    void followUpUpdateThenDeletePersistsChanges() throws Exception {
        Person person = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Mara", "Ionescu");
        PastoralFollowUp followUp = new PastoralFollowUp();
        followUp.setChurchId(activeChurchId);
        followUp.setPerson(person);
        followUp.setStatus(PastoralFollowUpStatus.OPEN);
        followUp = followUpRepository.save(followUp);

        mockMvc.perform(post("/follow-ups/{id}", followUp.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("personId", person.getId().toString())
                        .param("status", "IN_PROGRESS")
                        .param("contactMethod", "visit")
                        .param("notes", "Updated follow-up note")
                        .param("redirect", "/follow-ups"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/follow-ups"));

        PastoralFollowUp updated = followUpRepository.findByIdAndChurchId(followUp.getId(), activeChurchId).orElseThrow();
        assertEquals(PastoralFollowUpStatus.IN_PROGRESS, updated.getStatus());
        assertEquals("visit", updated.getContactMethod());
        assertEquals("Updated follow-up note", updated.getNotes());

        mockMvc.perform(post("/follow-ups/{id}/delete", followUp.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/follow-ups"));

        assertTrue(followUpRepository.findByIdAndChurchId(followUp.getId(), activeChurchId).isEmpty());
    }
}
