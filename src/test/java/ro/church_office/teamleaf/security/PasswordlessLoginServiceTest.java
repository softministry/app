package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordlessLoginServiceTest {

    @Test
    void defaultsToPasswordlessLoginWhenSettingIsMissing() {
        GlobalSettingRepository settings = mock(GlobalSettingRepository.class);
        UserAuthenticationService users = new UserAuthenticationService(mock(UserRepository.class));
        when(settings.findByKey(PasswordlessLoginService.USER_CREATE_REQUIRE_PASSWORD_KEY)).thenReturn(Optional.empty());

        PasswordlessLoginService service = new PasswordlessLoginService(settings, users);

        assertFalse(service.requirePasswordForLogin());
    }

    @Test
    void buildsDefaultAdminAuthenticationWhenPasswordlessLoginIsEnabled() {
        GlobalSettingRepository settings = mock(GlobalSettingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserAuthenticationService users = new UserAuthenticationService(userRepository);

        GlobalSetting setting = new GlobalSetting(PasswordlessLoginService.USER_CREATE_REQUIRE_PASSWORD_KEY, "false");
        when(settings.findByKey(PasswordlessLoginService.USER_CREATE_REQUIRE_PASSWORD_KEY)).thenReturn(Optional.of(setting));

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("enc");
        admin.setRole("ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        PasswordlessLoginService service = new PasswordlessLoginService(settings, users);
        Optional<Authentication> authentication = service.defaultAdminAuthentication();

        assertTrue(authentication.isPresent());
        assertEquals("admin", authentication.get().getName());
        assertTrue(authentication.get().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void doesNotBuildDefaultAuthenticationWhenPasswordIsRequired() {
        GlobalSettingRepository settings = mock(GlobalSettingRepository.class);
        UserAuthenticationService users = new UserAuthenticationService(mock(UserRepository.class));

        GlobalSetting setting = new GlobalSetting(PasswordlessLoginService.USER_CREATE_REQUIRE_PASSWORD_KEY, "true");
        when(settings.findByKey(PasswordlessLoginService.USER_CREATE_REQUIRE_PASSWORD_KEY)).thenReturn(Optional.of(setting));

        PasswordlessLoginService service = new PasswordlessLoginService(settings, users);

        assertTrue(service.requirePasswordForLogin());
        assertTrue(service.defaultAdminAuthentication().isEmpty());
    }
}
