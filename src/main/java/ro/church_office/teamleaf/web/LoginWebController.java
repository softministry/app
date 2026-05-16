package ro.church_office.teamleaf.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.church_office.teamleaf.security.PasswordlessLoginService;

@Controller
public class LoginWebController {

    private final PasswordlessLoginService passwordlessLoginService;

    public LoginWebController(PasswordlessLoginService passwordlessLoginService) {
        this.passwordlessLoginService = passwordlessLoginService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "reset", required = false) String reset,
                        Model model) {
        model.addAttribute("logoutSuccess", logout != null);
        model.addAttribute("loginError", error != null);
        model.addAttribute("passwordResetSuccess", reset != null);
        boolean requirePasswordForLogin = passwordlessLoginService.requirePasswordForLogin();
        if (!requirePasswordForLogin && logout == null && error == null && reset == null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("requirePasswordForLogin", requirePasswordForLogin);
        model.addAttribute("defaultLoginUsername", PasswordlessLoginService.DEFAULT_ADMIN_USERNAME);
        model.addAttribute("passwordlessLoginEnabled", !requirePasswordForLogin);
        return "auth/login";
    }
}
