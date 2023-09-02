package com.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DataManager {

    private final MessageSenderService messageSenderService;

    public DataManager(MessageSenderService messageSenderService) {
        this.messageSenderService = messageSenderService;
    }

    public void prepareData(long startTime,
                              long endTime,
                              int numberOfBookParts,
                              double minPriceOrderBuy,
                              double maxPriceOrderBuy,
                              int intervalMinutes,
                              OrderBookManager orderBookManager){
        OrderBookSnapshot orderBookSnapshot = null;
        try {
            while (startTime < endTime) {
                orderBookSnapshot = orderBookManager.collectData(startTime, startTime + intervalMinutes);
                if (orderBookSnapshot != null) {
                    List<OrderBookEvent.PriceQuantityPair> bids = orderBookSnapshot.getBids();
                    List<OrderBookEvent.PriceQuantityPair> asks = orderBookSnapshot.getAsks();
                    // Prepare the JSON object
                    Gson gson = new Gson();
                    String json = gson.toJson(new SummaryData(startTime, startTime + intervalMinutes,
                            getAsksInterval(asks, maxPriceOrderBuy, numberOfBookParts),
                            getBidsInterval(bids, minPriceOrderBuy, numberOfBookParts),
                            calculateAverage(bids),
                            calculateAverage(asks),
                            calculateTotalQuantity(bids),
                            calculateTotalQuantity(asks)));

                    System.out.println(json);
                    messageSenderService.sendMessage(json);
                }

                startTime += intervalMinutes;
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    public static List<Double> getBidsInterval(List<OrderBookEvent.PriceQuantityPair> bids, double minPriceOrderBuy, int numberOfBookParts) {
        List<Double> result = new ArrayList<>();

        // Convert price strings to doubles and find minimum bid price
        List<Double> bidPrices = new ArrayList<>();
        for (OrderBookEvent.PriceQuantityPair bid : bids) {
            bidPrices.add(Double.parseDouble(bid.getPrice()));
        }
        double minimumOfBids = Collections.min(bidPrices);

        // Calculate minBids and stepForBids
        double minBids = minimumOfBids + minimumOfBids * minPriceOrderBuy;
        double stepForBids = (minBids - minimumOfBids) / numberOfBookParts;

        // Generate the list with values
        double currentValue = minBids;
        for (int i = 0; i < numberOfBookParts; i++) {
            result.add(currentValue);
            currentValue += stepForBids;
        }

        return result;
    }


    public static List<Double> getAsksInterval(List<OrderBookEvent.PriceQuantityPair> asks, double maxPriceOrderBuy, int numberOfBookParts) {
        List<Double> result = new ArrayList<>();

        // Convert price strings to doubles and find maximum ask price
        List<Double> askPrices = new ArrayList<>();
        for (OrderBookEvent.PriceQuantityPair ask : asks) {
            askPrices.add(Double.parseDouble(ask.getPrice()));
        }
        double maximumOfAsks = Collections.max(askPrices);

        // Calculate maxAsks and stepForAsks
        double maxAsks = maximumOfAsks - maximumOfAsks * maxPriceOrderBuy;
        double stepForAsks = (maximumOfAsks - maxAsks) / numberOfBookParts;

        // Generate the list with values
        double currentValue = maxAsks;
        for (int i = 0; i < numberOfBookParts; i++) {
            result.add(currentValue);
            currentValue += stepForAsks;
        }

        return result;
    }

    public static double calculateAverage(List<OrderBookEvent.PriceQuantityPair> objects) {
        if (objects == null || objects.isEmpty()) {
            throw new IllegalArgumentException("Bids list is null or empty");
        }

        double totalBidPrice = 0.0;
        for (OrderBookEvent.PriceQuantityPair object : objects) {
            totalBidPrice += Double.parseDouble(object.getPrice());
        }

        return totalBidPrice / objects.size();
    }


    public static double calculateTotalQuantity(List<OrderBookEvent.PriceQuantityPair> objects) {
        if (objects == null || objects.isEmpty()) {
            throw new IllegalArgumentException("PriceQuantityPairs list is null or empty");
        }

        double totalQuantity = 0.0;
        for (OrderBookEvent.PriceQuantityPair object : objects) {
            totalQuantity += Double.parseDouble(object.getQuantity());
        }

        return totalQuantity;
    }


}
