package com.aibot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final WebClient webClient;
    private final EnhancedAiTelegramBot bot;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.files.temp-dir:./temp_files}")
    private String tempDir;

    /**
     * ✅ Скачивание документа из Telegram
     */
    public byte[] downloadDocument(Message message) {
        try {
            if (!message.hasDocument()) {
                throw new IllegalArgumentException("Сообщение не содержит документа");
            }

            Document document = message.getDocument();
            String fileId = document.getFileId();
            String fileName = document.getFileName();

            log.info("📥 Скачивание документа: {}", fileName);

            // ✅ ИСПРАВЛЕНО: используем GetFile методом execute
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);

            File telegramFile = bot.execute(getFile);
            String filePath = telegramFile.getFilePath();

            // Формируем URL для скачивания
            String downloadUrl = String.format(
                    "https://api.telegram.org/file/bot%s/%s",
                    botToken, filePath
            );

            log.debug("🔗 URL для скачивания: {}", downloadUrl);

            // Скачиваем файл
            byte[] fileContent = webClient.get()
                    .uri(downloadUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            log.info("✅ Документ скачан: {} (размер: {} KB)", fileName, fileContent.length / 1024);
            return fileContent;

        } catch (TelegramApiException e) {
            log.error("❌ Ошибка Telegram API: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить файл из Telegram", e);
        } catch (Exception e) {
            log.error("❌ Ошибка при скачивании документа: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось скачать документ", e);
        }
    }

    /**
     * ✅ Скачивание изображения из Telegram
     */
    public byte[] downloadPhoto(Message message) {
        try {
            if (!message.hasPhoto()) {
                throw new IllegalArgumentException("Сообщение не содержит фото");
            }

            // Получаем самое большое фото
            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largestPhoto = photos.stream()
                    .max((p1, p2) -> Integer.compare(
                            p1.getHeight() * p1.getWidth(),
                            p2.getHeight() * p2.getWidth()
                    ))
                    .orElseThrow(() -> new IllegalArgumentException("Фото не найдено"));

            String fileId = largestPhoto.getFileId();

            log.info("📥 Скачивание фото: {}x{}", largestPhoto.getWidth(), largestPhoto.getHeight());

            // ✅ ИСПРАВЛЕНО: используем GetFile методом execute
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);

            File telegramFile = bot.execute(getFile);
            String filePath = telegramFile.getFilePath();

            // Формируем URL для скачивания
            String downloadUrl = String.format(
                    "https://api.telegram.org/file/bot%s/%s",
                    botToken, filePath
            );

            // Скачиваем файл
            byte[] fileContent = webClient.get()
                    .uri(downloadUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            log.info("✅ Фото скачано (размер: {} KB)", fileContent.length / 1024);
            return fileContent;

        } catch (TelegramApiException e) {
            log.error("❌ Ошибка Telegram API: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить фото из Telegram", e);
        } catch (Exception e) {
            log.error("❌ Ошибка при скачивании фото: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось скачать фото", e);
        }
    }

    /**
     * ✅ Сохранение временного файла
     */
    public String saveTempFile(byte[] fileContent, String fileName) {
        try {
            // Создаём временную директорию если её нет
            Path tempPath = Paths.get(tempDir);
            Files.createDirectories(tempPath);

            // Генерируем уникальное имя файла
            String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
            Path filePath = tempPath.resolve(uniqueFileName);

            // Сохраняем файл
            Files.write(filePath, fileContent);

            log.info("✅ Файл сохранён: {}", filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {
            log.error("❌ Ошибка при сохранении файла: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось сохранить файл", e);
        }
    }

    /**
     * ✅ Удаление временного файла
     */
    public void deleteTempFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("✅ Временный файл удалён: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("⚠️ Не удалось удалить временный файл: {}", e.getMessage());
        }
    }

    /**
     * ✅ Получение имени файла из сообщения
     */
    public String getFileName(Message message) {
        if (message.hasDocument()) {
            return message.getDocument().getFileName();
        } else if (message.hasPhoto()) {
            return "screenshot_" + System.currentTimeMillis() + ".jpg";
        }
        return "file_" + System.currentTimeMillis();
    }
}