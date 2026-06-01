package ro.church_office.teamleaf.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.users.DAO.GlobalSettingRepository;
import ro.church_office.info.visits.VisitDTO;
import ro.church_office.info.visits.VisitService;
import ro.church_office.info.visits.dao.Visit;

@Controller
@RequestMapping("/visits")
public class VisitWebController {

    private static final String ROWS_KEY = "rows_per_page";

    private final ChurchContextService churchContextService;
    private final VisitService visitService;
    private final GlobalSettingRepository globalSettingRepository;

    public VisitWebController(ChurchContextService churchContextService,
                              VisitService visitService,
                              GlobalSettingRepository globalSettingRepository) {
        this.churchContextService = churchContextService;
        this.visitService = visitService;
        this.globalSettingRepository = globalSettingRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "size", required = false) Integer size,
                       Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        List<VisitDTO> allVisits = visitService.getAllVisits(churchId).stream()
                .map(VisitDTO::new)
                .toList();

        int defaultSize = defaultRowsPerPage();
        int normalizedSize = normalizeSize(size == null ? defaultSize : size);
        int totalItems = allVisits.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / normalizedSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * normalizedSize, totalItems);
        int toIndex = Math.min(fromIndex + normalizedSize, totalItems);

        model.addAttribute("visits", allVisits.subList(fromIndex, toIndex));
        model.addAttribute("page", currentPage);
        model.addAttribute("size", normalizedSize);
        model.addAttribute("defaultSize", defaultSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSizes", List.of(5, 7, 10, 20, 25, 50, 100));
        return "visits/list";
    }

    private int normalizeSize(int size) {
        return switch (size) {
            case 5, 7, 10, 20, 25, 50, 100 -> size;
            default -> 7;
        };
    }

    private int defaultRowsPerPage() {
        return normalizeSize(globalSettingRepository.findBySettingKey(ROWS_KEY)
                .map(setting -> setting.getIntValue())
                .orElse(7));
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        VisitDTO dto = new VisitDTO();
        dto.setVisitDate(LocalDate.now());
        model.addAttribute("visitDto", dto);
        return "visits/form";
    }

    @PostMapping
    public String create(@ModelAttribute("visitDto") VisitDTO dto,
                         RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Visit visit = dto.toEntity();
        visit.setChurchId(churchId);
        visitService.saveVisit(visit);
        redirectAttributes.addFlashAttribute("success", "Vizita a fost adăugată.");
        return "redirect:/visits";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Visit visit = visitService.getVisitById(id, churchId);
        if (visit == null) {
            redirectAttributes.addFlashAttribute("error", "Vizita nu a putut fi găsită.");
            return "redirect:/visits";
        }
        VisitDTO dto = new VisitDTO(visit);
        if (dto.getVisitDate() == null) {
            dto.setVisitDate(LocalDate.now());
        }
        model.addAttribute("visitDto", dto);
        return "visits/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("visitDto") VisitDTO dto,
                         RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        Visit existing = visitService.getVisitById(id, churchId);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Vizita nu a putut fi găsită.");
            return "redirect:/visits";
        }
        existing.setPersonName(dto.getPersonName());
        existing.setAddress(dto.getAddress());
        existing.setPhone(dto.getPhone());
        existing.setVisitDate(dto.getVisitDate());
        existing.setNotes(dto.getNotes());
        visitService.saveVisit(existing);
        redirectAttributes.addFlashAttribute("success", "Modificările au fost salvate.");
        return "redirect:/visits";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        visitService.deleteVisit(id, churchId);
        redirectAttributes.addFlashAttribute("success", "Vizita a fost ștearsă.");
        return "redirect:/visits";
    }
}
