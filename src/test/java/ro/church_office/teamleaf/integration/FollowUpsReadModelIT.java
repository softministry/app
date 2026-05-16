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

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class FollowUpsReadModelIT extends AbstractContainerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ChurchInfoRepository churchInfoRepository;
    @Autowired
    private ChurchContextService churchContextService;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private PastoralFollowUpRepository followUpRepository;

    private Long activeChurchId;
    private Person targetPerson;

    @BeforeEach
    void setupData() {
        ChurchInfo church = IntegrationFixtures.ensureChurch(churchInfoRepository, "IT Church Read");
        activeChurchId = church.getId();
        churchContextService.setActiveChurchId(activeChurchId);

        targetPerson = IntegrationFixtures.createPerson(personRepository, activeChurchId, "Ioan", "Marin");

        PastoralFollowUp openDue = new PastoralFollowUp();
        openDue.setChurchId(activeChurchId);
        openDue.setPerson(targetPerson);
        openDue.setStatus(PastoralFollowUpStatus.OPEN);
        openDue.setNextContactDate(LocalDate.now().minusDays(1));
        openDue.setNotes("Needs call this week");
        followUpRepository.save(openDue);

        PastoralFollowUp done = new PastoralFollowUp();
        done.setChurchId(activeChurchId);
        done.setPerson(targetPerson);
        done.setStatus(PastoralFollowUpStatus.DONE);
        done.setNextContactDate(LocalDate.now().plusDays(10));
        done.setNotes("Resolved");
        followUpRepository.save(done);
    }

    @Test
    void listDueFilterShowsRelevantCase() throws Exception {
        mockMvc.perform(get("/follow-ups")
                        .with(user("admin").roles("ADMIN"))
                        .param("due", "true")
                        .param("personId", targetPerson.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Needs call this week")));
    }

    @Test
    void resultsFragmentCanBeRenderedWithFilters() throws Exception {
        mockMvc.perform(post("/follow-ups/results")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("status", "OPEN")
                        .param("personId", targetPerson.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Needs call this week")));
    }

    @Test
    void exportReturnsCsvAttachmentWithPersonAndNotes() throws Exception {
        mockMvc.perform(get("/follow-ups/export")
                        .with(user("admin").roles("ADMIN"))
                        .param("personId", targetPerson.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("follow-up-pastoral.csv")))
                .andExpect(content().string(containsString("Persoana,Telefon,Grup,Status")))
                .andExpect(content().string(containsString("Needs call this week")));
    }
}
