import { Music, Play, Pause, Download, ArrowLeft, Heart, Share2, History, Volume2, SkipBack, SkipForward } from 'lucide-react'
import { useState } from 'react'

export default function Header() {
  return (
    <header className="sticky top-0 z-50 border-b border-border/50 bg-background/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-secondary shadow-lg shadow-primary/20">
              <Music className="h-6 w-6 text-on-primary" />
            </div>
            <div>
              <h1 className="font-heading text-2xl font-bold text-foreground tracking-tight">
                Music Composer
              </h1>
              <p className="text-xs text-muted-foreground hidden sm:block">AI Music Creation Platform</p>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex items-center gap-2">
            <a href="#history" className="px-4 py-2 text-sm font-medium text-foreground hover:text-primary transition-colors">
              历史作品
            </a>
            <a href="#about" className="px-4 py-2 text-sm font-medium text-foreground hover:text-primary transition-colors">
              关于我们
            </a>
            <button className="px-4 py-2 text-sm font-medium text-primary hover:bg-primary/10 rounded-lg transition-colors">
              登录
            </button>
            <button className="px-4 py-2 text-sm font-medium text-on-primary bg-primary hover:bg-secondary rounded-lg transition-colors shadow-lg shadow-primary/20">
              立即创作
            </button>
          </nav>
        </div>
      </div>
    </header>
  )
}
