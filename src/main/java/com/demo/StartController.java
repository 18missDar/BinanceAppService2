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

    @Autowired
    private MessageSenderService messageSenderService;

    @GetMapping
    private String start(@RequestParam String eventSymbol,
                         @RequestParam long startTime,
                         @RequestParam long endTime,
                         @RequestParam int numberOfBookParts,
                         @RequestParam double minPriceOrderBuy,
                         @RequestParam double maxPriceOrderBuy,
                         @RequestParam int intervalMinutes,
                         @RequestParam String name_queue,
                         @RequestParam String idService){
        AppConfig appConfig = new AppConfig();
        appConfig.setEventSymbol(eventSymbol);
        ForServiceDescriptorService forServiceDescriptorService = new ForServiceDescriptorService();
        forServiceDescriptorService.loadOutputFoldersDescriptors();
        ServiceDescriptor serviceDescriptor = forServiceDescriptorService.getService(idService);
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setDbUrl(serviceDescriptor.getDatasource_url());
        databaseConfig.setDbUsername(serviceDescriptor.getDatasource_username());
        databaseConfig.setDbPassword(serviceDescriptor.getDatasource_password());
        String requestForBook = "http://" + serviceDescriptor.getHost() + ":" + serviceDescriptor.getPort() + "/getOrderBook";
        OrderBookManager orderBookManager = new OrderBookManager();
        TradeEventManager tradeEventManager = new TradeEventManager();
        tradeEventManager.startTradeEventManager(databaseConfig, appConfig);
        try {
            DataManager dataManager = new DataManager(messageSenderService);
            dataManager.prepareData(startTime, endTime, numberOfBookParts, minPriceOrderBuy, maxPriceOrderBuy, intervalMinutes, name_queue, orderBookManager, tradeEventManager, requestForBook, eventSymbol);
            return "All starts successfully";
        }
        catch (Exception e){
            return "Something went wrong " + e.getMessage();
        }

    }
}
