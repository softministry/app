package ro.church_office.teamleaf.security;

import org.springframework.stereotype.Service;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

@Service
public class PasswordPolicyService {

    public static final String PASSWORD_RESTRICTIONS_ENABLED_KEY = "password_restrictions_enabled";
    public static final String PASSWORD_MIN_SIX_ENABLED_KEY = "password_min_six_enabled";

    private final GlobalSettingRepository globalSettingRepository;

    public PasswordPolicyService(GlobalSettingRepository globalSettingRepository) {
        this.globalSettingRepository = globalSettingRepository;
    }

    public boolean restrictionsEnabled() {
        return globalSettingRepository.findByKey(PASSWORD_RESTRICTIONS_ENABLED_KEY)
                .map(GlobalSetting::getStringValue)
                .map(value -> value != null && ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim())))
                .orElse(false);
    }

    public int minimumLength() {
        boolean minSix = globalSettingRepository.findByKey(PASSWORD_MIN_SIX_ENABLED_KEY)
                .map(GlobalSetting::getStringValue)
                .map(value -> value != null && ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim())))
                .orElse(true);
        return minSix ? 6 : 4;
    }

    public boolean isAccepted(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        if (!restrictionsEnabled()) {
            return true;
        }
        return password.trim().length() >= minimumLength();
    }

    public String validationMessage() {
        if (!restrictionsEnabled()) {
            return "Parola este obligatorie.";
        }
        return "Parola trebuie să aibă minim " + minimumLength() + " caractere.";
    }
}
