package com.aibot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long telegramUserId;

    private String username;
    private LocalDateTime registrationDate;
    private LocalDateTime lastActive;

    private Boolean isPremium = false;
    private LocalDate premiumExpiryDate;

    private Integer dailySearchRequests = 0;
    private LocalDate lastSearchRequestDate;
    private Integer totalFileRequests = 0;
    private Integer totalRequests = 0;

    public UserEntity() {
        this.registrationDate = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
        this.isPremium = false;
        this.dailySearchRequests = 0;
        this.totalRequests = 0;
        this.totalFileRequests = 0;
    }

    // ✅ Только проверяет, не мутирует состояние
    public boolean canMakeSearchRequest(int dailySearchLimit) {
        if (isPremium != null && isPremium && isActivePremium()) return true;
        if (lastSearchRequestDate == null || !LocalDate.now().equals(lastSearchRequestDate)) {
            return true; // Новый день — сброс произойдёт в incrementSearchRequests()
        }
        return dailySearchRequests < dailySearchLimit;
    }

    public boolean canAnalyzeDocument() {
        return isPremium != null && isPremium && isActivePremium();
    }

    public void incrementSearchRequests() {
        if (dailySearchRequests == null) dailySearchRequests = 0;
        LocalDate today = LocalDate.now();
        if (!today.equals(lastSearchRequestDate)) {
            dailySearchRequests = 0;
            lastSearchRequestDate = today;
        }
        dailySearchRequests++;
        totalRequests++;
        lastActive = LocalDateTime.now();
    }

    public void incrementFileAnalysis() {
        if (totalFileRequests == null) totalFileRequests = 0;
        totalFileRequests++;
        totalRequests++;
        lastActive = LocalDateTime.now();
    }

    public boolean isActivePremium() {
        if (isPremium == null || !isPremium) return false;
        if (premiumExpiryDate == null) return false;
        return !LocalDate.now().isAfter(premiumExpiryDate);
    }

    public void activatePremium(int days) {
        this.isPremium = true;
        if (premiumExpiryDate != null && premiumExpiryDate.isAfter(LocalDate.now())) {
            this.premiumExpiryDate = premiumExpiryDate.plusDays(days);
        } else {
            this.premiumExpiryDate = LocalDate.now().plusDays(days);
        }
        this.lastActive = LocalDateTime.now();
    }

    // ==================== ГЕТТЕРЫ И СЕТТЕРЫ ====================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(Long telegramUserId) { this.telegramUserId = telegramUserId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
    public Boolean getIsPremium() { return isPremium; }
    public void setIsPremium(Boolean isPremium) { this.isPremium = isPremium; }
    public LocalDate getPremiumExpiryDate() { return premiumExpiryDate; }
    public void setPremiumExpiryDate(LocalDate premiumExpiryDate) { this.premiumExpiryDate = premiumExpiryDate; }
    public Integer getDailySearchRequests() { return dailySearchRequests; }
    public void setDailySearchRequests(Integer dailySearchRequests) { this.dailySearchRequests = dailySearchRequests; }
    public LocalDate getLastSearchRequestDate() { return lastSearchRequestDate; }
    public void setLastSearchRequestDate(LocalDate lastSearchRequestDate) { this.lastSearchRequestDate = lastSearchRequestDate; }
    public Integer getTotalFileRequests() { return totalFileRequests; }
    public void setTotalFileRequests(Integer totalFileRequests) { this.totalFileRequests = totalFileRequests; }
    public Integer getTotalRequests() { return totalRequests; }
    public void setTotalRequests(Integer totalRequests) { this.totalRequests = totalRequests; }
}