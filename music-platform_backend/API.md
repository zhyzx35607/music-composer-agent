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

MIDI / WAV 文件通过相对路径返回（如 `/outputs/v1/v1.mid`），前端拼接 Base URL 即可访问：

```
http://localhost:8080/outputs/v1/v1.mid
http://localhost:8080/outputs/v1/v1.wav
```

> 建议前端配置开发代理，将 `/outputs` 路径转发到 `localhost:8080`。

---

## 2. 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/generate` | 首次生成音乐 |
| `POST` | `/api/revise` | 反馈修改 |
| `GET` | `/api/versions` | 分页获取版本列表 |
| `GET` | `/api/version/{id}` | 获取版本详情 |
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/outputs/{filename}` | 静态文件访问（MIDI / WAV） |

---

## 3. 接口详情

### 3.1 POST /api/generate — 首次生成音乐

根据用户中文需求生成作曲方案和音频文件。

**Request**

```json
{
  "user_prompt": "我想要一首毕业季的歌，抒情一点",
  "style": "pop ballad",
  "mood": "nostalgic",
  "tempo": "medium",
  "instruments": ["piano", "strings"]
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `user_prompt` | string | ✅ 必填 | — | 用户中文需求，最长 2000 字符 |
| `style` | string | 选填 | `"pop"` | 音乐风格 |
| `mood` | string | 选填 | `"calm"` | 情绪 |
| `tempo` | string | 选填 | `"medium"` | 速度（slow / medium / fast） |
| `instruments` | string[] | 选填 | `["piano"]` | 乐器列表 |

**Response** `201 Created`

```json
{
  "code": 201,
  "message": "created",
  "data": {
    "version_id": "v1",
    "caption": "A pop ballad instrumental piece with a nostalgic mood, medium tempo, featuring piano, strings.",
    "midi_url": "/outputs/v1/v1.mid",
    "audio_url": "/outputs/v1/v1.wav",
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

| 字段 | 类型 | 说明 |
|------|------|------|
| `version_id` | string | 版本 ID，如 `v1`, `v2`, `v3` |
| `caption` | string | 英文描述（供 Text2MIDI 使用） |
| `midi_url` | string | MIDI 文件相对路径 |
| `audio_url` | string | WAV 文件相对路径 |
| `plan` | object | 结构化作曲方案 |

---

### 3.2 POST /api/revise — 反馈修改

基于已有版本和用户反馈，生成新的修改版本。

**Request**

```json
{
  "version_id": "v1",
  "feedback": "这版太悲伤了，想要更有希望一点，加一点钢琴"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `version_id` | string | ✅ 必填 | 要修改的历史版本 ID |
| `feedback` | string | ✅ 必填 | 用户反馈意见 |

**Response** `201 Created`

与 `/api/generate` 结构相同，额外包含：

```json
{
  "code": 201,
  "message": "created",
  "data": {
    "version_id": "v2",
    "caption": "...",
    "midi_url": "/outputs/v2/v2.mid",
    "audio_url": "/outputs/v2/v2.wav",
    "plan": { "...": "..." },
    "parent_version_id": "v1",
    "change_reason": "根据用户反馈，调整了音乐参数..."
  }
}
```

| 额外字段 | 类型 | 说明 |
|----------|------|------|
| `parent_version_id` | string | 父版本 ID |
| `change_reason` | string | 智能体给出的修改原因 |

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
        "created_at": "2026-07-09T14:30:00",
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
| `items[].created_at` | string | 创建时间（ISO 8601） |
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
    "midi_url": "/outputs/v1/v1.mid",
    "audio_url": "/outputs/v1/v1.wav",
    "change_reason": null,
    "created_at": "2026-07-09T14:30:00",
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

### 3.5 GET /api/health — 健康检查

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

### 3.6 GET /outputs/{filename} — 静态文件

直接访问 MIDI / WAV 文件。文件名格式：`{version_id}/{version_id}.mid` 或 `{version_id}/{version_id}.wav`

```
GET /outputs/v1/v1.mid   → 下载 MIDI 文件
GET /outputs/v1/v1.wav   → 下载 WAV 文件
```

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

2. **文件 URL 拼接**：接口返回的 `midi_url` / `audio_url` 是相对路径（如 `/outputs/v1/v1.mid`），需拼接 Base URL：
   ```
   http://localhost:8080/outputs/v1/v1.mid
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

4. **create → list → detail 流程**：先 `POST /api/generate` 拿到 `version_id`，再 `GET /api/versions` 展示列表，点击具体版本时 `GET /api/version/{id}` 获取详情。

5. **修订流程**：`POST /api/revise` 传入已有 `version_id` + `feedback`，返回新的 `version_id`，前端展示新版本即可。
