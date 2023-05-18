package com.tradplus.ads.helium;

public class HeliumPidReward {
    private String currency;
    private String amount;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public HeliumPidReward(String currency, String amount) {
        this.currency = currency;
        this.amount = amount;
    }
}
