package com.aibot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bot")
public class BotConfig {

    private String paymentToken;
    private Integer subscriptionPrice;
    private Integer subscriptionDays;
    private Integer freeSearchDailyLimit = 5;
    private Long ownerId;

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }

    public Integer getSubscriptionPrice() { return subscriptionPrice; }
    public void setSubscriptionPrice(Integer subscriptionPrice) { this.subscriptionPrice = subscriptionPrice; }

    public Integer getSubscriptionDays() { return subscriptionDays; }
    public void setSubscriptionDays(Integer subscriptionDays) { this.subscriptionDays = subscriptionDays; }

    public Integer getFreeSearchDailyLimit() {
        return freeSearchDailyLimit != null ? freeSearchDailyLimit : 5;
    }
    public void setFreeSearchDailyLimit(Integer freeSearchDailyLimit) { this.freeSearchDailyLimit = freeSearchDailyLimit; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}