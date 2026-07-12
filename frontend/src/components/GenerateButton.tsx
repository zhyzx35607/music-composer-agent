import { Music2 } from 'lucide-react'

interface GenerateButtonProps {
  onClick: () => void
  disabled: boolean
  isLoading?: boolean
}

export default function GenerateButton({ onClick, disabled, isLoading }: GenerateButtonProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled || isLoading}
      className="group relative w-full max-w-md mx-auto px-8 py-4 bg-gradient-to-r from-primary via-secondary to-primary rounded-2xl font-heading text-2xl font-bold text-on-primary transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] disabled:scale-100 disabled:opacity-60 disabled:cursor-not-allowed shadow-2xl shadow-primary/30 hover:shadow-primary/40 overflow-hidden"
    >
      {/* Gradient animation background */}
      <div className="absolute inset-0 bg-gradient-to-r from-primary via-secondary to-primary opacity-0 group-hover:opacity-20 transition-opacity duration-300" />

      <span className="relative z-10 flex items-center justify-center gap-3">
        {isLoading ? (
          <>
            <svg className="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            <span>正在生成...</span>
          </>
        ) : (
          <>
            <Music2 className="h-7 w-7 group-hover:rotate-12 transition-transform duration-300" />
            <span>生成音乐</span>
          </>
        )}
      </span>
    </button>
  )
}
