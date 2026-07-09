import { Music, Menu, X, Heart, User } from 'lucide-react'
import { useState, useEffect } from 'react'

interface HeaderProps {
  onMenuClick?: () => void
}

export default function Header({ onMenuClick }: HeaderProps) {
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)

  // 监听滚动事件
  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  // 点击链接后关闭移动端菜单
  const handleNavClick = () => {
    setIsMobileMenuOpen(false)
  }

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled ? 'bg-white/90 backdrop-blur-md shadow-sm py-3' : 'bg-white py-5'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-3 group cursor-pointer">
            <div className="flex items-center justify-center h-12 w-12 rounded-2xl bg-gradient-to-br from-indigo-600 to-pink-500 shadow-lg shadow-indigo-500/20 transition-transform group-hover:scale-105">
              <Music className="h-6 w-6 text-white" />
            </div>
            <div>
              <h1 className="font-heading text-2xl font-bold bg-gradient-to-r from-indigo-600 to-pink-500 bg-clip-text text-transparent tracking-tight">
                Composer AI
              </h1>
              <p className="text-xs text-muted-foreground font-medium">AI Music Creation</p>
            </div>
          </div>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-sm font-medium text-muted-foreground hover:text-indigo-600 transition-colors">
              特性
            </a>
            <a href="#how-it-works" className="text-sm font-medium text-muted-foreground hover:text-indigo-600 transition-colors">
              如何使用
            </a>
            <a href="#pricing" className="text-sm font-medium text-muted-foreground hover:text-indigo-600 transition-colors">
              价格
            </a>
            <a href="#faq" className="text-sm font-medium text-muted-foreground hover:text-indigo-600 transition-colors">
              常见问题
            </a>
          </nav>

          {/* Actions */}
          <div className="hidden md:flex items-center gap-4">
            <button className="p-2 text-muted-foreground hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-all">
              <Heart className="h-5 w-5" />
            </button>
            <button className="p-2 text-muted-foreground hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-all">
              <User className="h-5 w-5" />
            </button>
            <button className="px-5 py-2.5 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-lg shadow-indigo-500/20 transition-all hover:shadow-indigo-500/30">
              登录
            </button>
            <button className="px-5 py-2.5 text-sm font-semibold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 rounded-xl transition-all">
              免费试用
            </button>
          </div>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="md:hidden p-2 text-muted-foreground hover:bg-gray-100 rounded-lg transition-colors"
          >
            {isMobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      {isMobileMenuOpen && (
        <div className="md:hidden absolute top-full left-0 right-0 bg-white border-b border-border shadow-lg animate-in slide-in-from-top-2 duration-200">
          <div className="px-4 py-4 space-y-2">
            <a href="#features" onClick={handleNavClick} className="block px-4 py-2 text-sm font-medium text-muted-foreground hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
              特性
            </a>
            <a href="#how-it-works" onClick={handleNavClick} className="block px-4 py-2 text-sm font-medium text-muted-foreground hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
              如何使用
            </a>
            <a href="#pricing" onClick={handleNavClick} className="block px-4 py-2 text-sm font-medium text-muted-foreground hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
              价格
            </a>
            <a href="#faq" onClick={handleNavClick} className="block px-4 py-2 text-sm font-medium text-muted-foreground hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
              常见问题
            </a>
            <div className="pt-4 flex flex-col gap-2">
              <button className="w-full px-5 py-2.5 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-lg shadow-indigo-500/20">
                登录
              </button>
              <button className="w-full px-5 py-2.5 text-sm font-semibold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 rounded-xl transition-all">
                免费试用
              </button>
            </div>
          </div>
        </div>
      )}
    </header>
  )
}
