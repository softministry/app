package ro.church_office.info.finance;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InMemoryTransactionService implements TransactionService {
    private final TransactionRepository transactionRepository;

    public InMemoryTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<Transaction> getAllTransactions(){
        return transactionRepository.findAll();
    }

    @Override
    public Transaction addTransaction(Transaction tx){
        return transactionRepository.save(tx);
    }

    @Override
    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }
}
