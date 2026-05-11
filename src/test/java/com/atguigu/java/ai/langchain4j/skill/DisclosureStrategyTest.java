package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.model.DisclosureLevel;
import com.atguigu.java.ai.langchain4j.skill.model.DisclosureStrategy;
import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisclosureStrategyTest {

    private final DisclosureStrategy strategy = new DisclosureStrategy();

    @Test
    void shouldReturnBasicForNewActivation() {
        SkillActivation activation = new SkillActivation("test", 10);

        DisclosureLevel level = strategy.calculateLevel(activation);

        assertThat(level).isEqualTo(DisclosureLevel.BASIC);
    }

    @Test
    void shouldUpgradeToStandardAfter3Turns() {
        SkillActivation activation = new SkillActivation("test", 10);

        // 模拟 3 轮对话
        activation.decrementRemainingTurns();
        activation.decrementRemainingTurns();
        activation.decrementRemainingTurns();

        DisclosureLevel level = strategy.calculateLevel(activation);

        assertThat(level).isEqualTo(DisclosureLevel.STANDARD);
        assertThat(activation.getActiveTurns()).isEqualTo(3);
    }

    @Test
    void shouldUpgradeToAdvancedAfter6Turns() {
        SkillActivation activation = new SkillActivation("test", 10);

        // 模拟 6 轮对话
        for (int i = 0; i < 6; i++) {
            activation.decrementRemainingTurns();
        }

        DisclosureLevel level = strategy.calculateLevel(activation);

        assertThat(level).isEqualTo(DisclosureLevel.ADVANCED);
    }

    @Test
    void shouldUpgradeToAdvancedAfter2ToolCalls() {
        SkillActivation activation = new SkillActivation("test", 10);

        activation.incrementToolCallCount();
        activation.incrementToolCallCount();

        DisclosureLevel level = strategy.calculateLevel(activation);

        assertThat(level).isEqualTo(DisclosureLevel.ADVANCED);
    }

    @Test
    void shouldUpgradeToAdvancedOnError() {
        SkillActivation activation = new SkillActivation("test", 10);

        activation.markError();

        DisclosureLevel level = strategy.calculateLevel(activation);

        assertThat(level).isEqualTo(DisclosureLevel.ADVANCED);
    }

    @Test
    void shouldReturnBasicForNullActivation() {
        DisclosureLevel level = strategy.calculateLevel(null);

        assertThat(level).isEqualTo(DisclosureLevel.BASIC);
    }
}
