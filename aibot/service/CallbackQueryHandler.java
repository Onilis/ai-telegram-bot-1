package com.aibot.service;

import com.aibot.constants.BotCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackQueryHandler {

    // ✅ userRepository удалён — не использовался
    private final PaymentService paymentService;
    private final EnhancedAiTelegramBot bot;

    public void handleCallback(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Long userId = callbackQuery.getFrom().getId();
        String callbackData = callbackQuery.getData();

        log.info("📞 Callback от {}: {}", userId, callbackData);

        try {
            switch (callbackData) {
                case BotCommands.CALLBACK_PREMIUM_MONTH:
                    SendInvoice monthInvoice = paymentService.createPremiumPayment(chatId, "month");
                    bot.execute(monthInvoice);
                    answerCallback(callbackQuery.getId(), "💳 Счёт на оплату создан");
                    break;

                case BotCommands.CALLBACK_PREMIUM_YEAR:
                    SendInvoice yearInvoice = paymentService.createPremiumPayment(chatId, "year");
                    bot.execute(yearInvoice);
                    answerCallback(callbackQuery.getId(), "💳 Счёт на оплату создан");
                    break;

                case BotCommands.CALLBACK_CANCEL:
                case "back_to_menu":
                    sendMessage(chatId, "◀️ Вы вернулись в главное меню");
                    answerCallback(callbackQuery.getId(), "Отменено");
                    break;

                default:
                    log.warn("⚠️ Неизвестный callback: {}", callbackData);
                    answerCallback(callbackQuery.getId(), "⚠️ Неизвестная команда");
            }

        } catch (TelegramApiException e) {
            log.error("❌ Ошибка обработки callback: {}", e.getMessage(), e);
            answerCallback(callbackQuery.getId(), "❌ Ошибка обработки запроса");
        }
    }

    private void answerCallback(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            answer.setText(text);
            answer.setShowAlert(false);
            bot.execute(answer);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка ответа на callback: {}", e.getMessage());
        }
    }

    private void sendMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения: {}", e.getMessage());
        }
    }
}
