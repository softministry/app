package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserServiceTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserReturnsEmptyWhenUnauthenticated() {
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService service = new CurrentUserService(userRepository);

        assertTrue(service.currentUser().isEmpty());
    }

    @Test
    void currentUserResolvesNormalizedUsernameFromSecurityContext() {
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService service = new CurrentUserService(userRepository);

        User user = new User();
        user.setUsername("john");
        user.setRole("pastor");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "John", "n/a", List.of(() -> "ROLE_PASTOR"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("john", service.currentUsernameOr("fallback"));
        assertEquals("PASTOR", service.currentRoleOr("VIEWER"));
        assertTrue(service.hasRole("pastor"));
        assertFalse(service.hasRole("admin"));
    }
}
