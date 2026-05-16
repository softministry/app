package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordPolicyServiceTest {

    private GlobalSettingRepository repository;
    private PasswordPolicyService service;

    @BeforeEach
    void setUp() {
        repository = mock(GlobalSettingRepository.class);
        service = new PasswordPolicyService(repository);
    }

    @Test
    void restrictionsDisabledByDefault() {
        when(repository.findByKey(PasswordPolicyService.PASSWORD_RESTRICTIONS_ENABLED_KEY)).thenReturn(Optional.empty());

        assertFalse(service.restrictionsEnabled());
    }

    @Test
    void restrictionsEnabledWhenTrueString() {
        GlobalSetting setting = mock(GlobalSetting.class);
        when(setting.getStringValue()).thenReturn(" true ");
        when(repository.findByKey(PasswordPolicyService.PASSWORD_RESTRICTIONS_ENABLED_KEY)).thenReturn(Optional.of(setting));

        assertTrue(service.restrictionsEnabled());
    }

    @Test
    void minimumLengthDefaultsToSix() {
        when(repository.findByKey(PasswordPolicyService.PASSWORD_MIN_SIX_ENABLED_KEY)).thenReturn(Optional.empty());

        assertEquals(6, service.minimumLength());
    }

    @Test
    void minimumLengthCanBeFour() {
        GlobalSetting setting = mock(GlobalSetting.class);
        when(setting.getStringValue()).thenReturn("false");
        when(repository.findByKey(PasswordPolicyService.PASSWORD_MIN_SIX_ENABLED_KEY)).thenReturn(Optional.of(setting));

        assertEquals(4, service.minimumLength());
    }

    @Test
    void isAcceptedRejectsBlankAlways() {
        assertFalse(service.isAccepted(" "));
        assertFalse(service.isAccepted(null));
    }

    @Test
    void isAcceptedAllowsAnyNonBlankWhenRestrictionsDisabled() {
        when(repository.findByKey(PasswordPolicyService.PASSWORD_RESTRICTIONS_ENABLED_KEY)).thenReturn(Optional.empty());

        assertTrue(service.isAccepted("abc"));
    }

    @Test
    void isAcceptedValidatesMinLengthWhenRestrictionsEnabled() {
        GlobalSetting restrictions = mock(GlobalSetting.class);
        GlobalSetting minSix = mock(GlobalSetting.class);
        when(restrictions.getStringValue()).thenReturn("1");
        when(minSix.getStringValue()).thenReturn("true");
        when(repository.findByKey(PasswordPolicyService.PASSWORD_RESTRICTIONS_ENABLED_KEY)).thenReturn(Optional.of(restrictions));
        when(repository.findByKey(PasswordPolicyService.PASSWORD_MIN_SIX_ENABLED_KEY)).thenReturn(Optional.of(minSix));

        assertFalse(service.isAccepted("12345"));
        assertTrue(service.isAccepted("123456"));
    }

    @Test
    void validationMessageDependsOnRestrictions() {
        when(repository.findByKey(PasswordPolicyService.PASSWORD_RESTRICTIONS_ENABLED_KEY)).thenReturn(Optional.empty());
        assertEquals("Parola este obligatorie.", service.validationMessage());

        GlobalSetting restrictions = mock(GlobalSetting.class);
        GlobalSetting minSix = mock(GlobalSetting.class);
        when(restrictions.getStringValue()).thenReturn("true");
        when(minSix.getStringValue()).thenReturn("false");
        when(repository.findByKey(PasswordPolicyService.PASSWORD_RESTRICTIONS_ENABLED_KEY)).thenReturn(Optional.of(restrictions));
        when(repository.findByKey(PasswordPolicyService.PASSWORD_MIN_SIX_ENABLED_KEY)).thenReturn(Optional.of(minSix));

        assertEquals("Parola trebuie să aibă minim 4 caractere.", service.validationMessage());
    }
}
