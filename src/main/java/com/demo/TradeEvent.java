package com.demo;


public class TradeEvent {
    private String price;
    private String quantity;
    private boolean isBuyerMarketMaker;

    public TradeEvent(String price, String quantity, boolean isBuyerMarketMaker) {
        this.price = price;
        this.quantity = quantity;
        this.isBuyerMarketMaker = isBuyerMarketMaker;
    }


    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public boolean isBuyerMarketMaker() {
        return isBuyerMarketMaker;
    }

    public void setBuyerMarketMaker(boolean buyerMarketMaker) {
        isBuyerMarketMaker = buyerMarketMaker;
    }

}

