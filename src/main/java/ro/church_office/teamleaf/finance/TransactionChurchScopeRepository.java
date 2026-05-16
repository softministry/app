package ro.church_office.teamleaf.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionChurchScopeRepository extends JpaRepository<TransactionChurchScope, Long> {
    List<TransactionChurchScope> findAllByChurchId(Long churchId);
    List<TransactionChurchScope> findAllByTransactionIdIn(Collection<Long> transactionIds);
    Optional<TransactionChurchScope> findByTransactionId(Long transactionId);
}
