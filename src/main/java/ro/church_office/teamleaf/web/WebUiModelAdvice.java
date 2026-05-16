package ro.church_office.teamleaf.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.DTO.ChurchInfoDTO;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.teamleaf.security.CurrentUserService;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class WebUiModelAdvice {

    private static final String UI_THEME_KEY = "ui_theme";
    private static final String PASTORAL_ENABLE_BIRTHDAY_KEY = "pastoral_enable_birthday";
    private static final String PASTORAL_BIRTHDAY_WINDOW_DAYS_KEY = "pastoral_birthday_window_days";
    public static final String BIRTHDAY_NOTIFICATION_SESSION_KEY = "header.birthday.notification.actions";
    private static final String PASSWORD_REMINDER_DISMISS_KEY_PREFIX = "password_reminder_dismissed.";
    private static final DateTimeFormatter BIRTH_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM", Locale.forLanguageTag("ro-RO"));
    private static final Set<String> SUPPORTED_UI_THEMES = Set.of(
            "midnight", "forest", "ember", "violet", "softday", "clearblue");

    private final ChurchInfoService churchInfoService;
    private final PersonRepository personRepository;
    private final GlobalSettingRepository globalSettingRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public WebUiModelAdvice(ChurchInfoService churchInfoService,
                            PersonRepository personRepository,
                            GlobalSettingRepository globalSettingRepository,
                            CurrentUserService currentUserService,
                            PasswordEncoder passwordEncoder,
                            Environment environment) {
        this.churchInfoService = churchInfoService;
        this.personRepository = personRepository;
        this.globalSettingRepository = globalSettingRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @ModelAttribute("headerChurches")
    public List<ChurchInfoDTO> headerChurches(HttpServletRequest request) {
        if (isAuthPage(request)) {
            return List.of();
        }
        return churchInfoService.getAll();
    }

    @ModelAttribute("headerActiveChurchId")
    public Long headerActiveChurchId(HttpServletRequest request) {
        if (isAuthPage(request)) {
            return null;
        }
        return churchInfoService.getDefaultChurchId();
    }

    @ModelAttribute("headerActiveChurch")
    public ChurchInfoDTO headerActiveChurch(HttpServletRequest request) {
        if (isAuthPage(request)) {
            return null;
        }
        Long churchId = churchInfoService.getDefaultChurchId();
        if (churchId == null) {
            return null;
        }
        return churchInfoService.getAll().stream()
                .filter(church -> church != null && church.id != null && church.id.equals(churchId))
                .findFirst()
                .orElse(null);
    }

    @ModelAttribute("headerUiTheme")
    public String headerUiTheme(HttpServletRequest request) {
        if (isAuthPage(request)) {
            return "midnight";
        }
        return globalSettingRepository.findByKey(UI_THEME_KEY)
                .map(GlobalSetting::getStringValue)
                .filter(SUPPORTED_UI_THEMES::contains)
                .orElse("midnight");
    }

    @ModelAttribute("headerCurrentPath")
    public String headerCurrentPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return uri;
        }
        return uri + "?" + query;
    }

    @ModelAttribute("headerCanManageUsers")
    public boolean headerCanManageUsers() {
        return currentUserService.hasRole("ADMIN");
    }

    @ModelAttribute("headerShutdownAvailable")
    public boolean headerShutdownAvailable(HttpServletRequest request) {
        if (isAuthPage(request)) {
            return false;
        }
        String value = environment.getProperty("ministryadmin.desktop.shutdown.enabled", "true");
        return value == null || !"false".equalsIgnoreCase(value.trim());
    }

    @ModelAttribute("headerPasswordReminderVisible")
    public boolean headerPasswordReminderVisible(HttpServletRequest request) {
        if (isAuthPage(request)) {
            return false;
        }
        return currentUserService.currentUser()
                .filter(user -> user.getUsername() != null && !user.getUsername().isBlank())
                .filter(user -> user.getPassword() != null && passwordEncoder.matches("admin", user.getPassword()))
                .map(user -> !isPasswordReminderDismissed(user.getUsername()))
                .orElse(false);
    }

    @ModelAttribute("headerBirthdayNotifications")
    public List<BirthdayNotification> headerBirthdayNotifications(HttpServletRequest request) {
        if (isAuthPage(request)) {
            return List.of();
        }
        Long churchId = churchInfoService.getDefaultChurchId();
        if (churchId == null || !booleanSetting(PASTORAL_ENABLE_BIRTHDAY_KEY, true)) {
            return List.of();
        }
        int windowDays = intSetting(PASTORAL_BIRTHDAY_WINDOW_DAYS_KEY, 14, 0, 90);
        LocalDate today = LocalDate.now();
        Map<String, LocalDate> actions = birthdayActions(request.getSession(false), today);

        return personRepository.findAllByChurchIdAndBirthDateIsNotNull(churchId, Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName")))
                .stream()
                .map(person -> toBirthdayNotification(person, today))
                .filter(notification -> notification.daysUntil() >= 0 && notification.daysUntil() <= windowDays)
                .filter(notification -> {
                    LocalDate hiddenUntil = actions.get(notification.notificationKey());
                    return hiddenUntil == null || hiddenUntil.isBefore(today);
                })
                .sorted((left, right) -> {
                    int byDays = Integer.compare(left.daysUntil(), right.daysUntil());
                    if (byDays != 0) return byDays;
                    return left.personName().compareToIgnoreCase(right.personName());
                })
                .limit(5)
                .toList();
    }

    private BirthdayNotification toBirthdayNotification(Person person, LocalDate today) {
        int daysUntil = daysUntilBirthday(today, person.getBirthDate());
        Long churchId = churchInfoService.getDefaultChurchId();
        return new BirthdayNotification(
                personDisplayName(person),
                daysUntil,
                daysUntilLabel(daysUntil),
                birthDateLabel(person.getBirthDate()),
                blankToDash(person.getAddress()),
                blankToDash(person.getPhone()),
                person.getId(),
                notificationKey(churchId, person.getId()),
                completedYears(person.getBirthDate(), today)
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, LocalDate> birthdayActions(HttpSession session, LocalDate today) {
        if (session == null) {
            return Map.of();
        }
        Object raw = session.getAttribute(BIRTHDAY_NOTIFICATION_SESSION_KEY);
        if (!(raw instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, LocalDate> sanitized = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof LocalDate value)) {
                continue;
            }
            if (!value.isBefore(today)) {
                sanitized.put(key, value);
            }
        }
        session.setAttribute(BIRTHDAY_NOTIFICATION_SESSION_KEY, sanitized);
        return sanitized;
    }

    public static String notificationKey(Long churchId, Long personId) {
        return (churchId == null ? "0" : churchId) + ":" + (personId == null ? "0" : personId);
    }

    public static String passwordReminderDismissKey(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return PASSWORD_REMINDER_DISMISS_KEY_PREFIX + normalized;
    }

    private int daysUntilBirthday(LocalDate today, LocalDate birthDate) {
        if (birthDate == null) {
            return Integer.MAX_VALUE;
        }
        LocalDate nextBirthday = birthDate.withYear(today.getYear());
        if (nextBirthday.isBefore(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }
        return (int) ChronoUnit.DAYS.between(today, nextBirthday);
    }

    private String personDisplayName(Person person) {
        String firstName = person.getFirstName() == null ? "" : person.getFirstName().trim();
        String lastName = person.getLastName() == null ? "" : person.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return person.getId() == null ? "Persoană" : "Persoană #" + person.getId();
    }

    private String daysUntilLabel(int daysUntil) {
        if (daysUntil <= 0) {
            return "Azi";
        }
        if (daysUntil == 1) {
            return "Mâine";
        }
        return "Peste " + daysUntil + " zile";
    }

    private String birthDateLabel(LocalDate birthDate) {
        if (birthDate == null) {
            return "—";
        }
        return birthDate.format(BIRTH_DATE_FORMAT);
    }

    private Integer completedYears(LocalDate birthDate, LocalDate today) {
        if (birthDate == null || today == null || birthDate.isAfter(today)) {
            return null;
        }
        return Period.between(birthDate, today).getYears();
    }

    private String blankToDash(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value.trim();
    }

    private boolean booleanSetting(String key, boolean defaultValue) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> "true".equalsIgnoreCase(value) || "1".equals(value))
                .orElse(defaultValue);
    }

    private int intSetting(String key, int defaultValue, int min, int max) {
        int resolved = globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getIntValue)
                .orElse(defaultValue);
        if (resolved < min) return min;
        return Math.min(resolved, max);
    }

    private boolean isPasswordReminderDismissed(String username) {
        return globalSettingRepository.findByKey(passwordReminderDismissKey(username))
                .map(GlobalSetting::getStringValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> "true".equalsIgnoreCase(value) || "1".equals(value))
                .orElse(false);
    }

    private boolean isAuthPage(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        return "/login".equals(uri) || "/logged-out".equals(uri) || "/forgot-password".equals(uri);
    }

    public record BirthdayNotification(String personName,
                                       int daysUntil,
                                       String whenLabel,
                                       String birthDateLabel,
                                       String address,
                                       String phone,
                                       Long personId,
                                       String notificationKey,
                                       Integer ageYears) {}
}
