import { FileAudio, FileMusic } from 'lucide-react'

interface DownloadButtonsProps {
  midiUrl: string
  audioUrl: string
  mock?: boolean
}

export default function DownloadButtons({ midiUrl, audioUrl, mock }: DownloadButtonsProps) {
  if (mock) {
    return (
      <div className="flex flex-wrap gap-3">
        <span className="px-5 py-3 bg-muted rounded-xl text-sm text-muted-foreground flex items-center gap-2">
          <FileMusic className="h-5 w-5" />
          MIDI（Mock 模式不可用）
        </span>
        <span className="px-5 py-3 bg-muted rounded-xl text-sm text-muted-foreground flex items-center gap-2">
          <FileAudio className="h-5 w-5" />
          WAV（Mock 模式不可用）
        </span>
      </div>
    )
  }

  return (
    <div className="flex flex-wrap gap-3">
      <a
        href={midiUrl}
        download
        className="flex items-center gap-2 px-5 py-3 bg-muted hover:bg-muted/80 rounded-xl font-medium transition-all duration-200 text-foreground border border-border"
      >
        <FileMusic className="h-5 w-5 text-muted-foreground" />
        <span>下载 MIDI</span>
      </a>

      <a
        href={audioUrl}
        download
        className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-primary to-secondary hover:from-secondary hover:to-primary rounded-xl font-medium transition-all duration-200 text-on-primary shadow-lg shadow-primary/20"
      >
        <FileAudio className="h-5 w-5" />
        <span>下载 WAV</span>
      </a>
    </div>
  )
}
