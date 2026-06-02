package ro.church_office.teamleaf.web;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.church_office.info.api.DatabaseService;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.DTO.ChurchInfoDTO;
import ro.church_office.info.church.Repository.ChurchInfoRepository;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.teamleaf.security.PasswordPolicyService;

@Controller
@RequestMapping("/settings")
@org.springframework.transaction.annotation.Transactional
public class SettingsWebController {
    private static final Logger log = LoggerFactory.getLogger(SettingsWebController.class);

    private static final String STATUS_CUSTOM_KEY = "status_customization";
    private static final String PRIORITY_CUSTOM_KEY = "priority_customization";
    private static final String NAME_CUSTOM_KEY = "name_customization";
    private static final String REPORTS_LAYOUT_ORDER_KEY = "reports_layout_order";
    private static final String ROWS_KEY = "rows_per_page";
    private static final String EVENT_NAME_COLOR_KEY = "event_name_color";
    private static final String PERSON_NAME_COLOR_KEY = "person_name_color";
    private static final String GROUP_NAME_COLOR_KEY = "group_name_color";
    private static final String EVENT_NAME_FONT_SIZE_KEY = "event_name_font_size";
    private static final String PERSON_NAME_FONT_SIZE_KEY = "person_name_font_size";
    private static final String GROUP_NAME_FONT_SIZE_KEY = "group_name_font_size";
    private static final String EVENT_TASKS_ENABLED_KEY = "event_tasks_enabled";
    private static final String PRIVATE_MODE_KEY = "private_mode";
    private static final String PASSWORD_RESTRICTIONS_ENABLED_KEY = PasswordPolicyService.PASSWORD_RESTRICTIONS_ENABLED_KEY;
    private static final String PASSWORD_MIN_SIX_ENABLED_KEY = PasswordPolicyService.PASSWORD_MIN_SIX_ENABLED_KEY;
    private static final String USER_CREATE_REQUIRE_PASSWORD_KEY = "user_create_require_password";
    private static final String UI_THEME_KEY = "ui_theme";
    private static final String PASTORAL_ABSENCE_COUNT_KEY = "pastoral_absence_count";
    private static final String PASTORAL_ANALYSIS_DAYS_KEY = "pastoral_analysis_days";
    private static final String PASTORAL_BIRTHDAY_WINDOW_DAYS_KEY = "pastoral_birthday_window_days";
    private static final String PASTORAL_SNOOZE_DAYS_KEY = "pastoral_snooze_days";
    private static final String PASTORAL_MAX_RECOMMENDATIONS_KEY = "pastoral_max_recommendations";
    private static final String PASTORAL_ENABLE_ABSENCE_KEY = "pastoral_enable_absence";
    private static final String PASTORAL_ENABLE_CHILD_ABSENCE_KEY = "pastoral_enable_child_absence";
    private static final String PASTORAL_ENABLE_OVERDUE_FOLLOW_UP_KEY = "pastoral_enable_overdue_follow_up";
    private static final String PASTORAL_ENABLE_WITHOUT_GROUP_KEY = "pastoral_enable_without_group";
    private static final String PASTORAL_ENABLE_NEW_PERSON_KEY = "pastoral_enable_new_person";
    private static final String PASTORAL_ENABLE_BIRTHDAY_KEY = "pastoral_enable_birthday";
    private static final String PASTORAL_TEMPLATE_ABSENCE_KEY = "pastoral_template_absence";
    private static final String PASTORAL_TEMPLATE_CHILD_ABSENCE_KEY = "pastoral_template_child_absence";
    private static final String PASTORAL_TEMPLATE_OVERDUE_FOLLOW_UP_KEY = "pastoral_template_overdue_follow_up";
    private static final String PASTORAL_TEMPLATE_WITHOUT_GROUP_KEY = "pastoral_template_without_group";
    private static final String PASTORAL_TEMPLATE_NEW_PERSON_KEY = "pastoral_template_new_person";
    private static final String PASTORAL_TEMPLATE_BIRTHDAY_KEY = "pastoral_template_birthday";
    private static final String RUNTIME_MODE_PROD = "prod";
    private static final String RUNTIME_MODE_DEV = "dev";
    private static final long MAX_IMPORT_ZIP_SIZE_BYTES = 300L * 1024L * 1024L; // 300 MB
    private static final long MAX_IMPORT_TOTAL_UNZIPPED_BYTES = 2L * 1024L * 1024L * 1024L; // 2 GB
    private static final int MAX_IMPORT_ZIP_ENTRIES = 10_000;
    private static final DateTimeFormatter EXPORT_FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String FACTORY_RESET_CONFIRMATION = "RESETARE DATE";

    private final GlobalSettingRepository globalSettingRepository;
    private final ChurchInfoService churchInfoService;
    private final ChurchInfoRepository churchInfoRepository;
    private final DatabaseService databaseService;
    private final Environment environment;
    private final ConfigurableApplicationContext applicationContext;
    private final Path desktopHomeDir;
    private final Path desktopDataDir;
    private final Path defaultProdDatabaseFileBase;

    public SettingsWebController(GlobalSettingRepository globalSettingRepository,
                                 ChurchInfoService churchInfoService,
                                 ChurchInfoRepository churchInfoRepository,
                                 DatabaseService databaseService,
                                 Environment environment,
                                 ConfigurableApplicationContext applicationContext,
                                 @org.springframework.beans.factory.annotation.Value("${ministryadmin.desktop.home-dir:${user.home}/ChurchAdministrationPlatform}") String desktopHomeDir,
                                 @org.springframework.beans.factory.annotation.Value("${ministryadmin.desktop.data-dir:${user.home}/ChurchAdministrationPlatform/data}") String desktopDataDir,
                                 @org.springframework.beans.factory.annotation.Value("${ministryadmin.desktop.data-dir:${user.home}/ChurchAdministrationPlatform/data}/ministryadmin.sqlite.db") String defaultProdDatabaseFileBase) {
        this.globalSettingRepository = globalSettingRepository;
        this.churchInfoService = churchInfoService;
        this.churchInfoRepository = churchInfoRepository;
        this.databaseService = databaseService;
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.desktopHomeDir = Paths.get(desktopHomeDir).toAbsolutePath().normalize();
        this.desktopDataDir = Paths.get(desktopDataDir).toAbsolutePath().normalize();
        this.defaultProdDatabaseFileBase = Paths.get(defaultProdDatabaseFileBase).toAbsolutePath().normalize();
    }

    @GetMapping
    public String root() {
        return "redirect:/settings/appearance";
    }

    @GetMapping("/appearance")
    public String appearance(Model model) {
        prepareBaseModel(model, "appearance");
        model.addAttribute("appearanceForm", new AppearanceForm(
                getStringSetting(STATUS_CUSTOM_KEY, "{}"),
                getStringSetting(PRIORITY_CUSTOM_KEY, "{}"),
                getStringSetting(NAME_CUSTOM_KEY, "{}")));
        return "settings/index";
    }

    @PostMapping("/appearance")
    public String saveAppearance(@ModelAttribute AppearanceForm form, RedirectAttributes redirectAttributes) {
        saveStringSetting(STATUS_CUSTOM_KEY, normalizeJsonText(form.getStatusCustomization(), "{}"));
        saveStringSetting(PRIORITY_CUSTOM_KEY, normalizeJsonText(form.getPriorityCustomization(), "{}"));
        saveStringSetting(NAME_CUSTOM_KEY, normalizeJsonText(form.getNameCustomization(), "{}"));
        redirectAttributes.addFlashAttribute("success", "Setările de aspect au fost salvate.");
        return "redirect:/settings/appearance";
    }

    @PostMapping("/font-customization")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> updateFontCustomization(@RequestParam("category") String category,
                                                     @RequestParam("color") String color,
                                                     @RequestParam("fontSize") Double fontSize) {
        String colorKey;
        String sizeKey;

        switch (category) {
            case "event" -> {
                colorKey = EVENT_NAME_COLOR_KEY;
                sizeKey = EVENT_NAME_FONT_SIZE_KEY;
            }
            case "person" -> {
                colorKey = PERSON_NAME_COLOR_KEY;
                sizeKey = PERSON_NAME_FONT_SIZE_KEY;
            }
            case "group" -> {
                colorKey = GROUP_NAME_COLOR_KEY;
                sizeKey = GROUP_NAME_FONT_SIZE_KEY;
            }
            default -> {
                return ResponseEntity.badRequest().body("Categorie invalidă.");
            }
        }

        saveStringSetting(colorKey, normalizeHexColor(color, "#374151"));
        saveDoubleSetting(sizeKey, fontSize != null ? fontSize : 0.93);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/church")
    public String church(Model model) {
        prepareBaseModel(model, "church");
        ChurchInfoDTO currentChurch = churchInfoService.get().orElse(new ChurchInfoDTO());
        model.addAttribute("churchForm", new ChurchSettingsForm(
                churchInfoService.getDefaultChurchId(),
                currentChurch.id,
                currentChurch.name,
                currentChurch.avatarUrl,
                currentChurch.address,
                currentChurch.pastorName,
                currentChurch.pastorPhone,
                currentChurch.secretaryName,
                currentChurch.secretaryPhone,
                currentChurch.treasurerName,
                currentChurch.treasurerPhone));
        model.addAttribute("churches", churchInfoService.getAll());
        return "settings/index";
    }

    @PostMapping("/church")
    public String saveChurch(@ModelAttribute ChurchSettingsForm form, RedirectAttributes redirectAttributes) {
        if (form.getDefaultChurchId() != null) {
            churchInfoService.setDefaultChurchId(form.getDefaultChurchId());
        }

        ChurchInfoDTO dto = new ChurchInfoDTO();
        dto.id = form.getCurrentChurchId();
        dto.name = blankToNull(form.getCurrentChurchName());
        dto.avatarUrl = blankToNull(form.getAvatarUrl());
        dto.address = blankToNull(form.getAddress());
        dto.pastorName = blankToNull(form.getPastorName());
        dto.pastorPhone = blankToNull(form.getPastorPhone());
        dto.secretaryName = blankToNull(form.getSecretaryName());
        dto.secretaryPhone = blankToNull(form.getSecretaryPhone());
        dto.treasurerName = blankToNull(form.getTreasurerName());
        dto.treasurerPhone = blankToNull(form.getTreasurerPhone());
        churchInfoService.save(dto);

        redirectAttributes.addFlashAttribute("success", "Setările bisericii au fost salvate.");
        return "redirect:/settings/church";
    }

    @PostMapping("/church/delete")
    public String deleteChurch(@RequestParam("churchId") Long churchId, RedirectAttributes redirectAttributes) {
        if (churchId == null) {
            redirectAttributes.addFlashAttribute("error", "Selectează biserica de șters.");
            return "redirect:/settings/church";
        }
        if (churchInfoService.getAll().size() <= 1) {
            redirectAttributes.addFlashAttribute("error", "Nu poți șterge ultima biserică.");
            return "redirect:/settings/church";
        }
        try {
            if (churchInfoService.getAll().stream().noneMatch(church -> church != null && church.id != null && church.id.equals(churchId))) {
                redirectAttributes.addFlashAttribute("error", "Biserica selectată nu există.");
                return "redirect:/settings/church";
            }

            Long defaultChurchId = churchInfoService.getDefaultChurchId();
            if (defaultChurchId != null && defaultChurchId.equals(churchId)) {
                churchInfoService.getAll().stream()
                        .filter(church -> church != null && church.id != null && !church.id.equals(churchId))
                        .findFirst()
                        .ifPresent(church -> churchInfoService.setDefaultChurchId(church.id));
            }

            churchInfoRepository.deleteById(churchId);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Biserica nu poate fi ștearsă deoarece are date asociate (persoane, grupuri, evenimente etc.).");
            return "redirect:/settings/church";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut șterge biserica." : ex.getMessage());
            return "redirect:/settings/church";
        }

        redirectAttributes.addFlashAttribute("success", "Biserica a fost ștearsă.");
        return "redirect:/settings/church";
    }

    @GetMapping("/system")
    public String system(Model model) {
        prepareBaseModel(model, "system");
        model.addAttribute("systemForm", new SystemSettingsForm(
                intSetting(ROWS_KEY, 7),
                booleanSetting(EVENT_TASKS_ENABLED_KEY, true),
                booleanSetting(PRIVATE_MODE_KEY, false),
                booleanSetting(PASSWORD_RESTRICTIONS_ENABLED_KEY, false),
                booleanSetting(PASSWORD_MIN_SIX_ENABLED_KEY, true),
                booleanSetting(USER_CREATE_REQUIRE_PASSWORD_KEY, false)));
        model.addAttribute("rowOptions", List.of(5, 7, 10, 20, 25, 50, 100));
        return "settings/index";
    }

    @GetMapping("/database")
    public String databaseTools(Model model) {
        prepareBaseModel(model, "database");
        return "settings/database";
    }

    @PostMapping("/system")
    public String saveSystem(@ModelAttribute SystemSettingsForm form, RedirectAttributes redirectAttributes) {
        int rows = form.getRowsPerPage() == null || form.getRowsPerPage() <= 0 ? 20 : form.getRowsPerPage();
        saveIntSetting(ROWS_KEY, rows);
        saveBooleanSetting(EVENT_TASKS_ENABLED_KEY, form.getEventTasksEnabled());
        saveBooleanSetting(PRIVATE_MODE_KEY, form.getPrivateMode());
        saveBooleanSetting(PASSWORD_RESTRICTIONS_ENABLED_KEY, form.getPasswordRestrictionsEnabled());
        saveBooleanSetting(PASSWORD_MIN_SIX_ENABLED_KEY, form.getPasswordMinSixEnabled());
        saveBooleanSetting(USER_CREATE_REQUIRE_PASSWORD_KEY, form.getUserCreateRequirePassword());
        redirectAttributes.addFlashAttribute("success", "Setările de sistem au fost salvate.");
        return "redirect:/settings/system";
    }

    @GetMapping("/pastoral")
    public String pastoral(Model model) {
        prepareBaseModel(model, "pastoral");
        model.addAttribute("pastoralForm", new PastoralSettingsForm(
                intSetting(PASTORAL_ABSENCE_COUNT_KEY, 3),
                intSetting(PASTORAL_ANALYSIS_DAYS_KEY, 90),
                intSetting(PASTORAL_BIRTHDAY_WINDOW_DAYS_KEY, 14),
                intSetting(PASTORAL_SNOOZE_DAYS_KEY, 7),
                intSetting(PASTORAL_MAX_RECOMMENDATIONS_KEY, 12),
                booleanSetting(PASTORAL_ENABLE_ABSENCE_KEY, true),
                booleanSetting(PASTORAL_ENABLE_CHILD_ABSENCE_KEY, true),
                booleanSetting(PASTORAL_ENABLE_OVERDUE_FOLLOW_UP_KEY, true),
                booleanSetting(PASTORAL_ENABLE_WITHOUT_GROUP_KEY, true),
                booleanSetting(PASTORAL_ENABLE_NEW_PERSON_KEY, true),
                booleanSetting(PASTORAL_ENABLE_BIRTHDAY_KEY, true),
                getStringSetting(PASTORAL_TEMPLATE_ABSENCE_KEY, defaultPastoralTemplate("Absență")),
                getStringSetting(PASTORAL_TEMPLATE_CHILD_ABSENCE_KEY, defaultPastoralTemplate("Copil absent")),
                getStringSetting(PASTORAL_TEMPLATE_OVERDUE_FOLLOW_UP_KEY, defaultPastoralTemplate("Follow-up întârziat")),
                getStringSetting(PASTORAL_TEMPLATE_WITHOUT_GROUP_KEY, defaultPastoralTemplate("Fără grup")),
                getStringSetting(PASTORAL_TEMPLATE_NEW_PERSON_KEY, defaultPastoralTemplate("Persoană nouă")),
                getStringSetting(PASTORAL_TEMPLATE_BIRTHDAY_KEY, defaultPastoralTemplate("Zi de naștere"))));
        return "settings/index";
    }

    @GetMapping("/reports")
    public String reportsSettings(Model model) {
        prepareBaseModel(model, "reports");
        model.addAttribute("reportsOrderForm", new ReportsOrderForm(
                getStringSetting(REPORTS_LAYOUT_ORDER_KEY, defaultReportsLayoutOrder())));
        model.addAttribute("availableReportSections", defaultReportSections());
        return "settings/index";
    }

    @PostMapping("/reports")
    public String saveReportsSettings(@ModelAttribute ReportsOrderForm form, RedirectAttributes redirectAttributes) {
        String normalized = normalizeReportsLayoutOrder(form.getLayoutOrder());
        saveStringSetting(REPORTS_LAYOUT_ORDER_KEY, normalized);
        redirectAttributes.addFlashAttribute("success", "Ordinea componentelor din rapoarte a fost salvată.");
        return "redirect:/settings/reports";
    }

    @PostMapping("/pastoral")
    public String savePastoral(@ModelAttribute PastoralSettingsForm form, RedirectAttributes redirectAttributes) {
        saveIntSetting(PASTORAL_ABSENCE_COUNT_KEY, clamp(form.getPastoralAbsenceCount(), 1, 10, 3));
        saveIntSetting(PASTORAL_ANALYSIS_DAYS_KEY, clamp(form.getPastoralAnalysisDays(), 7, 365, 90));
        saveIntSetting(PASTORAL_BIRTHDAY_WINDOW_DAYS_KEY, clamp(form.getPastoralBirthdayWindowDays(), 0, 90, 14));
        saveIntSetting(PASTORAL_SNOOZE_DAYS_KEY, clamp(form.getPastoralSnoozeDays(), 1, 90, 7));
        saveIntSetting(PASTORAL_MAX_RECOMMENDATIONS_KEY, clamp(form.getPastoralMaxRecommendations(), 1, 50, 12));
        saveBooleanSetting(PASTORAL_ENABLE_ABSENCE_KEY, form.getPastoralEnableAbsence());
        saveBooleanSetting(PASTORAL_ENABLE_CHILD_ABSENCE_KEY, form.getPastoralEnableChildAbsence());
        saveBooleanSetting(PASTORAL_ENABLE_OVERDUE_FOLLOW_UP_KEY, form.getPastoralEnableOverdueFollowUp());
        saveBooleanSetting(PASTORAL_ENABLE_WITHOUT_GROUP_KEY, form.getPastoralEnableWithoutGroup());
        saveBooleanSetting(PASTORAL_ENABLE_NEW_PERSON_KEY, form.getPastoralEnableNewPerson());
        saveBooleanSetting(PASTORAL_ENABLE_BIRTHDAY_KEY, form.getPastoralEnableBirthday());
        saveStringSetting(PASTORAL_TEMPLATE_ABSENCE_KEY, normalizeText(form.getPastoralTemplateAbsence(), defaultPastoralTemplate("Absență")));
        saveStringSetting(PASTORAL_TEMPLATE_CHILD_ABSENCE_KEY, normalizeText(form.getPastoralTemplateChildAbsence(), defaultPastoralTemplate("Copil absent")));
        saveStringSetting(PASTORAL_TEMPLATE_OVERDUE_FOLLOW_UP_KEY, normalizeText(form.getPastoralTemplateOverdueFollowUp(), defaultPastoralTemplate("Follow-up întârziat")));
        saveStringSetting(PASTORAL_TEMPLATE_WITHOUT_GROUP_KEY, normalizeText(form.getPastoralTemplateWithoutGroup(), defaultPastoralTemplate("Fără grup")));
        saveStringSetting(PASTORAL_TEMPLATE_NEW_PERSON_KEY, normalizeText(form.getPastoralTemplateNewPerson(), defaultPastoralTemplate("Persoană nouă")));
        saveStringSetting(PASTORAL_TEMPLATE_BIRTHDAY_KEY, normalizeText(form.getPastoralTemplateBirthday(), defaultPastoralTemplate("Zi de naștere")));
        redirectAttributes.addFlashAttribute("success", "Setările pastorale au fost salvate.");
        return "redirect:/settings/pastoral";
    }

    @GetMapping("/system/export")
    public ResponseEntity<?> exportDb(@RequestParam(value = "mode", defaultValue = "full") String mode) {
        String normalizedMode = mode == null ? "full" : mode.trim().toLowerCase();
        if (!"full".equals(normalizedMode)) {
            return ResponseEntity.badRequest().body("Parametrul mode trebuie să fie 'full'.");
        }
        boolean fresh = false;
        try {
            String ts = LocalDateTime.now().format(EXPORT_FILE_TS);
            String filename = "db-full-" + ts + ".zip";
            log.info("Database export requested. mode={}, filename={}", normalizedMode, filename);
            Path tempExport = Files.createTempFile("ministryadmin-export-", ".zip");
            String checksumSha256;
            try (InputStream in = new BufferedInputStream(databaseService.exportDatabase(fresh));
                 OutputStream out = Files.newOutputStream(tempExport, StandardOpenOption.TRUNCATE_EXISTING)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
                checksumSha256 = toHex(digest.digest());
            }
            validateExportArchive(tempExport);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header("X-Checksum-SHA256", checksumSha256)
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(new FileSystemResource(tempExport.toFile()));
        } catch (Exception ex) {
            log.error("Database export failed. mode={}", normalizedMode, ex);
            return ResponseEntity.internalServerError().body("Export failed: " + ex.getMessage());
        }
    }

    @PostMapping("/system/import")
    public String importDb(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            validateImportFile(file);
            ImportArchiveValidationReport report = inspectImportArchive(file);
            if (!report.valid()) {
                redirectAttributes.addFlashAttribute("error", "Arhivă invalidă: " + String.join(" ", report.errors()));
                return "redirect:/settings/database";
            }
            String originalName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
            log.info("Database import requested. file={}, sizeBytes={}", originalName, file.getSize());
            databaseService.importDatabase(file);
            redirectAttributes.addFlashAttribute("success", "Baza de date a fost încărcată cu succes.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            log.error("Database import failed.", ex);
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Importul a eșuat." : ex.getMessage());
        }
        return "redirect:/settings/database";
    }

    @PostMapping("/system/import/validate")
    public String validateImportArchive(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            validateImportFile(file);
            ImportArchiveValidationReport report = inspectImportArchive(file);
            if (report.valid()) {
                redirectAttributes.addFlashAttribute(
                        "success",
                        "Dry-run OK: arhivă validă (" + report.entryCount() + " fișiere, " + report.totalBytes() + " bytes necomprimați).");
            } else {
                redirectAttributes.addFlashAttribute("error", "Dry-run eșuat: " + String.join(" ", report.errors()));
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            log.error("Dry-run import validation failed.", ex);
            redirectAttributes.addFlashAttribute("error", "Nu s-a putut valida arhiva: " + ex.getMessage());
        }
        return "redirect:/settings/database";
    }

    @PostMapping("/database/reset-settings")
    public String resetSettings(@RequestParam(value = "confirmation", required = false) String confirmation,
                                RedirectAttributes redirectAttributes) {
        if (!FACTORY_RESET_CONFIRMATION.equals(normalizeText(confirmation, ""))) {
            redirectAttributes.addFlashAttribute("error", "Confirmarea nu este corectă. Scrie exact: " + FACTORY_RESET_CONFIRMATION + ".");
            return "redirect:/settings/database";
        }
        try {
            globalSettingRepository.deleteAllInBatch();
            redirectAttributes.addFlashAttribute("success", "Setările au fost resetate la valorile implicite.");
        } catch (Exception ex) {
            log.error("Settings reset failed.", ex);
            redirectAttributes.addFlashAttribute("error", "Resetarea setărilor a eșuat: " + ex.getMessage());
        }
        return "redirect:/settings/database";
    }

    @PostMapping("/database/factory-reset")
    public String factoryReset(@RequestParam(value = "confirmation", required = false) String confirmation,
                               RedirectAttributes redirectAttributes) {
        if (!FACTORY_RESET_CONFIRMATION.equals(normalizeText(confirmation, ""))) {
            redirectAttributes.addFlashAttribute("error", "Confirmarea nu este corectă. Scrie exact: " + FACTORY_RESET_CONFIRMATION + ".");
            return "redirect:/settings/database";
        }
        try {
            databaseService.resetApplicationData();
            redirectAttributes.addFlashAttribute("success", "Datele și setările au fost resetate. Conturile de utilizator au fost păstrate.");
        } catch (Exception ex) {
            log.error("Factory reset failed.", ex);
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Resetarea datelor a eșuat." : ex.getMessage());
        }
        return "redirect:/settings/database";
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selectează un fișier .zip pentru import.");
        }
        String originalName = file.getOriginalFilename();
        String safeName = originalName == null ? "" : originalName.trim().toLowerCase();
        if (!safeName.endsWith(".zip")) {
            throw new IllegalArgumentException("Fișier invalid. Importul acceptă doar arhive .zip.");
        }
        if (file.getSize() <= 0) {
            throw new IllegalArgumentException("Fișierul de import este gol.");
        }
        if (file.getSize() > MAX_IMPORT_ZIP_SIZE_BYTES) {
            throw new IllegalArgumentException("Fișier prea mare pentru import (max 300 MB).");
        }
    }

    private ImportArchiveValidationReport inspectImportArchive(MultipartFile file) throws Exception {
        List<String> errors = new ArrayList<>();
        int entries = 0;
        long totalUnzipped = 0L;
        boolean foundExpectedPayload = false;
        try (InputStream in = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entries > MAX_IMPORT_ZIP_ENTRIES) {
                    errors.add("Arhiva conține prea multe fișiere.");
                    break;
                }
                String name = entry.getName();
                if (name == null || name.isBlank()) {
                    errors.add("Arhivă invalidă: nume de intrare lipsă.");
                    continue;
                }
                if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                    errors.add("Arhivă invalidă: cale nesigură detectată.");
                    continue;
                }
                if (entry.isDirectory()) {
                    continue;
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".sql")
                        || lower.endsWith(".dump")
                        || lower.endsWith(".backup")
                        || lower.endsWith(".db")
                        || lower.endsWith(".sqlite")
                        || lower.endsWith(".sqlite3")) {
                    foundExpectedPayload = true;
                }
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    totalUnzipped += read;
                    if (totalUnzipped > MAX_IMPORT_TOTAL_UNZIPPED_BYTES) {
                        errors.add("Arhiva depășește limita de date necomprimate admise.");
                        break;
                    }
                }
                if (!errors.isEmpty()) {
                    break;
                }
            }
        }
        if (entries == 0) {
            errors.add("Arhiva este goală.");
        }
        if (!foundExpectedPayload) {
            errors.add("Arhiva nu conține un dump recognoscibil (.sql/.dump/.backup/.db/.sqlite/.sqlite3).");
        }
        return new ImportArchiveValidationReport(errors.isEmpty(), errors, entries, totalUnzipped);
    }

    private void validateExportArchive(Path zipPath) throws Exception {
        if (!Files.exists(zipPath) || Files.size(zipPath) <= 0) {
            throw new IllegalArgumentException("Exportul a generat o arhivă goală.");
        }
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            if (zipFile.size() == 0) {
                throw new IllegalArgumentException("Exportul a generat o arhivă fără conținut.");
            }
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private record ImportArchiveValidationReport(boolean valid, List<String> errors, int entryCount, long totalBytes) {}

    @PostMapping("/system/shutdown")
    public String shutdownApp(RedirectAttributes redirectAttributes) {
        if (!isShutdownAvailable()) {
            redirectAttributes.addFlashAttribute("error", "Oprirea aplicației din interfață este disponibilă doar în profilul desktop.");
            return "redirect:/settings/system";
        }

        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(700);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            try {
                org.springframework.boot.SpringApplication.exit(applicationContext, () -> 0);
            } finally {
                System.exit(0);
            }
        }, "app-shutdown-thread");
        shutdownThread.setDaemon(false);
        shutdownThread.start();

        redirectAttributes.addFlashAttribute("success", "Aplicația se oprește...");
        return "redirect:/settings/system";
    }

    @PostMapping("/system/runtime-mode")
    public String switchRuntimeMode(@ModelAttribute RuntimeModeForm form, RedirectAttributes redirectAttributes) {
        if (!isDesktopProfile()) {
            redirectAttributes.addFlashAttribute("error", "Comutarea bazei de date este disponibilă doar în profilul desktop.");
            return "redirect:/settings/system";
        }

        String mode = normalizeRuntimeMode(form.getMode());
        Path configDir = desktopHomeDir.resolve("config");
        Path runtimeModeFile = configDir.resolve("runtime-mode.properties");
        Path prodBase = defaultProdDatabaseFileBase;
        Path devBase = desktopDataDir.resolve("ministryadmin-dev.sqlite.db");
        Path selectedBase = RUNTIME_MODE_DEV.equals(mode) ? devBase : prodBase;

        try {
            Files.createDirectories(configDir);
            Files.createDirectories(desktopDataDir);

            if (RUNTIME_MODE_DEV.equals(mode) && Boolean.TRUE.equals(form.getCloneFromCurrent()) && !dbExists(devBase)) {
                cloneDbFiles(prodBase, devBase);
            }

            Properties props = new Properties();
            props.setProperty("ministryadmin.desktop.runtime-mode", mode);
            props.setProperty("ministryadmin.desktop.database-file-base", selectedBase.toString());
            try (java.io.OutputStream output = Files.newOutputStream(runtimeModeFile)) {
                props.store(output, "Runtime DB mode for desktop profile");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Nu am putut salva modul de rulare: " + ex.getMessage());
            return "redirect:/settings/system";
        }

        String modeLabel = RUNTIME_MODE_DEV.equals(mode) ? "DEV (test)" : "PROD (actual)";
        redirectAttributes.addFlashAttribute("success",
                "Modul bazei de date a fost setat la " + modeLabel + ". Repornește aplicația pentru aplicare.");
        return "redirect:/settings/system";
    }

    private void prepareBaseModel(Model model, String tab) {
        model.addAttribute("activeTab", tab);
        model.addAttribute("desktopProfile", isDesktopProfile());
        model.addAttribute("shutdownAvailable", isShutdownAvailable());
        model.addAttribute("runtimeModeForm", new RuntimeModeForm(
                normalizeRuntimeMode(environment.getProperty("ministryadmin.desktop.runtime-mode", RUNTIME_MODE_PROD)),
                false));
        model.addAttribute("runtimeModeCurrent", normalizeRuntimeMode(environment.getProperty("ministryadmin.desktop.runtime-mode", RUNTIME_MODE_PROD)));
        model.addAttribute("runtimeDatabaseFileBase", environment.getProperty("ministryadmin.desktop.database-file-base", defaultProdDatabaseFileBase.toString()));
    }

    private boolean isDesktopProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("desktop".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShutdownAvailable() {
        String value = environment.getProperty("ministryadmin.desktop.shutdown.enabled", "true");
        return value == null || !"false".equalsIgnoreCase(value.trim());
    }

    private String getStringSetting(String key, String fallback) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(fallback);
    }

    private boolean booleanSetting(String key, boolean fallback) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .map(value -> {
                    String normalized = value == null ? "" : value.trim().toLowerCase();
                    return !("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized));
                })
                .orElse(fallback);
    }

    private int intSetting(String key, int fallback) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getIntValue)
                .filter(value -> value != null && value > 0)
                .orElse(fallback);
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        int normalized = value == null ? fallback : value;
        return Math.min(max, Math.max(min, normalized));
    }

    private String colorSetting(String key, String fallback) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .filter(v -> v != null && v.trim().matches("^#[0-9a-fA-F]{6}$"))
                .map(String::trim)
                .orElse(fallback);
    }

    private double doubleSetting(String key, double fallback) {
        return globalSettingRepository.findByKey(key)
                .map(GlobalSetting::getStringValue)
                .map(value -> {
                    try {
                        return Double.parseDouble(value.trim());
                    } catch (Exception ex) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private String normalizeHexColor(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String trimmed = value.trim().toLowerCase();
        return trimmed.matches("^#[0-9a-f]{6}$") ? trimmed : fallback;
    }

    private void saveStringSetting(String key, String value) {
        GlobalSetting setting = globalSettingRepository.findByKey(key).orElseGet(() -> new GlobalSetting(key, value));
        setting.setStringValue(value);
        globalSettingRepository.saveAndFlush(setting);
    }

    private void saveBooleanSetting(String key, Boolean value) {
        boolean normalized = value != null && value;
        GlobalSetting setting = globalSettingRepository.findByKey(key)
                .orElseGet(() -> new GlobalSetting(key, String.valueOf(normalized)));
        setting.setStringValue(String.valueOf(normalized));
        globalSettingRepository.saveAndFlush(setting);
    }

    private void saveIntSetting(String key, int value) {
        GlobalSetting setting = globalSettingRepository.findByKey(key)
                .orElseGet(() -> new GlobalSetting(key, value));
        setting.setIntValue(value);
        globalSettingRepository.saveAndFlush(setting);
    }

    private void saveDoubleSetting(String key, double value) {
        String stringValue = String.format(java.util.Locale.US, "%.2f", value);
        GlobalSetting setting = globalSettingRepository.findByKey(key)
                .orElseGet(() -> new GlobalSetting(key, stringValue));
        setting.setStringValue(stringValue);
        globalSettingRepository.saveAndFlush(setting);
    }

    private String normalizeJsonText(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeText(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String defaultPastoralTemplate(String type) {
        return switch (type) {
            case "Absență" -> "Contactează persoana pentru a verifica dacă are nevoie de sprijin și notează următorul pas.";
            case "Copil absent" -> "Ia legătura cu părinții și verifică dacă există o nevoie practică sau pastorală.";
            case "Follow-up întârziat" -> "Reia contactul și actualizează statusul follow-up-ului după discuție.";
            case "Fără grup" -> "Verifică dacă persoana poate fi invitată într-un grup sau într-o slujire potrivită.";
            case "Persoană nouă" -> "Programează un prim contact și identifică un grup potrivit pentru integrare.";
            case "Zi de naștere" -> "Trimite un mesaj de felicitare și notează dacă apare o nevoie de follow-up.";
            default -> "Stabilește următorul pas pastoral și notează rezultatul.";
        };
    }

    private String normalizeUiTheme(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return "midnight";
        }
        return switch (normalized) {
            case "midnight", "forest", "ember", "violet", "softday", "clearblue" -> normalized;
            default -> "midnight";
        };
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRuntimeMode(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return RUNTIME_MODE_PROD;
        }
        normalized = normalized.toLowerCase();
        return RUNTIME_MODE_DEV.equals(normalized) ? RUNTIME_MODE_DEV : RUNTIME_MODE_PROD;
    }

    private String defaultReportsLayoutOrder() {
        return String.join(",",
                "attendance-period",
                "attendance-groups",
                "pastoral-follow-up",
                "small-group-health",
                "age-distribution",
                "people-categories",
                "key-moments",
                "events-status",
                "events-type",
                "finance");
    }

    private List<String> defaultReportSections() {
        return List.of(
                "attendance-period",
                "attendance-groups",
                "pastoral-follow-up",
                "small-group-health",
                "age-distribution",
                "people-categories",
                "key-moments",
                "events-status",
                "events-type",
                "finance");
    }

    private String normalizeReportsLayoutOrder(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return defaultReportsLayoutOrder();
        }
        java.util.LinkedHashSet<String> allowed = new java.util.LinkedHashSet<>(defaultReportSections());
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String token : normalized.split(",")) {
            String item = token == null ? "" : token.trim();
            if (allowed.contains(item)) {
                seen.add(item);
            }
        }
        for (String item : allowed) {
            seen.add(item);
        }
        return String.join(",", seen);
    }

    private boolean dbExists(Path dbFile) {
        return Files.exists(dbFile);
    }

    private void cloneDbFiles(Path fromBase, Path toBase) throws java.io.IOException {
        copyIfExists(fromBase, toBase);
    }

    private void copyIfExists(Path from, Path to) throws java.io.IOException {
        if (!Files.exists(from)) {
            return;
        }
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    public static class AppearanceForm {
        private String statusCustomization;
        private String priorityCustomization;
        private String nameCustomization;

        public AppearanceForm() {}

        public AppearanceForm(String statusCustomization, String priorityCustomization, String nameCustomization) {
            this.statusCustomization = statusCustomization;
            this.priorityCustomization = priorityCustomization;
            this.nameCustomization = nameCustomization;
        }

        public String getStatusCustomization() { return statusCustomization; }
        public void setStatusCustomization(String statusCustomization) { this.statusCustomization = statusCustomization; }
        public String getPriorityCustomization() { return priorityCustomization; }
        public void setPriorityCustomization(String priorityCustomization) { this.priorityCustomization = priorityCustomization; }
        public String getNameCustomization() { return nameCustomization; }
        public void setNameCustomization(String nameCustomization) { this.nameCustomization = nameCustomization; }
    }

    public static class ChurchSettingsForm {
        private Long defaultChurchId;
        private Long currentChurchId;
        private String currentChurchName;
        private String avatarUrl;
        private String address;
        private String pastorName;
        private String pastorPhone;
        private String secretaryName;
        private String secretaryPhone;
        private String treasurerName;
        private String treasurerPhone;

        public ChurchSettingsForm() {}

        public ChurchSettingsForm(Long defaultChurchId,
                                  Long currentChurchId, String currentChurchName, String avatarUrl, String address,
                                  String pastorName, String pastorPhone, String secretaryName, String secretaryPhone,
                                  String treasurerName, String treasurerPhone) {
            this.defaultChurchId = defaultChurchId;
            this.currentChurchId = currentChurchId;
            this.currentChurchName = currentChurchName;
            this.avatarUrl = avatarUrl;
            this.address = address;
            this.pastorName = pastorName;
            this.pastorPhone = pastorPhone;
            this.secretaryName = secretaryName;
            this.secretaryPhone = secretaryPhone;
            this.treasurerName = treasurerName;
            this.treasurerPhone = treasurerPhone;
        }

        public Long getDefaultChurchId() { return defaultChurchId; }
        public void setDefaultChurchId(Long defaultChurchId) { this.defaultChurchId = defaultChurchId; }
        public Long getCurrentChurchId() { return currentChurchId; }
        public void setCurrentChurchId(Long currentChurchId) { this.currentChurchId = currentChurchId; }
        public String getCurrentChurchName() { return currentChurchName; }
        public void setCurrentChurchName(String currentChurchName) { this.currentChurchName = currentChurchName; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPastorName() { return pastorName; }
        public void setPastorName(String pastorName) { this.pastorName = pastorName; }
        public String getPastorPhone() { return pastorPhone; }
        public void setPastorPhone(String pastorPhone) { this.pastorPhone = pastorPhone; }
        public String getSecretaryName() { return secretaryName; }
        public void setSecretaryName(String secretaryName) { this.secretaryName = secretaryName; }
        public String getSecretaryPhone() { return secretaryPhone; }
        public void setSecretaryPhone(String secretaryPhone) { this.secretaryPhone = secretaryPhone; }
        public String getTreasurerName() { return treasurerName; }
        public void setTreasurerName(String treasurerName) { this.treasurerName = treasurerName; }
        public String getTreasurerPhone() { return treasurerPhone; }
        public void setTreasurerPhone(String treasurerPhone) { this.treasurerPhone = treasurerPhone; }
    }

    public static class SystemSettingsForm {
        private Integer rowsPerPage;
        private Boolean eventTasksEnabled;
        private Boolean privateMode;
        private Boolean passwordRestrictionsEnabled;
        private Boolean passwordMinSixEnabled;
        private Boolean userCreateRequirePassword;

        public SystemSettingsForm() {}

        public SystemSettingsForm(Integer rowsPerPage,
                                  Boolean eventTasksEnabled,
                                  Boolean privateMode,
                                  Boolean passwordRestrictionsEnabled,
                                  Boolean passwordMinSixEnabled,
                                  Boolean userCreateRequirePassword) {
            this.rowsPerPage = rowsPerPage;
            this.eventTasksEnabled = eventTasksEnabled;
            this.privateMode = privateMode;
            this.passwordRestrictionsEnabled = passwordRestrictionsEnabled;
            this.passwordMinSixEnabled = passwordMinSixEnabled;
            this.userCreateRequirePassword = userCreateRequirePassword;
        }

        public Integer getRowsPerPage() { return rowsPerPage; }
        public void setRowsPerPage(Integer rowsPerPage) { this.rowsPerPage = rowsPerPage; }
        public Boolean getEventTasksEnabled() { return eventTasksEnabled; }
        public void setEventTasksEnabled(Boolean eventTasksEnabled) { this.eventTasksEnabled = eventTasksEnabled; }
        public Boolean getPrivateMode() { return privateMode; }
        public void setPrivateMode(Boolean privateMode) { this.privateMode = privateMode; }
        public Boolean getPasswordRestrictionsEnabled() { return passwordRestrictionsEnabled; }
        public void setPasswordRestrictionsEnabled(Boolean passwordRestrictionsEnabled) { this.passwordRestrictionsEnabled = passwordRestrictionsEnabled; }
        public Boolean getPasswordMinSixEnabled() { return passwordMinSixEnabled; }
        public void setPasswordMinSixEnabled(Boolean passwordMinSixEnabled) { this.passwordMinSixEnabled = passwordMinSixEnabled; }
        public Boolean getUserCreateRequirePassword() { return userCreateRequirePassword; }
        public void setUserCreateRequirePassword(Boolean userCreateRequirePassword) { this.userCreateRequirePassword = userCreateRequirePassword; }
    }

    public static class RuntimeModeForm {
        private String mode;
        private Boolean cloneFromCurrent;

        public RuntimeModeForm() {}

        public RuntimeModeForm(String mode, Boolean cloneFromCurrent) {
            this.mode = mode;
            this.cloneFromCurrent = cloneFromCurrent;
        }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public Boolean getCloneFromCurrent() { return cloneFromCurrent; }
        public void setCloneFromCurrent(Boolean cloneFromCurrent) { this.cloneFromCurrent = cloneFromCurrent; }
    }

    public static class PastoralSettingsForm {
        private Integer pastoralAbsenceCount;
        private Integer pastoralAnalysisDays;
        private Integer pastoralBirthdayWindowDays;
        private Integer pastoralSnoozeDays;
        private Integer pastoralMaxRecommendations;
        private Boolean pastoralEnableAbsence;
        private Boolean pastoralEnableChildAbsence;
        private Boolean pastoralEnableOverdueFollowUp;
        private Boolean pastoralEnableWithoutGroup;
        private Boolean pastoralEnableNewPerson;
        private Boolean pastoralEnableBirthday;
        private String pastoralTemplateAbsence;
        private String pastoralTemplateChildAbsence;
        private String pastoralTemplateOverdueFollowUp;
        private String pastoralTemplateWithoutGroup;
        private String pastoralTemplateNewPerson;
        private String pastoralTemplateBirthday;

        public PastoralSettingsForm() {}

        public PastoralSettingsForm(Integer pastoralAbsenceCount,
                                    Integer pastoralAnalysisDays,
                                    Integer pastoralBirthdayWindowDays,
                                    Integer pastoralSnoozeDays,
                                    Integer pastoralMaxRecommendations,
                                    Boolean pastoralEnableAbsence,
                                    Boolean pastoralEnableChildAbsence,
                                    Boolean pastoralEnableOverdueFollowUp,
                                    Boolean pastoralEnableWithoutGroup,
                                    Boolean pastoralEnableNewPerson,
                                    Boolean pastoralEnableBirthday,
                                    String pastoralTemplateAbsence,
                                    String pastoralTemplateChildAbsence,
                                    String pastoralTemplateOverdueFollowUp,
                                    String pastoralTemplateWithoutGroup,
                                    String pastoralTemplateNewPerson,
                                    String pastoralTemplateBirthday) {
            this.pastoralAbsenceCount = pastoralAbsenceCount;
            this.pastoralAnalysisDays = pastoralAnalysisDays;
            this.pastoralBirthdayWindowDays = pastoralBirthdayWindowDays;
            this.pastoralSnoozeDays = pastoralSnoozeDays;
            this.pastoralMaxRecommendations = pastoralMaxRecommendations;
            this.pastoralEnableAbsence = pastoralEnableAbsence;
            this.pastoralEnableChildAbsence = pastoralEnableChildAbsence;
            this.pastoralEnableOverdueFollowUp = pastoralEnableOverdueFollowUp;
            this.pastoralEnableWithoutGroup = pastoralEnableWithoutGroup;
            this.pastoralEnableNewPerson = pastoralEnableNewPerson;
            this.pastoralEnableBirthday = pastoralEnableBirthday;
            this.pastoralTemplateAbsence = pastoralTemplateAbsence;
            this.pastoralTemplateChildAbsence = pastoralTemplateChildAbsence;
            this.pastoralTemplateOverdueFollowUp = pastoralTemplateOverdueFollowUp;
            this.pastoralTemplateWithoutGroup = pastoralTemplateWithoutGroup;
            this.pastoralTemplateNewPerson = pastoralTemplateNewPerson;
            this.pastoralTemplateBirthday = pastoralTemplateBirthday;
        }

        public Integer getPastoralAbsenceCount() { return pastoralAbsenceCount; }
        public void setPastoralAbsenceCount(Integer pastoralAbsenceCount) { this.pastoralAbsenceCount = pastoralAbsenceCount; }
        public Integer getPastoralAnalysisDays() { return pastoralAnalysisDays; }
        public void setPastoralAnalysisDays(Integer pastoralAnalysisDays) { this.pastoralAnalysisDays = pastoralAnalysisDays; }
        public Integer getPastoralBirthdayWindowDays() { return pastoralBirthdayWindowDays; }
        public void setPastoralBirthdayWindowDays(Integer pastoralBirthdayWindowDays) { this.pastoralBirthdayWindowDays = pastoralBirthdayWindowDays; }
        public Integer getPastoralSnoozeDays() { return pastoralSnoozeDays; }
        public void setPastoralSnoozeDays(Integer pastoralSnoozeDays) { this.pastoralSnoozeDays = pastoralSnoozeDays; }
        public Integer getPastoralMaxRecommendations() { return pastoralMaxRecommendations; }
        public void setPastoralMaxRecommendations(Integer pastoralMaxRecommendations) { this.pastoralMaxRecommendations = pastoralMaxRecommendations; }
        public Boolean getPastoralEnableAbsence() { return pastoralEnableAbsence; }
        public void setPastoralEnableAbsence(Boolean pastoralEnableAbsence) { this.pastoralEnableAbsence = pastoralEnableAbsence; }
        public Boolean getPastoralEnableChildAbsence() { return pastoralEnableChildAbsence; }
        public void setPastoralEnableChildAbsence(Boolean pastoralEnableChildAbsence) { this.pastoralEnableChildAbsence = pastoralEnableChildAbsence; }
        public Boolean getPastoralEnableOverdueFollowUp() { return pastoralEnableOverdueFollowUp; }
        public void setPastoralEnableOverdueFollowUp(Boolean pastoralEnableOverdueFollowUp) { this.pastoralEnableOverdueFollowUp = pastoralEnableOverdueFollowUp; }
        public Boolean getPastoralEnableWithoutGroup() { return pastoralEnableWithoutGroup; }
        public void setPastoralEnableWithoutGroup(Boolean pastoralEnableWithoutGroup) { this.pastoralEnableWithoutGroup = pastoralEnableWithoutGroup; }
        public Boolean getPastoralEnableNewPerson() { return pastoralEnableNewPerson; }
        public void setPastoralEnableNewPerson(Boolean pastoralEnableNewPerson) { this.pastoralEnableNewPerson = pastoralEnableNewPerson; }
        public Boolean getPastoralEnableBirthday() { return pastoralEnableBirthday; }
        public void setPastoralEnableBirthday(Boolean pastoralEnableBirthday) { this.pastoralEnableBirthday = pastoralEnableBirthday; }
        public String getPastoralTemplateAbsence() { return pastoralTemplateAbsence; }
        public void setPastoralTemplateAbsence(String pastoralTemplateAbsence) { this.pastoralTemplateAbsence = pastoralTemplateAbsence; }
        public String getPastoralTemplateChildAbsence() { return pastoralTemplateChildAbsence; }
        public void setPastoralTemplateChildAbsence(String pastoralTemplateChildAbsence) { this.pastoralTemplateChildAbsence = pastoralTemplateChildAbsence; }
        public String getPastoralTemplateOverdueFollowUp() { return pastoralTemplateOverdueFollowUp; }
        public void setPastoralTemplateOverdueFollowUp(String pastoralTemplateOverdueFollowUp) { this.pastoralTemplateOverdueFollowUp = pastoralTemplateOverdueFollowUp; }
        public String getPastoralTemplateWithoutGroup() { return pastoralTemplateWithoutGroup; }
        public void setPastoralTemplateWithoutGroup(String pastoralTemplateWithoutGroup) { this.pastoralTemplateWithoutGroup = pastoralTemplateWithoutGroup; }
        public String getPastoralTemplateNewPerson() { return pastoralTemplateNewPerson; }
        public void setPastoralTemplateNewPerson(String pastoralTemplateNewPerson) { this.pastoralTemplateNewPerson = pastoralTemplateNewPerson; }
        public String getPastoralTemplateBirthday() { return pastoralTemplateBirthday; }
        public void setPastoralTemplateBirthday(String pastoralTemplateBirthday) { this.pastoralTemplateBirthday = pastoralTemplateBirthday; }
    }

    public static class ReportsOrderForm {
        private String layoutOrder;

        public ReportsOrderForm() {
        }

        public ReportsOrderForm(String layoutOrder) {
            this.layoutOrder = layoutOrder;
        }

        public String getLayoutOrder() {
            return layoutOrder;
        }

        public void setLayoutOrder(String layoutOrder) {
            this.layoutOrder = layoutOrder;
        }
    }
}
