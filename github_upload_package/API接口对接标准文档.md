# AI音乐创作平台 API 接口对接标准文档

## 1. 文档目的

本文档用于约定 AI 音乐创作平台与其他两个功能模块之间的接口标准，方便前端、音乐创作后端、合规检测模块、区块链版权存证模块进行统一对接。

本平台包含三个核心模块：

1. 音乐创作模块：根据用户需求生成 music_json、MIDI、WAV 音频。
2. 合规检测模块：检测 AI 生成音乐是否存在旋律、歌词、音色等版权风险。
3. 版权存证模块：将生成作品及创作过程信息进行区块链存证。

## 2. 总体架构

建议采用统一后端网关方式对接：

```text
前端
  -> 平台后端 API 网关
      -> 音乐创作模块
      -> 合规检测模块
      -> 区块链版权存证模块
```

前端只请求统一后端地址：

```text
http://localhost:8080
```

开发环境下，前端 Vite 已配置代理：

```text
/api     -> http://localhost:8080/api
/outputs -> http://localhost:8080/outputs
```

## 3. 通用接口规范

### 3.1 请求格式

所有接口默认使用 JSON 请求体：

```http
Content-Type: application/json
```

字符编码统一使用：

```text
UTF-8
```

### 3.2 统一响应格式

所有业务接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 状态码，0 表示成功，非 0 表示失败 |
| message | string | 响应说明 |
| data | object / array / null | 实际业务数据 |

### 3.3 通用错误码

| code | 含义 | 说明 |
|---|---|---|
| 0 | success | 请求成功 |
| 400 | bad_request | 请求参数错误 |
| 404 | not_found | 资源不存在 |
| 422 | invalid_data | 数据格式不合法 |
| 500 | server_error | 服务内部错误 |
| 502 | upstream_error | 调用其他模块失败 |
| 504 | timeout | 生成、检测或存证超时 |

错误示例：

```json
{
  "code": 422,
  "message": "invalid music_json format",
  "data": null
}
```

## 4. 核心数据标识

### 4.1 version_id

`version_id` 是三个模块之间最重要的关联字段。

每生成一次音乐，音乐创作模块都应该生成一个唯一版本 ID，例如：

```text
v_20260711_001
```

合规检测和版权存证都通过 `version_id` 找到对应作品。

### 4.2 文件地址

音乐创作模块需要提供：

```text
midi_url
audio_url
music_json_url
```

示例：

```json
{
  "midi_url": "/outputs/v_20260711_001/music.mid",
  "audio_url": "/outputs/v_20260711_001/music.wav",
  "music_json_url": "/outputs/v_20260711_001/music.json"
}
```

## 5. 音乐创作模块接口

### 5.1 生成音乐

```http
POST /api/generate
```

请求参数：

```json
{
  "user_prompt": "生成一段30秒紧张的游戏Boss战纯音乐",
  "style": "game battle",
  "mood": "tense",
  "tempo": "fast",
  "instruments": ["synth", "drums", "bass"]
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| user_prompt | string | 是 | 用户输入的自然语言需求 |
| style | string | 否 | 音乐风格 |
| mood | string | 否 | 情绪 |
| tempo | string | 否 | 速度，如 slow / medium / fast |
| instruments | string[] | 否 | 乐器列表 |

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "version_id": "v_20260711_001",
    "caption": "A tense game battle instrumental piece with synth, bass and drums.",
    "midi_url": "/outputs/v_20260711_001/music.mid",
    "audio_url": "/outputs/v_20260711_001/music.wav",
    "music_json_url": "/outputs/v_20260711_001/music.json",
    "plan": {
      "theme": "boss battle",
      "style": "game battle",
      "mood": ["tense", "energetic"],
      "tempo": 128,
      "key": "D minor",
      "instruments": ["lead synth", "bass", "drums"],
      "structure": ["intro", "loop", "ending"]
    }
  }
}
```

### 5.2 根据反馈修改音乐

```http
POST /api/revise
```

请求参数：

```json
{
  "version_id": "v_20260711_001",
  "feedback": "鼓点再强一点，旋律更紧张"
}
```

响应数据同 `/api/generate`，但新的版本需要记录 `parent_version_id`。

### 5.3 获取历史版本列表

```http
GET /api/versions?page=0&size=10
```

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "version_id": "v_20260711_001",
        "parent_version_id": null,
        "user_prompt": "生成一段30秒紧张的游戏Boss战纯音乐",
        "style": "game battle",
        "mood": "tense",
        "tempo": "fast",
        "instruments": ["synth", "drums", "bass"],
        "caption": "A tense game battle instrumental piece...",
        "caption_preview": "A tense game battle instrumental...",
        "created_at": "2026-07-11T10:30:00+08:00",
        "midi_url": "/outputs/v_20260711_001/music.mid",
        "audio_url": "/outputs/v_20260711_001/music.wav"
      }
    ],
    "total": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

### 5.4 获取单个版本详情

```http
GET /api/version/{version_id}
```

示例：

```http
GET /api/version/v_20260711_001
```

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "version_id": "v_20260711_001",
    "parent_version_id": null,
    "user_prompt": "生成一段30秒紧张的游戏Boss战纯音乐",
    "caption": "A tense game battle instrumental piece...",
    "created_at": "2026-07-11T10:30:00+08:00",
    "midi_url": "/outputs/v_20260711_001/music.mid",
    "audio_url": "/outputs/v_20260711_001/music.wav",
    "music_json_url": "/outputs/v_20260711_001/music.json",
    "plan": {
      "theme": "boss battle",
      "style": "game battle",
      "mood": ["tense", "energetic"],
      "tempo": 128,
      "key": "D minor",
      "instruments": ["lead synth", "bass", "drums"],
      "structure": ["intro", "loop", "ending"]
    }
  }
}
```

## 6. 合规检测模块接口

### 6.1 发起合规检测

```http
POST /api/compliance/check
```

请求参数：

```json
{
  "version_id": "v_20260711_001",
  "check_type": "all"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| version_id | string | 是 | 需要检测的音乐版本 ID |
| check_type | string | 是 | melody / lyrics / timbre / all |

`check_type` 可选值：

| 值 | 说明 |
|---|---|
| melody | 旋律相似度检测 |
| lyrics | 歌词相似度检测，纯音乐可返回 0 |
| timbre | 音色相似度检测 |
| all | 全面检测 |

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "overall_score": 18.5,
    "details": {
      "melody_similarity": 21.2,
      "lyric_similarity": 0,
      "timbre_similarity": 14.8
    },
    "matched_works": [
      {
        "title": "Example Song",
        "artist": "Example Artist",
        "similarity": 32.5,
        "section": "0:12-0:18"
      }
    ],
    "risk_level": "low",
    "checked_at": "2026-07-11T10:35:00+08:00"
  }
}
```

### 6.2 合规检测模块需要获取的作品信息

合规模块拿到 `version_id` 后，可通过音乐创作模块接口获取作品详情：

```http
GET /api/version/{version_id}
```

至少需要使用：

```json
{
  "version_id": "v_20260711_001",
  "midi_url": "/outputs/v_20260711_001/music.mid",
  "audio_url": "/outputs/v_20260711_001/music.wav",
  "music_json_url": "/outputs/v_20260711_001/music.json",
  "caption": "A tense game battle instrumental piece..."
}
```

建议检测依据：

| 检测类型 | 推荐输入 |
|---|---|
| melody | MIDI 或 music_json |
| lyrics | caption 或歌词文本，纯音乐可跳过 |
| timbre | WAV 音频 |
| all | MIDI + WAV + caption |

### 6.3 获取合规检测历史

```http
GET /api/compliance/history
```

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "check_001",
        "version_id": "v_20260711_001",
        "check_type": "all",
        "risk_level": "low",
        "overall_score": 18.5,
        "checked_at": "2026-07-11T10:35:00+08:00"
      }
    ],
    "total": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

## 7. 区块链版权存证模块接口

### 7.1 提交版权存证

```http
POST /api/copyright/register
```

请求参数：

```json
{
  "version_id": "v_20260711_001",
  "creator_name": "张三",
  "creator_id": "optional-user-id"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| version_id | string | 是 | 需要存证的音乐版本 ID |
| creator_name | string | 是 | 创作者姓名 |
| creator_id | string | 否 | 创作者身份标识 |

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "record_id": "cr_20260711_001",
    "version_id": "v_20260711_001",
    "creator_name": "张三",
    "certificate_hash": "0x6a8f3c...",
    "block_height": 1024001,
    "created_at": "2026-07-11T10:40:00+08:00",
    "prompt_history": [
      "生成一段30秒紧张的游戏Boss战纯音乐"
    ],
    "revision_history": [],
    "status": "confirmed"
  }
}
```

`status` 可选值：

| 值 | 说明 |
|---|---|
| pending | 已提交，等待链上确认 |
| confirmed | 已上链确认 |
| verified | 已完成校验 |

### 7.2 版权存证模块需要获取的作品信息

版权存证模块拿到 `version_id` 后，需要通过：

```http
GET /api/version/{version_id}
```

获取作品信息，并建议对以下内容计算哈希：

```json
{
  "version_id": "v_20260711_001",
  "user_prompt": "生成一段30秒紧张的游戏Boss战纯音乐",
  "caption": "A tense game battle instrumental piece...",
  "plan": {},
  "music_json_url": "/outputs/v_20260711_001/music.json",
  "midi_url": "/outputs/v_20260711_001/music.mid",
  "audio_url": "/outputs/v_20260711_001/music.wav",
  "created_at": "2026-07-11T10:30:00+08:00"
}
```

建议存证内容包括：

| 内容 | 说明 |
|---|---|
| version_id | 作品版本 ID |
| creator_name | 创作者姓名 |
| user_prompt | 用户原始需求 |
| prompt_history | 提示词交互历史 |
| revision_history | 修改反馈历史 |
| music_json_hash | AI 生成结构数据哈希 |
| midi_hash | MIDI 文件哈希 |
| wav_hash | WAV 文件哈希 |
| created_at | 作品生成时间 |

### 7.3 获取版权存证记录列表

```http
GET /api/copyright/records
```

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "record_id": "cr_20260711_001",
        "version_id": "v_20260711_001",
        "creator_name": "张三",
        "certificate_hash": "0x6a8f3c...",
        "block_height": 1024001,
        "created_at": "2026-07-11T10:40:00+08:00",
        "prompt_history": [
          "生成一段30秒紧张的游戏Boss战纯音乐"
        ],
        "revision_history": [],
        "status": "confirmed"
      }
    ],
    "total": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

### 7.4 获取单条版权存证记录

```http
GET /api/copyright/record/{record_id}
```

示例：

```http
GET /api/copyright/record/cr_20260711_001
```

响应数据同单条 `CopyrightRecord`。

## 8. 模块对接流程

### 8.1 音乐生成流程

```text
前端提交用户需求
-> POST /api/generate
-> 音乐创作模块生成 music_json
-> 转换 MIDI
-> 转换 WAV
-> 返回 version_id、midi_url、audio_url
```

### 8.2 合规检测流程

```text
前端选择 version_id
-> POST /api/compliance/check
-> 合规模块通过 version_id 获取 MIDI/WAV/music_json
-> 执行相似度检测
-> 返回 overall_score、details、risk_level
```

### 8.3 版权存证流程

```text
前端选择 version_id 并填写 creator_name
-> POST /api/copyright/register
-> 存证模块通过 version_id 获取作品和创作过程
-> 计算文件哈希和元数据哈希
-> 提交区块链存证
-> 返回 certificate_hash、block_height、status
```

## 9. 前端当前已使用的接口清单

当前前端已预留以下接口：

| 功能 | 方法 | 路径 |
|---|---|---|
| 生成音乐 | POST | `/api/generate` |
| 反馈修改 | POST | `/api/revise` |
| 历史列表 | GET | `/api/versions` |
| 版本详情 | GET | `/api/version/{version_id}` |
| 健康检查 | GET | `/api/health` |
| 合规检测 | POST | `/api/compliance/check` |
| 合规历史 | GET | `/api/compliance/history` |
| 版权存证 | POST | `/api/copyright/register` |
| 存证记录列表 | GET | `/api/copyright/records` |
| 存证记录详情 | GET | `/api/copyright/record/{record_id}` |

## 10. 健康检查接口

```http
GET /api/health
```

响应数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "music-platform-backend",
    "database": "ok",
    "totalVersions": 12,
    "healthy": true
  }
}
```

## 11. 对接注意事项

1. 合规检测和版权存证都必须以 `version_id` 作为作品唯一标识。
2. 音乐创作模块必须保证 MIDI/WAV 文件可访问。
3. 所有时间字段统一使用 ISO 8601 格式，例如 `2026-07-11T10:30:00+08:00`。
4. 所有文件 URL 建议使用相对路径，例如 `/outputs/v_001/music.wav`。
5. 如果某个模块暂时未完成，可以先返回 mock 数据，但字段结构必须保持一致。
6. 纯音乐场景下，歌词相似度 `lyric_similarity` 可返回 0。
7. 区块链存证不建议直接存大文件，只存文件哈希和关键元数据哈希。

## 12. 最小联调要求

三组模块最小联调需要满足：

```text
1. 生成音乐后，拿到 version_id。
2. 用该 version_id 调用合规检测接口，能返回 risk_level。
3. 用该 version_id 调用版权存证接口，能返回 certificate_hash。
4. 前端能展示 WAV 播放、合规结果、存证记录。
```

