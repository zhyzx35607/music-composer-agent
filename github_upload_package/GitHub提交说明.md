# GitHub 提交说明

## 1. 当前情况

当前目录：

```text
F:\music
```

不是一个有效 Git 仓库。

前端目录：

```text
F:\music\music-composer-agent-main\music-composer-agent-main
```

也是 GitHub zip 解压版，不是 `git clone` 下来的仓库。

所以不能直接 `git push`，需要先建立 Git 仓库或重新 clone 原仓库。

## 2. 不要上传的大文件

不要上传这些：

```text
F:\music\.venv
F:\music\soundfonts\FluidR3_GM.sf2
F:\music\tools\fluidsynth
F:\music\gpt_music_pipeline\outputs
node_modules
*.wav
*.mid
```

原因：

- `FluidR3_GM.sf2` 超过 GitHub 单文件限制。
- WAV/MIDI 是生成产物。
- `.venv` 和 `node_modules` 是本地依赖。
- FluidSynth 可以让后端本地安装或通过压缩包/网盘发。

## 3. 推荐提交到新分支的内容

建议提交这些源码和文档：

```text
gpt_music_pipeline/
API接口对接标准文档.md
AI音乐生成系统流程与分工文档.md
后端对接_音乐生成调用转换模块说明.md
给后端的音乐生成转换模块_最小接入说明.md
音乐生成模块_提交前检查清单.md
GitHub提交说明.md
.gitignore
```

其中 `gpt_music_pipeline/outputs` 不提交生成文件，只保留 `.gitkeep`。

## 4. 推荐方式：clone 原仓库后新建分支

打开 PowerShell：

```powershell
cd F:\music
git clone https://github.com/zhyzx35607/music-composer-agent.git music-composer-agent-submit
cd .\music-composer-agent-submit
git checkout -b music-generation-pipeline
```

然后把这些内容复制进新 clone 的仓库：

```text
F:\music\gpt_music_pipeline
F:\music\API接口对接标准文档.md
F:\music\AI音乐生成系统流程与分工文档.md
F:\music\后端对接_音乐生成调用转换模块说明.md
F:\music\给后端的音乐生成转换模块_最小接入说明.md
F:\music\音乐生成模块_提交前检查清单.md
F:\music\GitHub提交说明.md
F:\music\.gitignore
```

提交：

```powershell
git status
git add .
git commit -m "Add GPT music generation pipeline"
git push -u origin music-generation-pipeline
```

## 5. 如果只是发给后端同学

GitHub 只放源码和文档。

大文件单独发：

```text
F:\music\soundfonts\FluidR3_GM.sf2
F:\music\tools\fluidsynth
```

后端同学放到相同路径或按文档修改路径。

## 6. 后端接入重点

后端只需要调用：

```text
gpt_music_pipeline/run_music_pipeline.py
```

然后返回 manifest 里的：

```text
urls.wav
```

给前端播放。
