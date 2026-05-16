package ro.church_office.teamleaf.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DefaultAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DefaultAdminBootstrap.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredInitialPassword;

    public DefaultAdminBootstrap(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${ministryadmin.initial-admin-password:}") String configuredInitialPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredInitialPassword = configuredInitialPassword == null ? "" : configuredInitialPassword.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        String initialPassword = configuredInitialPassword.isBlank()
                ? generateTemporaryPassword()
                : configuredInitialPassword;

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setRole("ADMIN");
        admin.setSecurityAnswerOneHash(passwordEncoder.encode(generateTemporaryPassword()));
        admin.setSecurityAnswerTwoHash(passwordEncoder.encode(generateTemporaryPassword()));
        userRepository.save(admin);

        if (configuredInitialPassword.isBlank()) {
            log.warn("No users found. Created default admin user 'admin' with temporary password: {}", initialPassword);
            log.warn("Change the initial admin password immediately after first login.");
        } else {
            log.warn("No users found. Created default admin user 'admin' using configured initial password.");
        }
    }

    static String generateTemporaryPassword() {
        byte[] bytes = new byte[18];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

