interface LoadingSpinnerProps {
  message?: string
  subtitle?: string
}

export default function LoadingSpinner({ message = '加载中...', subtitle = '正在准备创作空间' }: LoadingSpinnerProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 animate-in fade-in duration-300">
      <div className="relative h-16 w-16 mb-6">
        <div className="absolute inset-0 rounded-full border-4 border-primary/20" />
        <div className="absolute inset-0 rounded-full border-4 border-t-primary border-r-transparent border-b-transparent animate-spin" />
        <div className="absolute inset-2 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center shadow-lg shadow-purple-500/30">
          <div className="h-2 w-2 rounded-full bg-white/20 backdrop-blur-sm" />
        </div>
      </div>
      <p className="text-lg font-medium text-foreground animate-pulse">{message}</p>
      <p className="text-sm text-muted-foreground mt-2">{subtitle}</p>
    </div>
  )
}
