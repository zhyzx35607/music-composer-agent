import { AlertTriangle } from 'lucide-react'

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <div className="max-w-md w-full text-center space-y-6 animate-in fade-in duration-500">
        <div className="inline-flex items-center justify-center h-24 w-24 rounded-full bg-gradient-to-br from-amber-100 to-orange-100 mb-6">
          <AlertTriangle className="h-12 w-12 text-amber-500" />
        </div>
        <div>
          <h1 className="text-4xl font-bold text-foreground mb-2">页面不存在</h1>
          <p className="text-muted-foreground">
            抱歉，您访问的页面不存在或已被移除。
          </p>
        </div>
        <div className="flex flex-col gap-3 max-w-xs mx-auto">
          <a
            href="/"
            className="px-6 py-3 bg-gradient-to-r from-primary to-accent text-on-primary rounded-xl font-semibold shadow-lg shadow-primary/20 hover:shadow-primary/30 transition-all"
          >
            返回首页
          </a>
          <a
            href="mailto:support@composer.ai"
            className="px-6 py-3 bg-muted hover:bg-muted/80 text-foreground rounded-xl font-medium transition-all"
          >
            联系支持
          </a>
        </div>
      </div>
    </div>
  )
}
