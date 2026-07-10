import { useState } from 'react'
import { MessageSquare, Send } from 'lucide-react'

interface FeedbackInputProps {
  onSubmit: () => void
  disabled: boolean
}

export default function FeedbackInput({ onSubmit, disabled }: FeedbackInputProps) {
  const [feedback, setFeedback] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (feedback.trim()) {
      onSubmit()
      setFeedback('')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div className="relative">
        <div className="absolute left-3 top-3 text-muted-foreground">
          <MessageSquare className="h-5 w-5" />
        </div>
        <textarea
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
          disabled={disabled}
          placeholder="输入修改意见，例如：更欢快一点，鼓少一点..."
          className="w-full bg-muted/50 border border-border px-4 py-3 pl-10 pr-24 rounded-xl focus:ring-2 focus:ring-accent focus:border-transparent focus:bg-background transition-all resize-none disabled:opacity-50 disabled:cursor-not-allowed text-foreground placeholder:text-muted-foreground/70"
          rows={3}
        />
        <button
          type="submit"
          disabled={!feedback.trim() || disabled}
          className="absolute bottom-3 right-3 px-4 py-2 bg-accent hover:bg-accent/90 disabled:bg-muted disabled:text-muted-foreground disabled:cursor-not-allowed rounded-lg font-medium transition-all duration-200 flex items-center gap-2 shadow-lg shadow-accent/20"
        >
          <Send className="h-4 w-4" />
          <span>提交修改</span>
        </button>
      </div>
      <p className="text-xs text-muted-foreground text-right">
        提交后将基于当前版本生成新版本
      </p>
    </form>
  )
}
