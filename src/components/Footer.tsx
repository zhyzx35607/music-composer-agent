import { Music, Shield, Stamp } from 'lucide-react'
import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="bg-muted/30 border-t border-border py-12 mt-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
          {/* 品牌信息 */}
          <div className="space-y-4">
            <Link to="/" className="flex items-center gap-2 group">
              <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-purple-600 to-pink-500 flex items-center justify-center">
                <Music className="h-4 w-4 text-white" />
              </div>
              <span className="font-heading font-bold text-foreground">Composer AI</span>
            </Link>
            <p className="text-sm text-muted-foreground leading-relaxed">
              AI 驱动的音乐创作平台，让每个人都能轻松创作专业级的音乐作品。
            </p>
          </div>

          {/* 快速链接 */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold text-foreground">快速链接</h3>
            <div className="flex flex-col gap-2">
              <Link to="/" className="text-sm text-muted-foreground hover:text-primary transition-colors">
                音乐创作
              </Link>
              <Link to="/history" className="text-sm text-muted-foreground hover:text-primary transition-colors">
                历史版本
              </Link>
              <Link to="/about" className="text-sm text-muted-foreground hover:text-primary transition-colors">
                关于我们
              </Link>
            </div>
          </div>

          {/* 合规与版权 */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold text-foreground">版权保护</h3>
            <div className="flex flex-col gap-2">
              <Link to="/compliance" className="flex items-center gap-2 text-sm text-muted-foreground hover:text-primary transition-colors">
                <Shield className="h-4 w-4" />
                AI 音乐版权合规检测
              </Link>
              <Link to="/copyright" className="flex items-center gap-2 text-sm text-muted-foreground hover:text-primary transition-colors">
                <Stamp className="h-4 w-4" />
                区块链版权存证
              </Link>
              <p className="text-xs text-muted-foreground mt-2">
                遵循《区块链版权存证规范》标准
              </p>
            </div>
          </div>
        </div>

        {/* 底部版权声明 */}
        <div className="border-t border-border pt-8">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <p className="text-sm text-muted-foreground">
              © {new Date().getFullYear()} Composer AI. 保留所有权利。
            </p>
            <p className="text-xs text-muted-foreground">
              根据《区块链版权存证规范》，本平台记录 AI 音乐创作全流程，为版权归属提供证据链。
            </p>
          </div>
        </div>
      </div>
    </footer>
  )
}
