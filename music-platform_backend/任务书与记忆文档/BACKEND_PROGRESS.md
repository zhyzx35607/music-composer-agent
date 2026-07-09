# 成员B — 后端开发进度跟踪

> 每次改动后更新本文档，方便记忆和交接。

---

## 1. 技术栈

| 项 | 选型 | 备注 |
|---|---|---|
| 语言 | Java 22 | `D:\Program Files\Java\jdk-22` |
| 框架 | Spring Boot 3.3.1 | |
| 构建 | Maven 3.9.7 | 装于 `C:\Users\Lenovo\apache-maven-3.9.7`，已配阿里云镜像 |
| 数据库 | SQLite | 文件 `music_platform.db`，由JPA自动建表 |
| ORM | Spring Data JPA + Hibernate | Hibernate Community Dialects（SQLite方言） |
| 端口 | 8080 | 可在 `application.yml` 修改 |

---

## 2. 已完成（2026-07-09）

### 2.1 项目骨架

```
music-platform_backend/
├── pom.xml
├── mvnw / mvnw.cmd
├── .mvn/wrapper/
├── outputs/                       # MIDI/WAV 输出目录
├── src/main/java/com/musicplatform/
│   ├── MusicPlatformApplication.java
│   ├── controller/MusicController.java
│   ├── model/MusicVersion.java
│   ├── repository/MusicVersionRepository.java
│   ├── service/MusicService.java
│   └── config/WebConfig.java
└── src/main/resources/
    └── application.yml
```

### 2.2 API 接口（全部已实现，第一阶段用Mock数据）

| 接口 | 方法 | 作用 | 状态 |
|---|---|---|---|
| `/api/generate` | POST | 首次生成音乐 | ✅ Mock |
| `/api/revise` | POST | 反馈修改 | ✅ Mock |
| `/api/versions` | GET | 历史版本列表 | ✅ |
| `/api/version/{version_id}` | GET | 版本详情 | ✅ |
| `/api/health` | GET | 健康检查 | ✅ |
| `/outputs/{filename}` | GET | 静态文件访问 | ✅ |

### 2.3 数据库表 `music_versions`

| 字段 | 类型 | 说明 |
|---|---|---|
| `version_id` | VARCHAR(32) PK | v1, v2, v3... |
| `parent_version_id` | VARCHAR(32) | 父版本ID，首次生成为null |
| `user_prompt` | TEXT | 用户中文需求 |
| `style` | VARCHAR(64) | 音乐风格 |
| `mood` | VARCHAR(64) | 情绪 |
| `tempo` | VARCHAR(32) | 速度 |
| `instruments` | TEXT | 乐器列表（JSON数组） |
| `feedback` | TEXT | 修改意见 |
| `plan` | TEXT | 作曲方案（JSON） |
| `caption` | TEXT | 英文描述 |
| `midi_path` | VARCHAR(512) | MIDI文件路径 |
| `audio_path` | VARCHAR(512) | WAV文件路径 |
| `change_reason` | TEXT | 修改原因 |
| `created_at` | DATETIME | 创建时间 |

### 2.4 接口响应格式（与计划书第10节对齐）

```json
// POST /api/generate 响应
{
  "version_id": "v1",
  "caption": "A pop ballad instrumental piece...",
  "midi_url": "/outputs/v1/v1.mid",
  "audio_url": "/outputs/v1/v1.wav",
  "plan": {
    "theme": "...",
    "style": "pop ballad",
    "mood": ["nostalgic"],
    "tempo": 88,
    "key": "C major",
    "instruments": ["piano", "strings"],
    "structure": ["intro", "verse", "chorus", "outro"]
  }
}

// POST /api/revise 响应（额外包含）
{
  ...同上,
  "parent_version_id": "v1",
  "change_reason": "..."
}
```

---

## 3. 待完成

### 3.1 第一阶段（Day 2-4）

| 任务 | 说明 | 截止 |
|---|---|---|
| 联调前端的 `/api/generate` | 确保成员A能成功调通、收到正确JSON | Day 3 |
| 创建 `outputs/` 目录确认 | 确保文件写入路径正确 | Day 2 |
| 对接成员C的智能体 | 替换 `MusicService.generate()` 中的Mock plan/caption | Day 3-4 |
| 对接成员D的生成链路 | 替换Mock midi_path/audio_path 为真实文件路径 | Day 3-4 |
| 联调修复 | 前后端+智能体+生成链路全链路跑通 | Day 4 |

### 3.2 第二阶段（Day 5-9）

| 任务 | 说明 | 截止 |
|---|---|---|
| `/api/revise` 真实实现 | 替换Mock，接入智能体的反馈修改逻辑 | Day 5-6 |
| 版本查询接口完善 | `/api/versions` 和 `/api/version/{id}` 确认正常 | Day 6 |
| 统一接口返回格式 | 错误处理、状态码规范化 | Day 7 |
| 文件路径管理 | 确保MIDI/WAV文件不会被覆盖，版本隔离 | Day 7 |
| 后端整体联调 | 全链路稳定运行 | Day 8 |
| 最终检查 | 修复联调问题，准备演示 | Day 9 |

### 3.3 可选增强（P2，时间充足时再做）

| 任务 | 说明 |
|---|---|
| 异步任务队列 | 音乐生成耗时长的话，改为异步+轮询 |
| 日志完善 | 记录每次生成耗时、错误日志 |

---

## 4. Mock数据说明

当前 `MusicService.java` 中的 `generate()` 和 `revise()` 使用Mock数据：

- **plan**: 根据用户选中的style/mood/tempo/instruments自动构造
- **caption**: 模板拼接英文描述
- **midi_path / audio_path**: 基于 version_id 生成路径（文件尚未真实生成）
- **change_reason**: 固定模板 + 拼接feedback

所有Mock位置都标注了 `TODO`，搜索即可定位。接入真实模块时只需修改 `MusicService` 中的这两个方法。

---

## 5. 启动方式

```bash
# 命令行启动
cd "/c/Users/Lenovo/Desktop/26短学期/music-composer-agent/music-platform_backend"
JAVA_HOME="D:/Program Files/Java/jdk-22"
"C:/Users/Lenovo/apache-maven-3.9.7/bin/mvn" spring-boot:run

# 或在IDE中直接运行 MusicPlatformApplication.main()
```

启动后访问 `http://localhost:8080`。

---

## 6. 与队友的接口约定（协作关键）

### 给成员A（前端）的说明

- 请求格式见计划书第10节（`/api/generate`）和第11节（`/api/revise`）
- 响应格式见本文档第2.4节
- 静态文件通过 `/outputs/{filename}` 直接访问
- 后端端口 `8080`，前端需配置代理或跨域（已加 `@CrossOrigin`）

### 给成员C（智能体）的说明

- 后端会调用智能体的函数，传入 `user_prompt + style + mood + tempo + instruments`
- 智能体需要返回：`plan`（JSON对象）、`caption`（字符串）
- 反馈修改模式下额外传入 `old_plan + feedback`，需要返回新的 `plan + caption + change_reason`

### 给成员D（生成链路）的说明

- 后端会调用生成函数，传入 `caption`（英文描述）
- 需要返回 MIDI 文件路径和 WAV 文件路径
- 文件统一放在 `outputs/` 目录下，以 `version_id` 命名

---

## 7. 改动日志

| 日期 | 改动内容 |
|---|---|
| 2026-07-09 | 初始化项目骨架、全部6个API（Mock）、SQLite数据库、Swagger、Maven环境配置 |
