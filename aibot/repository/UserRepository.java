package com.aibot.repository;

import com.aibot.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByTelegramUserId(Long telegramUserId);

    // ✅ Делегирующий default-метод вместо отдельного @Query
    default Optional<UserEntity> findByTelegramId(Long telegramId) {
        return findByTelegramUserId(telegramId);
    }

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isPremium = true AND u.premiumExpiryDate > CURRENT_DATE")
    long countActivePremiumUsers();

    @Query("SELECT COUNT(u) FROM UserEntity u")
    long countTotalUsers();

    @Query("SELECT COALESCE(SUM(u.totalRequests), 0) FROM UserEntity u")
    long sumTotalRequests();

    @Query("SELECT COALESCE(SUM(u.totalFileRequests), 0) FROM UserEntity u")
    long sumTotalFileRequests();

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.dailySearchRequests >= :limit AND u.lastSearchRequestDate = CURRENT_DATE AND (u.isPremium = false OR u.isPremium IS NULL)")
    long countUsersReachedDailyLimit(int limit);
}