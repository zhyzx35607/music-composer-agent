import { MessageSquare, Music, Type } from 'lucide-react'

interface RequirementInputProps {
  value: string
  onChange: (value: string) => void
  onSubmit?: () => void
  placeholder?: string
  disabled?: boolean
}

export default function RequirementInput({
  value,
  onChange,
  onSubmit,
  placeholder = '输入你的音乐创意，例如：一首欢快的流行歌曲，适合夏日海滩...',
  disabled = false
}: RequirementInputProps) {
  const characterCount = value.length
  const maxCharacters = 2000
  const isOverLimit = characterCount > maxCharacters

  const handleKeyDown = (e: React.KeyboardEvent) => {
    // Ctrl+Enter 或 Cmd+Enter 触发提交
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      if (onSubmit && !disabled && value.trim()) {
        onSubmit()
      }
    }
  }

  return (
    <div className="w-full space-y-4">
      {/* 标题区域 */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <label className="text-lg font-semibold text-foreground flex items-center gap-2">
            <div className="p-2 bg-indigo-100 rounded-lg">
              <MessageSquare className="h-5 w-5 text-indigo-600" />
            </div>
            你的音乐创意
          </label>
          <span
            className={`text-xs font-medium px-3 py-1 rounded-full transition-colors ${
              isOverLimit
                ? 'bg-red-100 text-red-600'
                : characterCount > 1500
                  ? 'bg-amber-100 text-amber-600'
                  : 'bg-muted text-muted-foreground'
            }`}
          >
            {characterCount}/{maxCharacters}
          </span>
        </div>
        <p className="text-sm text-muted-foreground ml-1">
          描述越详细，生成的音乐越接近你的想象
        </p>
      </div>

      {/* 输入区域 */}
      <div className="relative group">
        <div className="absolute left-4 top-4 text-muted-foreground/60 transition-colors group-focus-within:text-indigo-500">
          <Type className="h-5 w-5" />
        </div>
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          className={`w-full bg-white border-2 rounded-2xl px-12 py-5 text-base leading-relaxed transition-all duration-200 placeholder:text-muted-foreground/50 resize-y min-h-[160px] focus:ring-4 focus:ring-indigo-500/10 disabled:opacity-50 disabled:cursor-not-allowed ${
            isOverLimit
              ? 'border-red-300 focus:border-red-500 focus:ring-red-500/10'
              : 'border-border bg-white hover:border-indigo-200 focus:border-indigo-500 focus:ring-indigo-500/10'
          }`}
          maxLength={maxCharacters}
        />
        <div className="absolute right-4 bottom-4 text-xs font-medium text-muted-foreground/40">
          <span className="inline-flex items-center gap-1">
            <kbd className="font-sans px-1.5 py-0.5 rounded bg-muted text-muted-foreground text-[10px]">Enter</kbd>
            换行
          </span>
          <span className="mx-2 text-muted-foreground/30">|</span>
          <span className="inline-flex items-center gap-1">
            <kbd className="font-sans px-1.5 py-0.5 rounded bg-muted text-muted-foreground text-[10px]">Ctrl</kbd>
            <kbd className="font-sans px-1.5 py-0.5 rounded bg-muted text-muted-foreground text-[10px]">Enter</kbd>
            提交
          </span>
        </div>
      </div>

      {/* 快速提示 */}
      <div className="bg-indigo-50 rounded-xl p-4 border border-indigo-100/50">
        <div className="flex items-start gap-3">
          <div className="mt-1 p-1 bg-indigo-100 rounded-md">
            <Music className="h-4 w-4 text-indigo-600" />
          </div>
          <div className="space-y-1.5">
            <p className="text-sm font-semibold text-indigo-900">创作建议：</p>
            <div className="flex flex-wrap gap-2">
              {['音乐风格', '情绪氛围', '速度节奏', '主要乐器'].map((item, i) => (
                <span
                  key={i}
                  className="inline-flex items-center px-3 py-1.5 text-xs font-medium bg-white text-indigo-700 rounded-lg border border-indigo-200/50 shadow-sm"
                >
                  {item}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
