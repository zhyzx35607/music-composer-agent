import { Outlet } from 'react-router-dom'
import Header from './components/Header'
import Footer from './components/Footer'
import Toast from './components/Toast'
import { useToast } from './components/useToast'

function App() {
  const { toasts, removeToast } = useToast()

  return (
    <div className="min-h-screen bg-background text-foreground font-body antialiased">
      <Header />

      <main className="pt-16 pb-8">
        <Outlet />
      </main>

      <Footer />

      <Toast toasts={toasts} onRemove={removeToast} />
    </div>
  )
}

export default App
