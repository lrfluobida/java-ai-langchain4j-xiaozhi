# 硅谷小智医疗助手后端

一个基于 `Spring Boot 3 + LangChain4j + MongoDB + MySQL + Pinecone` 的医疗 AI 助手后端项目。项目围绕"医疗问答 + 会话记忆 + 通用 Skill 框架（渐进式披露） + RAG 检索"几条主链路展开，适合作为 AI 应用项目演示、课程实践和二次开发基础。

## 当前已实现

- **流式聊天接口**：基于 `LangChain4j @AiService` 输出 SSE 流式回复
- **会话记忆**：按 `memoryId` 隔离会话，通过 `MongoChatMemoryStore` 将聊天记录持久化到 MongoDB，SystemMessage 与普通消息分离存储
- **会话摘要**：基于 `ConversationProgress` 追踪总用户消息数，达到阈值后自动调用 LLM 提炼摘要，注入后续上下文
- **预约挂号工具调用**：支持查询号源、预约挂号、取消预约，含 LLM 引导式的信息确认流程
- **通用 Skill 框架**：
  - 从 `src/main/resources/skills/<name>/SKILL.md` 按需加载 Claude 风格技能
  - 支持 **渐进式披露（Progressive Disclosure）**：根据对话轮次、工具调用次数、错误状态自动升级披露级别（BASIC → STANDARD → ADVANCED），逐步展开规则细节
  - 混合路由：优先使用 **LLM 智能选择**（`LlmSkillSelector`），回退到 **关键词路由**（`SkillRouter`）
  - 粘性会话：命中技能后在指定轮次内保持激活状态
  - 通过 `skills.yml` 集中管理技能启用/禁用、优先级、路由关键词等配置
  - **Skill 调试接口**：查看会话技能状态、预览渐进式披露效果、模拟工具调用/错误触发级别升级
- **RAG 检索**：基于 DashScope Embedding（`text-embedding-v4`）+ Pinecone 向量存储的知识库增强问答
- **后端手动导入知识库**：支持查看文件列表并导入指定文件到向量存储（支持 `.md` / `.txt` / `.pdf`）

## 技术栈

- Java 17
- Spring Boot 3.5.0
- LangChain4j 1.12.1
- MongoDB
- MySQL 8.x
- MyBatis-Plus 3.5.16
- DashScope（Qwen 3.5 Flash + DeepSeek-v3.2）
- Pinecone（向量存储）
- Knife4j 4.3.0（API 文档）

## 项目结构

```text
src/main/java/com/atguigu/java/ai/langchain4j
├─ appMain.java                              # Spring Boot 启动类
├─ assistant/                                # AI 助手接口定义
│   ├─ Assistant.java                        # 无状态 AI 服务
│   ├─ MemoryChatAssistant.java              # 带记忆的 AI 服务
│   ├─ SeparateChatAssistant.java            # 按 memoryId 隔离的 AI 服务
│   └─ XiaozhiAgent.java                     # 主 AI Agent（流式 + RAG + 工具 + 技能）
├─ bean/                                     # 数据传输与 Mongo 文档对象
│   ├─ ChatForm.java                         # 聊天请求体
│   ├─ ConversationProgress.java             # 会话进度 Mongo 文档
│   ├─ ConversationSummary.java              # 会话摘要 Mongo 文档
│   ├─ KnowledgeFileInfo.java                # 知识库文件信息
│   ├─ KnowledgeIngestForm.java              # 知识库导入请求
│   ├─ KnowledgeIngestResult.java            # 知识库导入结果
│   └─ MyChatMessages.java                   # 聊天消息 Mongo 文档
├─ config/                                   # 模型、记忆、RAG 等配置
│   ├─ ChatRequestDebugConfig.java           # 调试日志配置
│   ├─ EmbeddingStoreConfig.java             # Pinecone EmbeddingStore Bean
│   ├─ MemoryChatAssistantConfig.java        # ChatMemory Bean
│   ├─ Qwen35FlashModelConfig.java           # Qwen 3.5 Flash 模型配置
│   ├─ RagConfig.java                        # 文档分割器与解析器
│   ├─ RagProperties.java                    # RAG 配置属性
│   ├─ SeparateChatAssistantConfig.java      # ChatMemoryProvider Bean
│   └─ XiaozhiAgentConfig.java               # XiaozhiAgent 专用配置
├─ controller/                               # REST 接口
│   ├─ RagController.java                    # 知识库管理接口
│   ├─ SkillDebugController.java             # Skill 调试接口
│   └─ XiaozhiController.java                # 聊天入口接口
├─ entity/                                   # MySQL 业务实体
│   └─ Appointment.java                      # 预约实体
├─ mapper/                                   # MyBatis Mapper
│   └─ AppointmentMapper.java
├─ service/                                  # 业务服务接口
│   ├─ AppointmentService.java
│   ├─ ConversationSummaryService.java
│   └─ KnowledgeService.java
├─ service/impl/                             # 业务服务实现
│   ├─ AppointmentServiceImpl.java
│   ├─ ConversationSummaryServiceImpl.java
│   └─ KnowledgeServiceImpl.java
├─ store/                                    # Mongo 会话记忆存储
│   └─ MongoChatMemoryStore.java
├─ tools/                                    # LangChain4j 工具调用
│   ├─ AppointmentTools.java                 # 预约工具
│   └─ CalculatorTools.java                  # 计算工具
└─ skill/                                    # 通用 Skill 框架
    ├─ config/
    │   ├─ SkillProperties.java              # skills.yml 配置加载
    │   └─ YamlPropertySourceFactory.java    # YAML PropertySource 工厂
    ├─ debug/
    │   └─ SkillDebugHelper.java             # 调试辅助工具
    ├─ loader/
    │   ├─ SkillLoader.java                  # 加载器接口
    │   ├─ ClasspathSkillLoader.java         # Classpath SKILL.md 加载
    │   └─ LayeredContentParser.java         # 分层内容解析器
    ├─ model/
    │   ├─ DisclosureLevel.java              # 披露级别枚举
    │   ├─ DisclosureStrategy.java           # 披露级别计算策略
    │   ├─ LayeredSkillContent.java          # 分层技能内容
    │   ├─ SkillActivation.java              # 技能运行时状态
    │   ├─ SkillContext.java                 # 技能上下文
    │   ├─ SkillDefinition.java              # 技能定义（加载后的完整模型）
    │   ├─ SkillFrontMatter.java             # SKILL.md YAML 前置元数据
    │   ├─ SkillRouteConfig.java             # skills.yml 路由配置
    │   └─ SkillSessionState.java            # 会话技能状态
    ├─ prompt/
    │   └─ SkillPromptAssembler.java         # 技能提示词装配器
    ├─ registry/
    │   └─ SkillRegistry.java                # 技能注册中心
    ├─ router/
    │   └─ SkillRouter.java                  # 关键词路由（回退策略）
    ├─ selector/
    │   ├─ SkillSelector.java                # 选择器接口
    │   ├─ LlmSkillSelector.java             # LLM 智能选择器
    │   ├─ SkillMetadata.java                # 技能元数据
    │   ├─ SkillSelectionContext.java        # 选择上下文
    │   ├─ SkillSelectionResult.java         # 选择结果
    │   └─ SkillSelectionService.java        # 选择编排服务
    ├─ service/
    │   └─ SkillPromptService.java           # Skill 提示词服务外观
    └─ session/
        └─ SkillSessionStore.java            # 技能会话存储

src/main/resources
├─ application.yml                            # 主配置文件
├─ skills.yml                                 # 技能路由配置
├─ xiaozhi-prompt-template.txt               # 系统提示词模板
└─ skills/                                    # Claude 风格 Skill 资源目录
    ├─ appointment/SKILL.md                   # 预约技能（标准版）
    └─ appointment-progressive/SKILL.md       # 预约技能（渐进式披露版）
```

## 核心链路

| 链路 | 说明 | 关键文件 |
|------|------|----------|
| 聊天入口 | `POST /xiaozhi/chat`，SSE 流式响应 | [XiaozhiController](src/main/java/com/atguigu/java/ai/langchain4j/controller/XiaozhiController.java) |
| AI Agent | 流式聊天 + RAG + 工具调用 + 技能注入 | [XiaozhiAgent](src/main/java/com/atguigu/java/ai/langchain4j/assistant/XiaozhiAgent.java) |
| 会话记忆 | 按 memoryId 隔离，MongoDB 持久化 | [MongoChatMemoryStore](src/main/java/com/atguigu/java/ai/langchain4j/store/MongoChatMemoryStore.java) |
| 会话摘要 | 按总用户消息数触发，LLM 提炼摘要 | [ConversationSummaryServiceImpl](src/main/java/com/atguigu/java/ai/langchain4j/service/impl/ConversationSummaryServiceImpl.java) |
| Skill 路由与选择 | LLM 选择 + 关键词回退，支持粘性会话 | [SkillSelectionService](src/main/java/com/atguigu/java/ai/langchain4j/skill/selector/SkillSelectionService.java) / [SkillRouter](src/main/java/com/atguigu/java/ai/langchain4j/skill/router/SkillRouter.java) |
| Skill 渐进式披露 | 根据对话进程自动升级规则披露级别 | [DisclosureStrategy](src/main/java/com/atguigu/java/ai/langchain4j/skill/model/DisclosureStrategy.java) / [SkillPromptAssembler](src/main/java/com/atguigu/java/ai/langchain4j/skill/prompt/SkillPromptAssembler.java) |
| 预约工具 | 预约/取消/查询号源 | [AppointmentTools](src/main/java/com/atguigu/java/ai/langchain4j/tools/AppointmentTools.java) |
| RAG 检索 | Pinecone 向量检索增强回答 | [RagController](src/main/java/com/atguigu/java/ai/langchain4j/controller/RagController.java) |
| Skill 调试 | 查看技能状态、预览披露效果、模拟触发 | [SkillDebugController](src/main/java/com/atguigu/java/ai/langchain4j/controller/SkillDebugController.java) |

## Skill 框架设计

### 技能定义

每个技能由两部分组成：

1. **SKILL.md** — 位于 `src/main/resources/skills/<name>/SKILL.md`，包含：
   - YAML 前置元数据：`name`、`description`、`version`、`progressive`（是否启用渐进式披露）
   - Markdown 正文：技能的提示词与规则。若 `progressive: true`，使用 `<!-- disclosure-level: basic|standard|advanced -->` 标记将内容分为三层
2. **skills.yml** — 位于 `src/main/resources/skills.yml`，配置路由规则：
   - `enabled`：是否启用
   - `priority`：优先级（数值越大越靠前）
   - `entryKeywords` / `exitKeywords`：触发/退出关键词
   - `stickySession`：是否保持粘性会话
   - `maxActiveTurns`：最大激活轮次

### 渐进式披露

对于 `progressive: true` 的技能，框架会根据对话状态自动调整披露级别：

| 级别 | 触发条件 | 披露内容 |
|------|----------|----------|
| BASIC | 默认（对话开始） | 基础规则（意图识别、核心流程） |
| STANDARD | 激活轮次 ≥ 3 | 基础 + 标准规则（信息确认细节） |
| ADVANCED | 激活轮次 ≥ 6 或 工具调用 ≥ 2 或 发生错误 | 基础 + 标准 + 高级规则（边缘场景处理） |

这样可以在对话初期保持简洁的提示词，随对话深入逐步展开更多规则，避免一次性注入过多指令导致 LLM 注意力分散。

### 路由策略

1. **LLM 选择（主策略）**：调用 Qwen 3.5 Flash，传入可用技能列表和用户消息，由 LLM 判断用户意图并返回应激活的技能
2. **关键词路由（回退）**：当 LLM 选择失败时，使用 keyword 匹配作为兜底

## 运行环境

- JDK 17
- Maven 3.9+
- MongoDB 6+
- MySQL 8+

## 启动前准备

### 1. 配置大模型 Key

项目通过环境变量 `DASHSCOPE_API_KEY` 配置阿里云 DashScope API Key。

PowerShell 示例：

```powershell
$env:DASHSCOPE_API_KEY="your_api_key"
```

项目使用两个模型：
- **DeepSeek-v3.2**：作为主聊天模型，提供深度推理能力
- **Qwen 3.5 Flash**：作为流式聊天模型、Skill 选择模型和摘要模型，提供快速响应

### 2. 配置 Pinecone Key

```powershell
$env:PINECONE_API_KEY="your_pinecone_api_key"
```

向量存储配置在 Pinecone 的 `xiaozhi-index` 索引 / `xiaozhi-namespace` 命名空间下。

### 3. 启动 MongoDB

默认配置见 [application.yml](src/main/resources/application.yml)：

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/chat_memory_db
```

### 4. 启动 MySQL

默认配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/guiguxiaozhi?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: 123456
```

建议先创建数据库：

```sql
CREATE DATABASE guiguxiaozhi DEFAULT CHARACTER SET utf8mb4;
```

说明：当前仓库里还没有完整的数据库初始化脚本，预约表结构需要自行准备。

### 5. 准备知识库目录

当前 RAG 知识库目录配置为：

```yaml
xiaozhi:
  rag:
    knowledge-dir: "E:/develop/JAVA/knowledge"
```

支持的文件类型：`.md`、`.txt`、`.pdf`

## 启动方式

在项目根目录执行：

```bash
mvn spring-boot:run
```

或者：

```bash
mvn clean package
java -jar target/java-ai-langchain4j-1.0-SNAPSHOT.jar
```

默认端口：

```text
http://localhost:8080
```

API 文档地址（Knife4j）：

```text
http://localhost:8080/doc.html
```

## 接口说明

### 1. 聊天接口

```text
POST /xiaozhi/chat
```

请求体示例：

```json
{
  "memoryId": 100001,
  "message": "我最近头痛，应该挂什么科？"
}
```

说明：
- 返回类型为 SSE 流式文本（`text/stream;charset=utf-8`）
- 同一个 `memoryId` 复用同一段会话记忆
- 如果命中配置的 skill 场景，会自动装配对应规则并注入系统提示词
- 渐进式技能会根据对话进度逐步展开规则细节

### 2. RAG 文件列表接口

```text
GET /rag/files
```

查看当前知识库目录下可导入的文件列表。

### 3. RAG 手动导入接口

```text
POST /rag/ingest
```

请求体示例：

```json
{
  "fileNames": [
    "医院信息.md",
    "科室信息.md"
  ]
}
```

将指定文件内容分段并写入 Pinecone 向量存储。

### 4. Skill 调试接口

所有接口位于 `/debug/skills` 路径下：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/debug/skills/status/{memoryId}` | 查看当前会话的技能激活状态（活动技能、轮次、工具调用、错误状态、披露级别） |
| POST | `/debug/skills/preview` | 预览 basic vs progressive 的披露效果对比 |
| POST | `/debug/skills/compare` | 对比有技能 vs 无技能时的系统提示差异 |
| POST | `/debug/skills/simulate-tool-call` | 模拟工具调用，触发披露级别升级 |
| POST | `/debug/skills/simulate-error` | 模拟错误，触发披露级别升级 |
| DELETE | `/debug/skills/clear/{memoryId}` | 清除指定会话的所有活动技能 |

## 当前记忆与摘要设计

- 聊天消息存储在 MongoDB `chat_messages` 集合，**SystemMessage 与普通消息分离**（SystemMessage 不持久化，每次动态注入）
- 会话摘要存储在 MongoDB `conversation_summaries` 集合
- 会话进度存储在 MongoDB `conversation_progress` 集合，追踪总用户消息数
- 记忆窗口使用 `MessageWindowChatMemory`，窗口大小为 `30`
- 摘要触发条件：总用户消息 ≥ 8 且自上次摘要以来新消息 ≥ 4
- 摘要由 Qwen 3.5 Flash 生成，聚焦医疗事实、用户画像、症状、预约和未完成任务，限制 200 字以内

## 当前已知边界

- 预约号源查询逻辑仍为示例实现，`queryDepartment` 为硬编码 stub，未接入真实排班系统
- Pinecone API Key 已通过环境变量配置，但索引和命名空间为固定值
- 数据库初始化脚本尚未补齐
- 部分源码注释和字符串在终端可能出现中文乱码，但文件按 UTF-8 保存

## 配套前端

当前配套前端目录：

```text
E:\develop\JAVA\xiaozhi-ui
```

后端默认 `8080` 端口，前端可通过代理转发到本项目。

## 演示建议

推荐按以下顺序演示：

1. 普通医疗问答，展示流式回复
2. 连续追问，展示多轮记忆和摘要注入
3. 发起挂号意图，展示 Skill 渐进式披露 + Function Calling
4. 使用 `/debug/skills` 接口查看技能状态和披露级别变化
5. 询问院内信息，展示 RAG 检索增强回答
6. 切换 `memoryId`，展示多会话隔离

## 后续可优化方向

- 增加数据库初始化脚本
- 拆分 `dev/prod` 配置
- 将 Pinecone 索引/命名空间等配置迁移到环境变量
- 增加统一异常处理和参数校验
- 增加 Docker / Docker Compose 启动方案
- 为预约流程补齐更真实的排班和号源逻辑
- 扩展更多业务场景的技能定义（如问诊、体检预约等）
- LLM Skill 选择结果可增加缓存，减少重复调用
