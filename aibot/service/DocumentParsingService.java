package com.aibot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class DocumentParsingService {

    public String parseDocument(String filePath, String mimeType) {
        try {
            log.info("📄 Начинаем парсинг документа: {}", filePath);
            return switch (mimeType) {
                case "application/pdf" -> parsePDF(filePath);
                case "application/msword",
                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocx(filePath);
                case "text/plain" -> parseTxt(filePath);
                default -> throw new IllegalArgumentException("Неподдерживаемый формат: " + mimeType);
            };
        } catch (Exception e) {
            log.error("❌ Ошибка при парсинге документа: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось распарить документ", e);
        }
    }

    // ✅ try-with-resources — нет утечки ресурсов
    private String parsePDF(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            log.info("📕 Парсинг PDF файла...");
            String text = new PDFTextStripper().getText(document);
            log.info("✅ PDF распарсен: {} символов", text.length());
            return text.trim();
        } catch (IOException e) {
            log.error("❌ Ошибка при парсинге PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при чтении PDF", e);
        }
    }

    // ✅ try-with-resources
    private String parseDocx(String filePath) {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(Paths.get(filePath)))) {
            log.info("📘 Парсинг DOCX файла...");
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph p : document.getParagraphs()) {
                text.append(p.getText()).append("\n");
            }
            log.info("✅ DOCX распарсен: {} символов", text.length());
            return text.toString().trim();
        } catch (IOException e) {
            log.error("❌ Ошибка при парсинге DOCX: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при чтении DOCX", e);
        }
    }

    private String parseTxt(String filePath) {
        try {
            log.info("📝 Парсинг TXT файла...");
            String text = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            log.info("✅ TXT распарсен: {} символов", text.length());
            return text.trim();
        } catch (IOException e) {
            log.error("❌ Ошибка при парсинге TXT: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при чтении TXT", e);
        }
    }

    // ✅ toLowerCase() — регистронезависимое определение типа
    public String getMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc"))  return "application/msword";
        if (lower.endsWith(".txt"))  return "text/plain";
        return "application/octet-stream";
    }

    public boolean isSupported(String mimeType) {
        return mimeType.equals("application/pdf") ||
                mimeType.equals("application/msword") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                mimeType.equals("text/plain");
    }

    public String parseDocumentFromBytes(byte[] fileContent, String mimeType) {
        try {
            log.info("📄 Парсинг документа из byte array...");
            return switch (mimeType) {
                case "application/pdf" -> parsePDFFromBytes(fileContent);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocxFromBytes(fileContent);
                case "text/plain" -> new String(fileContent, StandardCharsets.UTF_8).trim();
                default -> throw new IllegalArgumentException("Неподдерживаемый формат: " + mimeType);
            };
        } catch (Exception e) {
            log.error("❌ Ошибка при парсинге документа: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось распарить документ", e);
        }
    }

    // ✅ try-with-resources
    private String parsePDFFromBytes(byte[] fileContent) {
        try (PDDocument document = Loader.loadPDF(fileContent)) {
            log.info("📕 Парсинг PDF из byte array...");
            String text = new PDFTextStripper().getText(document);
            log.info("✅ PDF распарсен: {} символов", text.length());
            return text.trim();
        } catch (IOException e) {
            log.error("❌ Ошибка при парсинге PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при чтении PDF", e);
        }
    }

    // ✅ try-with-resources
    private String parseDocxFromBytes(byte[] fileContent) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileContent))) {
            log.info("📘 Парсинг DOCX из byte array...");
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph p : document.getParagraphs()) {
                text.append(p.getText()).append("\n");
            }
            log.info("✅ DOCX распарсен: {} символов", text.length());
            return text.toString().trim();
        } catch (IOException e) {
            log.error("❌ Ошибка при парсинге DOCX: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при чтении DOCX", e);
        }
    }
}