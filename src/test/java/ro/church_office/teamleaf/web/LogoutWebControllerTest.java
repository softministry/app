package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogoutWebControllerTest {

    @Test
    void returnsLoggedOutView() {
        LogoutWebController controller = new LogoutWebController();

        assertEquals("auth/logged-out", controller.loggedOut());
    }
}
