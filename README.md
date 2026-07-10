# 音乐创作智能体 - 前端

基于 React + TypeScript + Tailwind CSS 的音乐创作平台前端。

## 技术栈

- **框架**: React 19
- **构建工具**: Vite 8
- **类型系统**: TypeScript
- **样式**: Tailwind CSS
- **HTTP 客户端**: Axios

## 开发

```bash
cd frontend
npm run dev
```

前端服务将在 `http://localhost:5173` 启动。

## 与后端对接

后端 API 地址: `http://localhost:8080`

已配置代理：
- `/api/*` → `http://localhost:8080/api/*`
- `/outputs/*` → `http://localhost:8080/outputs/*`

## 项目结构

```
frontend/
├── src/
│   ├── components/       # UI 组件
│   │   ├── Header.tsx
│   │   ├── RequirementInput.tsx
│   │   ├── ParameterSelectors.tsx
│   │   ├── GenerateButton.tsx
│   │   ├── CompositionPlan.tsx
│   │   ├── CaptionDisplay.tsx
│   │   ├── AudioPlayer.tsx
│   │   ├── DownloadButtons.tsx
│   │   ├── HistoryList.tsx
│   │   └── FeedbackInput.tsx
│   ├── types/           # TypeScript 类型定义
│   │   └── api.ts
│   ├── services/        # API 服务层
│   ├── hooks/           # 自定义 Hook
│   ├── App.tsx          # 根组件
│   ├── main.tsx         # 入口
│   └── index.css        # 全局样式
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── package.json
```

## 功能列表

- [x] 中文需求输入
- [x] 音乐参数选择（风格、情绪、速度、乐器）
- [x] 音乐生成按钮
- [x] 作曲方案展示
- [x] 英文 caption 展示
- [x] 音频播放器
- [x] MIDI/WAV 下载
- [x] 历史版本列表
- [x] 反馈修改输入

## 开发进度

当前阶段：第一阶段（中期检查版）

- [x] 基础框架搭建
- [x] 页面组件开发
- [ ] 对接后端 API
- [ ] 完整链路测试
