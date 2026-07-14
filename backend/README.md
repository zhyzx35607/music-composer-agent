# 音乐创作智能体平台 — 后端服务

音乐创作智能体平台的后端服务，提供 REST API、数据持久化、文件管理和 AI 管线调度。

---

## 目录

1. [技术栈](#1-技术栈)
2. [项目结构](#2-项目结构)
3. [功能列表](#3-功能列表)
4. [API 接口](#4-api-接口)
5. [数据库设计](#5-数据库设计)
6. [启动说明](#6-启动说明)

---

## 1. 技术栈

| 项 | 选型 | 说明 |
|---|---|---|
| 语言 | Java 22 | `D:\Program Files\Java\jdk-22` |
| 框架 | Spring Boot 3.3.1 | Web + JPA + Validation |
| 构建 | Maven 3.9.7 | 已配阿里云镜像 |
| 数据库 | SQLite | 文件型 `music_platform.db`，JPA 自动建表 |
| ORM | Spring Data JPA + Hibernate | SQLite 方言（Hibernate Community Dialects） |
| API 文档 | SpringDoc OpenAPI 2.6 | Swagger UI |
| 文件解析 | Apache POI 5.3 | docx / xlsx 读取 |
| 端口 | **8080** | 可在 `application.yml` 修改 |

---

## 2. 项目结构

```
backend/
├── pom.xml
├── application.yml                        # 配置（端口、数据库、管线、上传）
├── music_platform.db                      # SQLite 数据库（启动时自动生成）
├── API.md                                 # 完整接口文档（给前端）
├── 任务书与记忆文档/
│   ├── music_agent_division_plan.md       # 团队原始 9 天任务书
│   ├── 后端第二阶段改进任务书.md           # 后端改造完整方案
│   ├── BACKEND_PROGRESS.md                # 开发进度跟踪
│   ├── 第二阶段前端对接文档.md             # 给成员 A 的对接说明
│   └── 第二阶段智能体对接文档.md           # 给成员 C 的管线说明
│
└── src/main/java/com/musicplatform/
    ├── MusicPlatformApplication.java      # 启动入口
    │
    ├── config/
    │   ├── WebConfig.java                 # 静态资源映射（/outputs → 本地目录）
    │   └── OpenApiConfig.java             # Swagger 配置
    │
    ├── controller/
    │   ├── MusicController.java           # 11 个核心接口（生成/回炉/版本/Track）
    │   ├── FileController.java            # 3 个文件上传接口
    │   ├── ComplianceController.java      # 合规检测接口
    │   ├── CopyrightController.java       # 版权存证接口
    │   └── HealthController.java          # 健康检查
    │
    ├── dto/
    │   ├── ApiResponse.java               # 统一响应包装 {code, message, data}
    │   ├── GenerateRequest.java           # 生成请求 DTO
    │   └── ReviseRequest.java             # 回炉请求 DTO
    │
    ├── model/
    │   ├── MusicVersion.java              # 音乐版本实体（核心表）
    │   ├── UploadedFile.java              # 上传文件实体
    │   ├── ComplianceRecord.java          # 合规检测记录
    │   └── CopyrightRecord.java           # 版权存证记录
    │
    ├── repository/
    │   ├── MusicVersionRepository.java    # 版本数据访问（含 Track 查询）
    │   ├── UploadedFileRepository.java    # 文件数据访问
    │   ├── ComplianceRecordRepository.java
    │   └── CopyrightRecordRepository.java
    │
    ├── service/
    │   ├── MusicService.java              # 核心业务逻辑（生成/回炉/管线调度）
    │   └── FileService.java               # 文件上传与内容提取
    │
    └── exception/
        └── GlobalExceptionHandler.java    # 全局异常处理
```

---

## 3. 功能列表

### 3.1 核心功能（Track 模型 + 回炉 = 第二阶段完成）

| 功能 | 说明 |
|---|---|
| **按音乐分组管理** | 每首音乐是一个 Track（UUID），Track 下可有多个版本 |
| **音乐名称** | 用户手动输入，同一 Track 下追加版本可沿用原名或修改 |
| **版本标签** | 类似 Git 的 commit message，用户可手动输入，不填则后端自动生成（"初次创作·xxx" / "回炉·xxx"） |
| **首次生成** | 中文需求 + 风格/情绪/速度/乐器 → AI 生成作曲方案 + MIDI + WAV |
| **反馈修改（回炉）** | 传入旧版 plan + 用户反馈 → AI 做对比式修改，输出修改原因和参数差异 |
| **参数差异对比** | 回炉时展示 mood/tempo/key/instruments 的 before/after |
| **AI 修改解释** | 回炉时 AI 生成中文 change_reason，说明修改了什么、为什么 |
| **版本历史** | 按 Track 分组查看所有历史版本，支持版本切换 |
| **音频播放** | WAV 格式，前端直接通过 `/outputs/{version_id}.wav` 访问 |
| **文件下载** | MIDI 和 WAV 文件下载 |

### 3.2 文件上传（第二阶段新增）

| 功能 | 说明 |
|---|---|
| **纯文本上传** | 支持 .txt 文件，UTF-8 读取内容注入 prompt |
| **Word 文档上传** | 支持 .docx，Apache POI 提取段落文字 |
| **Excel 表格上传** | 支持 .xlsx，提取表格数据 → Markdown + JSON |
| **乐谱上传** | 支持 .musicxml / .mxl / .xml，解析调号、拍号、速度、和弦、旋律 |
| **内容注入 prompt** | 上传后生成时自动将提取内容拼入 GPT prompt，零额外 token 消耗 |
| **文件管理** | 查询、删除已上传文件 |

### 3.3 系统可靠性

| 功能 | 说明 |
|---|---|
| **Mock 自动降级** | 管线不可用（API Key 未配 / Python 不存在）时自动生成占位音频和 Mock 数据 |
| **兜底音频** | Mock 模式下生成 5 秒 C 大调和弦 WAV，确保前端播放器不白屏 |
| **diff 兜底** | GPT 不输出 parameter_diff 时，后端自动对比新旧 plan 计算 |
| **异常处理** | 统一 `GlobalExceptionHandler`，返回规范化错误 JSON |

### 3.4 合规检测与版权存证

| 功能 | 说明 |
|---|---|
| **版权合规检测** | 对指定版本进行旋律/歌词/音色相似度检测，输出风险等级 |
| **版权存证** | 记录创作全流程，生成证书哈希，模拟区块链存证 |
| **检测历史** | 查询检测记录列表和详情 |

---

## 4. API 接口

后端共 **14 个 REST 接口**，均返回统一格式 `{code, message, data}`。

### 4.1 统一响应格式

所有接口统一使用 `ApiResponse<T>` 包装：

```json
{
  "code": 200,       // HTTP 状态码
  "message": "ok",   // 人类可读说明
  "data": { ... }    // 业务数据，可能为 null
}
```

状态码：`200` 成功 / `201` 创建成功 / `400` 参数错误 / `404` 不存在 / `500` 服务器错误。

### 4.2 核心创作接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/generate` | 首次生成音乐（Track 模型） |
| `POST` | `/api/revise` | 基于旧版 + 反馈生成新版本（回炉） |
| `GET` | `/api/tracks` | 获取所有 Track 列表（按最新版本分组） |
| `GET` | `/api/track/{track_id}` | 获取某 Track 的完整版本历史 |
| `GET` | `/api/versions` | 分页获取全局版本列表 |
| `GET` | `/api/version/{version_id}` | 获取单个版本完整详情 |
| `GET` | `/api/health` | 健康检查（服务/数据库/存储状态） |
| `GET` | `/outputs/{filename}` | 静态文件访问（MIDI / WAV） |

#### POST /api/generate — 请求

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `user_prompt` | string | ✅ | 中文音乐需求，最长 2000 字符 |
| `track_name` | string | 建议填 | 音乐名称，如"毕业季的歌" |
| `track_id` | string | 选填 | 已有 Track ID，传了则在该 Track 下追加版本 |
| `version_label` | string | 选填 | 版本描述（Git commit 风格），不填自动生成 |
| `style` | string | 选填 | 音乐风格，默认 "pop" |
| `mood` | string | 选填 | 情绪，默认 "calm" |
| `tempo` | string | 选填 | 速度 slow/medium/fast，默认 "medium" |
| `instruments` | string[] | 选填 | 乐器列表，默认 ["piano"] |

#### POST /api/generate — 响应

```json
{
  "code": 201,
  "message": "created",
  "data": {
    "version_id": "v7",
    "track_id": "a1b2c3d4-e5f6-...",
    "track_name": "毕业季的歌",
    "version_number": 1,
    "version_label": "初次创作·抒情告别曲",
    "parent_version_id": null,
    "caption": "A warm nostalgic pop ballad instrumental...",
    "midi_url": "/outputs/v7.mid",
    "audio_url": "/outputs/v7.wav",
    "plan": {
      "theme": "graduation farewell",
      "style": "pop ballad",
      "mood": ["nostalgic", "warm"],
      "tempo": 76,
      "key": "A minor",
      "instruments": ["piano", "strings"],
      "structure": ["intro", "verse", "chorus", "outro"]
    },
    "mock": false,
    "change_reason": null,
    "parameter_diff": null
  }
}
```

#### POST /api/revise — 请求

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `version_id` | string | ✅ | 要修改的历史版本 ID |
| `feedback` | string | ✅ | 用户反馈意见 |
| `version_label` | string | 选填 | 版本描述，不填自动生成 |

#### POST /api/revise — 响应

与 generate 响应结构相同，额外包含：

| 新增字段 | 类型 | 说明 |
|---|---|---|
| `parent_version_id` | string | 父版本 ID（即请求中的 version_id） |
| `change_reason` | string | AI 生成的中文修改解释 |
| `parameter_diff` | object | 修改前后的参数对比 |

`parameter_diff` 结构：
```json
{
  "mood": { "from": ["nostalgic", "warm"], "to": ["hopeful", "warm"] },
  "tempo": { "from": 76, "to": 88 },
  "key": { "from": "A minor", "to": "C major" },
  "instruments": { "add": ["acoustic guitar"], "remove": [] }
}
```

#### GET /api/tracks — Track 列表

无需参数。响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "items": [
      {
        "track_id": "a1b2c3d4...",
        "track_name": "毕业季的歌",
        "version_number": 3,
        "created_at": "2026-07-13T14:00:00",
        "updated_at": "2026-07-13T16:00:00",
        "latest_version": { "...": "..." }
      }
    ],
    "total": 2
  }
}
```

#### GET /api/track/{track_id} — Track 版本历史

返回指定 Track 下所有版本（按版本号倒序），每个版本含完整信息（plan / caption / change_reason / parameter_diff / 文件路径）。

#### GET /api/versions — 全局版本列表

分页参数：`page`（从 0 开始，默认 0）、`size`（默认 10，最大 100）。返回所有版本的摘要。

#### GET /api/version/{version_id} — 版本详情

返回单版本完整信息，包括 plan、caption、文件路径、change_reason、parameter_diff。

#### GET /api/health — 健康检查

返回服务状态、数据库状态、版本总数、存储信息（路径/存在/可写/剩余空间）。

### 4.3 文件上传接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/upload` | 上传参考文件（multipart/form-data） |
| `GET` | `/api/uploads/version/{version_id}` | 查看某版本的参考文件列表 |
| `DELETE` | `/api/uploads/{file_id}` | 删除指定文件 |

#### POST /api/upload — 请求参数

| 参数 | 类型 | 说明 |
|---|---|---|
| `file` | MultipartFile | 文件，必填 |
| `version_id` | string | 关联的版本 ID，选填 |
| `track_id` | string | 关联的 Track ID，选填（用于生成前查找） |

支持的格式：`.txt` / `.docx` / `.xlsx` / `.musicxml` / `.xml` / `.mxl`。`.mxl` 自动解压（ZIP 包内含 XML）。大小限制：10MB。

响应包含 `extracted_text`（提取的文本内容）和 `extracted_json`（MusicXML/xlsx 的结构化解析结果）。

### 4.4 合规检测与版权存证接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/compliance/check` | 合规检测（旋律/歌词/音色相似度） |
| `GET` | `/api/compliance/history` | 检测历史列表 |
| `POST` | `/api/copyright/register` | 版权存证申请 |
| `GET` | `/api/copyright/records` | 存证记录列表 |
| `GET` | `/api/copyright/record/{record_id}` | 存证记录详情 |

> 完整请求/响应示例见 **[API.md](./API.md)**

---

## 5. 数据库设计

JPA `ddl-auto: update` 自动建表。

### 5.1 `music_versions` — 音乐版本（核心表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `version_id` | VARCHAR(32) | PK | 全局自增版本号，如 v1, v2, v3 |
| `track_id` | VARCHAR(36) | INDEX | Track 标识（UUID），同一 Track 的所有版本共享 |
| `track_name` | VARCHAR(128) | | 音乐名称，用户手动输入 |
| `version_number` | INT | | Track 内的版本序号（从 1 开始） |
| `version_label` | VARCHAR(256) | | 版本描述，用户可编辑（类似 Git commit message） |
| `parent_version_id` | VARCHAR(32) | FK→version_id | 父版本 ID（首次生成为 null，回炉时指向上一版） |
| `user_prompt` | TEXT | | 用户最初的中文需求 |
| `style` | VARCHAR(64) | | 音乐风格 |
| `mood` | VARCHAR(64) | | 音乐情绪 |
| `tempo` | VARCHAR(32) | | 速度（slow / medium / fast 或 BPM） |
| `instruments` | TEXT | | 乐器列表（JSON 数组字符串） |
| `feedback` | TEXT | | 回炉时的用户反馈意见 |
| `plan` | TEXT | | AI 生成的结构化作曲方案（JSON） |
| `caption` | TEXT | | Text2MIDI 使用的英文描述 |
| `midi_path` | VARCHAR(512) | | MIDI 文件路径 |
| `audio_path` | VARCHAR(512) | | WAV 音频文件路径 |
| `change_reason` | TEXT | | AI 生成的中文修改原因 |
| `parameter_diff` | TEXT | | 参数差异对比（JSON，回炉时填充） |
| `is_mock` | BOOLEAN | | 是否 Mock 降级版本 |
| `created_at` | DATETIME | | 创建时间 |

### 5.2 `uploaded_files` — 上传文件

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 自增 ID |
| `version_id` | VARCHAR(32) FK | 关联的版本 |
| `track_id` | VARCHAR(36) | 关联的 Track |
| `original_name` | VARCHAR(256) | 原始文件名 |
| `file_type` | VARCHAR(16) | txt / docx / xlsx / musicxml |
| `stored_path` | VARCHAR(512) | 服务器存储路径 |
| `extracted_text` | TEXT | 提取的文本内容（供 prompt 使用） |
| `extracted_json` | TEXT | MusicXML 或 xlsx 的结构化解析结果 |
| `file_size` | BIGINT | 文件大小（字节） |
| `created_at` | DATETIME | 上传时间 |

### 5.3 其他表

| 表名 | 说明 |
|---|---|
| `compliance_records` | 版权合规检测记录 |
| `copyright_records` | 版权存证记录 |

---

## 6. 启动说明

### 前置条件

- **JDK 22** — `D:\Program Files\Java\jdk-22`
- **Maven 3.9.7** — `C:\Users\Lenovo\apache-maven-3.9.7`
- **Python 3.14** — `C:/Python314/python.exe`（管线调用，可选）
- **FluidSynth + SoundFont**（管线生成 WAV 用，可选）

### 启动命令

```bash
cd backend
set JAVA_HOME=D:\Program Files\Java\jdk-22
C:\Users\Lenovo\apache-maven-3.9.7\bin\mvn spring-boot:run
```

或在 IDE 中直接运行 `MusicPlatformApplication.main()`。

### 环境变量

| 变量 | 说明 | 必填 |
|---|---|---|
| `MUSIC_API_KEY` | GPT API Key | 管线模式必填 |
| `MUSIC_API_BASE_URL` | GPT API 端点 | 管线模式必填 |
| `MUSIC_MODEL` | GPT 模型名（默认 gpt-5.5） | 选填 |

> 不设置 API Key 也可启动，后端自动降级为 Mock 模式。

### 启动后访问

| 地址 | 说明 |
|---|---|
| `http://localhost:8080` | 后端服务 |
| `http://localhost:8080/swagger-ui.html` | Swagger 在线接口调试 |
| `http://localhost:8080/api/health` | 健康检查 |
| `http://localhost:8080/outputs/{filename}` | 静态文件（MIDI / WAV） |

---

## 7. 完整数据流

```
前端 → POST /api/generate {track_name, user_prompt, style, mood, tempo, instruments}
  │
  ├─ [可选] POST /api/upload {file} → 上传歌词/乐谱 → 提取内容
  │
  ▼
后端 MusicService.generate()
  ├─ 创建 Track（UUID）或追加版本
  ├─ isPipelineAvailable()?
  │   ├─ YES → runPipeline() → Python --mode generate → GPT → music_json → MIDI → WAV
  │   └─ NO  → populateVersionMock() → 占位 WAV + Mock plan
  └─ 存入 music_versions 表 → 返回 JSON

前端 → POST /api/revise {version_id, feedback}
  │
  ▼
后端 MusicService.revise()
  ├─ buildRevisePrompt(parent, feedback)  ← 含旧 plan 全量参数
  ├─ runPipeline(..., "revise") → Python --mode revise
  │   → GPT 返回 wrapper JSON {music_json, change_reason, parameter_diff}
  │   → 拆解 → music_json → MIDI → WAV
  ├─ change_reason / parameter_diff 写入 manifest
  └─ 存入 music_versions 表 → 返回 JSON（含 diff 和修改原因）
```
