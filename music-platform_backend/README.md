# 音乐创作智能体平台 — 后端服务

音乐创作智能体平台的后端服务，负责 API 接口、版本管理、文件存储，连接前端（成员A）、智能体（成员C）和生成链路（成员D）。

## 技术栈

| 项 | 选型 | 说明 |
|---|---|---|
| 语言 | Java 22 | `D:\Program Files\Java\jdk-22` |
| 框架 | Spring Boot 3.3.1 | |
| 构建 | Maven 3.9.7 | 已配阿里云镜像 |
| 数据库 | SQLite | 文件型 `music_platform.db`，JPA 自动建表 |
| ORM | Spring Data JPA + Hibernate | SQLite 方言（Hibernate Community Dialects） |
| API 文档 | SpringDoc OpenAPI 2.6 | Swagger UI |
| 校验 | Jakarta Validation | `@Valid` + DTO |
| 端口 | **8080** | 可在 `application.yml` 修改 |

## 前置条件

- **JDK 22** — `D:\Program Files\Java\jdk-22`
- **Maven 3.9.7** — `C:\Users\Lenovo\apache-maven-3.9.7`
- 确保 `JAVA_HOME` 指向 JDK 22

## 快速启动

```bash
# 1. 进入项目目录
cd music-platform_backend

# 2. 启动（命令⾏）
set JAVA_HOME=D:\Program Files\Java\jdk-22
C:\Users\Lenovo\apache-maven-3.9.7\bin\mvn spring-boot:run

# 3. 或在 IDE 中直接运行
# MusicPlatformApplication.main()
```

启动后访问：

| 地址 | 说明 |
|---|---|
| `http://localhost:8080` | 后端服务 |
| `http://localhost:8080/swagger-ui.html` | Swagger 接口文档（在线调试） |
| `http://localhost:8080/api/health` | 健康检查 |
| `http://localhost:8080/outputs/{filename}` | 静态文件（MIDI/WAV） |

## API 接口

全部接口返回统一格式 `{code, message, data}`，共 6 个接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/generate` | 首次生成音乐 |
| `POST` | `/api/revise` | 反馈修改 |
| `GET` | `/api/versions` | 分页获取版本列表 |
| `GET` | `/api/version/{id}` | 获取版本详情 |
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/outputs/{filename}` | 静态文件访问（MIDI/WAV） |

> 完整的请求/响应示例和字段说明见 **[API.md](./API.md)**

## 项目结构

```
music-platform_backend/
├── pom.xml
├── mvnw / mvnw.cmd
├── application.yml
├── API.md                               # 接口文档（给前端）
├── music_platform.db                    # SQLite 数据库（自动生成）
├── outputs/                             # MIDI / WAV 输出目录
└── src/main/java/com/musicplatform/
    ├── MusicPlatformApplication.java    # 启动入口
    ├── config/                          # Web / Swagger 配置
    ├── controller/                      # MusicController + HealthController
    ├── dto/                             # ApiResponse, GenerateRequest, ReviseRequest
    ├── exception/                       # GlobalExceptionHandler
    ├── model/                           # MusicVersion (JPA 实体)
    ├── repository/                      # MusicVersionRepository
    └── service/                         # MusicService (核心逻辑, TODO 标记 Mock 位置)
```

## 数据库表 `music_versions`

| 字段 | 类型 | 说明 |
|---|---|---|
| `version_id` | VARCHAR(32) PK | v1, v2, v3... |
| `parent_version_id` | VARCHAR(32) | 父版本 ID（首次生成为 null） |
| `user_prompt` | TEXT | 用户中文需求 |
| `style` | VARCHAR(64) | 音乐风格 |
| `mood` | VARCHAR(64) | 情绪 |
| `tempo` | VARCHAR(32) | 速度 |
| `instruments` | TEXT | 乐器列表（JSON 数组） |
| `feedback` | TEXT | 修改意见 |
| `plan` | TEXT | 作曲方案（JSON） |
| `caption` | TEXT | 英文描述（Text2MIDI 入口） |
| `midi_path` | VARCHAR(512) | MIDI 文件路径 |
| `audio_path` | VARCHAR(512) | WAV 文件路径 |
| `change_reason` | TEXT | 修改原因 |
| `created_at` | DATETIME | 创建时间 |

## 与队友的接口约定

| 成员 | 输入 | 需返回 |
|------|------|--------|
| **A（前端）** | 见 [API.md](./API.md) | — |
| **C（智能体）** | `user_prompt + style + mood + tempo + instruments` | `plan`（JSON）、`caption`（字符串） |
| **C（修订模式）** | `old_plan + feedback` | `plan + caption + change_reason` |
| **D（生成链路）** | `caption`（英文描述） | MIDI + WAV 文件 → 放入 `outputs/{version_id}/` |

## 开发状态

- ✅ 基础框架、6 个 API、数据库、校验、异常处理、Swagger
- ⬜ 对接成员C智能体（当前 `MusicService.java` 中为 Mock，搜索 `TODO` 定位）
- ⬜ 对接成员D生成链路（同上）
- ⬜ 前后端联调

## License

本项目为团队课程项目，用于短期学期。
