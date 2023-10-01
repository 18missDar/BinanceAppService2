package com.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DataManager {

    private MessageSenderService messageSenderService;

    public DataManager(MessageSenderService messageSenderService) {
        this.messageSenderService = messageSenderService;
    }

    public void prepareData(long startTime,
                              long endTime,
                              int numberOfBookParts,
                              double minPriceOrderBuy,
                              double maxPriceOrderBuy,
                              int intervalMinutes,
                              String name_queue,
                              OrderBookManager orderBookManager,
                              TradeEventManager tradeEventManager,
                              String requestForBook,
                              String eventSymbol,
                            String idService){
        OrderBookSnapshot orderBookSnapshot = null;
        String name_queueForWriting = idService + "_" + eventSymbol;
        String requestForInitiating = requestForBook + "/startQueueForBook?startTime=" + startTime
                + "&endTime=" + endTime
                + "&intervalMinutes=" + intervalMinutes
                + "&eventSymbol=" + eventSymbol
                + "&name_queue=" + name_queueForWriting;
        orderBookManager.initiateProcess(requestForInitiating);
        while (startTime < endTime) {
            String request = requestForBook + "/getOrderTemporaryBook?name_queue=" + name_queueForWriting;

            orderBookSnapshot = orderBookManager.getSnapshot(request);

            double sumQuantityTrue = 0;
            double sumQuantityFalse = 0;
            double weightedSumPriceTrue = 0;
            double weightedSumPriceFalse = 0;
            double weightedAveragePriceTrue = 0;
            double weightedAveragePriceFalse = 0;

            List<TradeEvent> tradeEvents = tradeEventManager.findTradeEventsBetweenTimes(startTime, startTime + intervalMinutes);
            if (tradeEvents.size() > 0) {
                for (TradeEvent tradeEvent : tradeEvents) {
                    double price = Double.parseDouble(tradeEvent.getPrice());
                    double quantity = Double.parseDouble(tradeEvent.getQuantity());
                    boolean isBuyerMarketMaker = tradeEvent.isBuyerMarketMaker();

                    if (isBuyerMarketMaker) {
                        sumQuantityTrue += quantity;
                        weightedSumPriceTrue += price * quantity; // Calculate weighted sum for true
                    } else {
                        sumQuantityFalse += quantity;
                        weightedSumPriceFalse += price * quantity; // Calculate weighted sum for false
                    }
                }

                if (sumQuantityTrue != 0)
                    weightedAveragePriceTrue = weightedSumPriceTrue / sumQuantityTrue;
                else
                    weightedAveragePriceTrue = 0;
                if (sumQuantityFalse != 0)
                    weightedAveragePriceFalse = weightedSumPriceFalse / sumQuantityFalse;
                else
                    weightedAveragePriceFalse = 0;
            }
            if (orderBookSnapshot != null) {
                List<OrderBookEvent.PriceQuantityPair> bids = orderBookSnapshot.getBids();
                List<OrderBookEvent.PriceQuantityPair> asks = orderBookSnapshot.getAsks();
                // Prepare the JSON object
                Gson gson = new Gson();
                String json = gson.toJson(new SummaryData(startTime, startTime + intervalMinutes,
                        getAsksInterval(asks, minPriceOrderBuy, numberOfBookParts),
                        getBidsInterval(bids, maxPriceOrderBuy, numberOfBookParts),
                        weightedAveragePriceFalse,
                        weightedAveragePriceTrue,
                        sumQuantityFalse,
                        sumQuantityTrue,
                        weightedSumPriceFalse,
                        weightedSumPriceTrue));

                System.out.println(json);
                messageSenderService.sendMessage(name_queue, json);
            }

            startTime += intervalMinutes;
        }

    }



    public static List<Double> getBidsInterval(List<OrderBookEvent.PriceQuantityPair> bids, double maxPriceOrderBuy, int numberOfBookParts) {
        List<Double> result = new ArrayList<>();

        // Convert price strings to doubles and find maximum bid price
        List<Double> bidPrices = new ArrayList<>();
        for (OrderBookEvent.PriceQuantityPair bid : bids) {
            bidPrices.add(Double.parseDouble(bid.getPrice()));
        }
        double maximumOfBids = Collections.max(bidPrices);

        // Calculate maxBids and stepForBids
        double maxBids = maximumOfBids - maximumOfBids * maxPriceOrderBuy;
        double stepForBids = (maximumOfBids - maxBids) / numberOfBookParts;

        // Generate the list with values
        double currentValue = maxBids;
        result.add(maxBids);
        for (int i = 0; i < numberOfBookParts; i++) {
            currentValue += stepForBids;
            result.add(currentValue);
        }


        return findQuantitiesInPriceIntervals(bids,result);
    }


    public static List<Double> findQuantitiesInPriceIntervals(
            List<OrderBookEvent.PriceQuantityPair> asks,
            List<Double> priceIntervals) {
        List<Double> quantitiesInIntervals = new ArrayList<>();

        for (int i = 0; i < priceIntervals.size() - 1; i++) {
            double startPrice = priceIntervals.get(i);
            double endPrice = priceIntervals.get(i + 1);
            double quantityInInterval = 0.0;

            for (OrderBookEvent.PriceQuantityPair ask : asks) {
                double askPrice = Double.parseDouble(ask.getPrice());

                // Check if the ask price is within the current interval
                if (askPrice >= startPrice && askPrice < endPrice) {
                    quantityInInterval += Double.parseDouble(ask.getQuantity());
                }
            }

            quantitiesInIntervals.add(quantityInInterval);
        }

        return quantitiesInIntervals;
    }


    public static List<Double> getAsksInterval(List<OrderBookEvent.PriceQuantityPair> asks, double minPriceOrderBuy, int numberOfBookParts) {
        List<Double> result = new ArrayList<>();

        // Convert price strings to doubles and find minimum ask price
        List<Double> askPrices = new ArrayList<>();
        for (OrderBookEvent.PriceQuantityPair ask : asks) {
            askPrices.add(Double.parseDouble(ask.getPrice()));
        }
        double minimumOfAsks = Collections.min(askPrices);

        // Calculate minAsks and stepForAsks
        double minAsks = minimumOfAsks + minimumOfAsks * minPriceOrderBuy;
        double stepForAsks = (minAsks - minimumOfAsks) / numberOfBookParts;

        // Generate the list with values
        double currentValue = minimumOfAsks;
        result.add(minimumOfAsks);
        for (int i = 0; i < numberOfBookParts; i++) {
            currentValue += stepForAsks;
            result.add(currentValue);
        }

        return findQuantitiesInPriceIntervals(asks, result);
    }

}
