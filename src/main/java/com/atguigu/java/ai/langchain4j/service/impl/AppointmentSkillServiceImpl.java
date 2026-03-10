package com.atguigu.java.ai.langchain4j.service.impl;

import com.atguigu.java.ai.langchain4j.service.AppointmentSkillService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AppointmentSkillServiceImpl implements AppointmentSkillService {

    // 预约相关关键词
    private static final List<String> APPOINTMENT_KEYWORDS = List.of(
            "挂号", "预约", "预约挂号", "取消预约", "取消挂号", "号源", "门诊", "看病", "看诊"
    );

    // 已进入预约流程的会话
    private final Set<Long> activeAppointmentSessions = ConcurrentHashMap.newKeySet();

    // 预约技能规则
    private final String appointmentSkillRules = loadAppointmentSkillRules();

    @Override
    public String resolveAppointmentSkill(Long memoryId, String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }

        if (memoryId != null && isAppointmentMessage(userMessage)) {
            activeAppointmentSessions.add(memoryId);
        }

        if (memoryId != null && activeAppointmentSessions.contains(memoryId)) {
            return appointmentSkillRules;
        }

        return "";
    }

    private boolean isAppointmentMessage(String userMessage) {
        return APPOINTMENT_KEYWORDS.stream().anyMatch(userMessage::contains);
    }

    private String loadAppointmentSkillRules() {
        ClassPathResource resource = new ClassPathResource("skills/appointment-skill.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取预约技能规则失败", e);
        }
    }
}
