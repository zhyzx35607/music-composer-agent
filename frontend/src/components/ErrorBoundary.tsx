import React from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'

interface ErrorBoundaryProps {
  children: React.ReactNode
  fallback?: React.ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false, error: null }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <div className="min-h-screen flex items-center justify-center bg-background p-4 animate-in fade-in duration-300">
          <div className="max-w-md w-full bg-card rounded-3xl p-8 text-center border border-border shadow-2xl shadow-red-500/20">
            <div className="h-16 w-16 mx-auto bg-red-100 rounded-full flex items-center justify-center mb-6">
              <AlertTriangle className="h-8 w-8 text-red-600" />
            </div>
            <h2 className="text-2xl font-bold text-foreground mb-3">发生错误</h2>
            <p className="text-muted-foreground mb-6">
              抱歉，页面出现了一些问题。请尝试重新加载页面。
            </p>
            {this.state.error && (
              <div className="mb-6 text-left">
                <p className="text-xs font-mono text-red-600 bg-red-50 p-3 rounded-lg mb-2">
                  {this.state.error.toString()}
                </p>
              </div>
            )}
            <button
              onClick={this.handleReset}
              className="w-full py-3.5 px-5 bg-gradient-to-r from-primary to-accent text-on-primary rounded-xl font-semibold shadow-lg shadow-primary/20 hover:shadow-primary/30 hover:scale-[1.02] transition-all flex items-center justify-center gap-2"
            >
              <RefreshCw className="h-5 w-5" />
              重新加载页面
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
