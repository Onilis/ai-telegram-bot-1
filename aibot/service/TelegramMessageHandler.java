package com.aibot.service;

import com.aibot.config.BotConfig;
import com.aibot.constants.BotCommands;
import com.aibot.entity.UserEntity;
import com.aibot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageHandler {

    private final UserRepository userRepository;
    private final UserService userService;
    private final GigaChatService gigaChatService;
    private final FileAnalysisService fileAnalysisService;
    private final BotConfig botConfig;
    private final EnhancedAiTelegramBot bot;

    /**
     * Главный обработчик входящих сообщений
     */
    public void handleMessage(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String username = message.getFrom().getUserName();
        String text = message.getText();

        // Регистрация/получение пользователя
        UserEntity user = userService.findOrCreateUser(userId, username);

        // Обработка команд меню
        if (text != null) {
            handleTextCommand(chatId, userId, text, user);
        }
        // Обработка документов и изображений
        else if (message.hasDocument() || message.hasPhoto()) {
            handleFileMessage(message, user);
        }
    }

    /**
     * Обработка текстовых команд
     */
    private void handleTextCommand(Long chatId, Long userId, String text, UserEntity user) {

        switch (text) {

            case BotCommands.CMD_START:
                sendWelcomeMessage(chatId, user);
                break;

            case BotCommands.STATUS:
            case BotCommands.CMD_STATUS:
                String stats = userService.getUserStats(userId);
                sendMessage(chatId, stats);
                break;

            case BotCommands.MAIN_MENU:
            case BotCommands.CMD_MENU:
                sendMainMenu(chatId);
                break;

            case BotCommands.HELP:
            case BotCommands.CMD_HELP:
                sendHelpMessage(chatId);
                break;

            case BotCommands.PREMIUM:
            case BotCommands.CMD_PREMIUM:
                sendPremiumInfo(chatId, user);
                break;

            case BotCommands.NEW_SEARCH:
            case BotCommands.NEW_REQUEST:
                sendMessage(chatId, "🔍 Введите ваш поисковый запрос:");
                break;

            case BotCommands.ANALYZE_FILE:
                if (user.canAnalyzeDocument()) {
                    sendMessage(chatId, "📄 Отправьте документ или скриншот для анализа");
                } else {
                    sendMessage(chatId,
                            "⚠️ Анализ документов доступен только в Premium подписке.\n\n" +
                                    "Нажмите 💎 Premium для подробностей."
                    );
                }
                break;

            default:
                handleSearchQuery(chatId, userId, text, user);
                break;
        }
    }

    /**
     * 🔍 Обработка поискового запроса
     */
    private void handleSearchQuery(Long chatId, Long userId, String query, UserEntity user) {

        // Получаем лимит из конфига
        Integer dailyLimit = botConfig.getFreeSearchDailyLimit();
        if (dailyLimit == null) {
            dailyLimit = 5; // Значение по умолчанию
        }

        // Проверка лимита для бесплатных
        if (!user.canMakeSearchRequest(dailyLimit)) {
            sendMessage(chatId,
                    "⚠️ Вы исчерпали дневной лимит бесплатных запросов (5/день).\n\n" +
                            "Для безлимитного доступа оформите Premium подписку 💎"
            );
            return;
        }

        sendTypingAction(chatId);

        try {
            // ✅ ИСПРАВЛЕНО: используем gigaChatService.sendMessage(String)
            String response = gigaChatService.sendMessage(query);

            // Увеличиваем счётчик
            userService.incrementSearchRequestIfAllowed(userId, dailyLimit);

            // ✅ ИСПРАВЛЕНО: sendMessage с chatId
            sendMessage(chatId, response);
            log.info("✅ Ответ отправлен пользователю: {}", userId);

        } catch (Exception e) {
            log.error("❌ Ошибка обработки запроса: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Произошла ошибка при обработке запроса. Попробуйте позже.");
        }
    }

    /**
     * 📄 Обработка файлов (документы и изображения)
     */
    private void handleFileMessage(Message message, UserEntity user) {
        Long chatId = message.getChatId();
        Long userId = user.getTelegramUserId();

        // Проверка Premium статуса
        if (!user.canAnalyzeDocument()) {
            sendMessage(chatId,
                    "⚠️ Анализ документов и изображений доступен только в Premium подписке.\n\n" +
                            "Нажмите 💎 Premium для оформления."
            );
            return;
        }

        sendTypingAction(chatId);
        sendMessage(chatId, "📄 Анализирую документ...");

        try {
            // ✅ ИСПРАВЛЕНО: передаём Message в analyzeFile
            String analysis = fileAnalysisService.analyzeFile(message);

            if (analysis != null && !analysis.isEmpty()) {
                userService.incrementFileAnalysisIfAllowed(userId);
                sendMessage(chatId, "✅ Анализ завершён:\n\n" + analysis);
                log.info("✅ Файл проанализирован для пользователя: {}", userId);
            } else {
                sendMessage(chatId, "❌ Не удалось проанализировать файл");
            }

        } catch (Exception e) {
            log.error("❌ Ошибка анализа файла: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при анализе файла. Попробуйте другой файл.");
        }
    }

    /**
     * 🏠 Приветственное сообщение
     */
    private void sendWelcomeMessage(Long chatId, UserEntity user) {
        String welcomeText = String.format(
                "👋 Добро пожаловать, %s!\n\n" +
                        "🤖 Я AI-бот с поддержкой GigaChat\n\n" +
                        "✨ *Возможности:*\n" +
                        "🔍 Поиск информации (5 запросов/день бесплатно)\n" +
                        "📄 Анализ документов (только Premium)\n" +
                        "🖼 Анализ изображений (только Premium)\n\n" +
                        "Используйте кнопки меню для навигации 👇",
                user.getUsername() != null ? user.getUsername() : "пользователь"
        );

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(welcomeText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(createMainKeyboard(user));

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки приветствия: {}", e.getMessage());
        }
    }

    private void sendMainMenu(Long chatId) {
        sendMessage(chatId, "👤 Главное меню\n\nВыберите действие:");
    }

    private void sendHelpMessage(Long chatId) {
        String helpText =
                "❓ *Справка по использованию бота*\n\n" +
                        "🆓 *Бесплатная версия:*\n" +
                        "• 5 поисковых запросов в день\n" +
                        "• Базовые ответы от AI\n\n" +
                        "💎 *Premium версия:*\n" +
                        "• ♾️ Безлимитные запросы\n" +
                        "• 📄 Анализ документов (PDF, DOCX, TXT)\n" +
                        "• 🖼 Анализ изображений и скриншотов\n" +
                        "• ⚡️ Приоритетная обработка\n\n" +
                        "📝 *Как использовать:*\n" +
                        "1. Просто напишите ваш вопрос\n" +
                        "2. Отправьте документ для анализа (Premium)\n" +
                        "3. Отправьте изображение для анализа (Premium)\n\n" +
                        "💡 Используйте кнопки меню для быстрого доступа к функциям!";

        sendMessage(chatId, helpText);
    }

    private void sendPremiumInfo(Long chatId, UserEntity user) {
        String premiumText;

        if (user.isActivePremium()) {
            String expiryDate = user.getPremiumExpiryDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            premiumText = String.format(
                    "💎 *Ваша Premium подписка активна*\n\n" +
                            "✅ Безлимитные запросы\n" +
                            "✅ Анализ документов\n" +
                            "✅ Анализ изображений\n\n" +
                            "⏰ Активна до: %s",
                    expiryDate
            );
        } else {
            premiumText =
                    "💎 *Premium подписка*\n\n" +
                            "🚀 *Что вы получите:*\n" +
                            "• ♾️ Безлимитные поисковые запросы\n" +
                            "• 📄 Анализ документов любых форматов\n" +
                            "• 🖼 Анализ изображений и скриншотов\n" +
                            "• ⚡️ Приоритетная обработка запросов\n" +
                            "• 🛡 Техподдержка 24/7\n\n" +
                            "💰 *Тарифы:*\n" +
                            "• 1 месяц — 149₽\n" +
                            "• 1 год — 1249₽ (экономия 30%)\n\n" +
                            "Выберите подходящий тариф 👇";
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(premiumText);
        message.setParseMode("Markdown");

        if (!user.isActivePremium()) {
            message.setReplyMarkup(createPremiumKeyboard());
        }

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки Premium info: {}", e.getMessage());
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard(UserEntity user) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(BotCommands.NEW_SEARCH));
        row1.add(new KeyboardButton(BotCommands.STATUS));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        if (user.canAnalyzeDocument()) {
            row2.add(new KeyboardButton(BotCommands.ANALYZE_FILE));
        }
        row2.add(new KeyboardButton(BotCommands.PREMIUM));
        rows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BotCommands.HELP));
        row3.add(new KeyboardButton(BotCommands.MAIN_MENU));
        rows.add(row3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardMarkup createPremiumKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton monthBtn = new InlineKeyboardButton();
        monthBtn.setText("💎 1 месяц — 149₽");
        monthBtn.setCallbackData(BotCommands.CALLBACK_PREMIUM_MONTH);
        row1.add(monthBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton yearBtn = new InlineKeyboardButton();
        yearBtn.setText("💎 1 год — 1249₽ (-30%)");
        yearBtn.setCallbackData(BotCommands.CALLBACK_PREMIUM_YEAR);
        row2.add(yearBtn);
        rows.add(row2);

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * ✅ ИСПРАВЛЕНО: правильная отправка сообщения
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    /**
     * ✅ ФИНАЛЬНОЕ ИСПРАВЛЕНИЕ: отправка typing action
     */
    private void sendTypingAction(Long chatId) {
        try {
            SendChatAction action = new SendChatAction();
            action.setChatId(chatId.toString());
            action.setAction(ActionType.TYPING);  // ✅ ENUM
            bot.execute(action);
        } catch (Exception e) {
            log.warn("⚠️ Не удалось отправить typing action: {}", e.getMessage());
        }
    }
}

