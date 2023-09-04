package com.demo;

import java.util.List;

public class SummaryData {
    long start_time;
    long end_time;
    List<Double> asks_interval;
    List<Double> bids_interval;
    double average_price_b;
    double average_price_a;
    double summary_count_b;
    double summary_count_a;
    double summary_amount_b;
    double summary_amount_a;

    public SummaryData(long start_time, long end_time, List<Double> asks_interval, List<Double> bids_interval,
                       double average_price_b, double average_price_a,
                       double summary_count_b, double summary_count_a,
                       double summary_amount_b, double summary_amount_a) {
        this.start_time = start_time;
        this.end_time = end_time;
        this.asks_interval = asks_interval;
        this.bids_interval = bids_interval;
        this.average_price_b = average_price_b;
        this.average_price_a = average_price_a;
        this.summary_count_b = summary_count_b;
        this.summary_count_a = summary_count_a;
        this.summary_amount_b = summary_amount_b;
        this.summary_amount_a = summary_amount_a;
    }
}
