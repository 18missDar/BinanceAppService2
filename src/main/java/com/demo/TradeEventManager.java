package com.demo;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TradeEventManager {

    private DatabaseConfig databaseConfig;
    private AppConfig appConfig;
    private String SOURCE_TRADE_EVENT;

    public void startTradeEventManager(DatabaseConfig databaseConfig, AppConfig appConfig){
        this.databaseConfig = databaseConfig;
        this.appConfig = appConfig;
        findLastCreatedTable();
    }

    public void findLastCreatedTable() {
        String sourcePattern = "^source_trade_event_" + appConfig.getEventSymbol() + "_([0-9]+)$";
        Pattern sourceTableNamePattern = Pattern.compile(sourcePattern);

        List<String> sourceTradeEventsNames = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseConfig.getDbUrl(), databaseConfig.getDbUsername(), databaseConfig.getDbPassword());
             Statement statement = connection.createStatement()) {
            String showTablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
            try (ResultSet resultSet = statement.executeQuery(showTablesQuery)) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString(1);
                    if (sourceTableNamePattern.matcher(tableName).matches()) {
                        sourceTradeEventsNames.add(tableName);
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

        // Now you can use maxSourceTableName and maxProceedTableName as needed.
        SOURCE_TRADE_EVENT = maxSourceTableName;
    }

    public List<TradeEvent> findTradeEventsBetweenTimes(long time1, long time2){
        List<TradeEvent> tradeEvents = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseConfig.getDbUrl(), databaseConfig.getDbUsername(), databaseConfig.getDbPassword())) {
            String selectQuery = "SELECT price, quantity, is_buyer_market_maker FROM " + SOURCE_TRADE_EVENT +
                    " WHERE event_time >= ? AND event_time < ?";

            try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
                statement.setLong(1, time1);
                statement.setLong(2, time2);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String price = resultSet.getString("price");
                        String quantity = resultSet.getString("quantity");
                        boolean isBuyerMarketMaker = resultSet.getBoolean("is_buyer_market_maker");

                        TradeEvent tradeEvent = new TradeEvent(price, quantity, isBuyerMarketMaker);
                        tradeEvents.add(tradeEvent);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tradeEvents;
    }

}
