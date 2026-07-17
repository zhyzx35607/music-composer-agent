# 音乐创作智能体平台 — API 接口文档

> 后端服务地址：`http://localhost:8080`  
> Swagger UI（在线调试）：`http://localhost:8080/swagger-ui.html`

---

## 1. 通用约定

### 1.1 统一响应格式

所有接口返回 JSON，结构为：

```json
{
  "code": 200,       // 业务状态码，对齐 HTTP 状态码
  "message": "ok",   // 人类可读的说明
  "data": { ... }    // 业务数据，可能为 null
}
```

### 1.2 状态码一览

| code | 含义 |
|------|------|
| 200  | 成功 |
| 201  | 创建成功（generate / revise） |
| 400  | 请求参数错误 |
| 404  | 资源不存在 |
| 500  | 服务器内部错误 |

### 1.3 静态文件 URL

MIDI / WAV 文件通过相对路径返回（如 `/outputs/v1.mid`），前端拼接 Base URL 即可访问：

```
http://localhost:8080/outputs/v1.mid
http://localhost:8080/outputs/v1.wav
```

> 建议前端配置开发代理，将 `/outputs` 路径转发到 `localhost:8080`。

---

## 2. 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/generate` | 首次生成音乐（Track 模型） |
| `POST` | `/api/revise` | 反馈修改（回炉，含参数 diff） |
| `GET` | `/api/tracks` | 获取 Track 列表（按音乐分组） |
| `GET` | `/api/track/{track_id}` | 获取某 Track 的完整版本历史 |
| `GET` | `/api/versions` | 分页获取全局版本列表 |
| `GET` | `/api/version/{id}` | 获取版本详情 |
| `GET` | `/api/health` | 健康检查 |
| `POST` | `/api/upload` | 上传参考文件（txt/docx/xlsx/musicxml/mxl） |
| `GET` | `/api/uploads/version/{version_id}` | 查看某版本关联的参考文件 |
| `DELETE` | `/api/uploads/{file_id}` | 删除指定文件 |
| `POST` | `/api/compliance/check` | 合规检测 |
| `GET` | `/api/compliance/history` | 检测历史 |
| `POST` | `/api/copyright/register` | 版权存证 |
| `GET` | `/api/copyright/records` | 存证记录列表 |
| `GET` | `/api/copyright/record/{id}` | 存证记录详情 |
| `GET` | `/api/copyright/evidence-package/{version_id}` | 版权存证统一证据包 |
| `GET` | `/outputs/{filename}` | 静态文件访问（MIDI / WAV） |

---

## 3. 接口详情

### 3.1 POST /api/generate — 首次生成音乐

根据用户中文需求和可选文件，生成作曲方案和音频文件。

**Request**

```json
{
  "user_prompt": "我想要一首毕业季的歌，抒情一点",
  "track_name": "毕业季的歌",
  "track_id": "a1b2c3d4-e5f6...",
  "version_label": "抒情初版",
  "style": "pop ballad",
  "mood": "nostalgic",
  "tempo": "medium",
  "instruments": ["piano", "strings"]
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `user_prompt` | string | ✅ 必填 | — | 用户中文需求，最长 2000 字符 |
| `track_name` | string | 建议填 | — | 音乐名称，如"毕业季的歌" |
| `track_id` | string | 选填 | — | 已有 Track ID，传了则在对应 Track 下追加版本 |
| `version_label` | string | 选填 | 自动生成 | 版本描述（类似 Git commit message） |
| `style` | string | 选填 | `"pop"` | 音乐风格 |
| `mood` | string | 选填 | `"calm"` | 情绪 |
| `tempo` | string | 选填 | `"medium"` | 速度（slow / medium / fast） |
| `instruments` | string[] | 选填 | `["piano"]` | 乐器列表 |

> 如果上传了参考文件（`POST /api/upload`），后端会在生成时自动将文件内容注入 prompt。

**Response** `201 Created`

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

| 字段 | 类型 | 说明 |
|------|------|------|
| `version_id` | string | 全局自增版本 ID，如 `v1`, `v2`, `v3` |
| `track_id` | string | Track 标识（UUID） |
| `track_name` | string | 音乐名称 |
| `version_number` | number | Track 内的版本序号，从 1 开始 |
| `version_label` | string | 版本描述 |
| `parent_version_id` | string\|null | 父版本 ID，首次生成为 null |
| `caption` | string | 英文描述（供 Text2MIDI 使用） |
| `midi_url` | string | MIDI 文件相对路径 |
| `audio_url` | string | WAV 文件相对路径 |
| `plan` | object | 结构化作曲方案 |
| `mock` | boolean | 是否 Mock 降级版本 |
| `change_reason` | string\|null | 首次生成为 null，回炉时有值 |
| `parameter_diff` | object\|null | 首次生成为 null，回炉时有值 |

---

### 3.2 POST /api/revise — 反馈修改（回炉）

基于已有版本和用户反馈，AI 做对比式修改，生成新版本。

**Request**

```json
{
  "version_id": "v1",
  "feedback": "这版太悲伤了，想要更有希望一点，加一点钢琴",
  "version_label": "希望版"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `version_id` | string | ✅ 必填 | 要修改的历史版本 ID |
| `feedback` | string | ✅ 必填 | 用户反馈意见 |
| `version_label` | string | 选填 | 版本描述，不填则自动生成 |

**Response** `201 Created`

与 `/api/generate` 响应结构相同，包含所有 Track 字段，额外增加回炉专属字段：

```json
{
  "code": 201,
  "message": "created",
  "data": {
    "version_id": "v8",
    "track_id": "a1b2c3d4-...",
    "track_name": "毕业季的歌",
    "version_number": 2,
    "version_label": "回炉·更欢快，加吉他",
    "parent_version_id": "v7",
    "caption": "A brighter and more hopeful pop ballad...",
    "midi_url": "/outputs/v8.mid",
    "audio_url": "/outputs/v8.wav",
    "plan": {
      "theme": "graduation farewell",
      "style": "pop ballad",
      "mood": ["hopeful", "warm"],
      "tempo": 88,
      "key": "C major",
      "instruments": ["piano", "strings", "acoustic guitar"],
      "structure": ["intro", "verse", "chorus", "outro"]
    },
    "mock": false,
    "change_reason": "根据用户反馈「更欢快一点，加吉他」，将 BPM 从 76 提升至 88，调式从 A minor 改为 C major，加入木吉他声部，整体氛围从忧郁转为温暖希望。",
    "parameter_diff": {
      "mood": { "from": ["nostalgic", "warm"], "to": ["hopeful", "warm"] },
      "tempo": { "from": 76, "to": 88 },
      "key": { "from": "A minor", "to": "C major" },
      "instruments": { "add": ["acoustic guitar"], "remove": [] }
    }
  }
}
```

| 额外字段 | 类型 | 说明 |
|----------|------|------|
| `parent_version_id` | string | 父版本 ID |
| `change_reason` | string | AI 生成的中文修改解释 |
| `parameter_diff` | object | 修改前后的参数差异对比 |

`parameter_diff` 子字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `mood.from` / `mood.to` | string[] | 情绪变化 |
| `tempo.from` / `tempo.to` | number | 速度 BPM 变化 |
| `key.from` / `key.to` | string | 调式变化 |
| `instruments.add` | string[] | 新增乐器 |
| `instruments.remove` | string[] | 移除乐器 |

> `change_reason` 由 AI 生成，非模板拼接。如果管线不可用，后端会自行对比新旧 plan 计算 `parameter_diff`。

---

### 3.3 GET /api/versions — 版本列表

分页获取所有历史版本摘要，按创建时间倒序。

**Request**

```
GET /api/versions?page=0&size=10
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | `0` | 页码，从 0 开始 |
| `size` | int | `10` | 每页条数，最大 100 |

**Response** `200 OK`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "items": [
      {
        "version_id": "v1",
        "parent_version_id": null,
        "user_prompt": "我想要一首毕业季的歌，抒情一点",
        "style": "pop ballad",
        "mood": "nostalgic",
        "tempo": "medium",
        "created_at": "2026-07-09T14:30:00",
        "mock": false,
        "caption_preview": "A pop ballad instrumental piece..."
      }
    ],
    "total": 5,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `items` | array | 版本摘要列表 |
| `items[].version_id` | string | 版本 ID |
| `items[].parent_version_id` | string\|null | 父版本 ID，首次生成为 null |
| `items[].user_prompt` | string | 用户中文需求 |
| `items[].style` | string | 风格 |
| `items[].mood` | string | 情绪 |
| `items[].tempo` | string | 速度 |
| `items[].created_at` | string | 创建时间（ISO 8601） |
| `items[].mock` | boolean | 是否 Mock 降级版本 |
| `items[].caption_preview` | string | 英文描述预览（超过 100 字符截断） |
| `total` | number | 总版本数 |
| `totalPages` | number | 总页数 |
| `page` | number | 当前页码 |
| `size` | number | 每页条数 |

---

### 3.4 GET /api/version/{id} — 版本详情

获取单个版本的完整信息，包括 plan、文件路径等。

**Request**

```
GET /api/version/v1
```

**Response** `200 OK`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "version_id": "v1",
    "parent_version_id": null,
    "user_prompt": "我想要一首毕业季的歌，抒情一点",
    "style": "pop ballad",
    "mood": "nostalgic",
    "tempo": "medium",
    "instruments": "[\"piano\",\"strings\"]",
    "feedback": null,
    "caption": "A pop ballad instrumental piece with a nostalgic mood...",
    "midi_url": "/outputs/v1.mid",
    "audio_url": "/outputs/v1.wav",
    "change_reason": null,
    "created_at": "2026-07-09T14:30:00",
    "mock": false,
    "plan": {
      "theme": "a beautiful musical piece",
      "style": "pop ballad",
      "mood": ["nostalgic"],
      "tempo": 88,
      "key": "C major",
      "instruments": ["piano", "strings"],
      "structure": ["intro", "verse", "chorus", "outro"]
    }
  }
}
```

**404 响应**

```json
{
  "code": 404,
  "message": "版本不存在: v99",
  "data": null
}
```

---

### 3.5 GET /api/tracks — 获取 Track 列表

获取所有 Track（按最新版本分组），每个 Track 展示最新版本摘要。

**Request**

```
GET /api/tracks
```

**Response** `200 OK`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "items": [
      {
        "track_id": "a1b2c3d4-e5f6...",
        "track_name": "毕业季的歌",
        "version_number": 3,
        "created_at": "2026-07-13T14:00:00",
        "updated_at": "2026-07-13T16:00:00",
        "latest_version": {
          "version_id": "v3",
          "track_id": "a1b2c3d4...",
          "track_name": "毕业季的歌",
          "version_number": 3,
          "version_label": "回炉·放慢，情绪改温暖",
          "parent_version_id": "v2",
          "user_prompt": "我想要一首毕业季的歌...",
          "style": "pop ballad",
          "mood": "warm",
          "tempo": "slow",
          "created_at": "2026-07-13T16:00:00",
          "mock": false,
          "midi_url": "/outputs/v3.mid",
          "audio_url": "/outputs/v3.wav",
          "caption_preview": "A warm nostalgic...",
          "caption": "A warm nostalgic pop ballad...",
          "plan": { "...": "..." }
        }
      }
    ],
    "total": 2
  }
}
```

> `version_number` 即最新版本号，近似等于总版本数。

---

### 3.6 GET /api/track/{track_id} — 获取 Track 版本历史

获取指定 Track 下的所有版本（按版本号倒序）。

**Request**

```
GET /api/track/a1b2c3d4-e5f6...
```

**Response** `200 OK`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "track_id": "a1b2c3d4...",
    "track_name": "毕业季的歌",
    "total_versions": 3,
    "versions": [
      {
        "version_id": "v3",
        "version_number": 3,
        "version_label": "回炉·放慢，情绪改温暖",
        "parent_version_id": "v2",
        "user_prompt": "我想要一首毕业季的歌...",
        "style": "pop ballad",
        "mood": "warm",
        "tempo": "slow",
        "instruments": ["piano", "strings"],
        "feedback": "放慢一点，情绪温暖一些",
        "caption": "A warm nostalgic pop ballad...",
        "midi_url": "/outputs/v3.mid",
        "audio_url": "/outputs/v3.wav",
        "change_reason": "根据用户反馈...",
        "parameter_diff": {
          "mood": { "from": ["hopeful", "warm"], "to": ["warm", "nostalgic"] },
          "tempo": { "from": 88, "to": 72 }
        },
        "created_at": "2026-07-13T16:00:00",
        "mock": false
      }
    ]
  }
}
```

> 每个 version 包含 `change_reason` 和 `parameter_diff`（首次版本为 null）。

---

### 3.7 文件上传接口

#### POST /api/upload — 上传参考文件

**Request** `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file | ✅ | 文件 |
| `version_id` | string | 选填 | 关联的版本 ID |
| `track_id` | string | 选填 | 关联的 Track ID（用于生成前查找） |

支持格式：`.txt` / `.docx` / `.xlsx` / `.musicxml` / `.xml` / `.mxl`。`.mxl` 自动解压（ZIP 包内含 XML）。最大 10MB。

**Response** `201 Created`

```json
{
  "code": 201,
  "message": "created",
  "data": {
    "file_id": 1,
    "original_name": "毕业季歌词.txt",
    "file_type": "txt",
    "extracted_text": "夏天的风吹过操场，我们即将各奔东西...",
    "extracted_json": null,
    "file_size": 256,
    "version_id": "v1",
    "track_id": "a1b2c3d4...",
    "created_at": "2026-07-13T14:00:00"
  }
}
```

MusicXML 的响应还包含结构化数据：

```json
{
  "file_id": 2,
  "original_name": "theme.musicxml",
  "file_type": "musicxml",
  "extracted_text": "Key: D major, Time: 4/4, Tempo: 120 BPM, Notes: 156...",
  "extracted_json": {
    "key": "D major",
    "time_signature": "4/4",
    "tempo_bpm": 120,
    "note_count": 156,
    "range": "G3 ~ C6",
    "melody_notes": ["D4", "F#4", "A4", "G4", "F#4", "E4", "D4"],
    "chords": ["D major", "G major", "A7", "B minor"]
  },
  "file_size": 8192,
  "version_id": "v1",
  "track_id": "a1b2c3d4...",
  "created_at": "2026-07-13T14:01:00"
}
```

#### GET /api/uploads/version/{version_id} — 查看关联文件

**Request**

```
GET /api/uploads/version/v1
```

**Response** `200 OK`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "items": [ { "file_id": 1, "...": "..." } ],
    "total": 2,
    "version_id": "v1"
  }
}
```

#### DELETE /api/uploads/{file_id} — 删除文件

**Request**

```
DELETE /api/uploads/1
```

**Response** `200 OK`（文件不存在时返回 404）

---

### 3.8 GET /api/health — 健康检查

**Response** `200 OK`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "service": "UP",
    "database": "UP",
    "totalVersions": 3,
    "storage": {
      "path": "C:\\...\\outputs",
      "exists": true,
      "writable": true,
      "freeSpaceMB": 120000
    },
    "healthy": true
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `service` | string | 服务状态（`UP`） |
| `database` | string | 数据库连接状态（`UP` / `DOWN`） |
| `totalVersions` | number | 数据库中的版本总数 |
| `storage.path` | string | 文件存储路径 |
| `storage.exists` | boolean | 目录是否存在 |
| `storage.writable` | boolean | 目录是否可写 |
| `storage.freeSpaceMB` | number | 剩余空间（MB） |
| `healthy` | boolean | 整体健康状态 |

---

### 3.9 GET /outputs/{filename} — 静态文件

直接访问 MIDI / WAV 文件。文件名格式：`{version_id}.mid` 或 `{version_id}.wav`

```
GET /outputs/v1.mid   → 下载 MIDI 文件
GET /outputs/v1.wav   → 下载 WAV 文件
```

---

### 3.10 POST /api/compliance/check — 合规检测

对指定版本进行版权合规检测，返回相似度评分和风险等级。

**Request**

```json
{
  "version_id": "v1",
  "check_type": "all"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `version_id` | string | ✅ | 要检测的版本 ID |
| `check_type` | string | 选填 | `melody` / `lyrics` / `timbre` / `all`（默认 `all`） |

**Response** `201 Created`

```json
{
  "code": 201,
  "message": "created",
  "data": {
    "overall_score": 3.6,
    "details": {
      "melody_similarity": 2.48,
      "lyric_similarity": 2.96,
      "timbre_similarity": 5.46
    },
    "matched_works": [],
    "risk_level": "low",
    "checked_at": "2026-07-12T11:22:03.138"
  }
}
```

---

### 3.11 GET /api/compliance/history — 检测历史

```
GET /api/compliance/history
```

**Response** `200 OK`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "items": [
      {
        "id": "1",
        "version_id": "v1",
        "check_type": "all",
        "risk_level": "low",
        "overall_score": 3.6,
        "checked_at": "2026-07-12T11:22:03.138"
      }
    ],
    "total": 1,
    "totalPages": 1,
    "page": 0,
    "size": 1
  }
}
```

---

### 3.12 POST /api/copyright/register — 版权存证

提交版权存证申请，记录创作全流程。

**Request**

```json
{
  "version_id": "v1",
  "creator_name": "张三"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `version_id` | string | ✅ | 要存证的版本 ID |
| `creator_name` | string | ✅ | 创作者姓名 |

**Response** `201 Created`

```json
{
  "code": 201,
  "message": "created",
  "data": {
    "record_id": "cr-1783826523335",
    "version_id": "v1",
    "creator_name": "张三",
    "certificate_hash": "0x8836...",
    "block_height": 523336,
    "created_at": "2026-07-12T11:22:03.338",
    "prompt_history": ["test song"],
    "revision_history": [],
    "status": "pending"
  }
}
```

---

### 3.13 GET /api/copyright/records — 存证记录列表

```
GET /api/copyright/records
```

返回格式同 `/api/compliance/history`，`items` 为版权存证记录数组。

---

### 3.14 GET /api/copyright/record/{record_id} — 存证记录详情

```
GET /api/copyright/record/cr-1783826523335
```

返回单条版权存证记录的完整信息，结构与注册响应一致。

---

### 3.15 GET /api/copyright/evidence-package/{version_id} — 版权存证统一证据包

给版权声明/存证系统使用。该接口按 `version_id` 聚合数据库版本记录和 `outputs` 目录下的真实文件，返回项目关联信息、版本链、AI 生成/修订记录、输出文件列表和 SHA-256 哈希。

```http
GET /api/copyright/evidence-package/v27?creator_name=张三&creator_id=user-001
```

可选查询参数：

| 参数 | 说明 |
| --- | --- |
| `creator_name` | 创作者姓名 |
| `creator_id` | 创作者 ID |
| `external_project_id` | 对方存证系统中的项目 ID |
| `project_title` | 项目/作品名称 |

返回 `data` 主要字段：

| 字段 | 说明 |
| --- | --- |
| `project` | 项目关联信息 |
| `versions` | 从初始版本到当前版本的版本链 |
| `records` | 每个版本的结构化 AI 证据记录 |
| `generated_files` | WAV/MIDI/music_json/prompt/manifest/ai_record 等真实文件清单 |
| `file_hashes_sha256` | 文件名到 SHA-256 的哈希映射 |
| `logs` | 审计日志位置说明 |
| `minimum_package_ready` | 是否满足最小对接材料 |

---

## 4. 错误响应示例

### 400 — 参数校验失败

```json
{
  "code": 400,
  "message": "user_prompt 不能为空",
  "data": null
}
```

### 404 — 版本不存在

```json
{
  "code": 404,
  "message": "版本不存在: v99",
  "data": null
}
```

### 500 — 服务器错误

```json
{
  "code": 500,
  "message": "服务器内部错误: ...",
  "data": null
}
```

---

## 5. 前端对接注意事项

1. **响应结构**：后端返回 `{ code, message, data }`，业务数据在 `data` 字段内。以 axios 为例：
   ```js
   const res = await axios.post('/api/generate', payload);
   // res.data  → { code: 201, message: "created", data: { version_id: "v1", ... } }
   // 取业务数据: res.data.data.version_id
   ```

2. **文件 URL 拼接**：接口返回的 `midi_url` / `audio_url` 是相对路径（如 `/outputs/v1.mid`），需拼接 Base URL：
   ```
   http://localhost:8080/outputs/v1.mid
   ```

3. **开发代理配置**（Vite 示例）：
   ```ts
   // vite.config.ts
   export default {
     server: {
       proxy: {
         '/api': 'http://localhost:8080',
         '/outputs': 'http://localhost:8080',
       }
     }
   }
   ```

4. **Track 分组流程**：首页建议用 `GET /api/tracks` 展示 Track 列表（而非 `/api/versions` 的扁平列表）。点击某 Track 后用 `GET /api/track/{id}` 获取版本历史。

5. **修订流程**：`POST /api/revise` 传入已有 `version_id` + `feedback`，响应会额外包含 `change_reason` 和 `parameter_diff`，前端应展示这两个字段。

6. **文件上传流程**：`POST /api/upload` 上传文件（传 `track_id` 关联到 Track），之后调用 `POST /api/generate` 时后端会自动将文件内容注入 prompt。用 `GET /api/uploads/version/{version_id}` 查看某版本的关联文件。
