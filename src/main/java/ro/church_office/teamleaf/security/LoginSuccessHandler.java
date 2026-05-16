package ro.church_office.teamleaf.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    public LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        String username = authentication == null ? null : authentication.getName();
        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username.trim().toLowerCase())
                    .ifPresent(user -> {
                        user.setLastLogin(LocalDateTime.now());
                        userRepository.save(user);
                    });
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String redirectUrl = savedRequest.getRedirectUrl();
            if (redirectUrl != null && redirectUrl.contains("/.well-known/appspecific/com.chrome.devtools.json")) {
                requestCache.removeRequest(request, response);
                getRedirectStrategy().sendRedirect(request, response, "/dashboard");
                clearAuthenticationAttributes(request);
                return;
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
