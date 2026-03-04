package com.aibot.service;

import com.aibot.constants.BotCommands;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TelegramBotService {

    // ✅ @Setter от Lombok вместо ручного setBot()
    @Setter
    private EnhancedAiTelegramBot bot;

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.disableWebPagePreview();
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    public void sendMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(keyboard);
        message.disableWebPagePreview();
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки с клавиатурой: {}", e.getMessage());
        }
    }

    public void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(keyboard);
        message.disableWebPagePreview();
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки с inline клавиатурой: {}", e.getMessage());
        }
    }

    // ✅ Типизированные List<KeyboardRow> вместо сырых List
    public ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BotCommands.NEW_SEARCH);
        row1.add(BotCommands.STATUS);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BotCommands.PREMIUM);
        row2.add(BotCommands.ANALYZE_FILE);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(BotCommands.HELP);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        return keyboard;
    }

    public ReplyKeyboardMarkup createAfterResponseKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BotCommands.NEW_REQUEST);
        row1.add(BotCommands.MAIN_MENU);

        rows.add(row1);
        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        return keyboard;
    }

    // ✅ Типизированные List<List<InlineKeyboardButton>>
    public InlineKeyboardMarkup createPremiumMenuKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton monthButton = new InlineKeyboardButton();
        monthButton.setText("💎 Premium - 149₽/мес");
        monthButton.setCallbackData(BotCommands.CALLBACK_PREMIUM_MONTH);

        InlineKeyboardButton yearButton = new InlineKeyboardButton();
        yearButton.setText("💎 Premium - 1249₽/год (скидка 30%)");
        yearButton.setCallbackData(BotCommands.CALLBACK_PREMIUM_YEAR);

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(monthButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(yearButton);

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }
}