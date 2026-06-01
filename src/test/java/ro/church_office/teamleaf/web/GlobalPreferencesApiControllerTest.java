package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;

import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalPreferencesApiControllerTest {
    @Test
    void returnsStoredStatusCustomization() {
        GlobalSettingRepository repository = mock(GlobalSettingRepository.class);
        when(repository.findByKey("status_customization"))
                .thenReturn(Optional.of(new GlobalSetting("status_customization", "{\"PLANNED\":{\"color\":\"#2563eb\"}}")));

        GlobalPreferencesApiController controller = new GlobalPreferencesApiController(repository);

        var response = controller.statusCustomization();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("{\"PLANNED\":{\"color\":\"#2563eb\"}}", response.getBody());
    }

    @Test
    void returnsEmptyJsonWhenPreferenceIsMissingOrBlank() {
        GlobalSettingRepository repository = mock(GlobalSettingRepository.class);
        when(repository.findByKey("priority_customization")).thenReturn(Optional.empty());
        when(repository.findByKey("name_customization"))
                .thenReturn(Optional.of(new GlobalSetting("name_customization", " ")));

        GlobalPreferencesApiController controller = new GlobalPreferencesApiController(repository);

        assertEquals("{}", controller.priorityCustomization().getBody());
        assertEquals("{}", controller.nameCustomization().getBody());
    }
}
