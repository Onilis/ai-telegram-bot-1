package com.aibot.service;

import com.aibot.entity.UserEntity;
import com.aibot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Регистрация нового пользователя или получение существующего
     */
    @Transactional
    public UserEntity findOrCreateUser(Long telegramUserId, String username) {
        try {
            Optional<UserEntity> existingUser = userRepository.findByTelegramUserId(telegramUserId);
            if (existingUser.isPresent()) {
                logger.debug("👤 Пользователь найден: {}", telegramUserId);
                return existingUser.get();
            }

            UserEntity user = new UserEntity();
            user.setTelegramUserId(telegramUserId);
            user.setUsername(username);

            UserEntity savedUser = userRepository.save(user);
            logger.info("✅ Новый пользователь зарегистрирован: {} ({})", username, telegramUserId);
            return savedUser;

        } catch (Exception e) {
            logger.error("❌ Ошибка создания пользователя: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка регистрации пользователя", e);
        }
    }

    /**
     * 🆕 Инкремент поисковых запросов (для бесплатных 5/день)
     */
    @Transactional
    public void incrementSearchRequestIfAllowed(Long telegramUserId, int dailySearchLimit) {
        try {
            Optional<UserEntity> optionalUser = userRepository.findByTelegramUserId(telegramUserId);
            if (optionalUser.isEmpty()) {
                logger.warn("⚠️ Пользователь не найден: {}", telegramUserId);
                return;
            }

            UserEntity user = optionalUser.get();

            if (user.canMakeSearchRequest(dailySearchLimit)) {
                user.incrementSearchRequests();
                userRepository.save(user);
                logger.debug("✅ Счётчик поисковых запросов увеличен для: {}", telegramUserId);
            } else {
                logger.warn("⚠️ Дневной лимит поиска достигнут для: {}", telegramUserId);
            }

        } catch (Exception e) {
            logger.error("❌ Ошибка увеличения счётчика: {}", e.getMessage(), e);
        }
    }

    /**
     * 🆕 Инкремент анализа файлов (только для Premium)
     */
    @Transactional
    public void incrementFileAnalysisIfAllowed(Long telegramUserId) {
        try {
            Optional<UserEntity> optionalUser = userRepository.findByTelegramUserId(telegramUserId);
            if (optionalUser.isEmpty()) {
                logger.warn("⚠️ Пользователь не найден: {}", telegramUserId);
                return;
            }

            UserEntity user = optionalUser.get();

            if (user.canAnalyzeDocument()) {
                user.incrementFileAnalysis();
                userRepository.save(user);
                logger.debug("✅ Счётчик анализа файлов увеличен для: {}", telegramUserId);
            } else {
                logger.warn("⚠️ Анализ документов доступен только Premium для: {}", telegramUserId);
            }

        } catch (Exception e) {
            logger.error("❌ Ошибка увеличения счётчика файлов: {}", e.getMessage(), e);
        }
    }

    /**
     * 🆕 ОБНОВЛЁННАЯ статистика пользователя
     */
    public String getUserStats(Long telegramUserId) {
        try {
            Optional<UserEntity> optionalUser = userRepository.findByTelegramUserId(telegramUserId);
            if (optionalUser.isEmpty()) {
                return "❌ Пользователь не найден";
            }

            UserEntity user = optionalUser.get();
            String status = user.getIsPremium() && user.isActivePremium()
                    ? "💎 Premium"
                    : "🆓 Бесплатный";

            // Для бесплатных пользователей
            if (!user.getIsPremium() || !user.isActivePremium()) {
                int used = user.getDailySearchRequests() != null ? user.getDailySearchRequests() : 0;
                int remaining = Math.max(0, 5 - used);

                return String.format(
                        "📊 *Ваша статистика:*\n\n" +
                                "👤 Статус: %s\n" +
                                "🔍 Поисков сегодня: %d/5 (осталось: %d)\n" +
                                "📄 Анализ документов: ❌ Только Premium\n\n" +
                                "📈 Всего запросов: %d",
                        status,
                        used,
                        remaining,
                        user.getTotalRequests()
                );
            }

            // Для Premium пользователей
            LocalDate expiryDate = user.getPremiumExpiryDate();
            String expiryInfo = expiryDate != null
                    ? "\n⏰ Активна до: " + expiryDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : "";

            return String.format(
                    "📊 *Ваша статистика:*\n\n" +
                            "👤 Статус: %s%s\n" +
                            "🔍 Поисков сегодня: %d (безлимит ♾️)\n" +
                            "📄 Анализ документов: ✅ Доступен\n\n" +
                            "📈 Всего запросов: %d\n" +
                            "📊 Проанализировано файлов: %d",
                    status,
                    expiryInfo,
                    user.getDailySearchRequests(),
                    user.getTotalRequests(),
                    user.getTotalFileRequests()
            );

        } catch (Exception e) {
            logger.error("❌ Ошибка получения статистики: {}", e.getMessage(), e);
            return "❌ Ошибка получения статистики";
        }
    }

    /**
     * Активация Premium подписки
     */
    @Transactional
    public void activatePremium(Long telegramUserId, int days) {
        try {
            Optional<UserEntity> optionalUser = userRepository.findByTelegramUserId(telegramUserId);
            if (optionalUser.isEmpty()) {
                logger.warn("⚠️ Пользователь не найден: {}", telegramUserId);
                return;
            }

            UserEntity user = optionalUser.get();
            user.activatePremium(days);
            userRepository.save(user);

            logger.info("✅ Premium активирован для {} на {} дней", telegramUserId, days);

        } catch (Exception e) {
            logger.error("❌ Ошибка активации Premium: {}", e.getMessage(), e);
        }
    }
}
