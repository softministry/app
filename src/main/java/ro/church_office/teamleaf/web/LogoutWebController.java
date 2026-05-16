package ro.church_office.teamleaf.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogoutWebController {

    @GetMapping("/logged-out")
    public String loggedOut() {
        return "auth/logged-out";
    }
}
