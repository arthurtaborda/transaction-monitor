package com.arthurtaborda.transactionmonitor;

import com.arthurtaborda.transactionmonitor.repository.InMemTransactionRepository;
import com.arthurtaborda.transactionmonitor.repository.Transaction;
import com.arthurtaborda.transactionmonitor.repository.TransactionStatistics;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;

public class InMemTransactionRepositoryTest {

    private InMemTransactionRepository repository;

    @Before
    public void setUp() throws Exception {
        repository = new InMemTransactionRepository(Vertx.vertx());
    }

    @After
    public void tearDown() throws Exception {
        repository.stop();
    }

    private void addTransaction(int amount) {
        addTransaction(amount, currentTimeMillis());
    }

    private void addTransaction(int amount, long timestamp) {
        repository.addTransaction(new Transaction(amount, timestamp));
    }

    private void waitToGenerateStatistics() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(500);
    }

    @Test
    public void testConcurrency() throws Exception {
        Runnable add1000Transactions = () -> {
            IntStream.rangeClosed(1, 1000)
                     .forEach(i -> {
                         try {
                             TimeUnit.NANOSECONDS.sleep(1);
                             addTransaction(i);
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
                     });
        };
        allOf(runAsync(add1000Transactions),
              runAsync(add1000Transactions),
              runAsync(add1000Transactions),
              runAsync(add1000Transactions)).join();

        waitToGenerateStatistics();

        TransactionStatistics statistics = repository.getStatistics();
        assertThat(statistics.getCount()).isEqualTo(4000);
        assertThat(statistics.getSum()).isEqualTo(2002000);
        assertThat(statistics.getMax()).isEqualTo(1000);
        assertThat(statistics.getMin()).isEqualTo(1);
        assertThat(statistics.getAverage()).isEqualTo(500.5);
    }

    @Test
    public void whenAddTransactionWithMoreThan60Sec_doNotAdd() throws Exception {
        addTransaction(300, currentTimeMillis() - 61000);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(0);
    }

    @Test
    public void testMultipleGenerations() throws Exception {
        addTransaction(300);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(1);
        assertThat(repository.getStatistics().getSum()).isEqualTo(300);

        addTransaction(500);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(2);
        assertThat(repository.getStatistics().getSum()).isEqualTo(800);
    }

    @Test
    public void whenTransactionExpires_removeFromStatistics() throws Exception {
        addTransaction(300, currentTimeMillis() - 59400);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(1);
        assertThat(repository.getStatistics().getSum()).isEqualTo(300);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(0);
        assertThat(repository.getStatistics().getSum()).isEqualTo(0);
    }

    @Test
    public void whenAddTransactionWithLessThan60Sec_add() throws Exception {
        addTransaction(300);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(1);
    }

    @Test
    public void whenAddTransactionFromFuture_doNotAdd() throws Exception {
        addTransaction(300, currentTimeMillis() + 2000);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(0);
    }

    @Test
    public void testAverage() throws Exception {
        addTransaction(300);
        addTransaction(500);
        addTransaction(1000);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getAverage()).isEqualTo(600);
    }

    @Test
    public void testMax() throws Exception {
        addTransaction(300);
        addTransaction(500);
        addTransaction(1000);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getMax()).isEqualTo(1000);
    }

    @Test
    public void testMin() throws Exception {
        addTransaction(300);
        addTransaction(500);
        addTransaction(1000);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getMin()).isEqualTo(300);
    }

    @Test
    public void testSum() throws Exception {
        addTransaction(300);
        addTransaction(500);
        addTransaction(1000);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getSum()).isEqualTo(1800);
    }

    @Test
    public void testCount() throws Exception {
        addTransaction(300);
        addTransaction(500);
        addTransaction(1000);

        waitToGenerateStatistics();

        assertThat(repository.getStatistics().getCount()).isEqualTo(3);
    }
}