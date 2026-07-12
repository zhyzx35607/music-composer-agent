import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from 'lucide-react'
import type { ToastMessage, ToastType } from './useToast'

interface ToastProps {
  toasts: ToastMessage[]
  onRemove: (id: string) => void
}

export default function Toast({ toasts, onRemove }: ToastProps) {
  const getIcon = (type: ToastType) => {
    switch (type) {
      case 'success':
        return <CheckCircle className="h-5 w-5" />
      case 'error':
        return <AlertCircle className="h-5 w-5" />
      case 'warning':
        return <AlertTriangle className="h-5 w-5" />
      case 'info':
      default:
        return <Info className="h-5 w-5" />
    }
  }

  const getColors = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'bg-emerald-50 text-emerald-900 border-emerald-200'
      case 'error':
        return 'bg-red-50 text-red-900 border-red-200'
      case 'warning':
        return 'bg-amber-50 text-amber-900 border-amber-200'
      case 'info':
      default:
        return 'bg-blue-50 text-blue-900 border-blue-200'
    }
  }

  const getIconColor = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'text-emerald-600'
      case 'error':
        return 'text-red-600'
      case 'warning':
        return 'text-amber-600'
      case 'info':
      default:
        return 'text-blue-600'
    }
  }

  return (
    <div
      className="fixed bottom-4 right-4 z-[60] flex flex-col gap-3"
      role="status"
      aria-live="polite"
      aria-label="通知消息"
    >
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`flex items-start gap-3 px-4 py-3 rounded-xl border shadow-lg transition-all duration-300 animate-in slide-in-from-bottom-2 fade-in ${
            getColors(toast.type)
          }`}
          style={{ minWidth: '300px' }}
          role="alert"
        >
          <div className={`mt-0.5 flex-shrink-0 ${getIconColor(toast.type)}`}>
            {getIcon(toast.type)}
          </div>
          <div className="flex-1 text-sm font-medium leading-relaxed">{toast.message}</div>
          <button
            onClick={() => onRemove(toast.id)}
            className={`mt-0.5 flex-shrink-0 ${getIconColor(toast.type)} hover:opacity-70 transition-opacity`}
            aria-label={`关闭通知: ${toast.message}`}
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ))}
    </div>
  )
}
