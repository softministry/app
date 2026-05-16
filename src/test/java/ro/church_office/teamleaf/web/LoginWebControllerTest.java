package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import ro.church_office.teamleaf.security.PasswordlessLoginService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginWebControllerTest {

    private final PasswordlessLoginService passwordlessLoginService = mock(PasswordlessLoginService.class);
    private final LoginWebController controller = new LoginWebController(passwordlessLoginService);

    @Test
    void setsFlagsWhenParamsPresent() {
        when(passwordlessLoginService.requirePasswordForLogin()).thenReturn(true);
        Model model = new ExtendedModelMap();

        String view = controller.login("1", "1", "1", model);

        assertEquals("auth/login", view);
        assertEquals(true, model.getAttribute("logoutSuccess"));
        assertEquals(true, model.getAttribute("loginError"));
        assertEquals(true, model.getAttribute("passwordResetSuccess"));
    }

    @Test
    void setsFlagsFalseWhenParamsMissing() {
        when(passwordlessLoginService.requirePasswordForLogin()).thenReturn(true);
        Model model = new ExtendedModelMap();

        controller.login(null, null, null, model);

        assertEquals(false, model.getAttribute("logoutSuccess"));
        assertEquals(false, model.getAttribute("loginError"));
        assertEquals(false, model.getAttribute("passwordResetSuccess"));
    }

    @Test
    void redirectsToDashboardWhenPasswordlessLoginIsEnabled() {
        when(passwordlessLoginService.requirePasswordForLogin()).thenReturn(false);
        Model model = new ExtendedModelMap();

        String view = controller.login(null, null, null, model);

        assertEquals("redirect:/dashboard", view);
    }
}
