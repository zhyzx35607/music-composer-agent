import { Music, Info, Clock, Share2, Archive, Wand2, Heart } from 'lucide-react'

export default function AboutPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="text-center mb-12">
        <div className="inline-flex items-center justify-center h-16 w-16 rounded-2xl bg-gradient-to-br from-indigo-600 to-pink-500 mb-4 shadow-lg shadow-indigo-500/20">
          <Music className="h-8 w-8 text-white" />
        </div>
        <h1 className="text-3xl font-bold text-foreground mb-2">关于 Composer AI</h1>
        <p className="text-muted-foreground">AI 驱动的音乐创作平台</p>
      </div>

      <div className="space-y-8">
        {/* 关于卡片 */}
        <div className="bg-card rounded-2xl p-6 border border-border shadow-sm">
          <div className="flex items-center gap-3 mb-4">
            <Info className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-semibold text-foreground">平台介绍</h2>
          </div>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              Composer AI 是一个基于人工智能技术的音乐创作平台，致力于让每个人都能轻松创作专业级的音乐作品。
              无论你是专业的音乐人还是刚入门的爱好者，都可以通过简单的文字描述生成属于自己的音乐。
            </p>
            <p>
              我们使用先进的深度学习模型，将你的创意转化为完整的音乐作品，包括作曲、编曲和音频生成的全流程。
            </p>
          </div>
        </div>

        {/* 功能特性 */}
        <div className="bg-card rounded-2xl p-6 border border-border shadow-sm">
          <div className="flex items-center gap-3 mb-6">
            <Wand2 className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-semibold text-foreground">核心功能</h2>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-muted/50 border border-border/50">
              <div className="h-10 w-10 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center mb-3">
                <Wand2 className="h-5 w-5" />
              </div>
              <h3 className="font-medium text-foreground mb-1">AI 智能生成</h3>
              <p className="text-sm text-muted-foreground">根据你的描述自动生成高质量音乐作品</p>
            </div>
            <div className="p-4 rounded-xl bg-muted/50 border border-border/50">
              <div className="h-10 w-10 rounded-lg bg-pink-100 text-pink-600 flex items-center justify-center mb-3">
                <Music className="h-5 w-5" />
              </div>
              <h3 className="font-medium text-foreground mb-1">多格式导出</h3>
              <p className="text-sm text-muted-foreground">支持 MIDI 和 WAV 格式，满足专业制作需求</p>
            </div>
            <div className="p-4 rounded-xl bg-muted/50 border border-border/50">
              <div className="h-10 w-10 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center mb-3">
                <Archive className="h-5 w-5" />
              </div>
              <h3 className="font-medium text-foreground mb-1">版本管理</h3>
              <p className="text-sm text-muted-foreground">自动保存每个版本，方便追溯创作过程</p>
            </div>
            <div className="p-4 rounded-xl bg-muted/50 border border-border/50">
              <div className="h-10 w-10 rounded-lg bg-emerald-100 text-emerald-600 flex items-center justify-center mb-3">
                <Share2 className="h-5 w-5" />
              </div>
              <h3 className="font-medium text-foreground mb-1">反馈修改</h3>
              <p className="text-sm text-muted-foreground">根据反馈迭代修改，合作创作</p>
            </div>
          </div>
        </div>

        {/* 使用说明 */}
        <div className="bg-card rounded-2xl p-6 border border-border shadow-sm">
          <div className="flex items-center gap-3 mb-6">
            <Clock className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-semibold text-foreground">如何使用</h2>
          </div>
          <ol className="space-y-4 pl-6">
            <li className="space-y-2">
              <p className="font-medium text-foreground">1. 输入创意</p>
              <p className="text-sm text-muted-foreground">用中文描述你想要的音乐，例如"一首欢快的流行歌曲，适合夏日海滩"</p>
            </li>
            <li className="space-y-2">
              <p className="font-medium text-foreground">2. 选择参数</p>
              <p className="text-sm text-muted-foreground">选择风格、情绪、速度和乐器等音乐参数</p>
            </li>
            <li className="space-y-2">
              <p className="font-medium text-foreground">3. 生成音乐</p>
              <p className="text-sm text-muted-foreground">点击生成按钮，等待 AI 创建你的音乐作品</p>
            </li>
            <li className="space-y-2">
              <p className="font-medium text-foreground">4. 播放下载</p>
              <p className="text-sm text-muted-foreground">播放 preview，下载 MIDI 或 WAV 文件</p>
            </li>
          </ol>
        </div>

        {/* 技术栈 */}
        <div className="bg-card rounded-2xl p-6 border border-border shadow-sm">
          <div className="flex items-center gap-3 mb-6">
            <div className="h-5 w-5 text-primary">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </div>
            <h2 className="text-xl font-semibold text-foreground">技术栈</h2>
          </div>
          <div className="flex flex-wrap gap-2">
            {['React', 'TypeScript', 'Tailwind CSS', 'Vite', 'Axios', 'Lucide Icons'].map(tag => (
              <span key={tag} className="px-3 py-1.5 bg-muted text-muted-foreground rounded-lg text-sm">
                {tag}
              </span>
            ))}
          </div>
        </div>

        {/* 联系信息 */}
        <div className="bg-gradient-to-br from-indigo-600 to-pink-600 rounded-2xl p-6 text-white shadow-lg">
          <div className="flex items-center gap-3 mb-4">
            <Heart className="h-5 w-5 text-white" />
            <h2 className="text-xl font-semibold">支持我们</h2>
          </div>
          <p className="mb-6 text-indigo-100">
            如果你喜欢这个项目，欢迎分享给朋友或提出宝贵建议！
          </p>
          <div className="flex gap-3">
            <button className="flex-1 py-2.5 bg-white/10 hover:bg-white/20 rounded-xl font-medium transition-colors">
              提交建议
            </button>
            <button className="flex-1 py-2.5 bg-white/10 hover:bg-white/20 rounded-xl font-medium transition-colors">
              捐助支持
            </button>
          </div>
        </div>

        <div className="text-center pt-8">
          <button onClick={() => window.location.href = '/'} className="px-6 py-2 text-muted-foreground hover:text-foreground transition-colors">
            返回首页
          </button>
        </div>
      </div>
    </div>
  )
}
