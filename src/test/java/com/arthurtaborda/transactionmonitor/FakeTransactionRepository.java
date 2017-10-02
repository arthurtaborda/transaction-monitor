package com.arthurtaborda.transactionmonitor;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;

public class FakeTransactionRepository implements TransactionRepository {

    private final Collection<Transaction> transactions;

    public FakeTransactionRepository() {
        transactions = new LinkedList<>();
    }

    @Override
    public boolean addTransaction(Transaction transaction) {
        boolean happenedInLastSecond = transaction.happenedInLastSecond();
        if (happenedInLastSecond) {
            transactions.add(transaction);
        }
        return happenedInLastSecond;
    }

    @Override
    public DoubleSummaryStatistics getStatistics() {
        return transactions.stream()
                           .filter(Transaction::happenedInLastSecond)
                           .mapToDouble(Transaction::getAmount)
                           .summaryStatistics();
    }

    public void clear() {
        transactions.clear();
    }
}
