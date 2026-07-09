import { Music, Wand2, Headphones, Clock, Share2, Archive } from 'lucide-react'

interface Feature {
  icon: React.ElementType
  title: string
  description: string
  gradient: string
}

const features: Feature[] = [
  {
    icon: Wand2,
    title: 'AI 智能生成',
    description: '基于深度学习模型，根据你的描述自动生成高质量音乐作品',
    gradient: 'from-indigo-500 to-purple-600',
  },
  {
    icon: Headphones,
    title: '多格式导出',
    description: '支持 MIDI 和 WAV 格式导出，满足专业制作需求',
    gradient: 'from-pink-500 to-rose-600',
  },
  {
    icon: Clock,
    title: '快速响应',
    description: ' optimized 模型架构，快速生成音乐草稿',
    gradient: 'from-blue-500 to-cyan-600',
  },
  {
    icon: Share2,
    title: '版本管理',
    description: '自动保存每个版本，方便追溯和比较创作过程',
    gradient: 'from-emerald-500 to-teal-600',
  },
  {
    icon: Archive,
    title: '历史归档',
    description: '云端保存所有创作历史，随时随地访问你的作品',
    gradient: 'from-orange-500 to-amber-600',
  },
  {
    icon: Music,
    title: '风格多样',
    description: '支持多种音乐风格，从古典到现代，应有尽有',
    gradient: 'from-violet-500 to-fuchsia-600',
  },
]

export default function Features() {
  return (
    <section id="features" className="py-20 bg-background">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="font-heading text-3xl md:text-4xl font-bold text-foreground mb-4">
            为什么选择 Composer AI？
          </h2>
          <p className="text-lg text-muted-foreground">
            专为音乐创作者打造的 AI 辅助工具，让你的音乐创作灵感永不枯竭
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <div
              key={index}
              className="group bg-card rounded-2xl p-6 border border-border hover:border-primary/30 hover:shadow-xl hover:shadow-primary/10 transition-all duration-300"
            >
              <div className={`h-14 w-14 rounded-xl bg-gradient-to-br ${feature.gradient} flex items-center justify-center mb-6 shadow-lg shadow-${feature.gradient.split(' ')[1]}/20 group-hover:scale-110 transition-transform duration-300`}>
                <feature.icon className="h-7 w-7 text-on-primary" />
              </div>
              <h3 className="font-heading text-xl font-bold text-foreground mb-3">
                {feature.title}
              </h3>
              <p className="text-muted-foreground leading-relaxed">
                {feature.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
