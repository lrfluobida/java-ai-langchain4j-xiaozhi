---
name: appointment-progressive
description: 处理预约挂号、取消预约和号源查询的技能（渐进式披露版本）
version: 2
progressive: true
---

# Appointment Skill (Progressive)

## When To Use
当用户想要挂号、预约、取消预约、查询号源，或者讨论门诊就诊安排时启用。

<!-- disclosure-level: basic -->
## Basic Instructions
1. 识别用户意图：查询号源、预约挂号、取消预约
2. 预约前先调用"查询是否有号源"确认
3. 一次只追问一个缺失信息

<!-- disclosure-level: standard -->
## Standard Instructions
4. 预约需要确认：姓名、身份证号、科室、日期、时间
5. 用户确认信息无误后再调用"预约挂号"工具
6. 取消预约时先核对必要信息

<!-- disclosure-level: advanced -->
## Advanced Instructions
7. 如果用户没有指定医生，可以推荐但要明确说明这是推荐
8. 遇到号源不足时，主动建议其他时间段或科室
9. 处理多人预约时，逐个确认每个人的信息
10. 识别并处理预约冲突（同一时间段重复预约）
11. 对于特殊科室（如专家门诊），提醒用户可能需要额外准备的材料
