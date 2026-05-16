package ro.church_office.teamleaf.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityFlowIT extends AbstractContainerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GlobalSettingRepository globalSettingRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureTestUser() {
        setPasswordRequired(true);
        userRepository.findByUsername("it-admin").orElseGet(() -> {
            User user = new User();
            user.setUsername("it-admin");
            user.setPassword(passwordEncoder.encode("Secret123!"));
            user.setRole("ADMIN");
            return userRepository.save(user);
        });
    }

    @Test
    void unauthenticatedRequestToProtectedRouteRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/settings/system"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void passwordlessModeAuthenticatesDefaultAdminAndOpensDashboardDirectly() throws Exception {
        setPasswordRequired(false);
        ensureUser("admin", "ADMIN");

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("headerCanManageUsers", true));

        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void unauthenticatedRequestToAdminRouteRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void staticCssAssetIsPublic() throws Exception {
        mockMvc.perform(get("/css/app.css"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerRoleCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void validFormLoginAuthenticatesUser() throws Exception {
        mockMvc.perform(formLogin("/login").user("it-admin").password("Secret123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("it-admin"));
    }

    @Test
    void loginPostWithoutCsrfAuthenticatesUser() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "it-admin")
                        .param("password", "Secret123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("it-admin"));
    }

    @Test
    void invalidFormLoginKeepsUserUnauthenticated() throws Exception {
        mockMvc.perform(formLogin("/login").user("it-admin").password("WrongPass!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void postToSettingsWithoutCsrfIsForbiddenForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/settings/system")
                        .with(user("it-admin").roles("ADMIN"))
                        .param("rowsPerPage", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postToSettingsWithCsrfSucceedsForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/settings/system")
                        .with(user("it-admin").roles("ADMIN"))
                .with(csrf())
                .param("rowsPerPage", "10")
                .param("eventTasksEnabled", "true")
                .param("privateMode", "false")
                .param("passwordRestrictionsEnabled", "false")
                .param("passwordMinSixEnabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/system"));
    }

    @Test
    void logoutWithCsrfRedirectsToLoggedOut() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("it-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logged-out"));
    }

    @Test
    void followUpsStagePostWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/follow-ups/stage")
                        .with(user("it-admin").roles("ADMIN"))
                        .param("id", "1")
                        .param("stage", "contacted"))
                .andExpect(status().isForbidden());
    }

    @Test
    void followUpsStagePostWithCsrfButMissingRequiredParamsIsBadRequest() throws Exception {
        mockMvc.perform(post("/follow-ups/stage")
                        .with(user("it-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    private void setPasswordRequired(boolean required) {
        GlobalSetting setting = globalSettingRepository.findByKey("user_create_require_password")
                .orElseGet(() -> new GlobalSetting("user_create_require_password", String.valueOf(required)));
        setting.setStringValue(String.valueOf(required));
        globalSettingRepository.save(setting);
    }

    private void ensureUser(String username, String role) {
        userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("Secret123!"));
            user.setRole(role);
            return userRepository.save(user);
        });
    }
}
