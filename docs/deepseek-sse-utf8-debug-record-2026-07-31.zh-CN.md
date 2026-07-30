# DeepSeek SSE 中文乱码调试记录

## 1. 问题概述

- 调试日期：2026-07-31
- 运行环境：Windows、Java 17、Spring Boot、LangChain4j、Next.js
- 模型协议：OpenAI 兼容协议
- 模型服务：DeepSeek
- 影响范围：通过 SSE 返回的中文模型内容

在 NexaMind 中配置 DeepSeek 后，模型请求可以正常完成，但中文回复出现乱码。例如：

```text
期望：DeepSeek 连接成功
实际：DeepSeek 杩炴帴鎴愬姛
```

用户输入、页面中文 UI 和普通 JSON 接口均可正常显示，因此问题集中在模型流式响应链路。

## 2. 调用链

```text
浏览器输入中文
  -> NexaMind 前端发起聊天请求
  -> Spring Boot 组装 Agent 上下文
  -> LangChain4j 调用 DeepSeek OpenAI 兼容接口
  -> DeepSeek 返回 UTF-8 SSE 数据流
  -> LangChain4j 解析模型分片
  -> 后端保存消息并通过 SSE 转发
  -> 前端拼接并显示回复
```

## 3. 排查过程

### 3.1 检查前端解码

前端使用 `TextDecoder` 读取响应流。浏览器默认按 UTF-8 解码，用户输入和其他中文页面也显示正常，因此前端不是首要嫌疑点。

### 3.2 检查数据库落库结果

查询 PostgreSQL 中最新的消息记录：

```sql
SELECT role, content, created_at
FROM messages
ORDER BY created_at DESC
LIMIT 4;
```

结果表明：

```text
USER      | 只回复：DeepSeek 连接成功
ASSISTANT | DeepSeek 杩炴帴鎴愬姛
```

用户消息入库正确，而助手消息在保存前已经乱码。这排除了 React 渲染、浏览器字体、前端 Markdown 和 PostgreSQL 编码问题，将范围缩小到 DeepSeek 响应进入 Java 应用的阶段。

### 3.3 检查 LangChain4j SSE 解析器

当前依赖中的 `DefaultServerSentEventParser` 使用了未指定字符集的读取方式：

```java
new InputStreamReader(inputStream)
```

该构造方式使用 JVM 默认字符集。中文 Windows 环境通常使用 GBK，而 DeepSeek SSE 响应使用 UTF-8，因此 UTF-8 字节被当作 GBK 解码，形成稳定的乱码转换：

```text
连接成功 -> 杩炴帴鎴愬姛
```

## 4. 根因

根因不是 DeepSeek 模型、Prompt 或数据库，而是 LangChain4j SSE 解析器依赖 JVM 默认字符集。

```text
DeepSeek UTF-8 字节
  -> InputStreamReader 使用 Windows 默认 GBK
  -> Java String 已经乱码
  -> 乱码被正常保存并转发
```

即使后端再以 UTF-8 输出，也只能输出已经损坏的 Java 字符串，所以只修改 HTTP 响应头或前端 `TextDecoder` 无法解决根因。

## 5. 修复方案

### 5.1 增加 UTF-8 SSE 解析器

新增：

```text
NexaMind/src/main/java/org/xhy/infrastructure/llm/http/Utf8ServerSentEventParser.java
```

解析器显式使用：

```java
new InputStreamReader(inputStream, StandardCharsets.UTF_8)
```

同时保留 SSE 的 `event:`、`data:`、多行数据和空行分隔行为。

### 5.2 包装 LangChain4j HTTP 客户端

新增：

```text
NexaMind/src/main/java/org/xhy/infrastructure/llm/http/Utf8HttpClientBuilder.java
```

包装器继续使用 LangChain4j 的 JDK HTTP 客户端处理请求，但在流式请求中使用项目自己的 UTF-8 SSE 解析器。

### 5.3 注入模型工厂

修改：

```text
NexaMind/src/main/java/org/xhy/infrastructure/llm/factory/LLMProviderFactory.java
```

为 OpenAI 兼容协议的同步和流式模型统一注入 `Utf8HttpClientBuilder`。因此 DeepSeek 以及其他 OpenAI 兼容服务商都不再依赖 Windows 默认字符集。

## 6. 测试与验证

新增回归测试：

```text
NexaMind/src/test/java/org/xhy/infrastructure/llm/http/Utf8ServerSentEventParserTest.java
```

测试将包含中文的 SSE 数据编码为 UTF-8 字节，再验证解析后的事件类型和内容。

执行命令：

```powershell
cd F:\ai_agent\nexamind\NexaMind
.\mvnw.cmd '-Dmaven.repo.local=..\.local\maven-repository' '-Dtest=Utf8ServerSentEventParserTest' test
.\mvnw.cmd '-Dmaven.repo.local=..\.local\maven-repository' spotless:check
```

测试结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Spotless check: BUILD SUCCESS
```

重启后端后执行真实 DeepSeek 请求：

```text
输入：只回复：DeepSeek 中文编码正常
流式分片：Deep | Se | ek | 空格 | 中文 | 编码 | 正常
数据库结果：DeepSeek 中文编码正常
```

由此确认上游解析、后端转发、数据库持久化和前端可消费的 SSE 内容均已恢复正常。

## 7. Token 消耗判断

调试时观察到 3 次 API 请求合计使用 1270 Tokens，平均约 423 Tokens/次。Agent 请求除用户可见文本外，还可能包含：

- 系统 Prompt 和 Agent 配置；
- 工具及 MCP 描述；
- 历史消息与上下文；
- 首次对话的智能标题生成请求。

NexaMind 在首次对话后会额外调用默认模型生成会话标题，因此页面上的一轮对话不一定只对应一次模型 API 请求。

以 DeepSeek V4 Flash 调试时的价格为例：

```text
费用 =
缓存命中输入 Tokens * 0.02 / 1,000,000
+ 缓存未命中输入 Tokens * 1 / 1,000,000
+ 输出 Tokens * 2 / 1,000,000
```

1270 Tokens 即使全部按输出价格计算也约为 0.00254 元，因此控制台显示消费金额小于 0.01 元属于正常范围。实际费用需要结合输入、输出和缓存命中明细计算。

## 8. 注意事项

- 修复只影响后续模型回复，数据库中已经保存的乱码消息不会自动恢复。
- 不应在前端通过字符替换或二次转码修复，因为这会误伤正常文本。
- 不应依赖 `file.encoding` 或某台机器的系统区域设置，网络协议字符集应在代码中显式声明。
- 调试过程中不要在日志、截图或 Git 提交中记录 API Key。
- 真实模型验证会产生额外 API 请求和少量 Token 消耗。

## 9. 结论

本次问题通过“前端显示 -> SSE 数据 -> 数据库存储 -> 上游客户端实现”的顺序逐层缩小范围，最终确认是第三方 SSE 解析器使用平台默认字符集导致。通过在 HTTP 客户端边界显式指定 UTF-8，修复了 DeepSeek 中文流式输出，并用单元测试和真实 API 请求完成验证。
