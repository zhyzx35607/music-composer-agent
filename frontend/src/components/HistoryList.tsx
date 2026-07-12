import { History as HistoryIcon, Play } from 'lucide-react'
import type { HistoryItem } from '../types/api'

interface HistoryListProps {
  versions: HistoryItem[]
  onSelect: (version: HistoryItem) => void
  activeVersionId: string | null
}

export default function HistoryList({ versions, onSelect, activeVersionId }: HistoryListProps) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-lg bg-muted flex items-center justify-center">
            <HistoryIcon className="h-4 w-4 text-muted-foreground" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">历史版本</h3>
        </div>
        <span className="text-xs text-muted-foreground bg-muted px-2 py-1 rounded-full">
          {versions.length} versions
        </span>
      </div>

      <div className="space-y-2">
        {versions.map((version) => (
          <button
            key={version.version_id}
            onClick={() => onSelect(version)}
            className={`w-full text-left p-4 rounded-xl transition-all duration-200 border ${
              activeVersionId === version.version_id
                ? 'bg-primary/10 border-primary shadow-md shadow-primary/10'
                : 'bg-background hover:bg-muted/50 border-border hover:border-primary/30'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`h-10 w-10 rounded-lg flex items-center justify-center ${activeVersionId === version.version_id ? 'bg-primary text-on-primary' : 'bg-muted text-muted-foreground'}`}>
                  <Play className="h-5 w-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className={`font-semibold ${activeVersionId === version.version_id ? 'text-primary' : 'text-foreground'}`}>
                      {version.version_id}
                    </span>
                    {version.parent_version_id && (
                      <span className="text-xs text-muted-foreground bg-muted px-1.5 py-0.5 rounded">
                        from {version.parent_version_id}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground truncate max-w-[200px]">
                    {version.caption_preview || version.caption || '无描述'}
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-xs text-muted-foreground">
                  {new Date(version.created_at).toLocaleDateString('zh-CN')}
                </p>
              </div>
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
