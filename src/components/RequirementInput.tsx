import { MessageSquare } from 'lucide-react'

interface RequirementInputProps {
  value: string
  onChange: (value: string) => void
}

export default function RequirementInput({ value, onChange }: RequirementInputProps) {
  return (
    <div className="space-y-3">
      <label className="block text-sm font-semibold text-foreground">
        描述你想要的音乐
      </label>
      <div className="relative">
        <div className="absolute left-3 top-3 text-muted-foreground">
          <MessageSquare className="h-5 w-5" />
        </div>
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="例如：我想要一首毕业季的歌，抒情一点，用钢琴和弦乐..."
          className="w-full bg-muted/50 border border-border px-4 py-3 pl-10 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent focus:bg-background transition-all resize-y min-h-[120px] placeholder:text-muted-foreground/70 text-foreground"
        />
        <div className="absolute right-3 top-3 text-muted-foreground/50 text-xs">
          {value.length}/2000
        </div>
      </div>
      <p className="text-xs text-muted-foreground">
        描述越详细，生成的音乐越接近你的想象
      </p>
    </div>
  )
}
