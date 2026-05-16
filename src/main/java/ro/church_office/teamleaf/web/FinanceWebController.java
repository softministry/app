package ro.church_office.teamleaf.web;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.finance.Transaction;
import ro.church_office.info.finance.TransactionService;
import ro.church_office.teamleaf.finance.TransactionChurchScopeService;

@Controller
@RequestMapping("/finance")
public class FinanceWebController {

    private final TransactionService transactionService;
    private final ChurchContextService churchContextService;
    private final TransactionChurchScopeService transactionChurchScopeService;

    public FinanceWebController(TransactionService transactionService,
                                ChurchContextService churchContextService,
                                TransactionChurchScopeService transactionChurchScopeService) {
        this.transactionService = transactionService;
        this.churchContextService = churchContextService;
        this.transactionChurchScopeService = transactionChurchScopeService;
    }

    @GetMapping
    public String index(Model model) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        List<Transaction> allTransactions = transactionService.getAllTransactions().stream()
                .sorted(Comparator.comparing(Transaction::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::normalizeTransaction)
                .toList();
        transactionChurchScopeService.ensureLegacyTransactionsAssigned(allTransactions, churchId);
        List<Transaction> transactions = transactionChurchScopeService.filterForChurch(allTransactions, churchId);

        double totalIncome = transactions.stream()
                .filter(transaction -> "income".equals(transaction.getType()))
                .map(Transaction::getAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        double totalExpenses = transactions.stream()
                .filter(transaction -> "expense".equals(transaction.getType()))
                .map(Transaction::getAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        model.addAttribute("transactions", transactions);
        model.addAttribute("hasTransactions", !transactions.isEmpty());
        model.addAttribute("currentBalance", totalIncome - totalExpenses);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpenses", totalExpenses);
        if (!model.containsAttribute("transactionForm")) {
            model.addAttribute("transactionForm", new TransactionForm());
        }
        return "finance/index";
    }

    @PostMapping
    public String create(@ModelAttribute("transactionForm") TransactionForm form,
                         RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        try {
            Transaction transaction = new Transaction();
            transaction.setType(normalizeType(form.getType()));
            transaction.setName(requiredText(form.getName(), "Numele tranzacției este obligatoriu."));
            transaction.setDescription(blankToEmpty(form.getDescription()));
            transaction.setAmount(requiredAmount(form.getAmount()));
            Transaction saved = transactionService.addTransaction(transaction);
            transactionChurchScopeService.assignToChurch(saved.getId(), churchId);
            redirectAttributes.addFlashAttribute("success", "Tranzacția a fost adăugată.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", messageOf(ex, "Nu s-a putut adăuga tranzacția."));
            redirectAttributes.addFlashAttribute("transactionForm", form);
        }
        return "redirect:/finance";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long churchId = churchContextService.getOrCreateActiveChurchId();
        try {
            if (!transactionChurchScopeService.belongsToChurch(id, churchId)) {
                redirectAttributes.addFlashAttribute("error", "Tranzacția nu aparține bisericii active.");
                return "redirect:/finance";
            }
            transactionService.deleteTransaction(id);
            transactionChurchScopeService.removeScope(id);
            redirectAttributes.addFlashAttribute("success", "Tranzacția a fost ștearsă.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", messageOf(ex, "Nu s-a putut șterge tranzacția."));
        }
        return "redirect:/finance";
    }

    private Transaction normalizeTransaction(Transaction transaction) {
        transaction.setType(normalizeType(transaction.getType()));
        return transaction;
    }

    private String normalizeType(String value) {
        if (value == null) {
            return "expense";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "expense";
        }
        if (normalized.equals("income")
                || normalized.equals("in")
                || normalized.equals("intrare")
                || normalized.equals("venit")
                || normalized.equals("venituri")
                || normalized.equals("1")
                || normalized.equals("true")
                || normalized.contains("inc")) {
            return "income";
        }
        return "expense";
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private Double requiredAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Introdu o sumă validă.");
        }
        return amount;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String messageOf(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
    }

    public static class TransactionForm {
        private String type;
        private String name;
        private String description;
        private Double amount;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }
    }
}
