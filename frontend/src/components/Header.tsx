import { Music, Menu, X, Heart, User, Shield, Stamp } from 'lucide-react'
import { useState, useEffect, useCallback, useRef } from 'react'
import { Link, useLocation } from 'react-router-dom'

export default function Header() {
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const location = useLocation()
  const scrollYRef = useRef(0)

  // 使用 useCallback 和 useRef 优化滚动处理
  const handleScroll = useCallback(() => {
    scrollYRef.current = window.scrollY || window.pageYOffset
    setIsScrolled(scrollYRef.current > 10)
  }, [])

  useEffect(() => {
    // 添加滚动事件监听器
    window.addEventListener('scroll', handleScroll, { passive: true })

    // 初始化检查
    handleScroll()

    return () => {
      window.removeEventListener('scroll', handleScroll)
    }
  }, [handleScroll])

  // 点击链接后关闭移动端菜单
  const handleNavClick = useCallback(() => {
    setIsMobileMenuOpen(false)
  }, [])

  // 路由变化时关闭移动端菜单
  useEffect(() => {
    setIsMobileMenuOpen(false)
  }, [location.pathname])

  const isActive = useCallback((path: string) => location.pathname === path, [location.pathname])

  const navItems = [
    { name: '创作', path: '/', icon: Music },
    { name: '历史', path: '/history', icon: Music },
    { name: '合规检测', path: '/compliance', icon: Shield },
    { name: '版权存证', path: '/copyright', icon: Stamp },
    { name: '关于', path: '/about', icon: Music },
  ]

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled
          ? 'bg-white/80 backdrop-blur-md shadow-lg shadow-purple-500/10 py-2.5'
          : 'bg-white/90 backdrop-blur-sm py-4'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-3 group focus-visible:outline-2 focus-visible:outline-purple-500 focus-visible:outline-offset-2 rounded-lg">
            <div className="relative flex items-center justify-center h-10 w-10 sm:h-12 sm:w-12 rounded-xl sm:rounded-2xl bg-gradient-to-br from-purple-600 via-indigo-600 to-pink-500 shadow-lg shadow-purple-500/30 transition-all duration-300 group-hover:scale-105 group-hover:shadow-xl group-hover:shadow-purple-500/40">
              <Music className="h-5 w-5 sm:h-6 sm:w-6 text-white transition-transform duration-300 group-hover:rotate-12" />
              <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-purple-500 to-pink-500 opacity-20 blur-lg group-hover:opacity-30 transition-opacity duration-300" />
            </div>
            <div className="hidden sm:block">
              <h1 className="font-heading text-xl sm:text-2xl font-bold bg-gradient-to-r from-purple-700 via-indigo-700 to-pink-600 bg-clip-text text-transparent tracking-tight">
                Composer AI
              </h1>
              <p className="text-[10px] sm:text-xs text-muted-foreground font-medium tracking-wide">AI Music Creation</p>
            </div>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-1" role="navigation" aria-label="主导航">
            {navItems.map((item) => (
              <Link
                key={item.name}
                to={item.path}
                className={`relative px-4 py-2.5 sm:py-3 rounded-lg text-sm font-medium transition-all duration-200 hover:text-primary hover:bg-purple-50 hover:shadow-sm focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2 ${
                  isActive(item.path) ? 'text-primary bg-purple-50' : 'text-muted-foreground'
                }`}
                aria-current={isActive(item.path) ? 'page' : undefined}
              >
                <span className="flex items-center gap-1.5">
                  {item.name === '合规检测' && <Shield className="h-4 w-4" />}
                  {item.name === '版权存证' && <Stamp className="h-4 w-4" />}
                  {item.name}
                </span>
                {isActive(item.path) && (
                  <span className="absolute bottom-2 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-primary" />
                )}
              </Link>
            ))}
          </nav>

          {/* Desktop Actions */}
          <div className="hidden md:flex items-center gap-2">
            <button
              type="button"
              className="p-2.5 text-muted-foreground hover:text-primary hover:bg-purple-50 rounded-lg transition-all duration-200 focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2"
              aria-label="收藏"
            >
              <Heart className="h-5 w-5" />
            </button>
            <button
              type="button"
              className="p-2.5 text-muted-foreground hover:text-primary hover:bg-purple-50 rounded-lg transition-all duration-200 focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2"
              aria-label="个人中心"
            >
              <User className="h-5 w-5" />
            </button>
            <button
              type="button"
              className="px-4 py-2.5 rounded-lg text-sm font-semibold text-white bg-purple-600 hover:bg-purple-700 shadow-lg shadow-purple-500/20 transition-all duration-200 hover:shadow-xl hover:shadow-purple-500/30 hover:-translate-y-0.5 focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2"
            >
              登录
            </button>
          </div>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="md:hidden p-2.5 text-muted-foreground hover:text-primary hover:bg-purple-50 rounded-lg transition-all duration-200 focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2"
            aria-label="打开菜单"
            aria-expanded={isMobileMenuOpen}
          >
            {isMobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      <div
        className={`md:hidden overflow-hidden transition-all duration-300 ease-in-out ${
          isMobileMenuOpen ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'
        }`}
      >
        <div className="bg-white border-b border-border shadow-lg px-4 py-4 space-y-1 animate-in slide-in-from-top-2">
          {navItems.map((item) => (
            <Link
              key={item.name}
              to={item.path}
              onClick={handleNavClick}
              className={`block px-4 py-3.5 rounded-xl text-base font-medium transition-all duration-200 active:scale-98 transform ${
                isActive(item.path) ? 'text-primary bg-purple-50' : 'text-muted-foreground hover:text-primary hover:bg-purple-50'
              }`}
              aria-current={isActive(item.path) ? 'page' : undefined}
            >
              {item.name}
            </Link>
          ))}
          <div className="pt-4 flex flex-col gap-2">
            <button className="w-full px-5 py-3 rounded-xl text-sm font-semibold text-white bg-purple-600 hover:bg-purple-700 shadow-lg shadow-purple-500/20 transition-all duration-200 active:scale-98">
              登录
            </button>
            <button className="w-full px-5 py-3 rounded-xl text-sm font-semibold text-purple-600 bg-purple-50 hover:bg-purple-100 transition-all duration-200 active:scale-98">
              免费试用
            </button>
          </div>
        </div>
      </div>
    </header>
  )
}
