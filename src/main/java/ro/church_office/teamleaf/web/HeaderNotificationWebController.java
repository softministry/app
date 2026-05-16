package ro.church_office.teamleaf.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.teamleaf.security.CurrentUserService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HeaderNotificationWebController {

    private final ChurchInfoService churchInfoService;
    private final GlobalSettingRepository globalSettingRepository;
    private final CurrentUserService currentUserService;

    public HeaderNotificationWebController(ChurchInfoService churchInfoService,
                                           GlobalSettingRepository globalSettingRepository,
                                           CurrentUserService currentUserService) {
        this.churchInfoService = churchInfoService;
        this.globalSettingRepository = globalSettingRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/notifications/birthdays/action")
    public String action(@RequestParam("personId") Long personId,
                         @RequestParam("mode") String mode,
                         @RequestParam(value = "redirect", required = false) String redirect,
                         HttpSession session) {
        Long churchId = churchInfoService.getDefaultChurchId();
        String key = WebUiModelAdvice.notificationKey(churchId, personId);
        Map<String, LocalDate> actions = birthdayActions(session);
        LocalDate today = LocalDate.now();

        if ("done".equalsIgnoreCase(mode)) {
            actions.put(key, LocalDate.of(today.getYear(), 12, 31));
        } else {
            actions.put(key, today.plusDays(1));
        }

        session.setAttribute(WebUiModelAdvice.BIRTHDAY_NOTIFICATION_SESSION_KEY, actions);
        return "redirect:" + safeRedirect(redirect);
    }

    @SuppressWarnings("unchecked")
    private Map<String, LocalDate> birthdayActions(HttpSession session) {
        Object raw = session.getAttribute(WebUiModelAdvice.BIRTHDAY_NOTIFICATION_SESSION_KEY);
        if (raw instanceof Map<?, ?> rawMap) {
            Map<String, LocalDate> casted = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof LocalDate value) {
                    casted.put(key, value);
                }
            }
            return casted;
        }
        return new HashMap<>();
    }

    private String safeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || !redirect.startsWith("/")) {
            return "/dashboard";
        }
        return redirect;
    }

    @PostMapping("/notifications/password-reminder/dismiss")
    public String dismissPasswordReminder(@RequestParam(value = "redirect", required = false) String redirect) {
        currentUserService.currentUser()
                .map(user -> user.getUsername())
                .filter(username -> username != null && !username.isBlank())
                .ifPresent(username -> {
                    String key = WebUiModelAdvice.passwordReminderDismissKey(username);
                    GlobalSetting setting = globalSettingRepository.findByKey(key)
                            .orElseGet(() -> new GlobalSetting(key, "true"));
                    setting.setStringValue("true");
                    globalSettingRepository.save(setting);
                });
        return "redirect:" + safeRedirect(redirect);
    }
}
