package com.arthurtaborda.transactionmonitor.repository;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;

public class FakeTransactionRepository implements TransactionRepository {

    private Collection<Transaction> transactions;

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
    public TransactionStatistics getStatistics() {
        DoubleSummaryStatistics st = transactions.stream()
                                                 .filter(Transaction::happenedInLastSecond)
                                                 .mapToDouble(Transaction::getAmount)
                                                 .summaryStatistics();
        return new TransactionStatistics(st);
    }

    public void clear() {
        transactions = new LinkedList<>();
    }
}
