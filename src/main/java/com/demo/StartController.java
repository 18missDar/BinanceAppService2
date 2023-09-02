package com.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/start")
public class StartController {

    @Autowired
    private DatabaseConfig databaseConfig;

    private final OrderBookManager orderBookManager;

    private final DataManager dataManager;

    public StartController(OrderBookManager orderBookManager, DataManager dataManager) {
        this.orderBookManager = orderBookManager;
        this.dataManager = dataManager;
    }

    @GetMapping
    private String start(@RequestParam String eventSymbol,
                         @RequestParam long startTime,
                         @RequestParam long endTime,
                         @RequestParam int numberOfBookParts,
                         @RequestParam double minPriceOrderBuy,
                         @RequestParam double maxPriceOrderBuy,
                         @RequestParam int intervalMinutes){
        AppConfig appConfig = new AppConfig();
        appConfig.setEventSymbol(eventSymbol);
        orderBookManager.startOrderBookManage(databaseConfig, appConfig);
        try {
            dataManager.prepareData(startTime, endTime, numberOfBookParts, minPriceOrderBuy, maxPriceOrderBuy, intervalMinutes, orderBookManager);
            return "All starts successfully";
        }
        catch (Exception e){
            return "Something went wrong " + e.getMessage();
        }

    }
}
