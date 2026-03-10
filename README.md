# 硅谷小智医疗助手后端

一个基于 `Spring Boot 3 + LangChain4j + MongoDB + MySQL + Pinecone` 的医疗 AI 助手后端项目。当前项目围绕“医疗问答 + 会话记忆 + 预约挂号 + RAG 检索”这几条主链路展开，适合作为 AI 应用项目演示、课程实践和二次开发基础。

## 当前已实现

- 流式聊天接口：基于 `LangChain4j @AiService` 输出流式回复
- 会话记忆：按 `memoryId` 隔离会话，并将聊天记录持久化到 MongoDB
- 会话摘要：在多轮对话场景下自动提炼摘要，并注入后续上下文
- 预约挂号工具调用：支持查询号源、预约挂号、取消预约
- 预约技能规则：对预约相关会话注入单独的 skill 规则
- RAG 检索：支持基于 embedding + Pinecone 的知识库增强问答
- 后端手动导入知识库：支持查看文件列表并导入指定知识库文件

## 技术栈

- Java 17
- Spring Boot 3.2.6
- LangChain4j 1.0.0-beta3
- MongoDB
- MySQL 8.x
- MyBatis-Plus
- DashScope / OpenAI Compatible API
- Pinecone
- Knife4j

## 项目结构

```text
src/main/java/com/atguigu/java/ai/langchain4j
├─ appMain.java                     # Spring Boot 启动类
├─ assistant/                       # AI 助手接口定义
├─ bean/                            # 表单、返回对象、Mongo 文档对象
├─ config/                          # 模型、记忆、RAG 等配置
├─ controller/                      # REST 接口
├─ entity/                          # MySQL 业务实体
├─ mapper/                          # MyBatis Mapper
├─ service/                         # 业务服务接口
├─ service/impl/                    # 业务服务实现
├─ store/                           # Mongo 会话记忆存储
└─ tools/                           # LangChain4j 工具调用实现
```

## 核心链路

- 聊天入口：`POST /xiaozhi/chat`
- Agent：[`XiaozhiAgent`](src/main/java/com/atguigu/java/ai/langchain4j/assistant/XiaozhiAgent.java)
- 聊天控制器：[`XiaozhiController`](src/main/java/com/atguigu/java/ai/langchain4j/controller/XiaozhiController.java)
- 记忆存储：[`MongoChatMemoryStore`](src/main/java/com/atguigu/java/ai/langchain4j/store/MongoChatMemoryStore.java)
- 摘要服务：[`ConversationSummaryServiceImpl`](src/main/java/com/atguigu/java/ai/langchain4j/service/impl/ConversationSummaryServiceImpl.java)
- 预约技能：[`AppointmentSkillServiceImpl`](src/main/java/com/atguigu/java/ai/langchain4j/service/impl/AppointmentSkillServiceImpl.java)
- 工具调用：[`AppointmentTools`](src/main/java/com/atguigu/java/ai/langchain4j/tools/AppointmentTools.java)
- RAG 管理接口：[`RagController`](src/main/java/com/atguigu/java/ai/langchain4j/controller/RagController.java)

## 运行环境

- JDK 17
- Maven 3.9+
- MongoDB 6+
- MySQL 8+

## 启动前准备

### 1. 配置大模型 Key

项目当前通过环境变量读取 `QWEN_API_KEY`。

PowerShell 示例：

```powershell
$env:QWEN_API_KEY="your_api_key"
```

### 2. 启动 MongoDB

默认配置见 [`application.yml`](src/main/resources/application.yml)：

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/chat_memory_db
```

### 3. 启动 MySQL

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

说明：当前仓库里还没有完整的数据库初始化脚本，预约表结构需要你本地自行准备。

### 4. 准备知识库目录

当前 RAG 知识库目录配置为：

```yaml
xiaozhi:
  rag:
    knowledge-dir: "E:/develop/JAVA/knowledge"
```

支持的文件类型：

- `.md`
- `.txt`
- `.pdf`

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

## 接口说明

### 1. 聊天接口

请求地址：

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

- 返回类型为流式文本
- 同一个 `memoryId` 会复用同一段会话记忆
- 如果命中预约场景，会自动注入预约 skill 规则

### 2. RAG 文件列表接口

请求地址：

```text
GET /rag/files
```

作用：查看当前知识库目录下可导入的文件。

### 3. RAG 手动导入接口

请求地址：

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

说明：当前知识库导入只放在后端侧，由后端接口手动控制，没有接前端管理页面。

## 当前记忆设计

- 聊天消息存储在 MongoDB `chat_messages` 集合
- 会话摘要存储在 MongoDB `conversation_summaries` 集合
- 记忆窗口当前使用 `MessageWindowChatMemory`，窗口大小为 `10`
- 摘要会在多轮对话中按规则自动生成，用于补充后续上下文

## 当前已知边界

- 项目当前仍使用 `LangChain4j 1.0.0-beta3`
- 部分源码注释和字符串在终端里可能出现中文乱码，但文件本身按 UTF-8 保存
- 预约号源查询逻辑仍偏示例实现，未接入真实排班系统
- Pinecone 相关配置仍建议后续改为环境变量
- 数据库初始化脚本尚未补齐

## 配套前端

当前配套前端目录：

```text
E:\develop\JAVA\xiaozhi-ui
```

后端默认提供 `8080` 端口，前端可通过代理转发到本项目。

## 演示建议

推荐按这个顺序演示：

1. 普通医疗问答，展示流式回复
2. 连续追问，展示多轮记忆和摘要注入
3. 发起挂号意图，展示 skill + function calling
4. 询问院内信息，展示 RAG 检索增强回答
5. 切换 `memoryId`，展示多会话隔离

## 后续可继续优化

- 增加数据库初始化脚本
- 拆分 `dev/prod` 配置
- 将 Pinecone 等敏感配置迁移到环境变量
- 升级 LangChain4j 到更新版本
- 增加统一异常处理和参数校验
- 增加 Docker / Docker Compose 启动方案
- 为预约流程补齐更真实的排班和号源逻辑

注意我当前项目虽然有压缩会话功能，但是还不是很实用，有一个问题就是我提取摘要后的SystemMessage会被写到chat_messages的后面，另外因为我判断是否要提取摘要的依据是用当前窗口的UserMessages数量是否超过10条，而不是用整个会话的UserMessages数量是否超过10条，所以在连续追问时，由于我有maxMessages(10)的限制，它一超过会自动删去chat_messages集合中第一条信息，可能会删掉UserMessage所以当前窗口的UserMessages数量可能一直不超过10条，就不会触发提取摘要的逻辑，这样就无法很好地展示摘要功能在多轮对话中的作用了。后续我会继续优化这个逻辑，让它能更好地适应多轮对话的场景。
当前想法是将SystemMessage提取出来，chat_messages集合中只存储UserMessage和AssistantMessage，然后每次动态写到最前面，另外新建一个Conversation_Progress集合来存储整个会话的UserMessage数量等信息，并用它来判断是否需要提取摘要，这样就能更好地适应多轮对话的场景了。可是由于当前langchain4j版本限制，这个改法会报错，后续准备开一个分支升级相应依赖的版本来实现这个功能。