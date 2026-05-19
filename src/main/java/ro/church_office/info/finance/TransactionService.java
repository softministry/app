package ro.church_office.info.finance;

import java.util.List;

public interface TransactionService {
    default List<Transaction> findAll(Long churchId) { return List.of(); }
    default Transaction save(Transaction tx, Long churchId) { return tx; }
    default void delete(Long id, Long churchId) {}
    default List<Transaction> findTransactionsByChurch(Long churchId) { return List.of(); }
    default List<Transaction> getAllTransactions(){ return List.of(); }
    default Transaction addTransaction(Transaction tx){ return tx; }
    default void deleteTransaction(Long id) {}
}
