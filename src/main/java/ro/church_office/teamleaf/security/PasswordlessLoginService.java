package ro.church_office.teamleaf.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordlessLoginService {

    public static final String USER_CREATE_REQUIRE_PASSWORD_KEY = "user_create_require_password";
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    private final GlobalSettingRepository globalSettingRepository;
    private final UserAuthenticationService userAuthenticationService;

    public PasswordlessLoginService(GlobalSettingRepository globalSettingRepository,
                                    UserAuthenticationService userAuthenticationService) {
        this.globalSettingRepository = globalSettingRepository;
        this.userAuthenticationService = userAuthenticationService;
    }

    public boolean requirePasswordForLogin() {
        return globalSettingRepository.findByKey(USER_CREATE_REQUIRE_PASSWORD_KEY)
                .map(GlobalSetting::getStringValue)
                .map(value -> {
                    String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
                    return !("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized));
                })
                .orElse(false);
    }

    public Optional<Authentication> defaultAdminAuthentication() {
        if (requirePasswordForLogin()) {
            return Optional.empty();
        }
        try {
            UserDetails user = userAuthenticationService.loadUserByUsername(DEFAULT_ADMIN_USERNAME);
            return Optional.of(UsernamePasswordAuthenticationToken.authenticated(
                    user,
                    "",
                    user.getAuthorities()));
        } catch (UsernameNotFoundException ex) {
            return Optional.empty();
        }
    }
}
