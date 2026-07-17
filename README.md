# 音乐创作智能体平台（二型甲）

这是可运行提交版，只保留程序源码、FluidSynth 运行文件和必要说明，不包含个人报告、数据库、生成音频、缓存、API Key、`node_modules` 或编译目录。

## 1. 功能

- 自然语言生成纯音乐：GPT -> `music_json` -> MIDI -> WAV。
- 上传 MusicXML、MXL、XML、MIDI 作为乐谱输入。
- “保持原曲、转 WAV、变快、变慢”走确定性转换，不让 GPT 重写原曲。
- 复杂改编和反馈修改使用结构化 `revision_plan`。
- 保存父子版本、Prompt、AI 记录、manifest 和 SHA-256 证据。
- 提供合规检测与版权存证联调接口。

## 2. 运行环境

- Windows 10/11
- Java 22
- Node.js 20 或更高版本
- Python 3.10 或更高版本
- PowerShell 5.1 或更高版本
- 可用的 OpenAI-compatible Chat Completions API

Maven Wrapper 和 Windows x64 FluidSynth 已包含在仓库中。

## 3. SoundFont

GitHub 普通文件上限为 100 MB，项目原用的 `FluidR3_GM.sf2` 为 141.5 MB，因此本提交不包含该文件。

运行前请准备任意 General MIDI 兼容 `.sf2`，推荐将其放到：

```text
agent/soundfonts/FluidR3_GM.sf2
```

也可以在启动时指定其他路径：

```powershell
.\start.ps1 -SoundFontPath 'D:\SoundFonts\your-soundfont.sf2'
```

详见 [agent/soundfonts/README.md](agent/soundfonts/README.md)。

## 4. 首次安装

在项目根目录打开 PowerShell：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\setup.ps1
```

脚本会：

1. 检查 Java、Node.js、npm 和 Python。
2. 创建根目录 `.venv`。
3. 安装 Python 依赖。
4. 执行 `npm ci` 安装前端依赖。
5. 检查 FluidSynth 和 SoundFont。

## 5. 配置 API

不要把真实 Key 写进代码或提交到 Git。

```powershell
$env:MUSIC_API_KEY='你的 API Key'
$env:MUSIC_API_BASE_URL='https://api.openai.com/v1'
$env:MUSIC_MODEL='服务商实际支持的模型名'
```

第三方 OpenAI-compatible 服务通常也要求 Base URL 以 `/v1` 结尾。

## 6. 一键启动

```powershell
.\start.ps1
```

脚本会打开两个终端窗口：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:3000`
- 健康检查：`http://localhost:8080/api/health`
- Swagger：`http://localhost:8080/swagger-ui.html`

若不希望打开两个新窗口，可分别运行：

```powershell
.\start-backend.ps1
.\start-frontend.ps1
```

## 7. 验证

```powershell
.\verify.ps1
```

验证内容包括 Python 单元测试、Spring Boot 编译和前端生产构建。SoundFont 缺失只会给出提示，不影响源码编译，但实际 MIDI 转 WAV 时必须提供。

## 8. 输出位置

程序运行后统一输出到：

```text
agent/gpt_music_pipeline/outputs/
```

该目录中的 JSON、MIDI、WAV、Prompt、AI 记录和上传文件均被 Git 忽略。

## 9. 手动启动后端

```powershell
cd backend
$env:MUSIC_PLATFORM_PIPELINE_PYTHON=(Resolve-Path '..\.venv\Scripts\python.exe').Path
$env:MUSIC_PLATFORM_PIPELINE_SOUNDFONT_PATH=(Resolve-Path '..\agent\soundfonts\FluidR3_GM.sf2').Path
.\mvnw.cmd spring-boot:run
```

Spring Boot 启动后停在日志界面是正常现象，不要关闭该终端。

## 10. 注意事项

- 后端进入 `mock=true` 并生成 5 秒占位音频，表示完整管线执行失败，不是 GPT 只生成了 5 秒。
- 合规检测和版权登记当前是联调/Mock 实现，不代表已接入真实版权库或真实区块链。
- 上传 MXL 的兼容性不如 MusicXML 和 MIDI，演示优先使用 `.musicxml`、`.xml`、`.mid`。

