package ro.church_office.teamleaf.web;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;
import ro.church_office.teamleaf.security.CurrentUserService;
import ro.church_office.teamleaf.security.PasswordPolicyService;

import java.util.Locale;

@Controller
@RequestMapping("/account")
public class AccountWebController {

    private static final String QUESTION_ONE = ForgotPasswordWebController.QUESTION_ONE;
    private static final String QUESTION_TWO = ForgotPasswordWebController.QUESTION_TWO;

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    public AccountWebController(CurrentUserService currentUserService,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordPolicyService passwordPolicyService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
        User user = currentUserService.currentUser().orElse(null);
        model.addAttribute("passwordForm", new ChangePasswordForm());
        model.addAttribute("securityForm", new SecurityQuestionsForm());
        model.addAttribute("questionOne", QUESTION_ONE);
        model.addAttribute("questionTwo", QUESTION_TWO);
        model.addAttribute("requireCurrentPassword", requiresCurrentPassword(user));
        return "account/password";
    }

    @PostMapping("/password")
    public String changePassword(@ModelAttribute("passwordForm") ChangePasswordForm form,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUserService.currentUser().orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Sesiunea a expirat. Autentifică-te din nou.");
            return "redirect:/login";
        }

        String currentPassword = form.getCurrentPassword() == null ? "" : form.getCurrentPassword().trim();
        String newPassword = form.getNewPassword() == null ? "" : form.getNewPassword().trim();
        String confirmPassword = form.getConfirmPassword() == null ? "" : form.getConfirmPassword().trim();

        boolean requireCurrentPassword = requiresCurrentPassword(user);

        if ((requireCurrentPassword && currentPassword.isBlank()) || newPassword.isBlank() || confirmPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Completează toate câmpurile.");
            return "redirect:/account/password";
        }

        if (requireCurrentPassword && !passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Parola curentă nu este corectă.");
            return "redirect:/account/password";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Parolele noi nu coincid.");
            return "redirect:/account/password";
        }

        if (!passwordPolicyService.isAccepted(newPassword)) {
            redirectAttributes.addFlashAttribute("error", passwordPolicyService.validationMessage());
            return "redirect:/account/password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Parola a fost schimbată cu succes.");
        return "redirect:/account/password";
    }

    @PostMapping("/security-questions")
    public String updateSecurityQuestions(@ModelAttribute("securityForm") SecurityQuestionsForm form,
                                          RedirectAttributes redirectAttributes) {
        User user = currentUserService.currentUser().orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Sesiunea a expirat. Autentifică-te din nou.");
            return "redirect:/login";
        }

        String currentPassword = form.getCurrentPassword() == null ? "" : form.getCurrentPassword().trim();
        String answerOne = normalizeAnswer(form.getAnswerOne());
        String answerTwo = normalizeAnswer(form.getAnswerTwo());

        if (currentPassword.isBlank() || answerOne.isBlank() || answerTwo.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Completează parola curentă și ambele răspunsuri.");
            return "redirect:/account/password";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Parola curentă nu este corectă.");
            return "redirect:/account/password";
        }

        user.setSecurityAnswerOneHash(passwordEncoder.encode(answerOne));
        user.setSecurityAnswerTwoHash(passwordEncoder.encode(answerTwo));
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Întrebările de securitate au fost actualizate.");
        return "redirect:/account/password";
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    private boolean requiresCurrentPassword(User user) {
        if (user == null) {
            return true;
        }
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        return !passwordEncoder.matches("admin", storedPassword);
    }

    public static class ChangePasswordForm {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    public static class SecurityQuestionsForm {
        private String currentPassword;
        private String answerOne;
        private String answerTwo;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

        public String getAnswerOne() { return answerOne; }
        public void setAnswerOne(String answerOne) { this.answerOne = answerOne; }

        public String getAnswerTwo() { return answerTwo; }
        public void setAnswerTwo(String answerTwo) { this.answerTwo = answerTwo; }
    }
}
