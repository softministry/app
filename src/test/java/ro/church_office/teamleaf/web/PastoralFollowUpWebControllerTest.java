package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.followup.PastoralFollowUp;
import ro.church_office.info.followup.PastoralFollowUpRepository;
import ro.church_office.info.followup.PastoralFollowUpStatus;
import ro.church_office.info.groups.ChurchGroupRepository;
import ro.church_office.info.person.DAO.PersonRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PastoralFollowUpWebControllerTest {

    @Test
    void createRejectsWhenPersonIsInvalid() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        when(personRepository.findByIdAndChurchId(99L, 1L)).thenReturn(Optional.empty());

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        PastoralFollowUpWebController.FollowUpForm form = new PastoralFollowUpWebController.FollowUpForm();
        form.setPersonId(99L);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.create(form, redirect);

        assertEquals("redirect:/follow-ups/new", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        verify(followUpRepository, never()).save(any(PastoralFollowUp.class));
    }

    @Test
    void updateStageRejectsInvalidStageAndUsesSafeRedirectFallback() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);

        PastoralFollowUp followUp = new PastoralFollowUp();
        followUp.setId(12L);
        followUp.setChurchId(1L);
        when(followUpRepository.findByIdAndChurchId(12L, 1L)).thenReturn(Optional.of(followUp));

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.updateStage(12L, "bad_stage", "https://evil.example", redirect);

        assertEquals("redirect:/follow-ups", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        verify(followUpRepository, never()).save(any(PastoralFollowUp.class));
    }

    @Test
    void updateStatusReturnsSafeFallbackWhenFollowUpMissing() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        when(followUpRepository.findByIdAndChurchId(77L, 1L)).thenReturn(Optional.empty());

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.updateStatus(77L, PastoralFollowUpStatus.DONE, "https://bad.example", redirect);

        assertEquals("redirect:/follow-ups", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        verify(followUpRepository, never()).save(any(PastoralFollowUp.class));
    }

    @Test
    void markContactedSetsDatesAndRespectsInternalRedirect() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        PastoralFollowUp followUp = new PastoralFollowUp();
        followUp.setId(15L);
        followUp.setChurchId(1L);
        when(followUpRepository.findByIdAndChurchId(15L, 1L)).thenReturn(Optional.of(followUp));

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.markContacted(15L, "/dashboard", redirect);

        assertEquals("redirect:/dashboard", view);
        assertEquals(PastoralFollowUpStatus.OPEN, followUp.getStatus());
        assertNotNull(followUp.getLastContactDate());
        assertNotNull(followUp.getNextContactDate());
        verify(followUpRepository).save(followUp);
    }

    @Test
    void updateStageNewClearsLastContactDate() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        PastoralFollowUp followUp = new PastoralFollowUp();
        followUp.setId(20L);
        followUp.setChurchId(1L);
        followUp.setStatus(PastoralFollowUpStatus.IN_PROGRESS);
        followUp.setLastContactDate(LocalDate.now().minusDays(1));
        when(followUpRepository.findByIdAndChurchId(20L, 1L)).thenReturn(Optional.of(followUp));

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.updateStage(20L, "new", "/follow-ups?due=true", redirect);

        assertEquals("redirect:/follow-ups?due=true", view);
        assertEquals(PastoralFollowUpStatus.OPEN, followUp.getStatus());
        assertEquals(null, followUp.getLastContactDate());
        verify(followUpRepository).save(followUp);
    }

    @Test
    void updateStatusPersistsAndKeepsInternalRedirect() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        PastoralFollowUp followUp = new PastoralFollowUp();
        followUp.setId(31L);
        followUp.setChurchId(1L);
        when(followUpRepository.findByIdAndChurchId(31L, 1L)).thenReturn(Optional.of(followUp));

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.updateStatus(31L, PastoralFollowUpStatus.DONE, "/follow-ups", redirect);

        assertEquals("redirect:/follow-ups", view);
        assertEquals(PastoralFollowUpStatus.DONE, followUp.getStatus());
        assertNotNull(followUp.getLastContactDate());
        verify(followUpRepository).save(followUp);
    }

    @Test
    void markContactedReturnsFallbackWhenMissing() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        when(followUpRepository.findByIdAndChurchId(99L, 1L)).thenReturn(Optional.empty());

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.markContacted(99L, "https://evil.example", redirect);

        assertEquals("redirect:/follow-ups", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
    }

    @Test
    void updateStageResolvedMovesCaseToDone() {
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        PastoralFollowUpRepository followUpRepository = mock(PastoralFollowUpRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        ChurchGroupRepository groupRepository = mock(ChurchGroupRepository.class);

        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        PastoralFollowUp followUp = new PastoralFollowUp();
        followUp.setId(50L);
        followUp.setChurchId(1L);
        followUp.setStatus(PastoralFollowUpStatus.OPEN);
        when(followUpRepository.findByIdAndChurchId(50L, 1L)).thenReturn(Optional.of(followUp));

        PastoralFollowUpWebController controller = new PastoralFollowUpWebController(
                churchContextService, followUpRepository, personRepository, groupRepository);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.updateStage(50L, "resolved", "/follow-ups", redirect);

        assertEquals("redirect:/follow-ups", view);
        assertEquals(PastoralFollowUpStatus.DONE, followUp.getStatus());
        verify(followUpRepository).save(followUp);
    }
}
