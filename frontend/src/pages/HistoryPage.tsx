import { useState } from 'react'
import { History, Play, X } from 'lucide-react'
import { useAPI } from '../lib/apiContext'
import type { HistoryItem } from '../types/api'

interface HistoryPageProps {
  onClose: () => void
}

export default function HistoryPage({ onClose }: HistoryPageProps) {
  const { history, setCurrentVersion } = useAPI()
  const [activeVersionId, setActiveVersionId] = useState<string | null>(null)

  if (history.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <div className="text-center space-y-6 animate-in fade-in duration-500">
          <div className="inline-flex items-center justify-center h-24 w-24 rounded-3xl bg-gradient-to-br from-purple-100 to-pink-100 mb-6">
            <History className="h-12 w-12 text-purple-500" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-foreground mb-3">暂无历史记录</h1>
            <p className="text-muted-foreground max-w-md mx-auto">
              你还没有创作任何音乐作品。开始创建你的第一个音乐项目吧！
            </p>
          </div>
          <button
            onClick={onClose}
            className="px-8 py-3.5 bg-gradient-to-r from-primary to-accent text-on-primary rounded-xl font-semibold shadow-lg shadow-primary/20 hover:shadow-primary/30 transition-all hover:scale-105 active:scale-95"
          >
            开始创作
          </button>
        </div>
      </div>
    )
  }

  const handleSelect = (v: HistoryItem) => {
    if (v.plan && v.midi_url && v.audio_url && v.caption) {
      setCurrentVersion({
        version_id: v.version_id,
        caption: v.caption,
        midi_url: v.midi_url,
        audio_url: v.audio_url,
        plan: v.plan
      })
    }
    setActiveVersionId(v.version_id)
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* 头部 */}
      <div className="flex items-center justify-between mb-10 animate-in fade-in slide-in-from-bottom-4 duration-500">
        <div>
          <h1 className="text-3xl font-bold text-foreground mb-2">历史版本</h1>
          <p className="text-muted-foreground">
            查看和管理你的创作历史
          </p>
        </div>
        <button
          onClick={onClose}
          className="p-3 rounded-xl hover:bg-muted text-muted-foreground hover:text-primary transition-all focus-visible:outline-2 focus-visible:outline-primary"
          aria-label="返回"
        >
          <X className="h-6 w-6" />
        </button>
      </div>

      {/* 版本列表 */}
      <div className="space-y-4">
        {history.map((v) => (
          <div
            key={v.version_id}
            onClick={() => handleSelect(v)}
            className={`group bg-card rounded-2xl p-6 border transition-all duration-300 hover:shadow-xl hover:shadow-purple-500/10 hover:border-purple-200 cursor-pointer ${
              activeVersionId === v.version_id
                ? 'border-primary ring-1 ring-primary/20 bg-gradient-to-br from-primary/5 to-transparent'
                : 'border-border hover:border-primary/30'
            }`}
          >
            <div className="flex items-start justify-between">
              <div className="flex items-start gap-5">
                <div
                  className={`h-14 w-14 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                    activeVersionId === v.version_id
                      ? 'bg-gradient-to-br from-primary to-purple-600 text-on-primary shadow-lg shadow-primary/30'
                      : 'bg-muted text-muted-foreground group-hover:bg-primary group-hover:text-on-primary'
                  }`}
                >
                  <Play className="h-7 w-7" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-3">
                    <span className={`font-semibold text-xl ${activeVersionId === v.version_id ? 'text-primary' : 'text-foreground'}`}>
                      {v.version_id}
                    </span>
                    {v.parent_version_id && (
                      <span className="text-xs px-2.5 py-1 rounded-full bg-purple-50 text-purple-600 font-medium">
                        from {v.parent_version_id}
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-muted-foreground line-clamp-2 mb-4 min-h-[2.5em]">
                    {v.caption}
                  </p>
                  <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                    <div className="flex items-center gap-1.5">
                      <div className="h-2 w-2 rounded-full bg-primary" />
                      <span>风格: {v.style || '—'}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <div className="h-2 w-2 rounded-full bg-accent" />
                      <span>情绪: {v.mood || '—'}</span>
                    </div>
                    <span className="opacity-70">•</span>
                    <span>
                      {new Date(v.created_at).toLocaleString('zh-CN')}
                    </span>
                  </div>
                </div>
              </div>
              <div className="text-right">
                <span className="text-xs text-muted-foreground bg-muted px-3 py-1.5 rounded-lg">
                  {new Date(v.created_at).toLocaleDateString('zh-CN')}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 底部返回 */}
      <div className="mt-12 text-center animate-in fade-in duration-500 delay-200">
        <button
          onClick={onClose}
          className="px-8 py-3.5 text-muted-foreground hover:text-primary hover:bg-purple-50 rounded-xl font-medium transition-all active:scale-95"
        >
          返回创作页面
        </button>
      </div>
    </div>
  )
}
