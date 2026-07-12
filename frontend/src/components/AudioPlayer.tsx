import { Music } from 'lucide-react'

interface AudioPlayerProps {
  audioUrl: string
  mock?: boolean
}

export default function AudioPlayer({ audioUrl, mock }: AudioPlayerProps) {
  if (mock) {
    return (
      <div className="bg-card rounded-2xl p-5 border border-border">
        <h3 className="text-lg font-semibold text-foreground flex items-center gap-2 mb-3">
          <Music className="h-5 w-5 text-accent" />
          音频播放器
        </h3>
        <div className="p-4 bg-amber-50 rounded-xl border border-amber-200 text-center">
          <p className="text-sm text-amber-700 font-medium">Mock 模式 · 无音频文件</p>
          <p className="text-xs text-amber-500 mt-1">启动完整管线后可播放真实音频</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-card rounded-2xl p-5 border border-border">
      <h3 className="text-lg font-semibold text-foreground flex items-center gap-2 mb-3">
        <Music className="h-5 w-5 text-accent" />
        音频播放器
      </h3>
      <audio src={audioUrl} controls className="w-full" />
    </div>
  )
}
