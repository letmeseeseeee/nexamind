# NexaMind 简历项目描述

## 一页简历版

**NexaMind AI Agent 应用平台**
技术栈：Java 17、Spring Boot、DDD、MyBatis-Plus、LangChain4j、PostgreSQL/pgvector、RabbitMQ、Next.js、TypeScript、Docker Compose

- 采用 Spring Boot 与 DDD 分层架构构建 AI Agent 应用平台，覆盖 Agent 配置发布、多模型接入、MCP 工具调用、RAG 知识库、会话追踪及账户计费等核心能力。
- 梳理并实现 Agent 创建与运行链路，打通前端表单、Controller/DTO、应用服务编排、领域服务、仓储持久化与大模型调用配置，形成完整的智能体生命周期管理。
- 构建 RAG 知识处理流程，覆盖文档解析与切分、Embedding、pgvector 向量检索、HyDE/Rerank 增强召回及流式问答，并补充 Markdown 切分测试样本。
- 完成 Docker Desktop + WSL2 本地环境排障与数据盘迁移，将容器数据迁至 F 盘，验证 Compose 配置、VHDX 权限及前后端构建，减少系统盘占用并提升环境可复现性。

## 面试展开版

### 项目介绍

NexaMind 是一个面向 AI 应用开发的多智能体平台，提供 Agent 配置与发布、模型供应商接入、工具/MCP 调用、RAG 知识库、会话追踪、计费与 Docker 部署能力。项目采用前后端分离架构，后端以 Spring Boot 和领域驱动设计组织业务，前端使用 Next.js 与 TypeScript，检索链路使用 PostgreSQL/pgvector，异步任务通过 RabbitMQ 解耦。

### 个人工作

1. 负责 Agent 创建、编辑、调试、发布与运行链路梳理，明确 DTO 参数校验、应用层编排、领域规则和 Repository 持久化职责。
2. 整合 LangChain4j、模型供应商、Prompt、Embedding、向量数据库、MCP 工具调用与流式输出，建立 AI 能力与业务模块之间的对应关系。
3. 分析并验证 RAG 从文件上传、解析切分、向量化、检索增强到答案生成的完整流程，整理核心调用链与数据模型。
4. 排查 Docker Desktop 长时间停留在 Starting the Docker Engine 的问题，处理 WSL2 状态、数据 VHDX 迁移、ACL 与残留目录清理，并验证 Docker Engine 和 Compose 可用性。
5. 完善环境变量、敏感信息忽略规则、测试资料、部署配置和项目文档，保证公开仓库可安全运行和展示。

## 面试表达建议

- 先用一句话说明项目解决的问题，再展开 Agent 生命周期、RAG 检索链路和 MCP 工具调用三个核心模块。
- 个人工作重点讲清楚自己能够解释和演示的功能，不把团队或框架能力全部表述为个人从零实现。
- 被问到实现细节时，优先讲 Agent 创建链路、RAG 流程、Docker/WSL2 排障和部署验证，这些内容有对应代码、文档与提交记录可验证。
