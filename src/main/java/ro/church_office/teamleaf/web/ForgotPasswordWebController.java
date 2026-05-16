package ro.church_office.teamleaf.web;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpServletRequest;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;
import ro.church_office.teamleaf.security.PasswordPolicyService;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ForgotPasswordWebController {

    public static final String QUESTION_ONE = "Care este numele de fată al mamei?";
    public static final String QUESTION_TWO = "În ce oraș te-ai născut?";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final String GENERIC_RESET_ERROR = "Date invalide pentru resetare.";
    private static final ConcurrentHashMap<String, ResetAttemptWindow> RESET_ATTEMPTS = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    public ForgotPasswordWebController(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       PasswordPolicyService passwordPolicyService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
    }

    @GetMapping("/forgot-password")
    public String form(Model model) {
        model.addAttribute("questionOne", QUESTION_ONE);
        model.addAttribute("questionTwo", QUESTION_TWO);
        model.addAttribute("resetForm", new ResetByQuestionsForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String reset(@ModelAttribute("resetForm") ResetByQuestionsForm form,
                        HttpServletRequest request,
                        Model model) {
        model.addAttribute("questionOne", QUESTION_ONE);
        model.addAttribute("questionTwo", QUESTION_TWO);

        String username = normalizeUsername(form.getUsername());
        String answerOne = normalizeAnswer(form.getAnswerOne());
        String answerTwo = normalizeAnswer(form.getAnswerTwo());
        String newPassword = form.getNewPassword() == null ? "" : form.getNewPassword().trim();
        String confirmPassword = form.getConfirmPassword() == null ? "" : form.getConfirmPassword().trim();
        String attemptKey = resetAttemptKey(username, request);

        if (isRateLimited(attemptKey)) {
            model.addAttribute("error", "Prea multe încercări. Reîncearcă în câteva minute.");
            return "auth/forgot-password";
        }

        if (username.isBlank() || answerOne.isBlank() || answerTwo.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            model.addAttribute("error", "Completează toate câmpurile.");
            return "auth/forgot-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Parolele nu coincid.");
            return "auth/forgot-password";
        }

        if (!passwordPolicyService.isAccepted(newPassword)) {
            model.addAttribute("error", passwordPolicyService.validationMessage());
            return "auth/forgot-password";
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            registerFailedAttempt(attemptKey);
            model.addAttribute("error", GENERIC_RESET_ERROR);
            return "auth/forgot-password";
        }

        User user = userOpt.get();
        if (user.getSecurityAnswerOneHash() == null || user.getSecurityAnswerOneHash().isBlank()
                || user.getSecurityAnswerTwoHash() == null || user.getSecurityAnswerTwoHash().isBlank()) {
            registerFailedAttempt(attemptKey);
            model.addAttribute("error", GENERIC_RESET_ERROR);
            return "auth/forgot-password";
        }

        boolean firstOk = passwordEncoder.matches(answerOne, user.getSecurityAnswerOneHash());
        boolean secondOk = passwordEncoder.matches(answerTwo, user.getSecurityAnswerTwoHash());
        if (!firstOk || !secondOk) {
            registerFailedAttempt(attemptKey);
            model.addAttribute("error", GENERIC_RESET_ERROR);
            return "auth/forgot-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        RESET_ATTEMPTS.remove(attemptKey);

        return "redirect:/login?reset";
    }

    private String resetAttemptKey(String username, HttpServletRequest request) {
        String ip = request == null ? "" : normalizeUsername(request.getRemoteAddr());
        String userPart = username == null || username.isBlank() ? "-" : username;
        return userPart + "|" + ip;
    }

    private boolean isRateLimited(String key) {
        ResetAttemptWindow window = RESET_ATTEMPTS.get(key);
        if (window == null) {
            return false;
        }
        Instant now = Instant.now();
        if (now.isAfter(window.startedAt().plus(ATTEMPT_WINDOW))) {
            RESET_ATTEMPTS.remove(key);
            return false;
        }
        return window.failedAttempts() >= MAX_FAILED_ATTEMPTS;
    }

    private void registerFailedAttempt(String key) {
        Instant now = Instant.now();
        RESET_ATTEMPTS.compute(key, (ignored, existing) -> {
            if (existing == null || now.isAfter(existing.startedAt().plus(ATTEMPT_WINDOW))) {
                return new ResetAttemptWindow(now, 1);
            }
            return new ResetAttemptWindow(existing.startedAt(), existing.failedAttempts() + 1);
        });
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    public static class ResetByQuestionsForm {
        private String username;
        private String answerOne;
        private String answerTwo;
        private String newPassword;
        private String confirmPassword;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getAnswerOne() { return answerOne; }
        public void setAnswerOne(String answerOne) { this.answerOne = answerOne; }

        public String getAnswerTwo() { return answerTwo; }
        public void setAnswerTwo(String answerTwo) { this.answerTwo = answerTwo; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    private record ResetAttemptWindow(Instant startedAt, int failedAttempts) {}
}
