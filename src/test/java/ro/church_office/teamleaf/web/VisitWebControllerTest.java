package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.info.visits.VisitDTO;
import ro.church_office.info.visits.VisitService;
import ro.church_office.info.visits.dao.Visit;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisitWebControllerTest {

    private final ChurchContextService churchContextService = mock(ChurchContextService.class);
    private final VisitService visitService = mock(VisitService.class);
    private final GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);

    @Test
    void editFormDefaultsMissingVisitDateToToday() {
        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        Visit visit = new Visit();
        visit.setId(7L);
        visit.setPersonName("Ana Pop");
        when(visitService.getVisitById(7L, 1L)).thenReturn(visit);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller().editForm(7L, model, new RedirectAttributesModelMap());

        VisitDTO dto = (VisitDTO) model.get("visitDto");
        assertEquals("visits/form", view);
        assertEquals(LocalDate.now(), dto.getVisitDate());
    }

    @Test
    void editFormPreservesExistingVisitDate() {
        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        Visit visit = new Visit();
        visit.setId(7L);
        visit.setVisitDate(LocalDate.of(2026, 1, 15));
        when(visitService.getVisitById(7L, 1L)).thenReturn(visit);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller().editForm(7L, model, new RedirectAttributesModelMap());

        VisitDTO dto = (VisitDTO) model.get("visitDto");
        assertEquals("visits/form", view);
        assertEquals(LocalDate.of(2026, 1, 15), dto.getVisitDate());
    }

    private VisitWebController controller() {
        return new VisitWebController(churchContextService, visitService, globalSettingRepository);
    }
}
