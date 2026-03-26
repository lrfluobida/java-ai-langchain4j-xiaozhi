# Claude-Style Skills Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current appointment-only prompt injection service with a reusable Claude-style Skill framework that loads `SKILL.md` files, routes skills by configuration, and injects the assembled skill rules into `XiaozhiAgent`.

**Architecture:** Introduce a new `skill` package with loader, registry, router, session store, and prompt assembler components. Keep skill content in `src/main/resources/skills/<name>/SKILL.md`, keep routing settings in `src/main/resources/skills.yml`, and adapt the existing controller/agent/prompt chain to consume a generic `skill_rules` variable instead of `appointment_skill_rules`.

**Tech Stack:** Java 17, Spring Boot 3.5, LangChain4j 1.12.x, JUnit 5, Spring Boot Test, YAML resource loading via Spring.

---

### Task 1: Add Skill Resource Layout And Config Binding

**Files:**
- Create: `src/main/resources/skills/appointment/SKILL.md`
- Create: `src/main/resources/skills.yml`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/config/SkillProperties.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/config/YamlPropertySourceFactory.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillRouteConfig.java`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillPropertiesTest.java`

- [ ] **Step 1: Write the failing config binding test**

```java
@SpringBootTest(classes = SkillProperties.class)
@EnableConfigurationProperties(SkillProperties.class)
class SkillPropertiesTest {

    @Autowired
    private SkillProperties skillProperties;

    @Test
    void shouldBindAppointmentSkillConfig() {
        SkillRouteConfig config = skillProperties.getSkills().get(0);
        assertEquals("appointment", config.getName());
        assertTrue(config.getEnabled());
        assertTrue(config.getStickySession());
        assertEquals(12, config.getMaxActiveTurns());
        assertTrue(config.getEntryKeywords().contains("挂号"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.SkillPropertiesTest" test`
Expected: FAIL because `SkillProperties` and `skills.yml` do not exist yet.

- [ ] **Step 3: Add resource files and config classes**

`src/main/resources/skills/appointment/SKILL.md`

```md
---
name: appointment
description: 处理预约挂号、取消预约、号源查询相关对话
version: 1
---

# Appointment Skill

## When To Use
当用户表达挂号、预约、取消预约、查询号源等意图时启用。

## Instructions
1. 先识别用户意图：查询号源、预约挂号、取消预约。
2. 在预约挂号或取消预约前，逐步补齐必要信息。
3. 优先追问当前最关键的缺失信息，不要一次追问过多字段。
4. 预约挂号前先调用“查询是否有号源”工具。
5. 用户确认信息无误后再调用“预约挂号”工具。
6. 取消预约前先核对必要信息，再调用“取消预约挂号”工具。
7. 如果未指定医生，可以给出推荐，但要明确说明这是推荐结果。
```

`src/main/resources/skills.yml`

```yaml
skills:
  - name: appointment
    enabled: true
    priority: 100
    entryKeywords:
      - 挂号
      - 预约
      - 预约挂号
      - 取消预约
      - 取消挂号
      - 号源
      - 门诊
      - 看病
      - 看诊
    stickySession: true
    exitKeywords: []
    maxActiveTurns: 12
```

`SkillRouteConfig.java`

```java
@Data
public class SkillRouteConfig {
    private String name;
    private Boolean enabled = Boolean.TRUE;
    private Integer priority = 0;
    private List<String> entryKeywords = List.of();
    private Boolean stickySession = Boolean.FALSE;
    private List<String> exitKeywords = List.of();
    private Integer maxActiveTurns = 0;
}
```

`SkillProperties.java`

```java
@Data
@Component
@ConfigurationProperties(prefix = "")
@PropertySource(value = "classpath:skills.yml", factory = YamlPropertySourceFactory.class)
public class SkillProperties {
    private List<SkillRouteConfig> skills = List.of();
}
```

`YamlPropertySourceFactory.java`

```java
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        Properties properties = factory.getObject();
        return new PropertiesPropertySource(
                name != null ? name : resource.getResource().getFilename(),
                properties != null ? properties : new Properties()
        );
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.SkillPropertiesTest" test`
Expected: PASS with the appointment config bound from `skills.yml`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/skills/appointment/SKILL.md src/main/resources/skills.yml src/main/java/com/atguigu/java/ai/langchain4j/skill/config/SkillProperties.java src/main/java/com/atguigu/java/ai/langchain4j/skill/config/YamlPropertySourceFactory.java src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillRouteConfig.java src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillPropertiesTest.java
git commit -m "feat: add skill resource layout and config binding"
```

### Task 2: Implement Skill Loader And Registry

**Files:**
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillDefinition.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillFrontMatter.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/loader/SkillLoader.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/loader/ClasspathSkillLoader.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/registry/SkillRegistry.java`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/ClasspathSkillLoaderTest.java`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillRegistryTest.java`

- [ ] **Step 1: Write the failing loader and registry tests**

```java
class ClasspathSkillLoaderTest {

    @Test
    void shouldLoadAppointmentSkillFromClasspath() {
        ClasspathSkillLoader loader = new ClasspathSkillLoader(new SkillProperties());
        List<SkillDefinition> skills = loader.load();

        SkillDefinition appointment = skills.stream()
                .filter(skill -> "appointment".equals(skill.getName()))
                .findFirst()
                .orElseThrow();

        assertEquals("处理预约挂号、取消预约、号源查询相关对话", appointment.getDescription());
        assertTrue(appointment.getContent().contains("预约挂号前先调用“查询是否有号源”工具"));
    }
}
```

```java
class SkillRegistryTest {

    @Test
    void shouldReturnEnabledSkillsSortedByPriority() {
        SkillRegistry registry = new SkillRegistry(List.of(
                skill("appointment", 100, true),
                skill("followup", 10, false)
        ));

        List<SkillDefinition> enabledSkills = registry.getEnabledSkills();
        assertEquals(1, enabledSkills.size());
        assertEquals("appointment", enabledSkills.get(0).getName());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.ClasspathSkillLoaderTest,com.atguigu.java.ai.langchain4j.skill.SkillRegistryTest" test`
Expected: FAIL because loader and registry classes do not exist.

- [ ] **Step 3: Implement the loader and registry**

`SkillDefinition.java`

```java
@Data
@Builder
public class SkillDefinition {
    private String name;
    private String description;
    private Integer version;
    private String content;
    private SkillRouteConfig routeConfig;
}
```

`SkillLoader.java`

```java
public interface SkillLoader {
    List<SkillDefinition> load();
}
```

`SkillRegistry.java`

```java
@Component
public class SkillRegistry {

    private final Map<String, SkillDefinition> skillsByName;

    public SkillRegistry(SkillLoader skillLoader) {
        this.skillsByName = skillLoader.load().stream()
                .collect(Collectors.toMap(SkillDefinition::getName, Function.identity()));
    }

    public Optional<SkillDefinition> findByName(String name) {
        return Optional.ofNullable(skillsByName.get(name));
    }

    public List<SkillDefinition> getEnabledSkills() {
        return skillsByName.values().stream()
                .filter(skill -> Boolean.TRUE.equals(skill.getRouteConfig().getEnabled()))
                .sorted(Comparator.comparing(skill -> skill.getRouteConfig().getPriority(), Comparator.reverseOrder()))
                .toList();
    }
}
```

Implementation notes:
- Read all `classpath*:skills/*/SKILL.md` resources.
- Parse the first `--- ... ---` block as front matter.
- Keep the remaining Markdown body as `content`.
- Merge `SkillProperties` config into each `SkillDefinition` by `name`.
- Fail fast at startup if a `SKILL.md` has no matching route config.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.ClasspathSkillLoaderTest,com.atguigu.java.ai.langchain4j.skill.SkillRegistryTest" test`
Expected: PASS with one loaded `appointment` skill and correct enabled ordering.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillDefinition.java src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillFrontMatter.java src/main/java/com/atguigu/java/ai/langchain4j/skill/loader/SkillLoader.java src/main/java/com/atguigu/java/ai/langchain4j/skill/loader/ClasspathSkillLoader.java src/main/java/com/atguigu/java/ai/langchain4j/skill/registry/SkillRegistry.java src/test/java/com/atguigu/java/ai/langchain4j/skill/ClasspathSkillLoaderTest.java src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillRegistryTest.java
git commit -m "feat: add skill loader and registry"
```

### Task 3: Implement Skill Session Store And Router

**Files:**
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillContext.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillActivation.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillSessionState.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/session/SkillSessionStore.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/router/SkillRouter.java`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillRouterTest.java`

- [ ] **Step 1: Write the failing router test**

```java
class SkillRouterTest {

    @Test
    void shouldActivateStickyAppointmentSkillAcrossTurns() {
        SkillRegistry registry = registryWithAppointmentSkill();
        SkillSessionStore sessionStore = new SkillSessionStore();
        SkillRouter router = new SkillRouter(registry, sessionStore);

        List<SkillDefinition> firstTurn = router.route(new SkillContext(1001L, "我想预约挂号"));
        assertEquals(List.of("appointment"), firstTurn.stream().map(SkillDefinition::getName).toList());

        List<SkillDefinition> secondTurn = router.route(new SkillContext(1001L, "我叫张三"));
        assertEquals(List.of("appointment"), secondTurn.stream().map(SkillDefinition::getName).toList());
    }

    @Test
    void shouldExpireSkillAfterMaxActiveTurns() {
        SkillRouter router = new SkillRouter(registryWithAppointmentSkill(), new SkillSessionStore());
        router.route(new SkillContext(2001L, "我要挂号"));
        for (int i = 0; i < 12; i++) {
            router.route(new SkillContext(2001L, "继续补充信息"));
        }
        assertTrue(router.route(new SkillContext(2001L, "还有吗")).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.SkillRouterTest" test`
Expected: FAIL because router and session model classes do not exist.

- [ ] **Step 3: Implement session tracking and routing**

`SkillContext.java`

```java
public record SkillContext(Long memoryId, String userMessage) {
}
```

`SkillActivation.java`

```java
@Data
public class SkillActivation {
    private String skillName;
    private int activatedAtTurn;
    private int lastMatchedTurn;
    private int remainingTurns;
}
```

`SkillSessionStore.java`

```java
@Component
public class SkillSessionStore {

    private final Map<Long, SkillSessionState> sessions = new ConcurrentHashMap<>();

    public SkillSessionState getOrCreate(Long memoryId) {
        return sessions.computeIfAbsent(memoryId, id -> new SkillSessionState(id));
    }

    public void remove(Long memoryId) {
        sessions.remove(memoryId);
    }
}
```

Routing rules:
- If message contains any `exitKeywords`, deactivate that skill first.
- If a sticky skill is already active and `remainingTurns > 0`, keep it active and decrement turns.
- If current message contains any `entryKeywords`, activate or refresh that skill.
- Return active skills sorted by priority.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.SkillRouterTest" test`
Expected: PASS for activation, sticky routing, and expiry behavior.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillContext.java src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillActivation.java src/main/java/com/atguigu/java/ai/langchain4j/skill/model/SkillSessionState.java src/main/java/com/atguigu/java/ai/langchain4j/skill/session/SkillSessionStore.java src/main/java/com/atguigu/java/ai/langchain4j/skill/router/SkillRouter.java src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillRouterTest.java
git commit -m "feat: add skill routing and session lifecycle"
```

### Task 4: Implement Skill Prompt Assembly And Facade Service

**Files:**
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/prompt/SkillPromptAssembler.java`
- Create: `src/main/java/com/atguigu/java/ai/langchain4j/skill/service/SkillPromptService.java`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillPromptAssemblerTest.java`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillPromptServiceTest.java`

- [ ] **Step 1: Write the failing prompt assembly tests**

```java
class SkillPromptAssemblerTest {

    @Test
    void shouldAssembleSkillContentInPriorityOrder() {
        SkillPromptAssembler assembler = new SkillPromptAssembler();
        String prompt = assembler.assemble(List.of(
                skill("appointment", 100, "预约规则A"),
                skill("followup", 10, "复诊规则B")
        ));

        assertTrue(prompt.indexOf("预约规则A") < prompt.indexOf("复诊规则B"));
    }

    @Test
    void shouldReturnEmptyStringWhenNoSkillMatched() {
        assertEquals("", new SkillPromptAssembler().assemble(List.of()));
    }
}
```

```java
class SkillPromptServiceTest {

    @Test
    void shouldReturnAppointmentSkillPromptForAppointmentIntent() {
        SkillPromptService service = buildSkillPromptService();
        String prompt = service.resolveSkillRules(3001L, "我想挂号");
        assertTrue(prompt.contains("Appointment Skill"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.SkillPromptAssemblerTest,com.atguigu.java.ai.langchain4j.skill.SkillPromptServiceTest" test`
Expected: FAIL because prompt classes do not exist.

- [ ] **Step 3: Implement prompt assembly**

`SkillPromptAssembler.java`

```java
@Component
public class SkillPromptAssembler {

    public String assemble(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        return skills.stream()
                .map(SkillDefinition::getContent)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }
}
```

`SkillPromptService.java`

```java
@Service
public class SkillPromptService {

    private final SkillRouter skillRouter;
    private final SkillPromptAssembler skillPromptAssembler;

    public String resolveSkillRules(Long memoryId, String userMessage) {
        List<SkillDefinition> matchedSkills = skillRouter.route(new SkillContext(memoryId, userMessage));
        return skillPromptAssembler.assemble(matchedSkills);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.SkillPromptAssemblerTest,com.atguigu.java.ai.langchain4j.skill.SkillPromptServiceTest" test`
Expected: PASS with empty output for no-match and assembled content for matched skills.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/atguigu/java/ai/langchain4j/skill/prompt/SkillPromptAssembler.java src/main/java/com/atguigu/java/ai/langchain4j/skill/service/SkillPromptService.java src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillPromptAssemblerTest.java src/test/java/com/atguigu/java/ai/langchain4j/skill/SkillPromptServiceTest.java
git commit -m "feat: add skill prompt assembly service"
```

### Task 5: Replace Appointment-Specific Wiring In The Chat Flow

**Files:**
- Modify: `src/main/java/com/atguigu/java/ai/langchain4j/controller/XiaozhiController.java`
- Modify: `src/main/java/com/atguigu/java/ai/langchain4j/assistant/XiaozhiAgent.java`
- Modify: `src/main/resources/xiaozhi-prompt-template.txt`
- Modify: `src/main/java/com/atguigu/java/ai/langchain4j/store/MongoChatMemoryStore.java`
- Delete: `src/main/java/com/atguigu/java/ai/langchain4j/service/AppointmentSkillService.java`
- Delete: `src/main/java/com/atguigu/java/ai/langchain4j/service/impl/AppointmentSkillServiceImpl.java`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/XiaozhiSkillIntegrationTest.java`

- [ ] **Step 1: Write the failing integration test**

```java
@SpringBootTest
class XiaozhiSkillIntegrationTest {

    @Autowired
    private SkillPromptService skillPromptService;

    @Test
    void shouldReturnEmptySkillRulesForGeneralMedicalQuestion() {
        assertEquals("", skillPromptService.resolveSkillRules(4001L, "我最近头痛怎么办"));
    }

    @Test
    void shouldReturnAppointmentSkillRulesForAppointmentQuestion() {
        String prompt = skillPromptService.resolveSkillRules(4002L, "我想预约挂号");
        assertTrue(prompt.contains("Appointment Skill"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.XiaozhiSkillIntegrationTest" test`
Expected: FAIL until the generic chat flow and beans are wired correctly.

- [ ] **Step 3: Replace appointment-specific injection with generic skill rules**

`XiaozhiController.java`

```java
@Autowired
private SkillPromptService skillPromptService;

public Flux<String> chat(@RequestBody ChatForm chatForm) {
    String conversationSummary = conversationSummaryService.getSummary(chatForm.getMemoryId());
    String skillRules = skillPromptService.resolveSkillRules(chatForm.getMemoryId(), chatForm.getMessage());
    return xiaozhiAgent.chat(chatForm.getMemoryId(), chatForm.getMessage(), conversationSummary, skillRules);
}
```

`XiaozhiAgent.java`

```java
Flux<String> chat(
        @MemoryId Long memoryId,
        @UserMessage String userMessage,
        @V("conversation_summary") String conversationSummary,
        @V("skill_rules") String skillRules
);
```

`xiaozhi-prompt-template.txt`

```text
如果下面提供了历史会话摘要，请优先参考：
{{conversation_summary}}

今天是 {{current_date}}。

{{skill_rules}}
```

`MongoChatMemoryStore.java`

```java
@Autowired
private SkillSessionStore skillSessionStore;

@Override
public void deleteMessages(Object memoryId) {
    transientSystemMessages.remove(normalizeMemoryId(memoryId));
    mongoTemplate.remove(queryByMemoryId(memoryId), MyChatMessages.class);
    conversationSummaryService.deleteSummary(memoryId);
    if (memoryId instanceof Long longMemoryId) {
        skillSessionStore.remove(longMemoryId);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.XiaozhiSkillIntegrationTest" test`
Expected: PASS with generic `skill_rules` prompt injection.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/atguigu/java/ai/langchain4j/controller/XiaozhiController.java src/main/java/com/atguigu/java/ai/langchain4j/assistant/XiaozhiAgent.java src/main/resources/xiaozhi-prompt-template.txt src/main/java/com/atguigu/java/ai/langchain4j/store/MongoChatMemoryStore.java src/test/java/com/atguigu/java/ai/langchain4j/skill/XiaozhiSkillIntegrationTest.java
git rm src/main/java/com/atguigu/java/ai/langchain4j/service/AppointmentSkillService.java src/main/java/com/atguigu/java/ai/langchain4j/service/impl/AppointmentSkillServiceImpl.java
git commit -m "refactor: replace appointment skill service with generic skill framework"
```

### Task 6: Run Full Verification And Update Documentation

**Files:**
- Modify: `README.md`
- Test: `src/test/java/com/atguigu/java/ai/langchain4j/skill/*.java`

- [ ] **Step 1: Write the README update**

Update these sections:
- “当前已实现”
- “项目结构”
- “核心链路”
- “接口说明”

Add examples that mention:
- `skills/<name>/SKILL.md`
- `skills.yml`
- 通用 Skill 框架替代原预约专用 skill 服务

- [ ] **Step 2: Run focused skill tests**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.skill.*Test" test`
Expected: PASS for config binding, loader, registry, router, prompt, and integration tests.

- [ ] **Step 3: Run a broader regression slice**

Run: `mvn "-Dtest=com.atguigu.java.ai.langchain4j.ChatMemoryTest,com.atguigu.java.ai.langchain4j.PromptTest" test`
Expected: PASS or, if model/network-dependent tests are flaky, document which ones require external services.

- [ ] **Step 4: Run a full package build**

Run: `mvn clean package -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: describe generic claude-style skill framework"
```
