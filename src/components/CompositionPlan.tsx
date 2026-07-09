import { Music } from 'lucide-react'

interface CompositionPlanProps {
  plan: Record<string, any>
}

export default function CompositionPlan({ plan }: CompositionPlanProps) {
  const rows = [
    { label: '主题', key: 'theme' },
    { label: '风格', key: 'style' },
    { label: '情绪', key: 'mood' },
    { label: '速度', key: 'tempo' },
    { label: '调式', key: 'key' },
    { label: '乐器', key: 'instruments' },
    { label: '结构', key: 'structure' },
  ]

  return (
    <div className="bg-background rounded-2xl p-6 border border-border shadow-lg">
      <div className="flex items-center gap-2 mb-5 pb-4 border-b border-border">
        <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-secondary to-primary flex items-center justify-center shadow-lg shadow-primary/20">
          <Music className="h-6 w-6 text-on-primary" />
        </div>
        <div>
          <h3 className="text-lg font-semibold text-foreground">作曲方案</h3>
          <p className="text-xs text-muted-foreground">AI Generated Composition Plan</p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {rows.map((row) => {
          const value = plan[row.key]
          const displayValue = Array.isArray(value) ? value.join(', ') : String(value || 'N/A')

          return (
            <div key={row.key} className="space-y-1">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{row.label}</p>
              <p className="text-sm font-medium text-foreground leading-relaxed">{displayValue}</p>
            </div>
          )
        })}
      </div>
    </div>
  )
}
