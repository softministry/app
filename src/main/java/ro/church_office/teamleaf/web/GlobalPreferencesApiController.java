package ro.church_office.teamleaf.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

@Controller
@RequestMapping("/api/global-preferences")
public class GlobalPreferencesApiController {
    private static final String STATUS_CUSTOM_KEY = "status_customization";
    private static final String PRIORITY_CUSTOM_KEY = "priority_customization";
    private static final String NAME_CUSTOM_KEY = "name_customization";
    private static final String EMPTY_JSON_OBJECT = "{}";

    private final GlobalSettingRepository globalSettingRepository;

    public GlobalPreferencesApiController(GlobalSettingRepository globalSettingRepository) {
        this.globalSettingRepository = globalSettingRepository;
    }

    @GetMapping(value = "/status-customization", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> statusCustomization() {
        return preference(STATUS_CUSTOM_KEY);
    }

    @GetMapping(value = "/priority-customization", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> priorityCustomization() {
        return preference(PRIORITY_CUSTOM_KEY);
    }

    @GetMapping(value = "/name-customization", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> nameCustomization() {
        return preference(NAME_CUSTOM_KEY);
    }

    private ResponseEntity<String> preference(String key) {
        String value = globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .filter(text -> text != null && !text.isBlank())
                .orElse(EMPTY_JSON_OBJECT);
        return ResponseEntity.ok(value);
    }
}
