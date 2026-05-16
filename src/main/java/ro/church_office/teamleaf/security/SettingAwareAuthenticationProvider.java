package ro.church_office.teamleaf.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class SettingAwareAuthenticationProvider implements AuthenticationProvider {

    private final UserAuthenticationService userAuthenticationService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordlessLoginService passwordlessLoginService;

    public SettingAwareAuthenticationProvider(UserAuthenticationService userAuthenticationService,
                                              PasswordEncoder passwordEncoder,
                                              PasswordlessLoginService passwordlessLoginService) {
        this.userAuthenticationService = userAuthenticationService;
        this.passwordEncoder = passwordEncoder;
        this.passwordlessLoginService = passwordlessLoginService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();

        UserDetails user = userAuthenticationService.loadUserByUsername(username);
        if (passwordlessLoginService.requirePasswordForLogin() && !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        return UsernamePasswordAuthenticationToken.authenticated(user, authentication.getCredentials(), user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
