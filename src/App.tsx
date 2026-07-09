import { useState } from 'react'
import { useMusicGeneration } from './hooks/useMusicGeneration'
import Header from './components/Header'
import Hero from './components/Hero'
import Features from './components/Features'
import { Music } from 'lucide-react'
import RequirementInput from './components/RequirementInput'
import ParameterSelectors from './components/ParameterSelectors'
import GenerateButton from './components/GenerateButton'
import CompositionPlan from './components/CompositionPlan'
import CaptionDisplay from './components/CaptionDisplay'
import AudioPlayer from './components/AudioPlayer'
import DownloadButtons from './components/DownloadButtons'
import HistoryList from './components/HistoryList'
import FeedbackInput from './components/FeedbackInput'
import Toast, { useToast } from './components/Toast'
import type { HistoryItem } from './types/api'

function App() {
  // 输入状态
  const [userPrompt, setUserPrompt] = useState('')
  const [style, setStyle] = useState('pop')
  const [mood, setMood] = useState('calm')
  const [tempo, setTempo] = useState('medium')
  const [instruments, setInstruments] = useState<string[]>(['piano'])
  const [feedback, setFeedback] = useState('')
  const [error, setError] = useState<string | null>(null)

  const { toasts, success, error: addError, info } = useToast()
  const { isLoading, currentVersion, history, generate, setCurrentVersion } = useMusicGeneration()

  const handleGenerate = async () => {
    if (!userPrompt.trim()) {
      addError('请输入你的音乐创意')
      return
    }

    setError(null)

    try {
      const result = await generate({
        user_prompt: userPrompt,
        style,
        mood,
        tempo,
        instruments,
      })
      if (result) {
        success('音乐生成成功！')
      }
    } catch (err) {
      addError('生成失败，请重试')
      console.error('Generation error:', err)
    }
  }

  const handleRevise = async () => {
    if (!feedback.trim() || !currentVersion) return

    // TODO: 对接 revise API
    info('反馈修改功能开发中')
    setTimeout(() => {
      setFeedback('')
      setError(null)
    }, 2000)
  }

  const handleHistorySelect = (version: HistoryItem) => {
    if (version.plan && version.midi_url && version.audio_url && version.caption) {
      setCurrentVersion({
        version_id: version.version_id,
        caption: version.caption,
        midi_url: version.midi_url,
        audio_url: version.audio_url,
        plan: version.plan
      })
      // Update input parameters from selected version
      setStyle(version.style)
      setMood(version.mood)
      setTempo(version.tempo || 'medium')
      setInstruments(version.instruments || ['piano'])
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground font-body antialiased">
      <Header />

      {/* 首屏区域 */}
      <Hero />

      {/* 特性区域 */}
      <Features />

      {/* 主应用区域 */}
      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-8" id="create">
        {/* 输入区域 */}
        <section className="space-y-6">
          <RequirementInput
            value={userPrompt}
            onChange={setUserPrompt}
            onSubmit={handleGenerate}
            disabled={isLoading}
          />
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

        {/* 错误提示 */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl flex items-center gap-3">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {error}
          </div>
        )}

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

        {/* Toast 通知 */}
        <Toast toasts={toasts} onRemove={(id) => {
          // Remove toast logic is handled internally by the component's timeout
          // This callback is just for manual removal if needed
        }} />
      </main>

      {/* 页脚 */}
      <footer className="bg-muted/30 border-t border-border py-12 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-indigo-600 to-pink-500 flex items-center justify-center">
                  <Music className="h-4 w-4 text-white" />
                </div>
                <span className="font-heading font-bold text-lg text-foreground">Composer AI</span>
              </div>
              <p className="text-sm text-muted-foreground">
                AI 驱动的音乐创作平台，让每个人都能轻松创作专业级音乐作品。
              </p>
            </div>
            <div>
              <h4 className="font-semibold text-foreground mb-4">产品</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><a href="#features" className="hover:text-indigo-600 transition-colors">特性</a></li>
                <li><a href="#" className="hover:text-indigo-600 transition-colors">价格</a></li>
                <li><a href="#" className="hover:text-indigo-600 transition-colors">API</a></li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-foreground mb-4">支持</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><a href="#" className="hover:text-indigo-600 transition-colors">帮助中心</a></li>
                <li><a href="#" className="hover:text-indigo-600 transition-colors">API 文档</a></li>
                <li><a href="#" className="hover:text-indigo-600 transition-colors">隐私政策</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-border pt-8 text-center text-sm text-muted-foreground">
            <p>© {new Date().getFullYear()} Composer AI. 保留所有权利。</p>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default App
