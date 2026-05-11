package com.atguigu.java.ai.langchain4j.skill.loader;

import com.atguigu.java.ai.langchain4j.skill.config.SkillProperties;
import com.atguigu.java.ai.langchain4j.skill.model.LayeredSkillContent;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillFrontMatter;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class ClasspathSkillLoader implements SkillLoader {

    private static final String SKILL_PATTERN = "classpath*:skills/*/SKILL.md";

    private final SkillProperties skillProperties;
    private final ResourcePatternResolver resourcePatternResolver;
    private final LayeredContentParser layeredContentParser;

    @Autowired
    public ClasspathSkillLoader(SkillProperties skillProperties) {
        this(skillProperties, new PathMatchingResourcePatternResolver());
    }

    ClasspathSkillLoader(SkillProperties skillProperties, ResourcePatternResolver resourcePatternResolver) {
        this.skillProperties = Objects.requireNonNull(skillProperties, "skillProperties must not be null");
        this.resourcePatternResolver = Objects.requireNonNull(resourcePatternResolver, "resourcePatternResolver must not be null");
        this.layeredContentParser = new LayeredContentParser();
    }

    @Override
    public List<SkillDefinition> loadSkills() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(SKILL_PATTERN);
            List<Resource> sortedResources = Arrays.stream(resources)
                    .filter(Resource::exists)
                    .sorted(Comparator.comparing(this::resourceKey))
                    .toList();

            List<SkillDefinition> skills = new ArrayList<>(sortedResources.size());
            for (Resource resource : sortedResources) {
                skills.add(loadSkill(resource));
            }
            return List.copyOf(skills);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load skills from classpath", ex);
        }
    }

    private SkillDefinition loadSkill(Resource resource) {
        String markdown = readResource(resource);
        ParsedSkill parsedSkill = parseSkill(markdown, resource);
        SkillRouteConfig routeConfig = findRouteConfig(parsedSkill.frontMatter().getName(), resource);

        // 解析分层内容
        boolean progressive = Boolean.TRUE.equals(parsedSkill.frontMatter().getProgressive());
        LayeredSkillContent layeredContent = layeredContentParser.parse(parsedSkill.content(), progressive);

        return new SkillDefinition(
                parsedSkill.frontMatter().getName(),
                parsedSkill.frontMatter().getDescription(),
                parsedSkill.frontMatter().getVersion(),
                parsedSkill.content(),
                layeredContent,
                routeConfig
        );
    }

    private String readResource(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read skill resource " + resourceDescription(resource), ex);
        }
    }

    private ParsedSkill parseSkill(String markdown, Resource resource) {
        List<String> lines = markdown.lines().toList();
        if (lines.isEmpty() || !"---".equals(lines.get(0))) {
            throw new IllegalStateException("Skill markdown must start with front matter: " + resourceDescription(resource));
        }

        int frontMatterEnd = -1;
        for (int i = 1; i < lines.size(); i++) {
            if ("---".equals(lines.get(i))) {
                frontMatterEnd = i;
                break;
            }
        }
        if (frontMatterEnd < 0) {
            throw new IllegalStateException("Skill markdown is missing the closing front matter delimiter: " + resourceDescription(resource));
        }

        SkillFrontMatter frontMatter = parseFrontMatter(lines.subList(1, frontMatterEnd), resource);
        String content = String.join("\n", lines.subList(frontMatterEnd + 1, lines.size()));
        while (content.startsWith("\n")) {
            content = content.substring(1);
        }
        return new ParsedSkill(frontMatter, content);
    }

    private SkillFrontMatter parseFrontMatter(List<String> lines, Resource resource) {
        SkillFrontMatter frontMatter = new SkillFrontMatter();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int separatorIndex = line.indexOf(':');
            if (separatorIndex < 0) {
                throw new IllegalStateException("Invalid skill front matter line in " + resourceDescription(resource) + ": " + line);
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = stripQuotes(line.substring(separatorIndex + 1).trim());
            switch (key) {
                case "name" -> frontMatter.setName(value);
                case "description" -> frontMatter.setDescription(value);
                case "version" -> frontMatter.setVersion(Integer.valueOf(value));
                case "progressive" -> frontMatter.setProgressive(Boolean.valueOf(value));
                default -> {
                }
            }
        }
        if (frontMatter.getName() == null || frontMatter.getName().isBlank()) {
            throw new IllegalStateException("Skill front matter is missing a name in " + resourceDescription(resource));
        }
        if (frontMatter.getVersion() == null) {
            throw new IllegalStateException("Skill front matter is missing a version in " + resourceDescription(resource));
        }
        return frontMatter;
    }

    private SkillRouteConfig findRouteConfig(String skillName, Resource resource) {
        return skillProperties.getSkills().stream()
                .filter(Objects::nonNull)
                .filter(routeConfig -> skillName.equals(routeConfig.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No route config found for skill " + skillName + " in " + resourceDescription(resource)));
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String resourceKey(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (IOException ex) {
            return resourceDescription(resource);
        }
    }

    private String resourceDescription(Resource resource) {
        return resource.getDescription();
    }

    private record ParsedSkill(SkillFrontMatter frontMatter, String content) {
    }
}
