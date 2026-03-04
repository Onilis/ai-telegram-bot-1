package com.aibot.service;

import com.aibot.config.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BotConfig botConfig;

    /**
     * Создание инвойса для Premium подписки
     */
    public SendInvoice createPremiumPayment(Long chatId, String period) {
        SendInvoice invoice = new SendInvoice();
        invoice.setChatId(chatId.toString());
        invoice.setTitle("💎 Premium подписка");
        invoice.setProviderToken(botConfig.getPaymentToken());
        invoice.setCurrency("RUB");
        invoice.setPayload("premium_" + period);

        List<LabeledPrice> prices = new ArrayList<>();

        if ("month".equals(period)) {
            invoice.setDescription(
                    "Premium на 1 месяц:\n" +
                            "✅ Безлимитные поисковые запросы\n" +
                            "✅ Анализ документов и скриншотов\n" +
                            "✅ Приоритетная поддержка"
            );
            prices.add(new LabeledPrice("Premium (1 месяц)", 14900)); // 149 руб

        } else if ("year".equals(period)) {
            invoice.setDescription(
                    "Premium на 1 год:\n" +
                            "✅ Безлимитные поисковые запросы\n" +
                            "✅ Анализ документов и скриншотов\n" +
                            "✅ Приоритетная поддержка\n" +
                            "💰 Экономия 30%"
            );
            prices.add(new LabeledPrice("Premium (1 год)", 124900)); // 1249 руб (экономия)
        }

        invoice.setPrices(prices);
        log.info("💳 Создан инвойс Premium ({}) для chatId: {}", period, chatId);

        return invoice;
    }
}
