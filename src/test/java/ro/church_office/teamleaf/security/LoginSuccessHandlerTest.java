package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginSuccessHandlerTest {

    @Test
    void updatesLastLoginWhenUserExists() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        LoginSuccessHandler handler = new LoginSuccessHandler(userRepository);

        User user = new User();
        user.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "Admin", "n/a", List.of(() -> "ROLE_ADMIN"));

        handler.onAuthenticationSuccess(request, response, auth);

        verify(userRepository).save(user);
        assertTrue(user.getLastLogin() != null);
    }

    @Test
    void ignoresBlankUsername() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        LoginSuccessHandler handler = new LoginSuccessHandler(userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "   ", "n/a", List.of(() -> "ROLE_ADMIN"));

        handler.onAuthenticationSuccess(request, response, auth);

        verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
    }
}
