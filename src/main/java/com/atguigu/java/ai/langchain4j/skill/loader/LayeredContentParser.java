package com.atguigu.java.ai.langchain4j.skill.loader;

import com.atguigu.java.ai.langchain4j.skill.model.DisclosureLevel;
import com.atguigu.java.ai.langchain4j.skill.model.LayeredSkillContent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 SKILL.md 中的分层内容
 */
public class LayeredContentParser {

    private static final Pattern DISCLOSURE_MARKER = Pattern.compile(
            "<!--\\s*disclosure-level:\\s*(basic|standard|advanced)\\s*-->",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 解析分层内容
     *
     * @param content SKILL.md 的正文内容
     * @param progressive 是否启用渐进式披露
     * @return 分层内容对象
     */
    public LayeredSkillContent parse(String content, boolean progressive) {
        if (!progressive || content == null || content.isBlank()) {
            // 非渐进式或无内容，返回单层结构
            Map<DisclosureLevel, String> singleLayer = new HashMap<>();
            singleLayer.put(DisclosureLevel.BASIC, content != null ? content : "");
            return new LayeredSkillContent(singleLayer, false);
        }

        Map<DisclosureLevel, String> contentByLevel = new HashMap<>();
        Matcher matcher = DISCLOSURE_MARKER.matcher(content);

        int lastEnd = 0;
        DisclosureLevel currentLevel = DisclosureLevel.BASIC;
        StringBuilder currentContent = new StringBuilder();

        while (matcher.find()) {
            // 保存上一段内容
            if (lastEnd < matcher.start()) {
                String segment = content.substring(lastEnd, matcher.start()).trim();
                if (!segment.isEmpty()) {
                    if (currentContent.length() > 0) {
                        currentContent.append("\n\n");
                    }
                    currentContent.append(segment);
                }
            }

            // 保存当前级别的内容
            if (currentContent.length() > 0) {
                contentByLevel.merge(currentLevel, currentContent.toString(),
                        (old, newVal) -> old + "\n\n" + newVal);
                currentContent = new StringBuilder();
            }

            // 切换到新级别
            String levelName = matcher.group(1).toLowerCase();
            currentLevel = parseLevel(levelName);
            lastEnd = matcher.end();
        }

        // 处理最后一段内容
        if (lastEnd < content.length()) {
            String segment = content.substring(lastEnd).trim();
            if (!segment.isEmpty()) {
                if (currentContent.length() > 0) {
                    currentContent.append("\n\n");
                }
                currentContent.append(segment);
            }
        }

        if (currentContent.length() > 0) {
            contentByLevel.merge(currentLevel, currentContent.toString(),
                    (old, newVal) -> old + "\n\n" + newVal);
        }

        // 如果没有找到任何标记，将所有内容归为 BASIC
        if (contentByLevel.isEmpty()) {
            contentByLevel.put(DisclosureLevel.BASIC, content.trim());
        }

        return new LayeredSkillContent(contentByLevel, true);
    }

    private DisclosureLevel parseLevel(String levelName) {
        if ("standard".equals(levelName)) {
            return DisclosureLevel.STANDARD;
        } else if ("advanced".equals(levelName)) {
            return DisclosureLevel.ADVANCED;
        } else {
            return DisclosureLevel.BASIC;
        }
    }
}
