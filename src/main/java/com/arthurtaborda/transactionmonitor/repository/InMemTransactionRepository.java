package com.arthurtaborda.transactionmonitor.repository;

import io.vertx.core.Vertx;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.concurrent.locks.StampedLock;

import static org.slf4j.LoggerFactory.getLogger;

public class InMemTransactionRepository implements TransactionRepository {

    private static final Logger LOGGER = getLogger(InMemTransactionRepository.class.getName());

    private static final int TIMER_INTERVAL_MS = 400;

    private final StampedLock statisticsLock;
    private final StampedLock transactionsLock;

    private final Vertx vertx;
    private final TransactionStatistics statistics;

    private long timer;
    private Collection<Transaction> transactions;

    public InMemTransactionRepository(Vertx vertx) {
        this.statisticsLock = new StampedLock();
        this.transactionsLock = new StampedLock();

        this.vertx = vertx;
        this.transactions = new LinkedList<>();
        this.statistics = new TransactionStatistics(0, 0, 0, 0, 0);

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
        boolean happenedInLastSecond = transaction.happenedInLastSecond();
        if (happenedInLastSecond) {
            LOGGER.debug("Add transaction");
            long writeLock = transactionsLock.writeLock();
            try {
                transactions.add(transaction);
            } finally {
                transactionsLock.unlockWrite(writeLock);
            }
        }

        return happenedInLastSecond;
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

        DoubleSummaryStatistics st;
        long readLock = transactionsLock.readLock();
        try {
            st = transactions.stream()
                             .mapToDouble(Transaction::getAmount)
                             .summaryStatistics();
        } finally {
            transactionsLock.unlockRead(readLock);
        }

        long writeLock = statisticsLock.writeLock();
        try {
            statistics.setStats(st);
        } finally {
            statisticsLock.unlockWrite(writeLock);
        }
    }

    private void removeOld() {
        long writeLock = transactionsLock.writeLock();
        try {
            transactions.removeIf(t -> !t.happenedInLastSecond());
        } finally {
            transactionsLock.unlockWrite(writeLock);
        }
    }
}
