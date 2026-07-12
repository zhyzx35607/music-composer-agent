# 音乐创作智能体平台

用户输入中文需求 → AI 自动生成作曲方案、MIDI 和 WAV 音频。

```
用户 ──▶ 前端 (React :3000) ──▶ 后端 (Spring :8080) ──▶ 智能体管线 (Python)
                                                              │
                                                    GPT API → music_json
                                                              │
                                                         校验 + MIDI + WAV
                                                              │
                                              /outputs/v1.wav ◀── 前端播放
```

## 项目结构

```
music-composer-agent/
│
├── frontend/                        # React 19 + TypeScript + Tailwind CSS
│   ├── src/
│   │   ├── components/              # 组件（播放器、参数选择、下载…）
│   │   ├── pages/                   # 创作 / 历史 / 合规检测 / 版权存证
│   │   ├── hooks/                   # useMusicGeneration / useHistorySelector
│   │   ├── lib/                     # API 层、错误处理、Mock 降级
│   │   └── types/                   # TypeScript 类型
│   └── vite.config.ts               # 代理 /api、/outputs → :8080
│
├── backend/                         # Java 22 + Spring Boot 3.3 + SQLite
│   ├── src/main/java/com/musicplatform/
│   │   ├── controller/              # 10 个 REST 接口
│   │   ├── service/MusicService     # 调用 Python 管线 / Mock 降级
│   │   ├── model/                   # JPA 实体
│   │   └── dto/                     # 请求 / 响应 DTO
│   └── API.md                       # 完整接口文档
│
├── agent/                           # AI 智能体管线（Python）
│   └── gpt_music_pipeline/
│       ├── run_music_pipeline.py    # 总控入口（一键 GPT → WAV）
│       ├── gpt_music_client.py      # OpenAI 兼容 API 客户端
│       ├── prompt_builder.py        # 中文需求 → GPT prompt
│       ├── validate_music_json.py   # JSON Schema 校验
│       ├── music_json_to_midi.py    # JSON → MIDI
│       ├── render_wav.py            # MIDI → WAV（FluidSynth）
│       ├── schemas/                 # music_json 标准格式
│       └── outputs/                 # 产物：.json / .mid / .wav
│
└── README.md
```

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | React 19 · TypeScript 6.0 · Vite 8 · Tailwind CSS 4 · Axios |
| 后端 | Java 22 · Spring Boot 3.3 · JPA · SQLite · Swagger |
| 智能体 | Python · GPT API · music_json Schema · MIDI · FluidSynth |

## 工作流程

```
1. 用户输入中文需求 + 选择风格 / 情绪 / 速度 / 乐器
2. 前端 POST /api/generate
3. 后端调用 agent/gpt_music_pipeline/run_music_pipeline.py
4. 管线：prompt 优化 → GPT API → music_json → 校验 → MIDI → WAV
5. 后端解析 manifest JSON，存入 SQLite，返回 plan + caption + 文件 URL
6. 前端展示作曲方案、播放 /outputs/v1.wav、支持 MIDI/WAV 下载
```

> **Mock 降级**：当 Python 或 API Key 不可用时，后端自动切换 Mock 模式，返回示例数据保证前后端链路可用。

## 快速启动

**前端**

```bash
cd frontend
npm install
npm run dev          # http://localhost:3000
```

**后端**

```bash
cd backend
set JAVA_HOME=D:\Program Files\Java\jdk-22
set MUSIC_API_KEY=你的key
set MUSIC_API_BASE_URL=你的API地址
mvn spring-boot:run  # http://localhost:8080
```

> 不设置 API Key 也可启动，后端自动降级为 Mock 模式。

## API 接口

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/generate` | 首次生成音乐 |
| 2 | `POST` | `/api/revise` | 反馈修改 |
| 3 | `GET` | `/api/versions` | 版本列表（分页） |
| 4 | `GET` | `/api/version/{id}` | 版本详情 |
| 5 | `GET` | `/api/health` | 健康检查 |
| 6 | `POST` | `/api/compliance/check` | 合规检测 |
| 7 | `GET` | `/api/compliance/history` | 检测历史 |
| 8 | `POST` | `/api/copyright/register` | 版权存证 |
| 9 | `GET` | `/api/copyright/records` | 存证记录列表 |
| 10 | `GET` | `/api/copyright/record/{id}` | 存证记录详情 |

> 详见 [API 接口文档](backend/API.md) 和 Swagger UI (`/swagger-ui.html`)。

## 开发状态

- ✅ 前端 11 个功能页面 & 组件
- ✅ 后端 10 个 API · JPA · 校验 · 异常处理
- ✅ 前后端全字段对齐、编译通过
- ✅ 智能体管线集成（GPT → JSON → MIDI → WAV）
- ✅ 管线不可用时自动 Mock 降级
- ✅ 完整链路联调（10 个接口实测通过，Mock 降级可用）
