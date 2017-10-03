package com.arthurtaborda.transactionmonitor.repository;

import java.math.BigDecimal;
import java.util.DoubleSummaryStatistics;

import static java.math.RoundingMode.FLOOR;

public class TransactionStatistics {

    private final long count;
    private final double avg;
    private final double sum;
    private final double min;
    private final double max;

    public TransactionStatistics() {
        this.count = 0;
        this.avg = 0;
        this.sum = 0;
        this.min = 0;
        this.max = 0;
    }

    public TransactionStatistics(DoubleSummaryStatistics st) {
        this.count = st.getCount();
        this.avg = round(st.getAverage());
        this.sum = round(st.getSum());
        this.min = st.getMin() == Double.POSITIVE_INFINITY ? 0 : round(st.getMin());
        this.max = st.getMax() == Double.NEGATIVE_INFINITY ? 0 : round(st.getMax());
    }

    public long getCount() {
        return count;
    }

    public double getAverage() {
        return avg;
    }

    public double getSum() {
        return sum;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    private static double round(double val) {
        return new BigDecimal(val).setScale(2, FLOOR).doubleValue();
    }
}
