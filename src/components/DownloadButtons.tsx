import { Download, Play, FileAudio, FileMusic } from 'lucide-react'

interface DownloadButtonsProps {
  midiUrl: string
  audioUrl: string
}

export default function DownloadButtons({ midiUrl, audioUrl }: DownloadButtonsProps) {
  const handleDownload = (url: string, filename: string) => {
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="flex flex-wrap gap-3">
      <button
        onClick={() => handleDownload(midiUrl, 'composition.mid')}
        className="flex items-center gap-2 px-5 py-3 bg-muted hover:bg-muted/80 rounded-xl font-medium transition-all duration-200 text-foreground border border-border hover:border-primary/30"
      >
        <FileMusic className="h-5 w-5 text-muted-foreground" />
        <span>下载 MIDI</span>
      </button>

      <button
        onClick={() => handleDownload(audioUrl, 'composition.wav')}
        className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-primary to-secondary hover:from-secondary hover:to-primary rounded-xl font-medium transition-all duration-200 text-on-primary shadow-lg shadow-primary/20"
      >
        <FileAudio className="h-5 w-5" />
        <span>下载 WAV</span>
      </button>
    </div>
  )
}
