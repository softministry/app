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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.visits.VisitDTO;
import ro.church_office.info.visits.VisitService;
import ro.church_office.info.visits.dao.Visit;

@Controller
@RequestMapping("/visits")
public class VisitWebController {

    private final ChurchContextService churchContextService;
    private final VisitService visitService;

    public VisitWebController(ChurchContextService churchContextService,
                              VisitService visitService) {
        this.churchContextService = churchContextService;
        this.visitService = visitService;
    }

    @GetMapping
    public String list(Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        List<VisitDTO> visits = visitService.getAllVisits(churchId).stream()
                .map(VisitDTO::new)
                .toList();
        model.addAttribute("visits", visits);
        return "visits/list";
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
        model.addAttribute("visitDto", new VisitDTO(visit));
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
