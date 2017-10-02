package com.arthurtaborda.transactionmonitor;

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

    private final Vertx vertx;
    private final StampedLock lock;

    private long timer;
    private Collection<Transaction> transactions;
    private DoubleSummaryStatistics statistics;

    public InMemTransactionRepository(Vertx vertx) {
        this.lock = new StampedLock();
        this.vertx = vertx;
        this.transactions = new LinkedList<>();
        this.statistics = new DoubleSummaryStatistics();

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
            long writeLock = lock.writeLock();
            try {
                transactions.add(transaction);
            } finally {
                lock.unlockWrite(writeLock);
            }
        }

        return happenedInLastSecond;
    }

    @Override
    public DoubleSummaryStatistics getStatistics() {
        LOGGER.debug("Get statistics");
        return statistics;
    }

    private void generateStatistics() {
        LOGGER.debug("Generate statistics");
        long readLock = lock.readLock();
        try {
            statistics = transactions.stream()
                                     .mapToDouble(Transaction::getAmount)
                                     .summaryStatistics();
        } finally {
            lock.unlockRead(readLock);
        }
    }

    private void removeOld() {
        long writeLock = lock.writeLock();
        try {
            transactions.removeIf(t -> !t.happenedInLastSecond());
        } finally {
            lock.unlockWrite(writeLock);
        }
    }
}
