import { Music, ArrowRight } from 'lucide-react'

export default function Hero() {
  return (
    <section className="relative pt-32 pb-20 md:pt-40 md:pb-28 overflow-hidden">
      {/* Decorative background elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-[20%] -left-[10%] w-[70%] h-[70%] rounded-full bg-gradient-to-r from-indigo-500/20 to-purple-500/20 blur-3xl animate-pulse" />
        <div className="absolute top-[20%] -right-[10%] w-[60%] h-[60%] rounded-full bg-gradient-to-l from-pink-500/20 to-rose-500/20 blur-3xl animate-pulse delay-1000" />
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Text Content */}
          <div className="space-y-8 text-center lg:text-left">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-indigo-50 border border-indigo-100 text-indigo-700 text-sm font-medium">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75" />
                <span className="relative inline-flex rounded-full h-2 w-2 bg-indigo-500" />
              </span>
              AI 音乐创作新纪元
            </div>

            <h1 className="font-heading text-5xl md:text-6xl lg:text-7xl font-bold text-foreground leading-[1.1]">
              将你的音乐灵感 <br />
              <span className="bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
                变为现实
              </span>
            </h1>

            <p className="text-lg md:text-xl text-muted-foreground max-w-2xl mx-auto lg:mx-0 leading-relaxed">
              只需描述你心中的旋律，我们的 AI 将为你生成专业的音乐作品。
              从古典到现代，从电影配乐到流行歌曲，一切皆有可能。
            </p>

            <div className="flex flex-col sm:flex-row items-center gap-4 justify-center lg:justify-start">
              <a
                href="#create"
                className="w-full sm:w-auto px-8 py-4 bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 text-white rounded-xl font-semibold shadow-xl shadow-indigo-500/25 hover:shadow-indigo-500/40 hover:scale-[1.02] transition-all duration-300 flex items-center justify-center gap-2"
              >
                开始创作
                <ArrowRight className="h-5 w-5" />
              </a>
              <a
                href="#features"
                className="w-full sm:w-auto px-8 py-4 bg-white text-foreground border border-border rounded-xl font-semibold hover:bg-muted transition-all duration-300 flex items-center justify-center gap-2"
              >
                了解特性
              </a>
            </div>

            {/* Stats */}
            <div className="pt-8 grid grid-cols-3 gap-8 border-t border-border/50">
              {[
                { label: '已生成音乐', value: '100K+' },
                { label: '活跃用户', value: '5K+' },
                { label: '平均生成时间', value: '< 30s' },
              ].map((stat, index) => (
                <div key={index}>
                  <div className="font-heading text-3xl font-bold text-foreground mb-1">
                    {stat.value}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    {stat.label}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Visual Element */}
          <div className="relative">
            <div className="relative z-10 bg-gradient-to-br from-card to-card/95 backdrop-blur-xl rounded-3xl p-8 border border-border/50 shadow-2xl shadow-indigo-500/10">
              {/* Mock Interface */}
              <div className="space-y-6">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
                    <Music className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <div className="font-semibold text-foreground">正在生成...</div>
                    <div className="text-sm text-muted-foreground">根据你的描述创作音乐</div>
                  </div>
                </div>

                <div className="bg-muted/50 rounded-xl p-4 space-y-3">
                  <div className="text-sm text-muted-foreground">提示词</div>
                  <div className="text-sm font-medium text-foreground">
                    "一首欢快的流行歌曲，适合夏日海滩，使用吉他和鼓"
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">风格</span>
                    <span className="text-foreground font-medium">Pop</span>
                  </div>
                  <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                    <div className="h-full w-[85%] bg-gradient-to-r from-indigo-500 to-pink-500 rounded-full" />
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">情绪</span>
                    <span className="text-foreground font-medium">快乐</span>
                  </div>
                  <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                    <div className="h-full w-[70%] bg-gradient-to-r from-blue-500 to-cyan-500 rounded-full" />
                  </div>
                </div>

                <div className="pt-4 flex gap-3">
                  <div className="flex-1 h-20 rounded-xl bg-muted/50 border border-border/50 flex items-center justify-center">
                    <div className="w-12 h-1 bg-muted-foreground/20 rounded-full" />
                  </div>
                  <div className="flex-1 h-20 rounded-xl bg-muted/50 border border-border/50 flex items-center justify-center">
                    <div className="w-12 h-1 bg-muted-foreground/20 rounded-full" />
                  </div>
                  <div className="flex-1 h-20 rounded-xl bg-muted/50 border border-border/50 flex items-center justify-center">
                    <div className="w-12 h-1 bg-muted-foreground/20 rounded-full" />
                  </div>
                </div>
              </div>

              {/* Floating Elements */}
              <div className="absolute -top-4 -right-4 h-20 w-20 bg-gradient-to-br from-pink-500 to-purple-600 rounded-2xl rotate-12 shadow-lg flex items-center justify-center animate-bounce delay-700">
                <svg className="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
