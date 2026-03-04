package com.aibot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.aibot.service.EnhancedAiTelegramBot;

@SpringBootApplication
public class AiBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiBotApplication.class, args);
    }

    // ✅ @Bean теперь на уровне класса — Spring его увидит
    @Bean
    public TelegramBotsApi telegramBotsApi(EnhancedAiTelegramBot bot) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        return botsApi;
    }
}