package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAdminBootstrapTest {

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.count()).thenReturn(1L);

        DefaultAdminBootstrap bootstrap = new DefaultAdminBootstrap(userRepository, passwordEncoder, "Secret123!");
        bootstrap.run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createsAdminWithConfiguredInitialPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "enc:" + inv.getArgument(0));

        DefaultAdminBootstrap bootstrap = new DefaultAdminBootstrap(userRepository, passwordEncoder, " Secret123! ");
        bootstrap.run(null);

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User admin = captor.getValue();
        assertEquals("admin", admin.getUsername());
        assertEquals("ADMIN", admin.getRole());
        assertEquals("enc:Secret123!", admin.getPassword());
        assertNotEquals("enc:admin", admin.getSecurityAnswerOneHash());
        assertNotEquals("enc:admin", admin.getSecurityAnswerTwoHash());
    }

    @Test
    void generatedTemporaryPasswordIsNotTheOldDefault() {
        String generated = DefaultAdminBootstrap.generateTemporaryPassword();

        assertFalse(generated.isBlank());
        assertNotEquals("admin", generated);
    }
}
