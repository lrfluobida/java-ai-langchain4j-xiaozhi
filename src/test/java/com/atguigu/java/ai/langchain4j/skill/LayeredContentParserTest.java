package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.loader.LayeredContentParser;
import com.atguigu.java.ai.langchain4j.skill.model.DisclosureLevel;
import com.atguigu.java.ai.langchain4j.skill.model.LayeredSkillContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LayeredContentParserTest {

    private final LayeredContentParser parser = new LayeredContentParser();

    @Test
    void shouldParseProgressiveContent() {
        String content = """
                # Skill Title

                ## When To Use
                Some description

                <!-- disclosure-level: basic -->
                ## Basic Instructions
                1. Basic instruction 1
                2. Basic instruction 2

                <!-- disclosure-level: standard -->
                ## Standard Instructions
                3. Standard instruction 1
                4. Standard instruction 2

                <!-- disclosure-level: advanced -->
                ## Advanced Instructions
                5. Advanced instruction 1
                6. Advanced instruction 2
                """;

        LayeredSkillContent result = parser.parse(content, true);

        assertThat(result.isProgressive()).isTrue();
        assertThat(result.getContentForLevel(DisclosureLevel.BASIC))
                .contains("When To Use")
                .contains("Basic instruction 1")
                .doesNotContain("Standard instruction")
                .doesNotContain("Advanced instruction");

        assertThat(result.getContentForLevel(DisclosureLevel.STANDARD))
                .contains("When To Use")
                .contains("Basic instruction 1")
                .contains("Standard instruction 1")
                .doesNotContain("Advanced instruction");

        assertThat(result.getContentForLevel(DisclosureLevel.ADVANCED))
                .contains("When To Use")
                .contains("Basic instruction 1")
                .contains("Standard instruction 1")
                .contains("Advanced instruction 1");
    }

    @Test
    void shouldHandleNonProgressiveContent() {
        String content = """
                # Skill Title

                All instructions here
                No disclosure markers
                """;

        LayeredSkillContent result = parser.parse(content, false);

        assertThat(result.isProgressive()).isFalse();
        assertThat(result.getAllContent()).isEqualTo(content);
    }

    @Test
    void shouldHandleContentWithoutMarkers() {
        String content = """
                # Skill Title

                All content without markers
                """;

        LayeredSkillContent result = parser.parse(content, true);

        assertThat(result.isProgressive()).isTrue();
        assertThat(result.getContentForLevel(DisclosureLevel.BASIC))
                .contains("All content without markers");
    }

    @Test
    void shouldHandleCaseInsensitiveMarkers() {
        String content = """
                <!-- disclosure-level: BASIC -->
                Basic content

                <!-- disclosure-level: Standard -->
                Standard content

                <!-- disclosure-level: advanced -->
                Advanced content
                """;

        LayeredSkillContent result = parser.parse(content, true);

        assertThat(result.getContentForLevel(DisclosureLevel.BASIC))
                .contains("Basic content")
                .doesNotContain("Standard content");

        assertThat(result.getContentForLevel(DisclosureLevel.STANDARD))
                .contains("Basic content")
                .contains("Standard content");
    }
}
