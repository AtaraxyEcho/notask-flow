package com.notaskflow.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import com.notaskflow.common.enums.NoteExportFormat;
import com.notaskflow.domain.vo.NoteExportFileVO;
import com.notaskflow.domain.vo.NoteVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.service.NoteExportService;
import com.notaskflow.service.NoteService;
import com.notaskflow.utils.HtmlSanitizerUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 笔记导出服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteExportServiceImpl implements NoteExportService {

    static {
        XRLog.setLoggingEnabled(false);
    }

    /** 导出文档时间格式。 */
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 导出图片渲染分辨率。 */
    private static final float IMAGE_RENDER_DPI = 168F;

    /** 多页图片之间的间距。 */
    private static final int IMAGE_PAGE_GAP = 24;

    /** 常见中文字体路径，用于提升 PDF/图片导出中文渲染兼容性。 */
    private static final List<String> FONT_PATHS = List.of(
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "C:\\Windows\\Fonts\\msyh.ttc",
            "C:\\Windows\\Fonts\\simsun.ttc"
    );

    private final NoteService noteService;

    private final HtmlSanitizerUtil htmlSanitizerUtil;

    /**
     * 导出笔记文件。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @param format 导出格式
     * @return 导出文件
     */
    @Override
    public NoteExportFileVO export(Long spaceId, Long noteId, NoteExportFormat format) {
        NoteVO note = noteService.get(spaceId, noteId);
        String html = buildExportHtml(note);
        byte[] content = switch (format) {
            case PDF -> renderPdf(html);
            case WORD -> renderWord(html);
            case IMAGE -> renderImage(html);
        };
        return new NoteExportFileVO(
                buildFileName(note.getTitle(), format.extension()),
                format.contentType(),
                content
        );
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            registerFonts(builder);
            builder.useFastMode();
            builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(html)), null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PDF导出失败");
        }
    }

    private byte[] renderWord(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            WordprocessingMLPackage packageFile = WordprocessingMLPackage.createPackage();
            XHTMLImporterImpl importer = new XHTMLImporterImpl(packageFile);
            packageFile.getMainDocumentPart().getContent().addAll(importer.convert(html, null));
            packageFile.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            log.warn("Word导出失败", exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Word导出失败");
        }
    }

    private byte[] renderImage(String html) {
        byte[] pdfContent = renderPdf(html);
        try (PDDocument document = Loader.loadPDF(pdfContent);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (document.getNumberOfPages() == 0) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片导出失败");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> pages = renderPdfPages(renderer, document.getNumberOfPages());
            BufferedImage combinedImage = combinePages(pages);
            ImageIO.write(combinedImage, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片导出失败");
        }
    }

    private List<BufferedImage> renderPdfPages(PDFRenderer renderer, int pageCount) throws IOException {
        ArrayList<BufferedImage> pages = new ArrayList<>();
        for (int index = 0; index < pageCount; index++) {
            pages.add(renderer.renderImageWithDPI(index, IMAGE_RENDER_DPI, ImageType.RGB));
        }
        return pages;
    }

    private BufferedImage combinePages(List<BufferedImage> pages) {
        int width = pages.stream().mapToInt(BufferedImage::getWidth).max().orElse(1);
        int height = pages.stream().mapToInt(BufferedImage::getHeight).sum()
                + IMAGE_PAGE_GAP * Math.max(0, pages.size() - 1);
        BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = combinedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            int currentY = 0;
            for (BufferedImage page : pages) {
                int currentX = (width - page.getWidth()) / 2;
                graphics.drawImage(page, currentX, currentY, null);
                currentY += page.getHeight() + IMAGE_PAGE_GAP;
            }
        } finally {
            graphics.dispose();
        }
        return combinedImage;
    }

    private void registerFonts(PdfRendererBuilder builder) {
        for (String fontPath : FONT_PATHS) {
            File fontFile = Path.of(fontPath).toFile();
            if (fontFile.isFile()) {
                builder.useFont(fontFile, "NotaskExportFont");
            }
        }
    }

    private String buildExportHtml(NoteVO note) {
        Document document = Document.createShell("");
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false);
        document.head().appendElement("meta").attr("charset", StandardCharsets.UTF_8.name());
        document.head().appendElement("style").appendText(exportCss());
        Element article = document.body().appendElement("main").addClass("note-document");
        article.appendElement("h1").text(defaultTitle(note.getTitle()));
        article.appendElement("div").addClass("note-meta").text(buildMetaText(note));
        article.appendElement("section").addClass("note-content").append(resolveBodyHtml(note));
        return "<!DOCTYPE html>\n" + document.outerHtml();
    }

    private String resolveBodyHtml(NoteVO note) {
        if (StringUtils.hasText(note.getContentHtml())) {
            return normalizeExportFileCards(htmlSanitizerUtil.sanitize(note.getContentHtml()));
        }
        String content = note.getContent();
        if (!StringUtils.hasText(content)) {
            return "<p></p>";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : content.split("\\R", -1)) {
            builder.append("<p>")
                    .append(Entities.escape(Jsoup.parse(line).text()))
                    .append("</p>");
        }
        return builder.toString();
    }

    private String normalizeExportFileCards(String html) {
        if (!StringUtils.hasText(html)) {
            return html;
        }
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false);
        Elements cards = document.select(".file-card[data-managed-file-id], .notask-audio-card[data-managed-file-id], "
                + ".notask-media-card[data-managed-file-id], a[data-managed-file-id]");
        for (Element card : cards) {
            card.replaceWith(buildExportFileCard(document, card));
        }
        return document.body().html();
    }

    private Element buildExportFileCard(Document document, Element source) {
        String mimeType = source.attr("data-mime");
        String fileName = resolveExportFileName(source);
        String fileUrl = resolveExportFileUrl(source);
        String sizeLabel = resolveExportFileSize(source);
        boolean audio = isAudioFile(source, mimeType, fileName);
        Element card = document.createElement("div")
                .addClass("export-file-card")
                .addClass(audio ? "export-file-card-audio" : "export-file-card-attachment")
                .attr("data-managed-file-id", source.attr("data-managed-file-id"));
        if (StringUtils.hasText(source.attr("data-attachment-id"))) {
            card.attr("data-attachment-id", source.attr("data-attachment-id"));
        }
        Element icon = document.createElement("span")
                .addClass("export-file-icon")
                .text(audio ? "音频" : "文件");
        Element main = document.createElement("span").addClass("export-file-main");
        main.appendElement("strong").text(fileName);
        main.appendElement("span").text(buildExportFileMeta(audio, mimeType, sizeLabel));
        if (StringUtils.hasText(fileUrl)) {
            main.appendElement("a")
                    .addClass("export-file-link")
                    .attr("href", fileUrl)
                    .attr("target", "_blank")
                    .text("打开/下载");
        }
        card.appendChild(icon);
        card.appendChild(main);
        return card;
    }

    private String resolveExportFileName(Element source) {
        if (StringUtils.hasText(source.attr("data-name"))) {
            return source.attr("data-name").trim();
        }
        Element nameElement = source.selectFirst(".notask-media-card-name, .notask-audio-name, .file-main strong, strong");
        if (nameElement != null && StringUtils.hasText(nameElement.text())) {
            return nameElement.text().trim();
        }
        String text = source.text();
        return StringUtils.hasText(text) ? text.trim() : "附件";
    }

    private String resolveExportFileUrl(Element source) {
        if (StringUtils.hasText(source.attr("data-url"))) {
            return source.attr("data-url").trim();
        }
        if (StringUtils.hasText(source.attr("data-preview-href"))) {
            return source.attr("data-preview-href").trim();
        }
        if (StringUtils.hasText(source.attr("href"))) {
            return source.attr("href").trim();
        }
        Element link = source.selectFirst("a[href]");
        if (link != null && StringUtils.hasText(link.attr("href"))) {
            return link.attr("href").trim();
        }
        Element audio = source.selectFirst("audio[src], source[src]");
        if (audio != null && StringUtils.hasText(audio.attr("src"))) {
            return audio.attr("src").trim();
        }
        return "";
    }

    private String resolveExportFileSize(Element source) {
        if (StringUtils.hasText(source.attr("data-size"))) {
            return source.attr("data-size").trim();
        }
        Element meta = source.selectFirst(".notask-media-card-meta, .notask-audio-meta, .file-meta");
        if (meta != null && meta.text().contains("B")) {
            return meta.text().trim();
        }
        return "大小未知";
    }

    private boolean isAudioFile(Element source, String mimeType, String fileName) {
        String type = source.attr("data-type");
        if ("audio".equalsIgnoreCase(type) || "audio-file-card".equalsIgnoreCase(type)) {
            return true;
        }
        if (StringUtils.hasText(mimeType) && mimeType.toLowerCase().startsWith("audio/")) {
            return true;
        }
        String normalizedName = fileName.toLowerCase();
        return normalizedName.endsWith(".mp3")
                || normalizedName.endsWith(".wav")
                || normalizedName.endsWith(".m4a")
                || normalizedName.endsWith(".aac")
                || normalizedName.endsWith(".ogg")
                || normalizedName.endsWith(".flac");
    }

    private String buildExportFileMeta(boolean audio, String mimeType, String sizeLabel) {
        String typeLabel = audio ? "音频文件" : "附件";
        String mimeLabel = StringUtils.hasText(mimeType) ? mimeType.trim() : typeLabel;
        return typeLabel + " · " + mimeLabel + " · " + sizeLabel;
    }

    private String buildMetaText(NoteVO note) {
        String modifiedAt = note.getGmtModified() == null ? "" : EXPORT_TIME_FORMATTER.format(note.getGmtModified());
        String projectName = StringUtils.hasText(note.getProjectName()) ? " | " + note.getProjectName() : "";
        return modifiedAt + projectName;
    }

    private String buildFileName(String title, String extension) {
        String safeTitle = defaultTitle(title).replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        if (safeTitle.length() > 80) {
            safeTitle = safeTitle.substring(0, 80);
        }
        return safeTitle + "." + extension;
    }

    private String defaultTitle(String title) {
        return StringUtils.hasText(title) ? title.trim() : "Notask Flow 笔记";
    }

    private String exportCss() {
        return """
                @page { size: A4; margin: 20mm 18mm; }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  background: #ffffff;
                  color: #242424;
                  font-family: NotaskExportFont, "Noto Sans CJK SC", "Microsoft YaHei", "PingFang SC", sans-serif;
                  line-height: 1.6;
                }
                .note-document { width: 100%; }
                h1 {
                  margin: 0 0 8px;
                  font-size: 28px;
                  line-height: 1.25;
                  font-weight: 700;
                  color: #1f1f1f;
                }
                .note-meta {
                  margin-bottom: 24px;
                  color: #777777;
                  font-size: 12px;
                }
                .note-content { font-size: 14px; }
                .note-content p { margin: 0 0 10px; }
                .note-content p[data-first-line-indent="true"] { text-indent: 2em; }
                .note-content [data-text-align="center"] { text-align: center; }
                .note-content [data-text-align="right"] { text-align: right; }
                .note-content h1, .note-content h2, .note-content h3 {
                  margin: 20px 0 10px;
                  color: #1f1f1f;
                  line-height: 1.35;
                }
                .note-content blockquote {
                  margin: 12px 0;
                  padding: 10px 14px;
                  border-left: 4px solid #cfcfcf;
                  background: #f7f7f7;
                  color: #555555;
                }
                .note-content table {
                  width: 100%;
                  border-collapse: collapse;
                  margin: 14px 0;
                  table-layout: fixed;
                }
                .note-content th, .note-content td {
                  border: 1px solid #d6d6d6;
                  padding: 8px;
                  vertical-align: top;
                  word-wrap: break-word;
                }
                .note-content img {
                  display: block;
                  max-width: 100%;
                  height: auto;
                  margin: 12px 0;
                }
                .note-content .notask-audio-card,
                .note-content .file-card,
                .note-content .export-file-card {
                  margin: 12px 0;
                  padding: 12px 14px;
                  border: 1px solid #dddddd;
                  border-radius: 12px;
                  background: #fafafa;
                }
                .note-content .export-file-card {
                  display: table;
                  width: 100%;
                  border-color: #d9e2f0;
                  background: #f8fbff;
                }
                .note-content .export-file-icon {
                  display: table-cell;
                  width: 54px;
                  height: 42px;
                  padding-right: 12px;
                  color: #2563eb;
                  font-size: 12px;
                  font-weight: 700;
                  text-align: center;
                  vertical-align: middle;
                }
                .note-content .export-file-main {
                  display: table-cell;
                  vertical-align: middle;
                }
                .note-content .export-file-main strong {
                  display: block;
                  color: #1f2937;
                  font-size: 14px;
                }
                .note-content .export-file-main span {
                  display: block;
                  margin-top: 3px;
                  color: #667085;
                  font-size: 12px;
                }
                .note-content .export-file-link {
                  display: inline-block;
                  margin-top: 8px;
                  color: #2563eb;
                  font-size: 12px;
                  text-decoration: underline;
                }
                """;
    }
}
