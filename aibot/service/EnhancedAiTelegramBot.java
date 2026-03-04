package com.aibot.service;

import com.aibot.config.TelegramBotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class EnhancedAiTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotConfig telegramConfig;
    private final TelegramMessageHandler messageHandler;
    private final CallbackQueryHandler callbackHandler;

    public EnhancedAiTelegramBot(
            TelegramBotConfig telegramConfig,
            @Lazy TelegramMessageHandler messageHandler,
            @Lazy CallbackQueryHandler callbackHandler
    ) {
        super(telegramConfig.getToken());
        this.telegramConfig = telegramConfig;
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public String getBotUsername() {
        return telegramConfig.getName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                messageHandler.handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                callbackHandler.handleCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("❌ Error processing update: {}", e.getMessage(), e);
        }
    }

    // ✅ Лишний метод execute() удалён — используется унаследованный из TelegramLongPollingBot

    @Override
    public void clearWebhook() {
        try {
            super.clearWebhook();
            log.info("✅ Webhook cleared successfully");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("⚠️ Webhook not found (normal for first run or Long Polling mode)");
            } else {
                log.error("❌ Error clearing webhook: {}", e.getMessage());
            }
        }
    }
}