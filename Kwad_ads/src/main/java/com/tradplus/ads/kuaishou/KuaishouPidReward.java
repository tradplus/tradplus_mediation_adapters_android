package com.tradplus.ads.kuaishou;

/**
 * Created by sainase on 2020-06-16.
 */
public class KuaishouPidReward {

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

    public KuaishouPidReward(String currency, String amount) {
        this.currency = currency;
        this.amount = amount;
    }
}
