package com.arthurtaborda.transactionmonitor.repository;

import io.vertx.core.Vertx;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.locks.StampedLock;

import static org.slf4j.LoggerFactory.getLogger;

public class InMemTransactionRepository implements TransactionRepository {

    private static final Logger LOGGER = getLogger(InMemTransactionRepository.class.getName());

    private static final int TIMER_INTERVAL_MS = 400;

    private final StampedLock statisticsLock;
    private final StampedLock transactionsLock;

    private final Vertx vertx;

    private TransactionStatistics statistics;
    private long timer;
    private Collection<Transaction> transactions;

    public InMemTransactionRepository(Vertx vertx) {
        this.statisticsLock = new StampedLock();
        this.transactionsLock = new StampedLock();

        this.vertx = vertx;
        this.transactions = new LinkedList<>();
        this.statistics = new TransactionStatistics();

        setTimer();
    }

    /**
     * Stops any timer that might still be running
     */
    public void stop() {
        vertx.cancelTimer(timer);
    }

    private void setTimer() {
        timer = vertx.setTimer(TIMER_INTERVAL_MS, delay -> vertx.executeBlocking(future -> {
            removeOld();
            generateStatistics();
            future.complete();
        }, result -> setTimer()));
    }

    @Override
    public boolean addTransaction(Transaction transaction) {
        boolean happenedInLastMinute = transaction.happenedInLastMinute();
        if (happenedInLastMinute) {
            LOGGER.debug("Add transaction");
            long writeLock = transactionsLock.writeLock();
            try {
                transactions.add(transaction);
            } finally {
                transactionsLock.unlockWrite(writeLock);
            }
        }

        return happenedInLastMinute;
    }

    @Override
    public TransactionStatistics getStatistics() {
        LOGGER.debug("Get statistics");
        long readLock = statisticsLock.readLock();
        try {
            return statistics;
        } finally {
            statisticsLock.unlockRead(readLock);
        }
    }

    private void generateStatistics() {
        LOGGER.debug("Generate statistics");

        long writeLock = statisticsLock.writeLock();
        long readLock = transactionsLock.readLock();
        try {
            statistics = new TransactionStatistics(transactions.stream()
                                                               .mapToDouble(Transaction::getAmount)
                                                               .summaryStatistics());
        } finally {
            statisticsLock.unlockWrite(writeLock);
            transactionsLock.unlockRead(readLock);
        }
    }

    private void removeOld() {
        long writeLock = transactionsLock.writeLock();
        try {
            transactions.removeIf(t -> !t.happenedInLastMinute());
        } finally {
            transactionsLock.unlockWrite(writeLock);
        }
    }
}
