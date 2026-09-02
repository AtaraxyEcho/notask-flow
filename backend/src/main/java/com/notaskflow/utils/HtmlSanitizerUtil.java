package com.notaskflow.utils;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * HTML安全净化工具，用于清理富文本中的危险标签和事件属性。
 *
 * @author LIN
 */
@Component
public class HtmlSanitizerUtil {

    private static final String[] BLOCK_ELEMENTS = {
            "article", "caption", "div", "figcaption", "figure", "hr", "mark", "section", "span", "sub", "sup"
    };

    private static final String[] MEDIA_ELEMENTS = {
            "audio", "source"
    };

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.IMAGES)
            .and(new HtmlPolicyBuilder()
                    .allowElements(BLOCK_ELEMENTS)
                    .allowElements(MEDIA_ELEMENTS)
                    .allowStyling()
                    .allowUrlProtocols("http", "https", "mailto", "tel", "blob")
                    .allowAttributes("alt", "aria-label", "class", "colspan", "controls", "preload", "rel", "rowspan",
                            "target", "title")
                    .globally()
                    .allowAttributes("data-attachment-id", "data-checked", "data-first-line-indent", "data-managed-file-id",
                            "data-mime", "data-name", "data-preview-href", "data-size", "data-task-state",
                            "data-text-align", "data-type", "data-url")
                    .globally()
                    .allowAttributes("href")
                    .onElements("a")
                    .allowAttributes("src")
                    .onElements("audio", "img", "source")
                    .toFactory());

    /**
     * 净化HTML内容。
     *
     * @param html 原始HTML
     * @return 安全HTML
     */
    public String sanitize(String html) {
        if (!StringUtils.hasText(html)) {
            return html;
        }
        return POLICY.sanitize(html);
    }
}
