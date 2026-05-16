package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void passwordEncoderBeanIsHybridEncoder() {
        SecurityConfig config = new SecurityConfig();

        PasswordEncoder encoder = config.passwordEncoder();

        assertInstanceOf(HybridPasswordEncoder.class, encoder);
    }

    @Test
    void authenticationManagerBeanIsProviderManager() {
        SecurityConfig config = new SecurityConfig();
        SettingAwareAuthenticationProvider provider = mock(SettingAwareAuthenticationProvider.class);

        AuthenticationManager manager = config.authenticationManager(provider);

        assertNotNull(manager);
        assertInstanceOf(ProviderManager.class, manager);
    }
}
