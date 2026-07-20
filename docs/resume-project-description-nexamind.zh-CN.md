# NexaMind 简历项目描述

## 一页简历版

**NexaMind AI Agent 应用平台（二次开发与工程重构）**
技术栈：Java 17、Spring Boot、DDD、MyBatis-Plus、LangChain4j、PostgreSQL/pgvector、RabbitMQ、Next.js、TypeScript、Docker Compose

- 基于 Apache 2.0 开源 AgentX 项目进行二次开发，完成前后端目录、Java 启动类、Maven 坐标、环境变量、Docker 服务/网络/数据库、CI/CD 镜像及 Web 品牌界面的全链路 NexaMind 重构，并保留上游版权与变更声明。
- 梳理 Agent 创建与运行链路，覆盖前端表单、Controller/DTO、应用服务、领域服务、仓储持久化及大模型调用配置，形成可视化学习文档，掌握分层架构下智能体配置的生命周期。
- 分析 RAG 知识库处理流程，理解文档解析与切分、Embedding、pgvector 向量检索、HyDE/Rerank 增强召回及流式问答等关键模块，并为 Markdown 切分补充独立测试样本。
- 完成 Docker Desktop + WSL2 本地环境排障与数据盘迁移，将容器数据迁至 F 盘，验证 Compose 配置、VHDX 权限及前后端构建，减少系统盘占用并提升环境可复现性。

## 面试展开版

### 项目介绍

NexaMind 是一个面向 AI 应用开发的多智能体平台，提供 Agent 配置与发布、模型供应商接入、工具/MCP 调用、RAG 知识库、会话追踪、计费与 Docker 部署能力。项目采用前后端分离架构，后端以 Spring Boot 和领域驱动设计组织业务，前端使用 Next.js 与 TypeScript，检索链路使用 PostgreSQL/pgvector，异步任务通过 RabbitMQ 解耦。

### 个人工作

1. 对开源项目进行合规二次开发，制定 NexaMind 命名规范并完成代码、配置、镜像、部署脚本、文档及视觉资产的一致性重构。
2. 从页面请求出发追踪 Agent 创建流程，定位 DTO 参数校验、应用层编排、领域规则与 Repository 落库职责，整理调用链与核心数据模型。
3. 学习并分析 LangChain4j、Embedding、向量数据库、RAG、工具调用和流式输出在项目中的落点，将基础知识与实际代码对应起来。
4. 排查 Docker Desktop 长时间停留在 Starting the Docker Engine 的问题，处理 WSL2 状态、数据 VHDX 迁移、ACL 与残留目录清理，并验证 Docker Engine 和 Compose 可用性。
5. 清理本地敏感配置与个人简历源文件的版本控制风险，增加忽略规则、上游来源说明和独立测试资料，保证公开仓库可安全展示。

## 面试时应避免的表述

- 不要说“从零独立开发整个平台”；应说“基于开源项目完成二次开发、工程重构和核心链路学习”。
- 不要把未亲自实现的全部业务模块写成个人成果；可说明你已完成代码追踪、运行验证和局部改造。
- 被问到实现细节时，优先讲 Agent 创建链路、RAG 流程、Docker/WSL2 排障和本次重构，这些内容有对应代码、文档与提交记录可验证。
