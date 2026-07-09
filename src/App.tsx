import { useState } from 'react'
import { useMusicGeneration } from './hooks/useMusicGeneration'
import Header from './components/Header'
import RequirementInput from './components/RequirementInput'
import ParameterSelectors from './components/ParameterSelectors'
import GenerateButton from './components/GenerateButton'
import CompositionPlan from './components/CompositionPlan'
import CaptionDisplay from './components/CaptionDisplay'
import AudioPlayer from './components/AudioPlayer'
import DownloadButtons from './components/DownloadButtons'
import HistoryList from './components/HistoryList'
import FeedbackInput from './components/FeedbackInput'
import type { HistoryItem } from './types/api'

function App() {
  // 输入状态
  const [userPrompt, setUserPrompt] = useState('')
  const [style, setStyle] = useState('pop')
  const [mood, setMood] = useState('calm')
  const [tempo, setTempo] = useState('medium')
  const [instruments, setInstruments] = useState<string[]>(['piano'])
  const [feedback, setFeedback] = useState('')

  const { isLoading, currentVersion, history, generate } = useMusicGeneration()

  const handleGenerate = () => {
    if (!userPrompt.trim()) return

    generate({
      user_prompt: userPrompt,
      style,
      mood,
      tempo,
      instruments,
    })
  }

  const handleRevise = () => {
    if (!feedback.trim() || !currentVersion) return
    // TODO: 对接 revise API
    alert('反馈修改功能开发中')
    setFeedback('')
  }

  const handleHistorySelect = (version: HistoryItem) => {
    if (version.plan && version.midi_url && version.audio_url) {
      // TODO: 更新 currentVersion 状态
      console.log('Select version:', version.version_id)
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground font-body antialiased">
      <Header />

      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* 输入区域 */}
        <section className="space-y-6">
          <RequirementInput value={userPrompt} onChange={setUserPrompt} />
          <ParameterSelectors
            style={style}
            setStyle={setStyle}
            mood={mood}
            setMood={setMood}
            tempo={tempo}
            setTempo={setTempo}
            instruments={instruments}
            setInstruments={setInstruments}
          />
          <GenerateButton onClick={handleGenerate} disabled={isLoading || !userPrompt.trim()} isLoading={isLoading} />
        </section>

        {/* 加载中提示 */}
        {isLoading && (
          <div className="flex flex-col items-center justify-center py-12 space-y-4">
            <div className="relative w-16 h-16">
              <div className="absolute inset-0 rounded-full border-4 border-primary/20" />
              <div className="absolute inset-0 rounded-full border-4 border-primary border-t-transparent animate-spin" />
            </div>
            <p className="text-lg text-primary font-medium animate-pulse">正在生成音乐，请稍候...</p>
          </div>
        )}

        {/* 结果展示区域 */}
        {currentVersion && (
          <section className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            {/* 作曲方案 */}
            <CompositionPlan plan={currentVersion.plan} />

            {/* 英文描述 */}
            <CaptionDisplay caption={currentVersion.caption} />

            {/* 音频播放器 */}
            <AudioPlayer audioUrl={currentVersion.audio_url} />

            {/* 下载按钮 */}
            <div className="flex justify-center">
              <DownloadButtons
                midiUrl={currentVersion.midi_url}
                audioUrl={currentVersion.audio_url}
              />
            </div>

            {/* 反馈修改 */}
            <div className="bg-muted/30 rounded-2xl p-6 border border-border">
              <h3 className="text-lg font-semibold text-foreground mb-4">反馈修改</h3>
              <FeedbackInput onSubmit={handleRevise} disabled={isLoading} />
            </div>
          </section>
        )}

        {/* 历史版本 */}
        {history.length > 0 && (
          <section className="border-t border-border pt-8">
            <HistoryList
              versions={history}
              onSelect={handleHistorySelect}
              activeVersionId={currentVersion?.version_id || null}
            />
          </section>
        )}
      </main>
    </div>
  )
}

export default App
