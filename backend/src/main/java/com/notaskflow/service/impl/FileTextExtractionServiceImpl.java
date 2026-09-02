package com.notaskflow.service.impl;

import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.service.FileTextExtractionService;
import com.notaskflow.storage.MinioStorageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

/**
 * 基于 MinIO 对象内容的文件文本提取服务。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTextExtractionServiceImpl implements FileTextExtractionService {

    private static final int MAX_TEXT_EXTRACT_BYTES = 1024 * 1024;

    private static final int MAX_DEEP_EXTRACT_CHARS = 200_000;

    private static final long MAX_DEEP_EXTRACT_FILE_SIZE = 20L * 1024L * 1024L;

    private static final Set<String> TEXT_MIME_TYPES = Set.of(
            "application/json",
            "application/javascript",
            "application/xml",
            "application/x-yaml",
            "application/yaml",
            "text/markdown",
            "text/csv");

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".csv",
            ".json",
            ".log",
            ".md",
            ".markdown",
            ".txt",
            ".xml",
            ".yaml",
            ".yml");

    private static final Set<String> DEEP_EXTRACT_MIME_TYPES = Set.of(
            "application/msword",
            "application/pdf",
            "application/rtf",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.oasis.opendocument.presentation",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.text");

    private static final Set<String> DEEP_EXTRACT_EXTENSIONS = Set.of(
            ".doc",
            ".docx",
            ".odp",
            ".ods",
            ".odt",
            ".pdf",
            ".ppt",
            ".pptx",
            ".rtf",
            ".xls",
            ".xlsx");

    private final MinioStorageService minioStorageService;

    /**
     * 从附件对象中提取可搜索文本。
     *
     * @param attachment 附件元数据
     * @return 可搜索文本，无法提取时返回空字符串
     */
    @Override
    public String extract(Attachment attachment) {
        if (!hasStoragePath(attachment)) {
            return "";
        }
        if (isTextAttachment(attachment)) {
            return extractPlainText(attachment);
        }
        if (isDeepExtractAttachment(attachment)) {
            return extractByTika(attachment);
        }
        return "";
    }

    /**
     * 从附件对象中提取 HTML 预览内容。
     *
     * @param attachment 附件元数据
     * @return HTML 预览内容，无法提取时返回空字符串
     */
    @Override
    public String extractHtml(Attachment attachment) {
        if (!hasStoragePath(attachment) || !isDeepExtractAttachment(attachment)) {
            return "";
        }
        if (isDeepExtractFileTooLarge(attachment)) {
            log.info("文件超过 HTML 预览解析大小限制，跳过提取，attachmentId={}, fileSize={}",
                    attachment.getId(), attachment.getFileSize());
            return "";
        }
        try (InputStream inputStream = minioStorageService.openObject(attachment.getStoragePath())) {
            Parser parser = new AutoDetectParser();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ToHTMLContentHandler handler = new ToHTMLContentHandler(outputStream, StandardCharsets.UTF_8.name());
            Metadata metadata = buildMetadata(attachment);
            parser.parse(inputStream, handler, metadata, new ParseContext());
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (RuntimeException | TikaException | SAXException | IOException exception) {
            log.warn("文件 HTML 预览提取失败，attachmentId={}", attachment.getId(), exception);
            return "";
        }
    }

    private String extractPlainText(Attachment attachment) {
        try {
            byte[] bytes = minioStorageService.readObject(attachment.getStoragePath(), MAX_TEXT_EXTRACT_BYTES);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            log.warn("文件文本提取失败，attachmentId={}", attachment.getId(), exception);
            return "";
        }
    }

    private String extractByTika(Attachment attachment) {
        if (isDeepExtractFileTooLarge(attachment)) {
            log.info("文件超过深度解析大小限制，跳过提取，attachmentId={}, fileSize={}",
                    attachment.getId(), attachment.getFileSize());
            return "";
        }
        try (InputStream inputStream = minioStorageService.openObject(attachment.getStoragePath())) {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(MAX_DEEP_EXTRACT_CHARS);
            Metadata metadata = buildMetadata(attachment);
            parser.parse(inputStream, handler, metadata, new ParseContext());
            return handler.toString().trim();
        } catch (RuntimeException | TikaException | SAXException | IOException exception) {
            log.warn("文件深度文本提取失败，attachmentId={}", attachment.getId(), exception);
            return "";
        }
    }

    private Metadata buildMetadata(Attachment attachment) {
        Metadata metadata = new Metadata();
        if (StringUtils.hasText(attachment.getFileName())) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, attachment.getFileName());
        }
        if (StringUtils.hasText(attachment.getMimeType())) {
            metadata.set(Metadata.CONTENT_TYPE, attachment.getMimeType());
        }
        return metadata;
    }

    private boolean hasStoragePath(Attachment attachment) {
        return attachment != null && StringUtils.hasText(attachment.getStoragePath());
    }

    private boolean isTextAttachment(Attachment attachment) {
        return isTextMimeType(attachment.getMimeType()) || isTextFileName(attachment.getFileName());
    }

    private boolean isDeepExtractAttachment(Attachment attachment) {
        return isDeepExtractMimeType(attachment.getMimeType()) || isDeepExtractFileName(attachment.getFileName());
    }

    private boolean isDeepExtractFileTooLarge(Attachment attachment) {
        return attachment.getFileSize() != null && attachment.getFileSize() > MAX_DEEP_EXTRACT_FILE_SIZE;
    }

    private boolean isTextMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return false;
        }
        String normalizedMimeType = mimeType.toLowerCase(Locale.ROOT);
        return normalizedMimeType.startsWith("text/") || TEXT_MIME_TYPES.contains(normalizedMimeType);
    }

    private boolean isDeepExtractMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return false;
        }
        String normalizedMimeType = mimeType.toLowerCase(Locale.ROOT);
        return DEEP_EXTRACT_MIME_TYPES.contains(normalizedMimeType);
    }

    private boolean isTextFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        return TEXT_EXTENSIONS.stream().anyMatch(normalizedFileName::endsWith);
    }

    private boolean isDeepExtractFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        return DEEP_EXTRACT_EXTENSIONS.stream().anyMatch(normalizedFileName::endsWith);
    }
}
