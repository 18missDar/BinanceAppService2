package com.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrderBookManager {

    private DatabaseConfig databaseConfig;
    private AppConfig appConfig;
    private String SOURCE_SHAPSHOT_BOOK;
    private String SOURCE_ORDER_BOOK_EVENT;


    public void startOrderBookManage(DatabaseConfig databaseConfig, AppConfig appConfig){
        this.databaseConfig = databaseConfig;
        this.appConfig = appConfig;
        findLastCreatedTable();
    }


    public void findLastCreatedTable() {
        String sourcePattern = "^source_order_book_event_" + appConfig.getEventSymbol() + "_([0-9]+)$";
        String proceedPattern = "^source_snapshot_book_" + appConfig.getEventSymbol() + "_([0-9]+)$";
        Pattern sourceTableNamePattern = Pattern.compile(sourcePattern);
        Pattern proceedTableNamePattern = Pattern.compile(proceedPattern);

        List<String> sourceTradeEventsNames = new ArrayList<>();
        List<String> proceedTradeEventsNames = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseConfig.getDbUrl(), databaseConfig.getDbUsername(), databaseConfig.getDbPassword());
             Statement statement = connection.createStatement()) {
            String showTablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
            try (ResultSet resultSet = statement.executeQuery(showTablesQuery)) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString(1);
                    if (sourceTableNamePattern.matcher(tableName).matches()) {
                        sourceTradeEventsNames.add(tableName);
                    } else if (proceedTableNamePattern.matcher(tableName).matches()) {
                        proceedTradeEventsNames.add(tableName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Find the element with the maximum end number for sourceTradeEventsNames
        String maxSourceTableName = null;
        long maxSourceNumber = Long.MIN_VALUE;
        for (String tableName : sourceTradeEventsNames) {
            Matcher matcher = sourceTableNamePattern.matcher(tableName);
            if (matcher.matches()) {
                long number = Long.parseLong(matcher.group(1));
                if (number > maxSourceNumber) {
                    maxSourceTableName = tableName;
                    maxSourceNumber = number;
                }
            }
        }

        // Find the element with the maximum end number for proceedTradeEventsNames
        String maxProceedTableName = null;
        long maxProceedNumber = Long.MIN_VALUE;
        for (String tableName : proceedTradeEventsNames) {
            Matcher matcher = proceedTableNamePattern.matcher(tableName);
            if (matcher.matches()) {
                long number = Long.parseLong(matcher.group(1));
                if (number > maxProceedNumber) {
                    maxProceedTableName = tableName;
                    maxProceedNumber = number;
                }
            }
        }

        SOURCE_ORDER_BOOK_EVENT = maxSourceTableName;
        SOURCE_SHAPSHOT_BOOK = maxProceedTableName;
    }



    private OrderBookSnapshot parseOrderBookSnapshot(String responseBody) {
        OrderBookSnapshot snapshot = new OrderBookSnapshot();
        JsonParser parser = new JsonParser();

        try {
            // Parse the JSON response body
            JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();

            // Extract the lastUpdateId
            long lastUpdateId = jsonObject.get("lastUpdateId").getAsLong();
            snapshot.setLastUpdateId(lastUpdateId);

            // Extract the bids and asks arrays
            JsonArray bidsArray = jsonObject.getAsJsonArray("bids");
            JsonArray asksArray = jsonObject.getAsJsonArray("asks");

            // Populate the bids list
            List<OrderBookEvent.PriceQuantityPair> bids = new ArrayList<>();
            for (JsonElement bidElement : bidsArray) {
                JsonArray bidArray = bidElement.getAsJsonArray();
                String price = bidArray.get(0).getAsString();
                String quantity = bidArray.get(1).getAsString();
                OrderBookEvent.PriceQuantityPair bid = new OrderBookEvent.PriceQuantityPair(price, quantity);
                bids.add(bid);
            }

            snapshot.setBids(bids);

            // Populate the asks list
            List<OrderBookEvent.PriceQuantityPair> asks = new ArrayList<>();
            for (JsonElement askElement : asksArray) {
                JsonArray askArray = askElement.getAsJsonArray();
                String price = askArray.get(0).getAsString();
                String quantity = askArray.get(1).getAsString();
                OrderBookEvent.PriceQuantityPair ask = new OrderBookEvent.PriceQuantityPair(price, quantity);
                asks.add(ask);
            }
            snapshot.setAsks(asks);
        } catch (Exception e) {
            System.err.println("Error while parsing order book snapshot: " + e.getMessage());
            return null;
        }

        return snapshot;
    }



    public Optional<OrderBookSnapshot> findClosestSnapshot(long currentTime, long endTime) {
        try (Connection connection = DriverManager.getConnection(databaseConfig.getDbUrl(), databaseConfig.getDbUsername(), databaseConfig.getDbPassword())) {
            String selectQuery = "SELECT * FROM " + SOURCE_SHAPSHOT_BOOK +
                    " WHERE currentTime >= ? and currentTime < ? ORDER BY currentTime DESC LIMIT 1";

            try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
                statement.setLong(1, currentTime);
                statement.setLong(2, endTime);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        long currentTimeDB = resultSet.getLong("currentTime");
                        String partial_bookJson = resultSet.getString("partial_book");

                        // Assuming you have a method to parse JSON strings and create the OrderBookSnapshot object
                        OrderBookSnapshot snapshot = parseOrderBookSnapshot(partial_bookJson);
                        snapshot.setCurrentTime(currentTimeDB);
                        return Optional.of(snapshot);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public List<OrderBookEvent> getRowsBetweenTimes(long time1, long time2) throws JsonProcessingException {
        List<OrderBookEvent> result = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseConfig.getDbUrl(), databaseConfig.getDbUsername(), databaseConfig.getDbPassword())) {
            String selectQuery = "SELECT * FROM " + SOURCE_ORDER_BOOK_EVENT +
                    " WHERE event_time >= ? AND event_time < ?";

            try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
                statement.setLong(1, time1);
                statement.setLong(2, time2);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        OrderBookEvent event = mapResultSetToOrderBookEvent(resultSet);
                        result.add(event);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private OrderBookEvent mapResultSetToOrderBookEvent(ResultSet resultSet) throws SQLException, JsonProcessingException {
        // Implement a method to map the ResultSet to an OrderBookEvent object
        // Example:
        String eventType = resultSet.getString("event_type");
        long eventTime = resultSet.getLong("event_time");
        String symbol = resultSet.getString("symbol");
        long firstUpdateId = resultSet.getLong("first_update_id");
        long finalUpdateId = resultSet.getLong("final_update_id");
        String bidsJson = resultSet.getString("bids");
        List<OrderBookEvent.PriceQuantityPair> bids = parseBidsAsks(bidsJson);
        String asksJson = resultSet.getString("asks");
        List<OrderBookEvent.PriceQuantityPair> asks = parseBidsAsks(asksJson);

        // Assuming you have a method to parse JSON strings and create the OrderBookEvent object
        OrderBookEvent event = new OrderBookEvent(eventType, eventTime, symbol, firstUpdateId, finalUpdateId, bids, asks);
        return event;
    }

    public static List<OrderBookEvent.PriceQuantityPair> parseBidsAsks(String bidsJson) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<List<String>> parsedBids = objectMapper.readValue(bidsJson, List.class);

        List<OrderBookEvent.PriceQuantityPair> bids = new ArrayList<>();

        for (List<String> bid : parsedBids) {
            if (bid.size() >= 2) {
                OrderBookEvent.PriceQuantityPair priceQuantityPair = new OrderBookEvent.PriceQuantityPair(bid.get(0), bid.get(1));
                bids.add(priceQuantityPair);
            }
        }

        return bids;
    }

    private static List<OrderBookEvent.PriceQuantityPair> getAllBids(List<OrderBookEvent> orderBookEvents) {
        List<OrderBookEvent.PriceQuantityPair> allBids = new ArrayList<>();

        for (OrderBookEvent event : orderBookEvents) {
            if (event.getBids() != null) {
                allBids.addAll(event.getBids());
            }
        }

        return allBids;
    }

    public static List<OrderBookEvent.PriceQuantityPair> getAllAsks(List<OrderBookEvent> orderBookEvents) {
        List<OrderBookEvent.PriceQuantityPair> allAsks = new ArrayList<>();

        for (OrderBookEvent event : orderBookEvents) {
            if (event.getAsks() != null) {
                allAsks.addAll(event.getAsks());
            }
        }

        return allAsks;
    }

    private OrderBookSnapshot processBidsAndAsks(List<OrderBookEvent.PriceQuantityPair> bids, List<OrderBookEvent.PriceQuantityPair> asks) {
        Iterator<OrderBookEvent.PriceQuantityPair> bidsIterator = bids.iterator();
        Iterator<OrderBookEvent.PriceQuantityPair> asksIterator = asks.iterator();

        while (bidsIterator.hasNext()) {
            OrderBookEvent.PriceQuantityPair bid = bidsIterator.next();
            if (bid == null) {
                continue; // Skip null bid entries
            }
            String bidPrice = bid.getPrice();
            String bidQuantity = bid.getQuantity();

            while (asksIterator.hasNext()) {
                OrderBookEvent.PriceQuantityPair ask = asksIterator.next();
                if (ask == null) {
                    continue; // Skip null ask entries
                }
                String askPrice = ask.getPrice();
                String askQuantity = ask.getQuantity();

                if (bidPrice.equals(askPrice) && bidQuantity.equals(askQuantity)) {
                    // Delete both the bid and ask
                    bidsIterator.remove();
                    asksIterator.remove();
                    break;
                } else if (bidPrice.equals(askPrice)) {
                    double bidQuantityValue = Double.parseDouble(bidQuantity);
                    double askQuantityValue = Double.parseDouble(askQuantity);

                    if (bidQuantityValue > askQuantityValue) {
                        // Update bid quantity and delete the ask
                        bidQuantityValue -= askQuantityValue;
                        bid.setQuantity(String.valueOf(bidQuantityValue));
                        //asksIterator.remove();
                    } else {
                        // Update ask quantity and delete the bid
                        askQuantityValue -= bidQuantityValue;
                        ask.setQuantity(String.valueOf(askQuantityValue));
                        //bidsIterator.remove();
                    }
                }
            }
        }
        OrderBookSnapshot result = new OrderBookSnapshot();
        result.setBids(bids);
        result.setAsks(asks);
        return result;
    }


    private OrderBookSnapshot accumulateSnapshotActualBids(List<OrderBookEvent> orderBookEvents, OrderBookSnapshot orderBookSnapshot){
        List<OrderBookEvent.PriceQuantityPair> bidsFromShapshot = orderBookSnapshot.getBids();
        List<OrderBookEvent.PriceQuantityPair> asksFromShapshot = orderBookSnapshot.getAsks();
        bidsFromShapshot.addAll(getAllBids(orderBookEvents));
        asksFromShapshot.addAll(getAllAsks(orderBookEvents));

        OrderBookSnapshot result = processBidsAndAsks(bidsFromShapshot, asksFromShapshot);
        return result;
    }



    public OrderBookSnapshot collectData(long currentTime, long endTime) throws JsonProcessingException {
        Optional<OrderBookSnapshot> orderBookSnapshot = findClosestSnapshot(currentTime, endTime);
        List<OrderBookEvent> orderBookEvents = getRowsBetweenTimes(currentTime, endTime);
        if (orderBookSnapshot.isPresent()){
            return accumulateSnapshotActualBids(orderBookEvents, orderBookSnapshot.get());
        }
        else {
            if (orderBookEvents.size() > 0){
            OrderBookSnapshot orderBookSnapshot1 = processBidsAndAsks(getAllBids(orderBookEvents), getAllAsks(orderBookEvents));
            int lastIndex = orderBookEvents.size() - 1;
            orderBookSnapshot1.setLastUpdateId(orderBookEvents.get(lastIndex).getFinalUpdateId());
            return orderBookSnapshot1;}
            else return null;
        }
    }
}
