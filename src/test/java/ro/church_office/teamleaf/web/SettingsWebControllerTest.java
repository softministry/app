package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ro.church_office.info.api.DatabaseService;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.Repository.ChurchInfoRepository;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsWebControllerTest {
    private byte[] validImportZip() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("backup.sql"));
            zip.write("select 1;".getBytes());
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private byte[] validExportZip() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("export.sql"));
            zip.write("create table t(id int);".getBytes());
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    void saveSystemUsesDefaultRowsWhenInvalidAndStoresBooleanFlags() {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        when(globalSettingRepository.findByKey(anyString())).thenReturn(Optional.empty());

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        SettingsWebController.SystemSettingsForm form = new SettingsWebController.SystemSettingsForm();
        form.setRowsPerPage(0);
        form.setEventTasksEnabled(Boolean.TRUE);
        form.setPrivateMode(Boolean.FALSE);
        form.setPasswordRestrictionsEnabled(Boolean.TRUE);
        form.setPasswordMinSixEnabled(Boolean.FALSE);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.saveSystem(form, redirect);

        assertEquals("redirect:/settings/system", view);
        assertNotNull(redirect.getFlashAttributes().get("success"));

        ArgumentCaptor<GlobalSetting> captor = ArgumentCaptor.forClass(GlobalSetting.class);
        verify(globalSettingRepository, times(8)).save(captor.capture());

        Map<String, GlobalSetting> byKey = new HashMap<>();
        for (GlobalSetting setting : captor.getAllValues()) {
            byKey.put(setting.getKey(), setting);
        }

        assertEquals(20, byKey.get("rows_per_page").getIntValue());
        assertEquals("true", byKey.get("event_tasks_enabled").getStringValue());
        assertEquals("false", byKey.get("private_mode").getStringValue());
        assertEquals("true", byKey.get("password_restrictions_enabled").getStringValue());
        assertEquals("false", byKey.get("password_min_six_enabled").getStringValue());
        assertEquals("#374151", byKey.get("event_name_color").getStringValue());
        assertEquals("#374151", byKey.get("person_name_color").getStringValue());
    }

    @Test
    void switchRuntimeModeFailsOutsideDesktopProfile() {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        when(environment.getActiveProfiles()).thenReturn(new String[]{"default"});

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        SettingsWebController.RuntimeModeForm form = new SettingsWebController.RuntimeModeForm("dev", true);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.switchRuntimeMode(form, redirect);

        assertEquals("redirect:/settings/system", view);
        assertTrue(String.valueOf(redirect.getFlashAttributes().get("error")).contains("doar în profilul desktop"));
    }

    @Test
    void switchRuntimeModeNormalizesInvalidModeToProdAndWritesProperties() throws IOException {
        Path home = Files.createTempDirectory("settings-home-");
        Path data = Files.createTempDirectory("settings-data-");
        Path prodBase = data.resolve("ministryadmin-db");

        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        when(environment.getActiveProfiles()).thenReturn(new String[]{"desktop"});

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                home.toString(),
                data.toString(),
                prodBase.toString());

        SettingsWebController.RuntimeModeForm form = new SettingsWebController.RuntimeModeForm("garbage", true);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.switchRuntimeMode(form, redirect);

        assertEquals("redirect:/settings/system", view);
        assertNotNull(redirect.getFlashAttributes().get("success"));

        Path runtimeModeFile = home.resolve("config").resolve("runtime-mode.properties");
        assertTrue(Files.exists(runtimeModeFile));

        Properties props = new Properties();
        try (var in = Files.newInputStream(runtimeModeFile)) {
            props.load(in);
        }
        assertEquals("prod", props.getProperty("ministryadmin.desktop.runtime-mode"));
        assertEquals(prodBase.toString(), props.getProperty("ministryadmin.desktop.database-file-base"));
    }

    @Test
    void importDbAddsErrorFlashWhenServiceThrows() throws IOException {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        doThrow(new IOException("Import exploded")).when(databaseService).importDatabase(any());

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        MockMultipartFile file = new MockMultipartFile("file", "db.zip", "application/zip", validImportZip());
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.importDb(file, redirect);

        assertEquals("redirect:/settings/database", view);
        assertEquals("Import exploded", redirect.getFlashAttributes().get("error"));
    }

    @Test
    void exportDbReturnsServerErrorWhenServiceThrows() throws IOException {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        doThrow(new IOException("No export")).when(databaseService).exportDatabase(anyBoolean());

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        var response = controller.exportDb("full");

        assertEquals(500, response.getStatusCode().value());
        assertTrue(String.valueOf(response.getBody()).contains("Export failed"));
    }

    @Test
    void importDbAddsSuccessFlashWhenServiceSucceeds() throws IOException {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        MockMultipartFile file = new MockMultipartFile("file", "db.zip", "application/zip", validImportZip());
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.importDb(file, redirect);

        assertEquals("redirect:/settings/database", view);
        assertNotNull(redirect.getFlashAttributes().get("success"));
    }

    @Test
    void exportDbReturnsAttachmentOnSuccess() throws IOException {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        InputStream payload = new ByteArrayInputStream(validExportZip());
        when(databaseService.exportDatabase(false)).thenReturn(payload);

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        var response = controller.exportDb("full");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(String.valueOf(response.getHeaders().getFirst("Content-Disposition")).contains("db-full-"));
        assertNotNull(response.getHeaders().getFirst("X-Checksum-SHA256"));
    }

    @Test
    void exportDbRejectsFreshMode() {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        var response = controller.exportDb("fresh");
        assertEquals(400, response.getStatusCode().value());
        assertTrue(String.valueOf(response.getBody()).contains("mode trebuie să fie 'full'"));
    }

    @Test
    void shutdownAppWhenDisabledShowsError() {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"default"});
        when(environment.getProperty("ministryadmin.desktop.shutdown.enabled", "true")).thenReturn("false");

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.shutdownApp(redirect);

        assertEquals("redirect:/settings/system", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
    }

    @Test
    void rendersSettingsTabsWithExpectedModelDefaults() {
        GlobalSettingRepository globalSettingRepository = mock(GlobalSettingRepository.class);
        ChurchInfoService churchInfoService = mock(ChurchInfoService.class);
        ChurchInfoRepository churchInfoRepository = mock(ChurchInfoRepository.class);
        DatabaseService databaseService = mock(DatabaseService.class);
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        when(environment.getActiveProfiles()).thenReturn(new String[]{"desktop"});
        when(environment.getProperty("ministryadmin.desktop.runtime-mode", "prod")).thenReturn("dev");
        when(environment.getProperty("ministryadmin.desktop.database-file-base", "/tmp/ministry-data/ministryadmin-db"))
                .thenReturn("/tmp/ministry-data/ministryadmin-db-dev");
        when(globalSettingRepository.findByKey(anyString())).thenReturn(Optional.empty());

        SettingsWebController controller = new SettingsWebController(
                globalSettingRepository,
                churchInfoService,
                churchInfoRepository,
                databaseService,
                environment,
                applicationContext,
                "/tmp/ministry-home",
                "/tmp/ministry-data",
                "/tmp/ministry-data/ministryadmin-db");

        ExtendedModelMap appearanceModel = new ExtendedModelMap();
        String appearanceView = controller.appearance(appearanceModel);
        assertEquals("settings/index", appearanceView);
        assertEquals("appearance", appearanceModel.get("activeTab"));
        assertEquals(Boolean.TRUE, appearanceModel.get("desktopProfile"));
        assertEquals(Boolean.TRUE, appearanceModel.get("shutdownAvailable"));

        ExtendedModelMap systemModel = new ExtendedModelMap();
        String systemView = controller.system(systemModel);
        assertEquals("settings/index", systemView);
        assertEquals("system", systemModel.get("activeTab"));
        assertNotNull(systemModel.get("systemForm"));
        assertNotNull(systemModel.get("rowOptions"));

        ExtendedModelMap pastoralModel = new ExtendedModelMap();
        String pastoralView = controller.pastoral(pastoralModel);
        assertEquals("settings/index", pastoralView);
        assertEquals("pastoral", pastoralModel.get("activeTab"));
        assertNotNull(pastoralModel.get("pastoralForm"));
    }
}
