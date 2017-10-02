package com.arthurtaborda.transactionmonitor;

import java.util.DoubleSummaryStatistics;
import java.util.LongSummaryStatistics;

public interface TransactionRepository {

    /**
     * @param transaction
     * @return <tt>true</tt> if the transaction was added
     */
    boolean addTransaction(Transaction transaction);

    /**
     * @return Statistics about the transactions from the last 60 seconds
     */
    DoubleSummaryStatistics getStatistics();
}
