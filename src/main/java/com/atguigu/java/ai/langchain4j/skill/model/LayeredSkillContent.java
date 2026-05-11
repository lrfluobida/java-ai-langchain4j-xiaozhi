package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 分层的 Skill 内容
 */
public class LayeredSkillContent {

    private final Map<DisclosureLevel, String> contentByLevel;
    private final boolean progressive;

    public LayeredSkillContent(Map<DisclosureLevel, String> contentByLevel, boolean progressive) {
        this.contentByLevel = contentByLevel != null ? new HashMap<>(contentByLevel) : new HashMap<>();
        this.progressive = progressive;
    }

    /**
     * 获取指定披露级别应该包含的完整内容
     */
    public String getContentForLevel(DisclosureLevel level) {
        if (!progressive || contentByLevel.isEmpty()) {
            // 非渐进式或无分层内容，返回所有内容
            return getAllContent();
        }

        StringBuilder result = new StringBuilder();
        for (DisclosureLevel l : DisclosureLevel.values()) {
            if (level.includes(l) && contentByLevel.containsKey(l)) {
                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(contentByLevel.get(l));
            }
        }
        return result.toString();
    }

    /**
     * 获取所有内容（用于非渐进式场景）
     */
    public String getAllContent() {
        StringBuilder result = new StringBuilder();
        for (DisclosureLevel level : DisclosureLevel.values()) {
            if (contentByLevel.containsKey(level)) {
                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(contentByLevel.get(level));
            }
        }
        return result.toString();
    }

    public boolean isProgressive() {
        return progressive;
    }

    public Map<DisclosureLevel, String> getContentByLevel() {
        return new HashMap<>(contentByLevel);
    }
}
