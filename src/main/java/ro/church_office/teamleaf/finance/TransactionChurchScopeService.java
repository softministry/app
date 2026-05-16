package ro.church_office.teamleaf.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.church_office.info.church.ChurchInfoService;
import ro.church_office.info.finance.Transaction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionChurchScopeService {

    private final TransactionChurchScopeRepository scopeRepository;
    private final ChurchInfoService churchInfoService;

    public TransactionChurchScopeService(TransactionChurchScopeRepository scopeRepository,
                                         ChurchInfoService churchInfoService) {
        this.scopeRepository = scopeRepository;
        this.churchInfoService = churchInfoService;
    }

    @Transactional
    public void assignToChurch(Long transactionId, Long churchId) {
        if (transactionId == null || churchId == null) {
            return;
        }
        TransactionChurchScope scope = scopeRepository.findByTransactionId(transactionId)
                .orElseGet(() -> new TransactionChurchScope(transactionId, churchId));
        scope.setChurchId(churchId);
        scopeRepository.save(scope);
    }

    @Transactional
    public void ensureLegacyTransactionsAssigned(List<Transaction> transactions, Long activeChurchId) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        Long fallbackChurchId = churchInfoService.getDefaultChurchId();
        if (fallbackChurchId == null) {
            fallbackChurchId = activeChurchId;
        }
        if (fallbackChurchId == null) {
            return;
        }

        List<Long> ids = transactions.stream()
                .map(Transaction::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return;
        }

        Set<Long> assigned = scopeRepository.findAllByTransactionIdIn(ids).stream()
                .map(TransactionChurchScope::getTransactionId)
                .collect(Collectors.toSet());

        for (Long id : ids) {
            if (!assigned.contains(id)) {
                scopeRepository.save(new TransactionChurchScope(id, fallbackChurchId));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Transaction> filterForChurch(List<Transaction> allTransactions, Long churchId) {
        if (churchId == null || allTransactions == null || allTransactions.isEmpty()) {
            return List.of();
        }
        List<Long> transactionIds = scopeRepository.findAllByChurchId(churchId).stream()
                .map(TransactionChurchScope::getTransactionId)
                .toList();
        if (transactionIds.isEmpty()) {
            return List.of();
        }
        Set<Long> allowed = Set.copyOf(transactionIds);
        return allTransactions.stream()
                .filter(transaction -> transaction != null && transaction.getId() != null && allowed.contains(transaction.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean belongsToChurch(Long transactionId, Long churchId) {
        if (transactionId == null || churchId == null) {
            return false;
        }
        return scopeRepository.findByTransactionId(transactionId)
                .map(scope -> Objects.equals(scope.getChurchId(), churchId))
                .orElse(false);
    }

    @Transactional
    public void removeScope(Long transactionId) {
        if (transactionId == null) {
            return;
        }
        scopeRepository.deleteById(transactionId);
    }
}
