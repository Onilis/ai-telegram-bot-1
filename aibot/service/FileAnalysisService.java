package com.aibot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAnalysisService {

    private final FileDownloadService fileDownloadService;
    private final DocumentParsingService documentParsingService;
    private final ImageAnalysisService imageAnalysisService;
    private final GigaChatService gigaChatService;

    /**
     * ✅ ГЛАВНЫЙ МЕТОД: Анализ файла (документ или изображение)
     */
    public String analyzeFile(Message message) {
        try {
            if (message.hasDocument()) {
                return analyzeDocumentFromMessage(message);
            } else if (message.hasPhoto()) {
                return analyzePhotoFromMessage(message);
            }
            return "❌ Неподдерживаемый тип файла";
        } catch (Exception e) {
            log.error("❌ Ошибка анализа файла: {}", e.getMessage(), e);
            return "❌ Ошибка при анализе файла. Попробуйте другой файл.";
        }
    }

    private String analyzeDocumentFromMessage(Message message) {
        try {
            Document document = message.getDocument();
            String fileName = document.getFileName();
            String mimeType = documentParsingService.getMimeType(fileName); // ✅ регистронезависимо

            log.info("📄 Начинаем анализ документа: {} ({})", fileName, mimeType);

            if (!documentParsingService.isSupported(mimeType)) {
                return "❌ Неподдерживаемый формат: " + mimeType + "\n\nПоддерживаются: PDF, DOCX, TXT";
            }

            byte[] fileContent = fileDownloadService.downloadDocument(message);
            String documentContent = documentParsingService.parseDocumentFromBytes(fileContent, mimeType);

            if (documentContent.isBlank()) {
                return "❌ Документ пустой или невозможно прочитать";
            }

            String analysis = gigaChatService.analyzeDocument(
                    documentContent,
                    fileName,
                    "Проанализируй этот документ и дай краткое резюме с основными моментами."
            );

            log.info("✅ Анализ документа завершён");
            return "📄 *Анализ документа:* " + fileName + "\n\n" + analysis;

        } catch (Exception e) {
            log.error("❌ Ошибка при анализе документа: {}", e.getMessage(), e);
            return "❌ Ошибка при анализе документа: " + e.getMessage();
        }
    }

    private String analyzePhotoFromMessage(Message message) {
        try {
            log.info("📸 Начинаем анализ скриншота");

            byte[] imageContent = fileDownloadService.downloadPhoto(message);

            if (!imageAnalysisService.isImageSizeValid(imageContent)) {
                return "❌ Размер изображения слишком большой (максимум 5 MB)";
            }

            String question = message.getCaption();
            String fileName = "screenshot_" + System.currentTimeMillis() + ".jpg";

            String analysis = (question != null && !question.isBlank())
                    ? imageAnalysisService.analyzeImage(imageContent, fileName, question)
                    : imageAnalysisService.analyzeScreenshot(imageContent, fileName);

            log.info("✅ Анализ изображения завершён");
            return "🖼 *Анализ скриншота:*\n\n" + analysis;

        } catch (Exception e) {
            log.error("❌ Ошибка при анализе изображения: {}", e.getMessage(), e);
            return "❌ Ошибка при анализе изображения: " + e.getMessage();
        }
    }

    public String analyzeDocumentByPath(String filePath, String mimeType) {
        try {
            log.info("📄 Анализ документа по пути: {}", filePath);
            String documentContent = documentParsingService.parseDocument(filePath, mimeType);
            if (documentContent.isBlank()) return "❌ Документ пустой";
            return gigaChatService.analyzeDocument(documentContent, filePath, "Проанализируй этот документ и дай краткое резюме.");
        } catch (Exception e) {
            log.error("❌ Ошибка при анализе документа: {}", e.getMessage(), e);
            return "❌ Ошибка при анализе: " + e.getMessage();
        }
    }

    public String analyzeImageByPath(String filePath, String question) {
        try {
            log.info("🖼 Анализ изображения по пути: {}", filePath);
            byte[] imageContent = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            return imageAnalysisService.analyzeImage(imageContent, filePath, question);
        } catch (Exception e) {
            log.error("❌ Ошибка при анализе изображения: {}", e.getMessage(), e);
            return "❌ Ошибка при анализе: " + e.getMessage();
        }
    }

    /**
     * ✅ Исправлено: для фото берём реальный размер из PhotoSize
     */
    public FileInfo getFileInfo(Message message) {
        if (message.hasDocument()) {
            Document doc = message.getDocument();
            return new FileInfo(doc.getFileName(), doc.getMimeType(), doc.getFileSize(), "DOCUMENT");
        } else if (message.hasPhoto()) {
            // ✅ Берём самое большое фото (последнее в списке)
            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largest = photos.stream()
                    .max(Comparator.comparingInt(PhotoSize::getFileSize))
                    .orElse(photos.get(photos.size() - 1));
            return new FileInfo(
                    "screenshot_" + System.currentTimeMillis() + ".jpg",
                    "image/jpeg",
                    largest.getFileSize(),
                    "IMAGE"
            );
        }
        return null;
    }

    public static class FileInfo {
        public final String fileName;
        public final String mimeType;
        public final long fileSize;
        public final String type;

        public FileInfo(String fileName, String mimeType, long fileSize, String type) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.type = type;
        }

        public boolean isDocument() { return "DOCUMENT".equals(type); }
        public boolean isImage()    { return "IMAGE".equals(type); }

        public String getSizeInMB() {
            if (fileSize == 0) return "N/A";
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
    }
}