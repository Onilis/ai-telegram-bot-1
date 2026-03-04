package com.aibot.constants;

public final class BotCommands {

    public static final String NEW_SEARCH = "🔍 Новый поиск";
    public static final String STATUS = "📊 Статус";
    public static final String MAIN_MENU = "👤 Главное меню";
    public static final String HELP = "❓ Справка";
    public static final String PREMIUM = "💎 Premium";
    public static final String NEW_REQUEST = "🔄 Новый запрос";
    public static final String ANALYZE_FILE = "📄 Анализ документа";

    public static final String CMD_START = "/start";
    public static final String CMD_STATUS = "/status";
    public static final String CMD_MENU = "/menu";
    public static final String CMD_HELP = "/help";
    public static final String CMD_PREMIUM = "/premium";

    public static final String CALLBACK_PREMIUM_MONTH = "premium_month";
    public static final String CALLBACK_PREMIUM_YEAR = "premium_year";
    public static final String CALLBACK_CANCEL = "cancel";

    private BotCommands() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}