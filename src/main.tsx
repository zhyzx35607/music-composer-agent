import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider, createBrowserRouter } from 'react-router-dom'
import { ErrorBoundary } from './components/ErrorBoundary'
import { APIProvider } from './lib/apiContext'
import App from './App.tsx'
import CreatePage from './pages/CreatePage.tsx'
import { HistoryPage } from './pages/HistoryRouter.tsx'
import AboutPage from './pages/AboutPage.tsx'
import CompliancePage from './pages/CompliancePage.tsx'
import CopyrightPage from './pages/CopyrightPage.tsx'
import NotFoundPage from './pages/NotFoundPage.tsx'
import './index.css'

// 预连接 Google Fonts 以提升性能
// 这替换了 CSS 中的 @import，避免渲染阻塞
if (import.meta.env.DEV || typeof window !== 'undefined') {
  const link = document.createElement('link')
  link.rel = 'preconnect'
  link.href = 'https://fonts.googleapis.com'
  link.crossOrigin = 'anonymous'
  document.head.appendChild(link)

  const link2 = document.createElement('link')
  link2.rel = 'preconnect'
  link2.href = 'https://fonts.gstatic.com'
  link2.crossOrigin = 'anonymous'
  document.head.appendChild(link2)

  // 加载字体（使用 display=swap 防止 FOIT）
  const fontLink = document.createElement('link')
  fontLink.rel = 'stylesheet'
  fontLink.href = 'https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&family=Righteous&display=swap'
  document.head.appendChild(fontLink)
}

// 路由配置
const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: (
      <ErrorBoundary>
        <NotFoundPage />
      </ErrorBoundary>
    ),
    children: [
      { path: '', element: <CreatePage /> },
      { path: 'history', element: <HistoryPage /> },
      { path: 'about', element: <AboutPage /> },
      { path: 'compliance', element: <CompliancePage /> },
      { path: 'copyright', element: <CopyrightPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <APIProvider>
      <ErrorBoundary>
        <RouterProvider router={router} />
      </ErrorBoundary>
    </APIProvider>
  </StrictMode>,
)
