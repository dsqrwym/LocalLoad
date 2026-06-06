# LocalLoad

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- Web app:
  - Wasm target (faster, modern browsers): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
  - JS target (slower, supports older browsers): `./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

## LocalLoad Engine 架构说明（当前版本）

### 1. 项目定位

本项目是一个基于 Kotlin Multiplatform + Ktor 的轻量级 HTTP 压测引擎核心模块，主要目标是本地快速压测来查看自己的API开发性能是否提升或者变差。用于：

- 并发 HTTP 请求执行
- 请求延迟统计（默认 TTLB）
- 跨平台 HTTP Client 抽象
- 为后续 Scheduler / WorkerPool / Metrics 提供执行基础

### 2. 总体架构分层

```
┌──────────────────────────────┐
│        Load Test Layer        │
│ (Scheduler / WorkerPool -未完成) │
└──────────────┬───────────────┘
               ↓
┌──────────────────────────────┐
│       Execution Layer         │
│        KtorExecutor           │
│   RequestTask → RequestResult │
└──────────────┬───────────────┘
               ↓
┌──────────────────────────────┐
│      HTTP Abstraction Layer   │
│        HttpProvider           │
│   HttpConfig / Capabilities   │
└──────────────┬───────────────┘
               ↓
┌──────────────────────────────┐
│     Ktor / Platform Engine    │
│  JVM / Android / iOS / JS     │
└──────────────────────────────┘
```

### 3. 当前工程目录结构（已完成）

```
engine/
├── http/
│   ├── HttpProviderCapabilities.kt
│   └── executor/
│       ├── HttpConfig.kt
│       ├── HttpProvider.kt
│       ├── HttpVersion.kt
│       ├── HttpProvider.android.kt
│       ├── HttpProvider.ios.kt
│       ├── HttpProvider.jvm.kt
│       ├── HttpProvider.js.kt
│       └── HttpProvider.wasmJs.kt
└── execution/
    ├── RequestTask.kt          (执行输入模型)
    ├── RequestResult.kt        (执行输出模型)
    └── KtorExecutor.kt         (核心执行层)
```

### 4. 各层职责说明

#### 4.1 HTTP 抽象层（HttpProvider Layer）

**📌 HttpProvider.kt**

职责：
- 创建并持有 HttpClient
- 统一跨平台 HTTP 实现
- 管理连接池、线程池等资源生命周期

核心设计：
```kotlin
expect class HttpProvider(
    config: HttpConfig
) {
    val capabilities: HttpProviderCapabilities
    val client: HttpClient
}
```

**📌 HttpConfig.kt**

职责：
- 定义 HTTP 客户端能力配置（影响连接行为）

包含：
- HTTP version
- timeout
- redirect
- keepAlive
- compression

```kotlin
data class HttpConfig(
    val version: HttpVersion,
    val connectTimeoutMs: Long,
    val requestTimeoutMs: Long,
    val followRedirects: Boolean,
    val keepAlive: Boolean,
    val compression: Boolean
)
```

**📌 HttpVersion.kt**

职责：
- HTTP 协议抽象

```kotlin
enum class HttpVersion {
    AUTO,
    HTTP1_1,
    HTTP2
}
```

**📌 HttpProviderCapabilities.kt**

职责：
- 描述当前平台能力（UI 或上层决策用）

```kotlin
interface HttpProviderCapabilities {
    val supportsKeepAlive: Boolean
    val supportsCompressionControl: Boolean
    val supportsHttpVersionSelection: Boolean
}
```

#### 4.2 Execution Layer（核心执行层）

**📌 RequestTask（输入模型）**

职责：
- 定义"一个请求应该如何执行"

```kotlin
data class RequestTask(
    val id: String,
    val url: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)
```

特点：
- Ktor 原生 HttpMethod
- 支持 headers / body
- id 用于 trace 和 metrics

**📌 RequestResult（输出模型）**

职责：
- 记录一次 HTTP 执行结果

```kotlin
data class RequestResult(
    val requestId: String,
    val success: Boolean,
    val httpStatus: HttpStatusCode?,
    val latencyMs: Long,
    val errorMessage: String? = null
)
```

TTLB 定义：
- latencyMs = 请求开始 → 响应完全接收结束（Time To Last Byte）

**📌 KtorExecutor（执行核心）**

职责：
- 执行 HTTP 请求
- 计算 TTLB latency
- 捕获异常并转为 Result
- 不参与调度、不管理并发

```kotlin
class KtorExecutor(
    private val client: HttpClient
)
```

执行逻辑：
```
RequestTask
    ↓
HttpClient.request()
    ↓
bodyAsChannel().discard()   (保证 TTLB)
    ↓
RequestResult
```

当前实现关键点：
- ✔ TTLB 已固定（默认策略）
- ✔ Monotonic Time 计时
- ✔ CancellationException 正确透传
- ✔ Exception → Result 转换

### 5. 数据流（核心）

```
Scheduler (未实现)
      ↓
RequestTask
      ↓
KtorExecutor
      ↓
HttpClient (via HttpProvider)
      ↓
HTTP Request
      ↓
Response (TTLB consumed)
      ↓
RequestResult
      ↓
MetricsCollector (未实现)
```

### 6. 已完成 / 未完成状态

**✔ 已完成（Current State）**

HTTP 层：
- HttpProvider
- HttpConfig
- HttpVersion
- Capabilities

Execution 层：
- RequestTask
- RequestResult
- KtorExecutor（TTLB）

**❌ 未完成（Next Phase）**

1. **WorkerPool（并发控制）**
   - 职责：
     - coroutine 并发控制
     - channel / semaphore
     - worker lifecycle

2. **Scheduler（流量控制）**
   - 职责：
     - RPS 控制
     - ramp-up
     - burst traffic

3. **MetricsCollector（指标系统）**
   - 职责：
     - p50 / p95 / p99
     - success rate
     - throughput
     - error rate

4. **LoadTestEngine（总控）**
   - 职责：
     - orchestration
     - lifecycle
     - start / stop test

### 7. 核心设计原则（已确立）

**✔ 1. 分层职责清晰**
- HttpProvider → 网络能力
- KtorExecutor → 请求执行
- Scheduler → 流量控制
- WorkerPool → 并发控制
- Metrics → 统计分析

**✔ 2. TTLB 作为唯一标准**
- 当前定义：latency = request start → full response received
- 不支持 TTFB 模式（已明确设计决策）

**✔ 3. Executor 只做一件事**
- Execute HTTP request → return result
- 不负责：
  - 并发
  - 调度
  - 生命周期
  - metrics

**✔ 4. HttpClient 生命周期统一管理**
- HttpProvider owns HttpClient
- LoadTestEngine controls lifecycle

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).