package ro.church_office.teamleaf.web;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;
import ro.church_office.teamleaf.security.PasswordPolicyService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class UserAdminWebController {

    private static final List<String> ROLE_OPTIONS = List.of("ADMIN", "PASTOR", "ELDER", "GROUP_LEADER", "SECRETARY", "VIEWER");
    private static final String SECURITY_QUESTION_ONE = ForgotPasswordWebController.QUESTION_ONE;
    private static final String SECURITY_QUESTION_TWO = ForgotPasswordWebController.QUESTION_TWO;
    private static final String USER_CREATE_REQUIRE_PASSWORD_KEY = "user_create_require_password";

    private final UserRepository userRepository;
    private final GlobalSettingRepository globalSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    public UserAdminWebController(UserRepository userRepository,
                                  GlobalSettingRepository globalSettingRepository,
                                  PasswordEncoder passwordEncoder,
                                  PasswordPolicyService passwordPolicyService) {
        this.userRepository = userRepository;
        this.globalSettingRepository = globalSettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
    }

    @GetMapping
    public String list(@RequestParam(value = "edit", required = false) Long editId,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        List<User> users = userRepository.findAll(Sort.by(Sort.Order.asc("username")));
        UserForm createForm = new UserForm();
        createForm.setRole("VIEWER");
        boolean requirePasswordAtCreate = requirePasswordAtCreate();
        UserForm editForm = null;
        Long editingId = null;

        if (editId != null) {
            Optional<User> editUser = userRepository.findById(editId);
            if (editUser.isPresent()) {
                User user = editUser.get();
                editingId = user.getId();
                editForm = new UserForm();
                editForm.setUsername(user.getUsername());
                editForm.setRole(normalizeRole(user.getRole()));
            }
        }

        model.addAttribute("users", users);
        model.addAttribute("roleOptions", ROLE_OPTIONS);
        model.addAttribute("securityQuestionOne", SECURITY_QUESTION_ONE);
        model.addAttribute("securityQuestionTwo", SECURITY_QUESTION_TWO);
        model.addAttribute("createForm", createForm);
        model.addAttribute("editForm", editForm);
        model.addAttribute("editingId", editingId);
        model.addAttribute("requirePasswordAtCreate", requirePasswordAtCreate);
        return "users-admin/index";
    }

    @PostMapping
    public String create(@ModelAttribute("createForm") UserForm form,
                         RedirectAttributes redirectAttributes) {
        String username = normalizeUsername(form.getUsername());
        String password = form.getPassword() == null ? "" : form.getPassword().trim();
        String answerOne = normalizeAnswer(form.getAnswerOne());
        String answerTwo = normalizeAnswer(form.getAnswerTwo());
        String role = sanitizeRole(form.getRole());

        if (username.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Username-ul este obligatoriu.");
            return "redirect:/admin/users";
        }
        if (requirePasswordAtCreate() && password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Parola este obligatorie la creare.");
            return "redirect:/admin/users";
        }
        if (!password.isBlank() && !passwordPolicyService.isAccepted(password)) {
            redirectAttributes.addFlashAttribute("error", passwordPolicyService.validationMessage());
            return "redirect:/admin/users";
        }
        if (answerOne.isBlank() || answerTwo.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Completează cele 2 răspunsuri de securitate.");
            return "redirect:/admin/users";
        }
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Există deja un utilizator cu acest username.");
            return "redirect:/admin/users";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setSecurityAnswerOneHash(passwordEncoder.encode(answerOne));
        user.setSecurityAnswerTwoHash(passwordEncoder.encode(answerTwo));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Utilizatorul a fost creat.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("editForm") UserForm form,
                         RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Utilizatorul nu a fost găsit.");
            return "redirect:/admin/users";
        }

        User user = userOpt.get();
        String username = normalizeUsername(form.getUsername());
        String newPassword = form.getPassword() == null ? "" : form.getPassword().trim();
        String answerOne = normalizeAnswer(form.getAnswerOne());
        String answerTwo = normalizeAnswer(form.getAnswerTwo());
        String role = sanitizeRole(form.getRole());

        if (username.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Username-ul este obligatoriu.");
            return "redirect:/admin/users?edit=" + id;
        }

        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Există deja un utilizator cu acest username.");
            return "redirect:/admin/users?edit=" + id;
        }

        user.setUsername(username);
        user.setRole(role);
        if (!newPassword.isBlank()) {
            if (!passwordPolicyService.isAccepted(newPassword)) {
                redirectAttributes.addFlashAttribute("error", passwordPolicyService.validationMessage());
                return "redirect:/admin/users?edit=" + id;
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        if (!answerOne.isBlank()) {
            user.setSecurityAnswerOneHash(passwordEncoder.encode(answerOne));
        }
        if (!answerTwo.isBlank()) {
            user.setSecurityAnswerTwoHash(passwordEncoder.encode(answerTwo));
        }
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Utilizatorul a fost actualizat.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Utilizatorul nu a fost găsit.");
            return "redirect:/admin/users";
        }

        if (userRepository.count() <= 1) {
            redirectAttributes.addFlashAttribute("error", "Nu poți șterge ultimul utilizator.");
            return "redirect:/admin/users";
        }

        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Utilizatorul a fost șters.");
        return "redirect:/admin/users";
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    private String sanitizeRole(String role) {
        String normalized = normalizeRole(role);
        return ROLE_OPTIONS.contains(normalized) ? normalized : "VIEWER";
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    private boolean requirePasswordAtCreate() {
        return globalSettingRepository.findByKey(USER_CREATE_REQUIRE_PASSWORD_KEY)
                .map(GlobalSetting::getStringValue)
                .map(value -> {
                    String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
                    return !("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized));
                })
                .orElse(false);
    }

    public static class UserForm {
        private String username;
        private String password;
        private String role;
        private String answerOne;
        private String answerTwo;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getAnswerOne() { return answerOne; }
        public void setAnswerOne(String answerOne) { this.answerOne = answerOne; }

        public String getAnswerTwo() { return answerTwo; }
        public void setAnswerTwo(String answerTwo) { this.answerTwo = answerTwo; }
    }
}
