package ro.church_office.teamleaf.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.DTO.ChurchInfoDTO;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DAO.PersonRepository;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.teamleaf.security.CurrentUserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebUiModelAdviceTest {

    private WebUiModelAdvice advice(ChurchInfoService churchInfoService,
                                    PersonRepository personRepository,
                                    GlobalSettingRepository globalSettingRepository,
                                    CurrentUserService currentUserService) {
        return new WebUiModelAdvice(
                churchInfoService,
                personRepository,
                globalSettingRepository,
                currentUserService,
                mock(PasswordEncoder.class),
                mock(Environment.class));
    }

    @Test
    void authPageReturnsEmptyHeaderData() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        WebUiModelAdvice advice = advice(churchInfoService, personRepository, globalSettingRepository, currentUserService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/login");

        assertTrue(advice.headerChurches(request).isEmpty());
        assertNull(advice.headerActiveChurchId(request));
        assertNull(advice.headerActiveChurch(request));
        assertEquals("midnight", advice.headerUiTheme(request));
        assertTrue(advice.headerBirthdayNotifications(request).isEmpty());
    }

    @Test
    void nonAuthPageBuildsHeaderValuesAndFallbackTheme() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        ChurchInfoDTO c1 = new ChurchInfoDTO();
        c1.id = 1L;
        c1.name = "Betania";
        when(churchInfoService.getAll()).thenReturn(List.of(c1));
        when(churchInfoService.getDefaultChurchId()).thenReturn(1L);
        when(globalSettingRepository.findByKey("ui_theme")).thenReturn(Optional.of(new GlobalSetting("ui_theme", "invalid-theme")));
        when(globalSettingRepository.findByKey("pastoral_enable_birthday")).thenReturn(Optional.of(new GlobalSetting("pastoral_enable_birthday", "false")));
        when(currentUserService.hasRole("ADMIN")).thenReturn(true);

        WebUiModelAdvice advice = advice(churchInfoService, personRepository, globalSettingRepository, currentUserService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getQueryString()).thenReturn("tab=1");

        assertEquals(1, advice.headerChurches(request).size());
        assertEquals(1L, advice.headerActiveChurchId(request));
        assertEquals("Betania", advice.headerActiveChurch(request).name);
        assertEquals("midnight", advice.headerUiTheme(request));
        assertEquals("/dashboard?tab=1", advice.headerCurrentPath(request));
        assertTrue(advice.headerCanManageUsers());
        assertTrue(advice.headerBirthdayNotifications(request).isEmpty());
    }

    @Test
    void notificationKeyAndPathWithoutQueryAreStable() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        WebUiModelAdvice advice = advice(churchInfoService, personRepository, globalSettingRepository, currentUserService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/settings/system");
        when(request.getQueryString()).thenReturn(null);
        when(currentUserService.hasRole("ADMIN")).thenReturn(false);

        assertEquals("/settings/system", advice.headerCurrentPath(request));
        assertFalse(advice.headerCanManageUsers());
        assertEquals("2:3", WebUiModelAdvice.notificationKey(2L, 3L));
        assertEquals("0:0", WebUiModelAdvice.notificationKey(null, null));
    }

    @Test
    void birthdayNotificationsAreBuiltWhenFeatureEnabled() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        when(churchInfoService.getDefaultChurchId()).thenReturn(9L);
        when(globalSettingRepository.findByKey("pastoral_enable_birthday"))
                .thenReturn(Optional.of(new GlobalSetting("pastoral_enable_birthday", "true")));
        when(globalSettingRepository.findByKey("pastoral_birthday_window_days"))
                .thenReturn(Optional.of(new GlobalSetting("pastoral_birthday_window_days", 14)));

        Person person = new Person();
        person.setId(44L);
        person.setFirstName("Ana");
        person.setLastName("Pop");
        person.setBirthDate(LocalDate.now().plusDays(2).withYear(1999));
        when(personRepository.findAllByChurchIdAndBirthDateIsNotNull(org.mockito.Mockito.eq(9L), org.mockito.Mockito.any()))
                .thenReturn(List.of(person));

        WebUiModelAdvice advice = advice(churchInfoService, personRepository, globalSettingRepository, currentUserService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebUiModelAdvice.BIRTHDAY_NOTIFICATION_SESSION_KEY)).thenReturn(null);

        List<WebUiModelAdvice.BirthdayNotification> items = advice.headerBirthdayNotifications(request);

        assertEquals(1, items.size());
        assertEquals("Ana Pop", items.get(0).personName());
        assertEquals("9:44", items.get(0).notificationKey());
    }

    @Test
    void headerUiThemeAcceptsSupportedThemeFromSettings() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        when(globalSettingRepository.findByKey("ui_theme")).thenReturn(Optional.of(new GlobalSetting("ui_theme", "clearblue")));

        WebUiModelAdvice advice = advice(churchInfoService, personRepository, globalSettingRepository, currentUserService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/dashboard");

        assertEquals("clearblue", advice.headerUiTheme(request));
    }

    @Test
    void headerShutdownAvailabilityFollowsDesktopProperty() {
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Environment environment = mock(Environment.class);

        WebUiModelAdvice advice = new WebUiModelAdvice(
                churchInfoService,
                personRepository,
                globalSettingRepository,
                currentUserService,
                mock(PasswordEncoder.class),
                environment);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(environment.getProperty("ministryadmin.desktop.shutdown.enabled", "true")).thenReturn("false");

        assertFalse(advice.headerShutdownAvailable(request));
    }
}
