package com.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrderBookManager {

    private OrderBookSnapshot parseOrderBookSnapshot(String responseBody) {
        OrderBookSnapshot snapshot = new OrderBookSnapshot();
        JsonParser parser = new JsonParser();

        try {
            // Parse the JSON response body
            JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();

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

    public OrderBookSnapshot getSnapshot(String request){
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(request);

        try {
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            if (!responseBody.isEmpty()) {
                return parseOrderBookSnapshot(responseBody);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle exception
        }
        return null;

    }
}
