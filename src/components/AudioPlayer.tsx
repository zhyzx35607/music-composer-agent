import { useState } from 'react'
import { Play, Pause, Volume2, SkipBack, SkipForward, History as HistoryIcon } from 'lucide-react'

interface AudioPlayerProps {
  audioUrl: string
}

export default function AudioPlayer({ audioUrl }: AudioPlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)

  const handleTimeUpdate = (e: React.ChangeEvent<HTMLAudioElement>) => {
    setCurrentTime(e.target.currentTime)
  }

  const handleLoadedMetadata = (e: React.ChangeEvent<HTMLAudioElement>) => {
    setDuration(e.target.duration)
  }

  const formatTime = (time: number) => {
    const minutes = Math.floor(time / 60)
    const seconds = Math.floor(time % 60)
    return `${minutes}:${seconds.toString().padStart(2, '0')}`
  }

  const togglePlay = () => {
    const audio = document.querySelector('audio')
    if (audio) {
      if (isPlaying) {
        audio.pause()
      } else {
        audio.play()
      }
      setIsPlaying(!isPlaying)
    }
  }

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const audio = document.querySelector('audio')
    if (audio) {
      audio.currentTime = parseFloat(e.target.value)
      setCurrentTime(parseFloat(e.target.value))
    }
  }

  return (
    <div className="bg-background rounded-2xl p-5 border border-border shadow-lg">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold text-foreground flex items-center gap-2">
            <Play className="h-5 w-5 text-accent" />
            音频播放器
          </h3>
          <p className="text-xs text-muted-foreground">Current Preview</p>
        </div>
        <div className="flex items-center gap-2">
          <button className="p-2 rounded-lg bg-muted hover:bg-muted/80 text-foreground transition-colors">
            <Volume2 className="h-5 w-5" />
          </button>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="mb-4">
        <div className="flex items-center gap-3 mb-2">
          <span className="text-xs font-mono text-muted-foreground w-10 text-right">
            {formatTime(currentTime)}
          </span>
          <input
            type="range"
            min={0}
            max={duration}
            value={currentTime}
            onChange={handleSeek}
            className="flex-1 h-2 bg-muted rounded-lg appearance-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-accent [&::-webkit-slider-thumb]:transition-all [&::-webkit-slider-thumb]:hover:scale-125"
          />
          <span className="text-xs font-mono text-muted-foreground w-10">
            {formatTime(duration)}
          </span>
        </div>
        <div className="h-1.5 bg-muted/30 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-primary to-accent transition-all duration-100 ease-out"
            style={{ width: `${(currentTime / duration) * 100}%` }}
          />
        </div>
      </div>

      {/* Controls */}
      <div className="flex items-center justify-center gap-6">
        <button className="p-3 rounded-full bg-muted hover:bg-muted/80 text-foreground transition-all hover:scale-110">
          <SkipBack className="h-6 w-6" />
        </button>

        <button
          onClick={togglePlay}
          className="p-5 rounded-full bg-gradient-to-r from-primary to-secondary hover:from-secondary hover:to-primary text-on-primary shadow-lg shadow-primary/30 hover:scale-110 transition-all duration-200"
        >
          {isPlaying ? (
            <Pause className="h-8 w-8" />
          ) : (
            <Play className="h-8 w-8 fill-current" />
          )}
        </button>

        <button className="p-3 rounded-full bg-muted hover:bg-muted/80 text-foreground transition-all hover:scale-110">
          <SkipForward className="h-6 w-6" />
        </button>
      </div>

      <audio
        src={audioUrl}
        className="hidden"
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={() => setIsPlaying(false)}
      />
    </div>
  )
}
