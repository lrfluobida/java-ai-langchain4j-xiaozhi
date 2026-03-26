# Claude-Style Skills Design

**Date:** 2026-03-26

## Goal

将当前项目中“预约挂号 skill 文本注入”的单点实现，升级为一套可扩展的 Claude 风格 Skill 框架。

第一版目标：

- Skill 资产采用接近 Claude Code 的 `SKILL.md` 组织方式
- Java 运行时提供通用的加载、注册、路由、会话状态和 prompt 组装能力
- 第一批只落地一个样板 skill：`appointment`
- 后续新增业务 skill 时，只新增资源文件和少量配置，不改 Java 主流程

## Non-Goals

- 第一版不实现 Claude Code 全量能力，例如子代理、工作流编排、技能依赖树
- 第一版不实现模型优先的 skill 分类器
- 第一版不重构 `AppointmentTools` 的工具接口
- 第一版不引入后台可视化 skill 管理界面

## Current State

当前项目的 skill 实现本质上是一个“预约场景的动态提示注入器”：

- 控制器调用 `AppointmentSkillService`
- 服务类用关键词判断当前消息是否属于预约场景
- 命中后将 `appointment-skill.txt` 注入到 `xiaozhi-prompt-template.txt`

当前方案适合作为演示，但存在这些局限：

- Skill 能力写死在 `AppointmentSkillServiceImpl` 中，无法自然扩展到多个业务 skill
- 资源文件格式是纯文本，不具备结构化元数据
- 会话状态只有“命中过预约就一直有效”，缺少统一的生命周期管理
- 新增 skill 需要继续修改 Java 主流程

## Design Principles

### 1. Skill 资产与运行时解耦

业务规则优先保存在 `SKILL.md` 中，Java 运行时只负责读取、解析、选择和拼接。

### 2. 资源优先，代码稳定

新增 skill 的常规流程应当是：

1. 新增 `src/main/resources/skills/<skill-name>/SKILL.md`
2. 在 `src/main/resources/skills.yml` 中补充少量配置

不需要修改控制器、Agent 接口或核心路由主流程。

### 3. 规则路由优先

针对本项目的医疗与预约场景，第一版采用“规则/关键词匹配优先”的路由方式，保持低成本、可解释和易测试。

### 4. 先兼容 Claude 风格，再保留业务简洁性

目标是让 Skill 文档结构尽量贴近 Claude Code 的 `SKILL.md` 形式，但不照搬其全部工作流能力。

## Resource Layout

建议采用以下目录结构：

```text
src/main/resources/
├─ skills/
│  └─ appointment/
│     └─ SKILL.md
└─ skills.yml
```

说明：

- `skills/<name>/SKILL.md` 保存技能说明、触发场景和执行指令
- `skills.yml` 保存运行时配置，例如是否启用、优先级、进入关键词和会话粘性参数

## Skill Document Format

每个 skill 使用一个独立的 `SKILL.md` 文件，建议结构如下：

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
1. 先识别用户意图：查询号源、预约挂号、取消预约
2. 在预约或取消前，逐步补齐必要信息
3. 优先追问当前最关键的缺失信息
4. 预约前先调用“查询是否有号源”
5. 用户确认后再调用“预约挂号”
6. 取消预约前先核对必要信息
7. 如果未指定医生，可以明确说明是推荐结果
```

第一版需要支持的 front matter 字段：

- `name`
- `description`
- `version`

第一版正文只需要支持原样读取，不需要解析 Markdown 章节语义。

## Runtime Config Format

`skills.yml` 建议格式如下：

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

字段说明：

- `enabled`: 是否启用
- `priority`: 多个 skill 同时命中时的优先级
- `entryKeywords`: 触发技能的关键词
- `stickySession`: 命中后是否在后续轮次保持激活
- `exitKeywords`: 显式退出 skill 的关键词
- `maxActiveTurns`: skill 激活后可持续生效的最大轮次数

## Runtime Architecture

建议引入统一的 skill 子系统：

```text
skill/
├─ model/
├─ loader/
├─ registry/
├─ router/
├─ session/
└─ prompt/
```

### SkillLoader

职责：

- 扫描 `classpath:skills/*/SKILL.md`
- 解析 front matter 和正文内容
- 构造成 `SkillDefinition`

### SkillRegistry

职责：

- 持有全部已加载 skill
- 按 `name` 查询
- 返回启用中的 skill 列表
- 提供按优先级排序后的结果

### SkillRouter

职责：

- 接收 `memoryId`、用户消息和当前 skill 会话状态
- 基于规则判断本轮命中的 skill
- 处理 skill 的进入、续期和退出

第一版路由规则：

1. 先检查显式退出关键词
2. 再检查会话中已激活的 sticky skill 是否仍在有效期内
3. 再按 `entryKeywords` 命中新 skill
4. 输出本轮最终应注入的 skill 列表

### SkillSessionStore

职责：

- 维护每个 `memoryId` 的激活 skill 状态
- 记录激活轮次、最后命中时间、剩余有效轮次
- 支持过期清理和显式退出

### SkillPromptAssembler

职责：

- 将命中的 skill 正文拼接成统一的 prompt 文本
- 按优先级排序
- 输出给 Agent 模板变量，例如 `skill_rules`

## Java Model Design

建议的核心模型：

### SkillDefinition

表示一个完整 skill：

- `name`
- `description`
- `version`
- `content`
- `routeConfig`

### SkillRouteConfig

表示 `skills.yml` 中与路由有关的配置：

- `enabled`
- `priority`
- `entryKeywords`
- `stickySession`
- `exitKeywords`
- `maxActiveTurns`

### SkillSessionState

表示单个会话当前激活的 skill 状态：

- `memoryId`
- `activeSkills`
- 每个 skill 的 `activatedAtTurn`
- `lastMatchedTurn`
- `remainingTurns`

### SkillContext

表示路由时的输入上下文：

- `memoryId`
- `userMessage`
- 当前轮次信息

## Request Flow

`POST /xiaozhi/chat` 的建议执行链路：

1. `XiaozhiController` 读取 `memoryId`、用户消息和会话摘要
2. `SkillRouter` 根据消息与会话状态计算命中的 skill
3. `SkillPromptAssembler` 组装 skill prompt
4. `XiaozhiAgent` 将 `conversation_summary + skill_rules` 注入 system prompt
5. `SkillSessionStore` 更新本轮后的激活状态

## Session Lifecycle

第一版 skill 生命周期规则建议如下：

### Enter

当用户消息命中 `entryKeywords` 时，激活 skill。

### Keep Active

当 `stickySession=true` 时，skill 在后续若干轮自动保持有效，直到：

- 达到 `maxActiveTurns`
- 命中 `exitKeywords`

### Exit

当 skill 退出后，不再向 prompt 中注入其正文。

### Cleanup

当会话被删除时，相关的 skill 会话状态也应被一并清理。

## Prompt Integration

现有设计中建议将：

- `appointment_skill_rules`

改造成：

- `skill_rules`

对应改造：

- `XiaozhiController` 中不再依赖 `AppointmentSkillService`
- `XiaozhiAgent` 使用通用变量 `@V("skill_rules")`
- `xiaozhi-prompt-template.txt` 中保留一个统一占位符 `{{skill_rules}}`

这样 Agent 主流程不感知具体业务 skill 名称。

## Minimal Code Changes

需要修改或替换的现有位置：

- `controller/XiaozhiController.java`
- `assistant/XiaozhiAgent.java`
- `resources/xiaozhi-prompt-template.txt`

需要下线或废弃的现有位置：

- `service/AppointmentSkillService.java`
- `service/impl/AppointmentSkillServiceImpl.java`

需要新增的主要内容：

- `src/main/resources/skills/appointment/SKILL.md`
- `src/main/resources/skills.yml`
- 一组通用 skill 运行时类

## Testing Strategy

第一版至少覆盖以下测试：

### 1. SkillLoader 测试

- 能扫描到 `SKILL.md`
- 能解析 front matter
- 能保留正文内容

### 2. SkillRegistry 测试

- 能正确注册并按名称查询 skill
- 只返回已启用 skill
- 能按优先级排序

### 3. SkillRouter 测试

- 命中关键词时启用 skill
- sticky session 能跨轮次生效
- 到达 `maxActiveTurns` 后自动失效
- 命中退出关键词后失效

### 4. PromptAssembler 测试

- 能输出单个 skill 文本
- 多个 skill 时能按优先级拼接
- 未命中 skill 时返回空字符串

### 5. Controller 集成测试

- 普通医疗问题不注入预约 skill
- 预约问题会注入预约 skill
- 会话连续追问时 skill 能持续生效

## Risks And Trade-Offs

### 1. 纯关键词路由的误判风险

第一版选择规则路由是为了稳定和简单，但误判和漏判仍可能存在。后续可以在保持规则主路由的基础上，增加模型兜底分类。

### 2. Markdown 文档与运行时配置分离

`SKILL.md` 和 `skills.yml` 分离能让职责更清楚，但需要保证二者通过 `name` 保持一致。加载器应在启动时校验。

### 3. 多 skill 冲突尚未完全展开

第一版只上线一个 `appointment` skill，因此优先级和冲突处理先做基础支持，不提前设计复杂依赖系统。

## Recommended Implementation Order

1. 引入 `skills.yml` 与 `skills/<name>/SKILL.md` 目录结构
2. 完成 `SkillLoader` 与 `SkillRegistry`
3. 完成 `SkillSessionStore` 与 `SkillRouter`
4. 完成 `SkillPromptAssembler`
5. 替换 `AppointmentSkillService` 调用链
6. 迁移预约 skill 资源
7. 补齐单元测试与集成测试

## Success Criteria

完成后应满足以下结果：

- 项目中的预约 skill 使用 `SKILL.md` 形式保存
- Java 主聊天链路不再依赖 `AppointmentSkillService`
- 运行时通过通用 Skill 框架完成加载、路由和 prompt 注入
- 新增业务 skill 时只需要增加资源文件和少量配置
- 当前项目对用户的预约挂号体验保持不回退
