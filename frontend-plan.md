# 前端实现计划 - 音乐创作智能体

## Context

这是一个音乐创作智能体项目的前端部分。用户输入中文音乐需求，后端智能体生成作曲方案和英文 caption，再调用 Text2MIDI 生成 MIDI，FluidSynth 转 WAV，前端负责展示整个流程的结果并提供音频播放、下载、历史版本和反馈修改功能。

项目目前为空仓库，需要从零搭建前端。技术栈已确定：**React + Vite + TypeScript + Tailwind CSS**。

---

## 第一步：项目初始化

```bash
# 创建 Vite + React + TypeScript 项目
npm create vite@latest . -- --template react-ts

# 安装依赖
npm install

# 安装额外依赖
npm install axios
npm install -D tailwindcss postcss autoprefixer

# 初始化 Tailwind
npx tailwindcss init -p
```

## 第二步：配置文件

### `tailwind.config.js`
- content 路径设为 `["./index.html", "./src/**/*.{js,ts,jsx,tsx}"]`
- 扩展颜色主题，定义音乐暗色主题色板（bg/surface/primary/accent/text 系列）
- 添加渐变和字体配置

### `src/index.css`
- 添加 `@tailwind base/components/utilities` 指令
- 自定义滚动条样式
- 乐器 chip 选中态 glow 效果

### `.env.development`
- `VITE_API_BASE_URL=http://localhost:8000`

## 第三步：创建文件（共 14 个源文件）

```
src/
├── main.tsx                    # 入口
├── App.tsx                     # 根组件 + 全局状态
├── index.css                   # Tailwind + 自定义样式
├── types/
│   └── api.ts                  # TypeScript 类型定义
├── services/
│   └── api.ts                  # axios 实例 + 4 个 API 函数
├── hooks/
│   └── useMusicGeneration.ts   # 封装生成/修订逻辑的自定义 Hook
└── components/
    ├── Header.tsx              # 标题栏
    ├── RequirementInput.tsx    # 中文需求 textarea
    ├── ParameterSelectors.tsx  # 风格/情绪/速度/乐器选择器
    ├── GenerateButton.tsx      # 生成按钮
    ├── LoadingIndicator.tsx    # 加载动画
    ├── CompositionPlan.tsx     # 作曲方案卡片
    ├── CaptionDisplay.tsx      # 英文 caption 展示
    ├── AudioPlayer.tsx         # HTML5 音频播放器
    ├── DownloadButtons.tsx     # MIDI/WAV 下载
    ├── HistoryList.tsx         # 历史版本列表
    └── FeedbackInput.tsx       # 反馈修改输入框
```

## 第四步：组件详细说明

| 组件 | Props | 说明 |
|---|---|---|
| `Header` | 无 | 居中标题"音乐创作智能体" + 音符图标 |
| `RequirementInput` | `value`, `onChange` | 受控 textarea，placeholder 示例文本 |
| `ParameterSelectors` | `style`, `mood`, `tempo`, `instruments` + 各自 onChange | 三个 select 下拉 + 乐器 chip 多选 |
| `GenerateButton` | `onClick`, `disabled` | 渐变紫色大按钮，带音符图标 |
| `LoadingIndicator` | 无（或 `message` prop） | Spinner + "正在生成音乐，请稍候..." |
| `CompositionPlan` | `plan: Record<string, unknown>` | 卡片展示 theme/style/mood/tempo/key/instruments/structure |
| `CaptionDisplay` | `caption: string` | 英文 caption 展示，半透明深色背景 |
| `AudioPlayer` | `audioUrl: string` | HTML5 `<audio controls>` 元素 |
| `DownloadButtons` | `midiUrl`, `audioUrl` | 两个下载按钮 |
| `HistoryList` | `versions`, `onSelect?`, `activeVersionId?` | 版本列表，P0 只做展示 |
| `FeedbackInput` | `onSubmit`, `disabled` | textarea + 提交按钮 |

## 第五步：App.tsx 状态管理

使用 React `useState`，不引入外部状态库。维护：
- 输入状态（userPrompt, style, mood, tempo, instruments）
- 生成状态（isLoading, error）
- 当前版本结果（version_id, caption, midi_url, audio_url, plan）
- 历史版本数组
- 反馈内容

## 第六步：实现顺序

| 天数 | 内容 | 可交付状态 |
|---|---|---|
| 第 1 天 | 项目初始化 + Tailwind 配置 + types + api 层 + Header + RequirementInput + ParameterSelectors + App.tsx 组装 | 页面可打开，能输入需求和选择参数 |
| 第 2 天 | GenerateButton + LoadingIndicator + useMusicGeneration hook + CompositionPlan + CaptionDisplay + AudioPlayer + DownloadButtons | 点击生成能看到 loading 和 mock 结果展示 |
| 第 3 天 | HistoryList + FeedbackInput + reviseMusic API + 完整 App.tsx 布局组装 | 完整链路可演示（mock 数据） |
| 第 4 天 | 对接真实后端 + 错误处理优化 + loading 阶梯提示 + 与成员 B/C 联调 | 中期检查可用版本 |
| 第 5-8 天 | 历史版本切换 + 反馈修改对接 + 修改原因展示 + 页面美化 + 微动效 | 最终完善版 |

## 关键注意事项

1. **后端地址通过环境变量配置**，不要硬编码
2. **axios timeout 设为 120 秒**（生成链路可能较慢）
3. **乐器多选用 chip/tag 按钮**，不是原生 `<select multiple>`
4. **Mock 策略**：第 1-2 天后端未完成时，useMusicGeneration.ts 支持 mock 模式
5. **TypeScript 严格模式保持开启**
6. **空状态用 `display: none`**，避免页面布局跳动
7. **CORS**：开发时后端需允许 `http://localhost:5173`
