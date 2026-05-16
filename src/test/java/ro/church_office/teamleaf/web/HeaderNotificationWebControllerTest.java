package ro.church_office.teamleaf.web;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.teamleaf.security.CurrentUserService;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeaderNotificationWebControllerTest {

    @Test
    void actionStoresDoneMarkerAndRedirectsSafely() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        when(churchInfoService.getDefaultChurchId()).thenReturn(5L);

        HeaderNotificationWebController controller = new HeaderNotificationWebController(
                churchInfoService,
                mock(GlobalSettingRepository.class),
                mock(CurrentUserService.class));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebUiModelAdvice.BIRTHDAY_NOTIFICATION_SESSION_KEY)).thenReturn(Map.of());

        String view = controller.action(10L, "done", "", session);

        assertEquals("redirect:/dashboard", view);
    }

    @Test
    void actionStoresSnoozeMarkerForUnknownMode() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        when(churchInfoService.getDefaultChurchId()).thenReturn(5L);

        HeaderNotificationWebController controller = new HeaderNotificationWebController(
                churchInfoService,
                mock(GlobalSettingRepository.class),
                mock(CurrentUserService.class));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebUiModelAdvice.BIRTHDAY_NOTIFICATION_SESSION_KEY)).thenReturn(Map.of("5:10", LocalDate.now()));

        String view = controller.action(10L, "later", "/dashboard", session);

        assertEquals("redirect:/dashboard", view);
        assertTrue(view.startsWith("redirect:/"));
    }
}
