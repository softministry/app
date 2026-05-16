package ro.church_office.teamleaf.web;

import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.church.DTO.ChurchInfoDTO;

@Controller
@RequestMapping("/church")
public class ChurchWebController {

    private static final long MAX_AVATAR_BYTES = 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private final ChurchInfoService churchInfoService;
    private final String uploadsDir;

    public ChurchWebController(ChurchInfoService churchInfoService,
                               @Value("${ministryadmin.desktop.uploads-dir:uploads}") String uploadsDir) {
        this.churchInfoService = churchInfoService;
        this.uploadsDir = uploadsDir;
    }

    @GetMapping({"", "/"})
    public String view(Model model) {
        ChurchInfoDTO church = churchInfoService.get().orElse(new ChurchInfoDTO());
        model.addAttribute("church", church);
        model.addAttribute("avatarEnabled", true);
        return "church/view";
    }

    @GetMapping("/edit")
    public String edit(Model model) {
        ChurchInfoDTO dto = churchInfoService.get().orElse(new ChurchInfoDTO());
        model.addAttribute("churchForm", ChurchForm.fromDto(dto));
        model.addAttribute("avatarEnabled", true);
        return "church/form";
    }

    @GetMapping("/new")
    public String add(Model model) {
        model.addAttribute("churchForm", new ChurchForm());
        model.addAttribute("avatarEnabled", true);
        return "church/new";
    }

    @PostMapping({"", "/"})
    public String save(@ModelAttribute("churchForm") ChurchForm form,
                       RedirectAttributes redirectAttributes) {
        try {
            ChurchInfoDTO dto = form.toDto();
            churchInfoService.save(dto);
            redirectAttributes.addFlashAttribute("success", "Informațiile bisericii au fost salvate.");
            return "redirect:/church";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-au putut salva datele bisericii." : ex.getMessage());
            return "redirect:/church/edit";
        }
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("churchForm") ChurchForm form,
                         RedirectAttributes redirectAttributes) {
        try {
            String name = form.getName() == null ? null : form.getName().trim();
            if (name == null || name.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Introdu numele bisericii.");
                return "redirect:/church/new";
            }
            form.setName(name);
            ChurchInfoDTO dto = form.toDto();
            churchInfoService.create(dto);
            redirectAttributes.addFlashAttribute("success", "Biserica a fost adăugată.");
            return "redirect:/settings/church";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut adăuga biserica." : ex.getMessage());
            return "redirect:/church/new";
        }
    }

    @PostMapping("/avatar-upload")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = storeAvatar(file);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Nu am putut salva imaginea."));
        }
    }

    @PostMapping("/select")
    public String selectChurch(@RequestParam("churchId") Long churchId,
                               @RequestParam(value = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
        try {
            churchInfoService.setDefaultChurchId(churchId);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Nu s-a putut schimba biserica activă." : ex.getMessage());
        }

        if (redirect != null && redirect.startsWith("/")) {
            return "redirect:" + redirect;
        }
        return "redirect:/dashboard";
    }

    private String storeAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Alege o imagine pentru avatar.");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Imaginea este prea mare. Maxim 1 MB.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Format invalid. Folosește PNG, JPG, WEBP sau GIF.");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };

        Long churchId = churchInfoService.getDefaultChurchId();
        String filename = "church_" + (churchId == null ? "active" : churchId) + "_" + UUID.randomUUID() + extension;
        Path uploadDir = Paths.get(uploadsDir, "church").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        Path destination = uploadDir.resolve(filename).normalize();
        if (!destination.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Nume fișier invalid.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination);
        }
        return "/uploads/church/" + filename;
    }

    public static class ChurchForm {
        private Long id;
        private String name;
        private String avatarUrl;
        private String address;
        private String pastorName;
        private String pastorPhone;
        private String secretaryName;
        private String secretaryPhone;
        private String treasurerName;
        private String treasurerPhone;

        static ChurchForm fromDto(ChurchInfoDTO dto) {
            ChurchForm form = new ChurchForm();
            form.id = dto.id;
            form.name = dto.name;
            form.avatarUrl = dto.avatarUrl;
            form.address = dto.address;
            form.pastorName = dto.pastorName;
            form.pastorPhone = dto.pastorPhone;
            form.secretaryName = dto.secretaryName;
            form.secretaryPhone = dto.secretaryPhone;
            form.treasurerName = dto.treasurerName;
            form.treasurerPhone = dto.treasurerPhone;
            return form;
        }

        ChurchInfoDTO toDto() {
            ChurchInfoDTO dto = new ChurchInfoDTO();
            dto.id = this.id;
            dto.name = this.name;
            dto.avatarUrl = this.avatarUrl;
            dto.address = this.address;
            dto.pastorName = this.pastorName;
            dto.pastorPhone = this.pastorPhone;
            dto.secretaryName = this.secretaryName;
            dto.secretaryPhone = this.secretaryPhone;
            dto.treasurerName = this.treasurerName;
            dto.treasurerPhone = this.treasurerPhone;
            return dto;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
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
}
