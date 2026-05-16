package ro.church_office.teamleaf.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "finance_transaction_scope")
public class TransactionChurchScope {

    @Id
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "church_id", nullable = false)
    private Long churchId;

    public TransactionChurchScope() {
    }

    public TransactionChurchScope(Long transactionId, Long churchId) {
        this.transactionId = transactionId;
        this.churchId = churchId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getChurchId() {
        return churchId;
    }

    public void setChurchId(Long churchId) {
        this.churchId = churchId;
    }
}
