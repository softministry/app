package ro.church_office.teamleaf.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.util.List;
import java.util.Locale;

@Service
public class UserAuthenticationService {

    private final UserRepository userRepository;

    public UserAuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    public UserDetails updatePassword(UserDetails user, String newPassword) {
        User current = userRepository.findByUsername(normalizeUsername(user.getUsername()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        current.setPassword(newPassword);
        User saved = userRepository.save(current);
        return toPrincipal(saved);
    }

    private UserPrincipal toPrincipal(User user) {
        String role = normalizeRole(user.getRole());
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(), List.of(authority));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "VIEWER";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
