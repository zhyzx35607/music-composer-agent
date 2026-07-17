import { useState, useEffect } from 'react'
import { useAPI } from '../lib/apiContext'
import { FileMusic, History, Upload, X } from 'lucide-react'
import { api } from '../lib/api'
import type { HistoryItem, UploadedReferenceFile } from '../types/api'
import { useToast } from '../components/useToast'
import RequirementInput from '../components/RequirementInput'
import ParameterSelectors from '../components/ParameterSelectors'
import GenerateButton from '../components/GenerateButton'
import CompositionPlan from '../components/CompositionPlan'
import CaptionDisplay from '../components/CaptionDisplay'
import AudioPlayer from '../components/AudioPlayer'
import DownloadButtons from '../components/DownloadButtons'
import FeedbackInput from '../components/FeedbackInput'
import LoadingSpinner from '../components/LoadingSpinner'
import { useHistorySelector } from '../hooks/useHistorySelector'

// 反馈区域
function FeedbackSection({
  onRevise,
  disabled,
  files,
  onUpload,
  onRemove,
}: {
  onRevise: (feedback: string) => void
  disabled: boolean
  files: UploadedReferenceFile[]
  onUpload: (file: File) => void
  onRemove: (fileId: number) => void
}) {
  return (
    <div className="bg-muted/30 rounded-2xl p-6 border border-border space-y-5">
      <h3 className="text-sm font-semibold text-foreground mb-4">反馈修改</h3>
      <ScoreUploadSection
        files={files}
        onUpload={onUpload}
        onRemove={onRemove}
        disabled={disabled}
      />
      <FeedbackInput onSubmit={onRevise} disabled={disabled} />
    </div>
  )
}

function ScoreUploadSection({
  files,
  onUpload,
  onRemove,
  disabled,
}: {
  files: UploadedReferenceFile[]
  onUpload: (file: File) => void
  onRemove: (fileId: number) => void
  disabled: boolean
}) {
  return (
    <div className="space-y-3">
      <label className="flex items-center gap-2 text-sm font-semibold text-foreground">
        <FileMusic className="h-5 w-5 text-primary" />
        <span>乐谱文件</span>
      </label>
      <label className="flex min-h-24 cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-muted/30 px-4 py-5 text-sm text-muted-foreground transition-colors hover:bg-muted/50">
        <Upload className="h-5 w-5" />
        <span>上传 .musicxml / .mxl / .xml / .mid</span>
        <input
          type="file"
          accept=".musicxml,.mxl,.xml,.mid,.midi"
          className="hidden"
          disabled={disabled}
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (file) onUpload(file)
            event.currentTarget.value = ''
          }}
        />
      </label>
      {files.length > 0 && (
        <div className="space-y-2">
          {files.map(file => (
            <div key={file.file_id} className="flex items-center justify-between gap-3 rounded-lg border border-border bg-background px-3 py-2 text-sm">
              <span className="truncate">{file.original_name}</span>
              <button
                type="button"
                onClick={() => onRemove(file.file_id)}
                className="rounded-md p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                aria-label="移除乐谱"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// 历史列表弹窗
function HistoryListModal({ versions, onSelect, onClose, activeVersionId }: { versions: HistoryItem[], onSelect: (v: HistoryItem) => void, onClose: () => void, activeVersionId: string | null }) {
  const [showHistory, setShowHistory] = useState(true)

  const closeAndSelect = (v: HistoryItem) => {
    onSelect(v)
    setShowHistory(false)
  }

  if (!showHistory) return null

  return (
    <div
      className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="历史版本"
    >
      <div
        className="bg-card rounded-2xl w-full max-w-md max-h-[85vh] flex flex-col shadow-2xl animate-in slide-in-from-bottom-4 duration-300"
        onClick={e => e.stopPropagation()}
      >
        <div className="p-5 border-b border-border flex items-center justify-between">
          <h3 className="font-semibold text-foreground text-lg">历史版本</h3>
          <button
            onClick={onClose}
            className="p-2 hover:bg-muted rounded-lg transition-colors focus-visible:outline-2 focus-visible:outline-primary"
            aria-label="关闭"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {versions.map(v => (
            <button
              key={v.version_id}
              onClick={() => closeAndSelect(v)}
              className={`w-full text-left p-4 rounded-xl border transition-all duration-200 hover:shadow-md ${
                activeVersionId === v.version_id
                  ? 'bg-primary/10 border-primary shadow-md'
                  : 'hover:bg-muted/50 border-border'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className={`font-medium ${activeVersionId === v.version_id ? 'text-primary' : 'text-foreground'}`}>
                  {v.version_id}
                </span>
                <span className="text-xs text-muted-foreground">
                  {new Date(v.created_at).toLocaleDateString('zh-CN')}
                </span>
              </div>
              <p className="text-xs text-muted-foreground mt-1 truncate">{v.caption_preview || v.caption}</p>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

// 主创作页面
export default function CreatePage() {
  const [userPrompt, setUserPrompt] = useState('')
  const [style, setStyle] = useState('')
  const [mood, setMood] = useState('')
  const [tempo, setTempo] = useState('')
  const [instruments, setInstruments] = useState<string[]>([])
  const [showHistory, setShowHistory] = useState(false)
  const [referenceFiles, setReferenceFiles] = useState<UploadedReferenceFile[]>([])

  const { success, error: addError } = useToast()
  const { isLoading, currentVersion, history, generate, revise, error } = useAPI()
  const { handleSelect: handleHistorySelect } = useHistorySelector()

  // 显示 API 错误提示 - 使用 useEffect 避免渲染期间状态更新
  useEffect(() => {
    if (error) {
      addError(error)
    }
  }, [error, addError])

  const handleGenerate = async () => {
    if (!userPrompt.trim() && referenceFiles.length === 0) {
      addError('请输入音乐创意，或先上传 MusicXML 乐谱')
      return
    }
    try {
      const result = await generate({
        user_prompt: userPrompt.trim() || '根据上传的 MusicXML 乐谱生成一段纯音乐',
        style,
        mood,
        tempo,
        instruments,
        referenceFileIds: referenceFiles.map(file => file.file_id),
      })
      if (result) success('音乐生成成功！')
    } catch {
      // Error is already handled by useAPI hook
    }
  }

  const handleRevise = async (feedback: string) => {
    if (!currentVersion?.version_id) {
      addError('没有可修改的版本')
      return
    }
    if (!feedback.trim()) {
      addError('请输入修改意见')
      return
    }
    try {
      const result = await revise(
        currentVersion.version_id,
        feedback,
        referenceFiles.map(file => file.file_id),
      )
      if (result) success('修改版本生成成功！')
    } catch {
      // Error is already handled by useAPI hook
    }
  }

  const handleScoreUpload = async (file: File) => {
    try {
      const uploaded = await api.uploadReferenceFile(file, {
        versionId: currentVersion?.version_id,
        trackId: currentVersion?.track_id,
      })
      setReferenceFiles(prev => [...prev, uploaded])
      success('乐谱上传成功')
    } catch {
      addError('乐谱上传失败，请检查文件格式')
    }
  }

  const handleRemoveReferenceFile = (fileId: number) => {
    setReferenceFiles(prev => prev.filter(file => file.file_id !== fileId))
  }

  const handleHistorySelectComplete = (v: HistoryItem) => {
    handleHistorySelect(v)
    setStyle(v.style || '')
    setMood(v.mood || '')
    setTempo(v.tempo || '')
    setInstruments(v.instruments || [])
    setShowHistory(false)
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      {/* 标题 */}
      <div className="text-center space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
        <h1 className="text-4xl md:text-5xl font-bold text-foreground tracking-tight">
          AI 音乐创作
        </h1>
        <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
          将你的音乐灵感变为现实 - 用 AI 生成专业级的音乐作品
        </p>
      </div>

      {/* 输入区域 */}
      <div className="bg-card rounded-3xl p-6 sm:p-8 border border-border shadow-xl shadow-purple-500/10 space-y-8 animate-in fade-in slide-in-from-bottom-6 duration-500 delay-100">
        <RequirementInput value={userPrompt} onChange={setUserPrompt} disabled={isLoading} />
        <ParameterSelectors
          style={style} setStyle={setStyle} mood={mood} setMood={setMood}
          tempo={tempo} setTempo={setTempo} instruments={instruments} setInstruments={setInstruments}
        />
        <ScoreUploadSection
          files={referenceFiles}
          onUpload={handleScoreUpload}
          onRemove={handleRemoveReferenceFile}
          disabled={isLoading}
        />
        <GenerateButton onClick={handleGenerate} disabled={isLoading || (!userPrompt.trim() && referenceFiles.length === 0)} isLoading={isLoading} />
      </div>

      {/* 加载中 */}
      {isLoading && (
        <LoadingSpinner message="正在生成音乐..." subtitle="AI 正在分析你的创意并创作音乐" />
      )}

      {/* 结果区域 */}
      {currentVersion && (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-8 duration-700">
          <CompositionPlan plan={currentVersion.plan} />
          <CaptionDisplay caption={currentVersion.caption} />
          <AudioPlayer audioUrl={currentVersion.audio_url} mock={currentVersion.mock} />
          <div className="flex justify-center">
            <DownloadButtons midiUrl={currentVersion.midi_url} audioUrl={currentVersion.audio_url} mock={currentVersion.mock} />
          </div>
          <FeedbackSection
            onRevise={handleRevise}
            disabled={isLoading}
            files={referenceFiles}
            onUpload={handleScoreUpload}
            onRemove={handleRemoveReferenceFile}
          />
        </div>
      )}

      {/* 历史按钮 */}
      {history.length > 0 && (
        <div className="flex justify-center animate-in fade-in duration-500 delay-300">
          <button
            type="button"
            onClick={() => setShowHistory(true)}
            className="flex items-center gap-2 px-8 py-3.5 bg-muted hover:bg-muted/80 rounded-xl font-medium transition-all shadow-sm hover:shadow-md active:scale-95"
          >
            <History className="h-5 w-5" />
            <span>查看历史版本 ({history.length})</span>
          </button>
        </div>
      )}

      {/* 历史列表弹窗 */}
      {showHistory && (
        <HistoryListModal
          versions={history}
          onSelect={handleHistorySelectComplete}
          onClose={() => setShowHistory(false)}
          activeVersionId={currentVersion?.version_id || null}
        />
      )}
    </div>
  )
}
