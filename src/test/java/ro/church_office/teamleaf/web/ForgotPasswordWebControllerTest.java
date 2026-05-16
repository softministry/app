package ro.church_office.teamleaf.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;
import ro.church_office.teamleaf.security.PasswordPolicyService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForgotPasswordWebControllerTest {

    @Test
    void formPopulatesQuestionsAndFormModel() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        ForgotPasswordWebController controller = new ForgotPasswordWebController(userRepository, passwordEncoder, passwordPolicyService);

        Model model = new ExtendedModelMap();
        String view = controller.form(model);

        assertEquals("auth/forgot-password", view);
        assertEquals(ForgotPasswordWebController.QUESTION_ONE, model.getAttribute("questionOne"));
        assertEquals(ForgotPasswordWebController.QUESTION_TWO, model.getAttribute("questionTwo"));
        assertTrue(model.getAttribute("resetForm") instanceof ForgotPasswordWebController.ResetByQuestionsForm);
    }

    @Test
    void resetRejectsMissingFields() {
        ForgotPasswordWebController controller = new ForgotPasswordWebController(mock(UserRepository.class), mock(PasswordEncoder.class), mock(PasswordPolicyService.class));
        ForgotPasswordWebController.ResetByQuestionsForm form = new ForgotPasswordWebController.ResetByQuestionsForm();
        form.setUsername("user1");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        Model model = new ExtendedModelMap();
        String view = controller.reset(form, request, model);

        assertEquals("auth/forgot-password", view);
        assertEquals("Completează toate câmpurile.", model.getAttribute("error"));
    }

    @Test
    void resetRejectsPasswordMismatch() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        when(passwordPolicyService.isAccepted("secret1")).thenReturn(true);

        ForgotPasswordWebController controller = new ForgotPasswordWebController(userRepository, passwordEncoder, passwordPolicyService);
        ForgotPasswordWebController.ResetByQuestionsForm form = validForm("user2");
        form.setConfirmPassword("different");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        Model model = new ExtendedModelMap();
        String view = controller.reset(form, request, model);

        assertEquals("auth/forgot-password", view);
        assertEquals("Parolele nu coincid.", model.getAttribute("error"));
    }

    @Test
    void resetRejectsByPasswordPolicy() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        when(passwordPolicyService.isAccepted("secret1")).thenReturn(false);
        when(passwordPolicyService.validationMessage()).thenReturn("policy fail");

        ForgotPasswordWebController controller = new ForgotPasswordWebController(userRepository, passwordEncoder, passwordPolicyService);
        ForgotPasswordWebController.ResetByQuestionsForm form = validForm("user3");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");

        Model model = new ExtendedModelMap();
        String view = controller.reset(form, request, model);

        assertEquals("auth/forgot-password", view);
        assertEquals("policy fail", model.getAttribute("error"));
    }

    @Test
    void resetSucceedsWhenAnswersMatch() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);

        when(passwordPolicyService.isAccepted("secret1")).thenReturn(true);

        User user = new User();
        user.setUsername("good.user");
        user.setSecurityAnswerOneHash("h1");
        user.setSecurityAnswerTwoHash("h2");

        when(userRepository.findByUsername("good.user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ans1", "h1")).thenReturn(true);
        when(passwordEncoder.matches("ans2", "h2")).thenReturn(true);
        when(passwordEncoder.encode("secret1")).thenReturn("enc-secret");

        ForgotPasswordWebController controller = new ForgotPasswordWebController(userRepository, passwordEncoder, passwordPolicyService);
        ForgotPasswordWebController.ResetByQuestionsForm form = validForm("good.user");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.4");

        Model model = new ExtendedModelMap();
        String view = controller.reset(form, request, model);

        assertEquals("redirect:/login?reset", view);
        assertEquals("enc-secret", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void resetIsRateLimitedAfterRepeatedFailures() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);

        when(passwordPolicyService.isAccepted("secret1")).thenReturn(true);
        when(userRepository.findByUsername("rate.user")).thenReturn(Optional.empty());

        ForgotPasswordWebController controller = new ForgotPasswordWebController(userRepository, passwordEncoder, passwordPolicyService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        for (int i = 0; i < 5; i++) {
            Model model = new ExtendedModelMap();
            String view = controller.reset(validForm("rate.user"), request, model);
            assertEquals("auth/forgot-password", view);
        }

        Model blockedModel = new ExtendedModelMap();
        String blockedView = controller.reset(validForm("rate.user"), request, blockedModel);

        assertEquals("auth/forgot-password", blockedView);
        assertEquals("Prea multe încercări. Reîncearcă în câteva minute.", blockedModel.getAttribute("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetRejectsWhenSecurityAnswersAreMissingOnUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        when(passwordPolicyService.isAccepted("secret1")).thenReturn(true);

        User user = new User();
        user.setUsername("no.answers");
        user.setSecurityAnswerOneHash("");
        user.setSecurityAnswerTwoHash("");
        when(userRepository.findByUsername("no.answers")).thenReturn(Optional.of(user));

        ForgotPasswordWebController controller = new ForgotPasswordWebController(userRepository, passwordEncoder, passwordPolicyService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.6");

        Model model = new ExtendedModelMap();
        String view = controller.reset(validForm("no.answers"), request, model);

        assertEquals("auth/forgot-password", view);
        assertEquals("Date invalide pentru resetare.", model.getAttribute("error"));
    }

    @Test
    void resetRejectsWhenSecurityAnswerDoesNotMatch() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        when(passwordPolicyService.isAccepted("secret1")).thenReturn(true);

        User user = new User();
        user.setUsername("bad.answers");
        user.setSecurityAnswerOneHash("h1");
        user.setSecurityAnswerTwoHash("h2");
        when(userRepository.findByUsername("bad.answers")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ans1", "h1")).thenReturn(false);

        ForgotPasswordWebController controller = new ForgotPasswordWebController(userRepository, passwordEncoder, passwordPolicyService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.7");

        Model model = new ExtendedModelMap();
        String view = controller.reset(validForm("bad.answers"), request, model);

        assertEquals("auth/forgot-password", view);
        assertEquals("Date invalide pentru resetare.", model.getAttribute("error"));
    }

    private ForgotPasswordWebController.ResetByQuestionsForm validForm(String username) {
        ForgotPasswordWebController.ResetByQuestionsForm form = new ForgotPasswordWebController.ResetByQuestionsForm();
        form.setUsername(username);
        form.setAnswerOne("ans1");
        form.setAnswerTwo("ans2");
        form.setNewPassword("secret1");
        form.setConfirmPassword("secret1");
        return form;
    }
}
