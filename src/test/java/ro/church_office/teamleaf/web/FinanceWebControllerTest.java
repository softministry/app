package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.finance.Transaction;
import ro.church_office.info.finance.TransactionService;
import ro.church_office.teamleaf.finance.TransactionChurchScopeService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceWebControllerTest {

    @Test
    void indexBuildsBalancesAndNormalizesType() {
        TransactionService service = mock(TransactionService.class);
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        TransactionChurchScopeService transactionChurchScopeService = mock(TransactionChurchScopeService.class);
        FinanceWebController controller = new FinanceWebController(service, churchContextService, transactionChurchScopeService);

        Transaction t1 = new Transaction();
        t1.setId(1L);
        t1.setType("income");
        t1.setAmount(150.0);

        Transaction t2 = new Transaction();
        t2.setId(2L);
        t2.setType("Venituri");
        t2.setAmount(50.0);

        Transaction t3 = new Transaction();
        t3.setId(3L);
        t3.setType("expense");
        t3.setAmount(40.0);

        when(service.getAllTransactions()).thenReturn(List.of(t1, t2, t3));
        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        when(transactionChurchScopeService.filterForChurch(any(), any())).thenReturn(List.of(t1, t2, t3));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.index(model);

        assertEquals("finance/index", view);
        assertTrue((Boolean) model.get("hasTransactions"));
        assertEquals(200.0, (Double) model.get("totalIncome"));
        assertEquals(40.0, (Double) model.get("totalExpenses"));
        assertEquals(160.0, (Double) model.get("currentBalance"));
        assertNotNull(model.get("transactionForm"));
    }

    @Test
    void createRejectsInvalidAmount() {
        TransactionService service = mock(TransactionService.class);
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        TransactionChurchScopeService transactionChurchScopeService = mock(TransactionChurchScopeService.class);
        FinanceWebController controller = new FinanceWebController(service, churchContextService, transactionChurchScopeService);
        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);

        FinanceWebController.TransactionForm form = new FinanceWebController.TransactionForm();
        form.setType("income");
        form.setName("Oferta");
        form.setDescription("desc");
        form.setAmount(0.0);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.create(form, redirect);

        assertEquals("redirect:/finance", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        assertNotNull(redirect.getFlashAttributes().get("transactionForm"));
    }

    @Test
    void createPersistsWhenValid() {
        TransactionService service = mock(TransactionService.class);
        ChurchContextService churchContextService = mock(ChurchContextService.class);
        TransactionChurchScopeService transactionChurchScopeService = mock(TransactionChurchScopeService.class);
        FinanceWebController controller = new FinanceWebController(service, churchContextService, transactionChurchScopeService);
        when(churchContextService.getOrCreateActiveChurchId()).thenReturn(1L);
        Transaction saved = new Transaction();
        saved.setId(99L);
        when(service.addTransaction(any(Transaction.class))).thenReturn(saved);

        FinanceWebController.TransactionForm form = new FinanceWebController.TransactionForm();
        form.setType("intrare");
        form.setName("  Colecta ");
        form.setDescription(" d ");
        form.setAmount(10.0);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.create(form, redirect);

        assertEquals("redirect:/finance", view);
        assertNotNull(redirect.getFlashAttributes().get("success"));
        verify(service).addTransaction(any(Transaction.class));
    }
}
