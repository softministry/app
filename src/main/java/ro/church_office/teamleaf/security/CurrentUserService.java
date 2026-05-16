package ro.church_office.teamleaf.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.util.Locale;
import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return Optional.empty();
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT));
    }

    public String currentUsernameOr(String fallback) {
        return currentUser()
                .map(User::getUsername)
                .filter(value -> value != null && !value.isBlank())
                .orElse(fallback);
    }

    public String currentRoleOr(String fallback) {
        return currentUser()
                .map(User::getRole)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .orElse(fallback);
    }

    public boolean hasRole(String role) {
        String expected = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String auth = authority.getAuthority().trim().toUpperCase(Locale.ROOT);
            if (("ROLE_" + expected).equals(auth) || expected.equals(auth)) {
                return true;
            }
        }
        return false;
    }
}
