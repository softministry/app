package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAuthenticationServiceTest {

    @Test
    void loadUserByUsernameNormalizesInputAndBuildsPrincipal() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAuthenticationService service = new UserAuthenticationService(userRepository);

        User user = new User();
        user.setId(7L);
        user.setUsername("john");
        user.setPassword("enc");
        user.setRole("pastor");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername("  John ");

        assertEquals(7L, principal.getId());
        assertEquals("john", principal.getUsername());
        assertEquals("ROLE_PASTOR", principal.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAuthenticationService service = new UserAuthenticationService(userRepository);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }

    @Test
    void updatePasswordPersistsAndDefaultsRoleToViewerWhenBlank() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAuthenticationService service = new UserAuthenticationService(userRepository);

        User user = new User();
        user.setId(8L);
        user.setUsername("anna");
        user.setPassword("old");
        user.setRole("   ");

        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserPrincipal principal = (UserPrincipal) service.updatePassword(new UserPrincipal(8L, "anna", "old", java.util.List.of()), "new-pass");

        assertEquals("new-pass", user.getPassword());
        assertEquals("ROLE_VIEWER", principal.getAuthorities().iterator().next().getAuthority());
    }
}
