package com.arthurtaborda.transactionmonitor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.lang.System.currentTimeMillis;

public class Transaction {

    private final double amount;
    private final long timestamp;

    public Transaction(double amount, long timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean happenedInLastSecond() {
        return currentTimeMillis() - timestamp < 60000;
    }
}
