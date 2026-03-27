package com.atguigu.java.ai.langchain4j.skill.session;

import com.atguigu.java.ai.langchain4j.skill.model.SkillSessionState;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SkillSessionStore {

    private final ConcurrentMap<Long, SkillSessionState> sessions = new ConcurrentHashMap<>();

    public SkillSessionState getOrCreate(Long memoryId) {
        Objects.requireNonNull(memoryId, "memoryId must not be null");
        return sessions.computeIfAbsent(memoryId, SkillSessionState::new);
    }

    public Optional<SkillSessionState> find(Long memoryId) {
        if (memoryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(memoryId));
    }

    public void remove(Long memoryId) {
        if (memoryId != null) {
            sessions.remove(memoryId);
        }
    }
}