import { Music, Heart, Wind, Disc } from 'lucide-react'

interface ParameterSelectorsProps {
  style: string
  setStyle: (value: string) => void
  mood: string
  setMood: (value: string) => void
  tempo: string
  setTempo: (value: string) => void
  instruments: string[]
  setInstruments: (values: string[]) => void
}

const musicStyles = ['pop', 'rock', 'jazz', 'classical', 'electronic', 'ambient', 'hip-hop', 'country']
const moods = ['happy', 'sad', 'calm', 'exciting', 'romantic', 'nostalgic', 'hopeful', 'melancholic']
const tempos = ['slow', 'medium', 'fast']
const availableInstruments = ['piano', 'guitar', 'strings', 'drums', 'bass', 'synth', 'brass', 'woodwind']

export default function ParameterSelectors({
  style,
  setStyle,
  mood,
  setMood,
  tempo,
  setTempo,
  instruments,
  setInstruments,
}: ParameterSelectorsProps) {
  const toggleInstrument = (instrument: string) => {
    if (instruments.includes(instrument)) {
      setInstruments(instruments.filter((i) => i !== instrument))
    } else {
      setInstruments([...instruments, instrument])
    }
  }

  const toggleStyle = (s: string) => {
    if (style === s) {
      setStyle('')
    } else {
      setStyle(s)
    }
  }

  const toggleMood = (m: string) => {
    if (mood === m) {
      setMood('')
    } else {
      setMood(m)
    }
  }

  const toggleTempo = (t: string) => {
    if (tempo === t) {
      setTempo('')
    } else {
      setTempo(t)
    }
  }

  return (
    <div className="bg-muted/50 rounded-2xl p-6 border border-border">
      {/* 风格选择 */}
      <div className="space-y-3 mb-6">
        <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <Music className="h-4 w-4 text-primary" />
          <span>风格</span>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setStyle('')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
              style === ''
                ? 'bg-muted border-2 border-primary text-primary'
                : 'bg-background text-foreground hover:bg-muted/80 hover:border-border border border-transparent'
            }`}
          >
            自动(AI)
          </button>
          {musicStyles.map((s) => (
            <button
              key={s}
              onClick={() => toggleStyle(s)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                style === s
                  ? 'bg-primary text-on-primary shadow-lg shadow-primary/25 scale-105'
                  : 'bg-background text-foreground hover:bg-muted/80 hover:border-border border border-transparent'
              }`}
            >
              {s.charAt(0).toUpperCase() + s.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* 情绪选择 */}
      <div className="space-y-3 mb-6">
        <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <Heart className="h-4 w-4 text-accent" />
          <span>情绪</span>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setMood('')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
              mood === ''
                ? 'bg-muted border-2 border-accent text-accent'
                : 'bg-background text-foreground hover:bg-muted/80 hover:border-border border border-transparent'
            }`}
          >
            自动(AI)
          </button>
          {moods.map((m) => (
            <button
              key={m}
              onClick={() => toggleMood(m)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                mood === m
                  ? 'bg-accent text-on-primary shadow-lg shadow-accent/25 scale-105'
                  : 'bg-background text-foreground hover:bg-muted/80 hover:border-border border border-transparent'
              }`}
            >
              {m.charAt(0).toUpperCase() + m.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* 速度选择 */}
      <div className="space-y-3 mb-6">
        <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <Wind className="h-4 w-4 text-secondary" />
          <span>速度</span>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => setTempo('')}
            className={`flex-1 px-4 py-3 rounded-lg text-sm font-medium transition-all duration-200 ${
              tempo === ''
                ? 'bg-muted border-2 border-secondary text-secondary'
                : 'bg-background text-foreground hover:bg-muted/80 border border-transparent'
            }`}
          >
            自动(AI)
          </button>
          {tempos.map((t) => (
            <button
              key={t}
              onClick={() => toggleTempo(t)}
              className={`flex-1 px-4 py-3 rounded-lg text-sm font-medium transition-all duration-200 ${
                tempo === t
                  ? 'bg-muted border-2 border-primary text-primary'
                  : 'bg-background text-foreground hover:bg-muted/80 border border-transparent'
              }`}
            >
              {t === 'slow' ? '慢速' : t === 'medium' ? '中速' : '快速'}
            </button>
          ))}
        </div>
      </div>

      {/* 乐器选择 */}
      <div className="space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <Disc className="h-4 w-4 text-muted-foreground" />
          <span>乐器</span>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setInstruments([])}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
              instruments.length === 0
                ? 'bg-muted border-2 border-primary text-primary'
                : 'bg-background text-foreground hover:bg-muted/80 hover:border-border border border-transparent'
            }`}
          >
            自动(AI)
          </button>
          {availableInstruments.map((inst) => (
            <button
              key={inst}
              onClick={() => toggleInstrument(inst)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                instruments.includes(inst)
                  ? 'bg-primary/20 text-primary border border-primary/30'
                  : 'bg-background text-foreground hover:bg-muted/80 hover:border-border border border-transparent'
              }`}
            >
              {inst.charAt(0).toUpperCase() + inst.slice(1)}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
