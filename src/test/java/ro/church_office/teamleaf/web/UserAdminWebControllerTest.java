package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.info.users.DAO.User;
import ro.church_office.info.users.DAO.UserRepository;
import ro.church_office.teamleaf.security.PasswordPolicyService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdminWebControllerTest {

    @Test
    void createRejectsBlankUsername() {
        UserRepository userRepository = mock(UserRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        when(globalSettingRepository.findByKey("user_create_require_password")).thenReturn(Optional.empty());
        UserAdminWebController controller = new UserAdminWebController(userRepository, globalSettingRepository, passwordEncoder, passwordPolicyService);

        UserAdminWebController.UserForm form = new UserAdminWebController.UserForm();
        form.setUsername("   ");
        form.setPassword("Secret123!");
        form.setAnswerOne("a");
        form.setAnswerTwo("b");

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.create(form, redirect);

        assertEquals("redirect:/admin/users", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejectsWhenPasswordPolicyFails() {
        UserRepository userRepository = mock(UserRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        when(globalSettingRepository.findByKey("user_create_require_password")).thenReturn(Optional.empty());
        when(passwordPolicyService.isAccepted("short")).thenReturn(false);
        when(passwordPolicyService.validationMessage()).thenReturn("Parola nu respecta politica.");

        UserAdminWebController controller = new UserAdminWebController(userRepository, globalSettingRepository, passwordEncoder, passwordPolicyService);

        UserAdminWebController.UserForm form = new UserAdminWebController.UserForm();
        form.setUsername("admin");
        form.setPassword("short");
        form.setAnswerOne("a");
        form.setAnswerTwo("b");

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.create(form, redirect);

        assertEquals("redirect:/admin/users", view);
        assertEquals("Parola nu respecta politica.", redirect.getFlashAttributes().get("error"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createPersistsUserWithNormalizedFields() {
        UserRepository userRepository = mock(UserRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);

        when(globalSettingRepository.findByKey("user_create_require_password")).thenReturn(Optional.empty());
        when(passwordPolicyService.isAccepted("Secret123!")).thenReturn(true);
        when(userRepository.findByUsername("new.user")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "enc:" + inv.getArgument(0));

        UserAdminWebController controller = new UserAdminWebController(userRepository, globalSettingRepository, passwordEncoder, passwordPolicyService);

        UserAdminWebController.UserForm form = new UserAdminWebController.UserForm();
        form.setUsername("  New.User  ");
        form.setPassword("Secret123!");
        form.setRole("UNKNOWN");
        form.setAnswerOne("  Answer One ");
        form.setAnswerTwo(" Answer Two  ");

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.create(form, redirect);

        assertEquals("redirect:/admin/users", view);
        assertEquals("Utilizatorul a fost creat.", redirect.getFlashAttributes().get("success"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateRejectsDuplicateUsername() {
        UserRepository userRepository = mock(UserRepository.class);
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        UserAdminWebController controller = new UserAdminWebController(userRepository, globalSettingRepository, passwordEncoder, passwordPolicyService);

        User target = new User();
        target.setId(10L);
        target.setUsername("current");

        User other = new User();
        other.setId(11L);
        other.setUsername("dup");

        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("dup")).thenReturn(Optional.of(other));

        UserAdminWebController.UserForm form = new UserAdminWebController.UserForm();
        form.setUsername("dup");
        form.setRole("ADMIN");

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.update(10L, form, redirect);

        assertEquals("redirect:/admin/users?edit=10", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        verify(userRepository, never()).save(any(User.class));
    }
}
