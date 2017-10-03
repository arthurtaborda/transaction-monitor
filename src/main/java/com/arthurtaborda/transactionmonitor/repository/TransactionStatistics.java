package com.arthurtaborda.transactionmonitor.repository;

import java.math.BigDecimal;
import java.util.DoubleSummaryStatistics;

import static java.math.RoundingMode.FLOOR;

public class TransactionStatistics {

    private long count;
    private double avg;
    private double sum;
    private double min;
    private double max;

    public TransactionStatistics(DoubleSummaryStatistics st) {
        this(st.getCount(),
             round(st.getAverage()),
             round(st.getSum()),
             st.getMin() == Double.POSITIVE_INFINITY ? 0 : round(st.getMin()),
             st.getMax() == Double.NEGATIVE_INFINITY ? 0 : round(st.getMax()));
    }

    TransactionStatistics(long count, double avg, double sum, double min, double max) {
        this.count = count;
        this.avg = avg;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    void setStats(DoubleSummaryStatistics st) {
        count = st.getCount();
        avg = round(st.getAverage());
        sum = round(st.getSum());
        min = st.getMin() == Double.POSITIVE_INFINITY ? 0 : round(st.getMin());
        max = st.getMax() == Double.NEGATIVE_INFINITY ? 0 : round(st.getMax());
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
